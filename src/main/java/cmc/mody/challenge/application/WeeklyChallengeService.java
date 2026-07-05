package cmc.mody.challenge.application;

import cmc.mody.challenge.domain.Challenge;
import cmc.mody.challenge.domain.ChallengeProof;
import cmc.mody.challenge.domain.ChallengeType;
import cmc.mody.challenge.domain.GroupChallenge;
import cmc.mody.challenge.domain.GroupChallengeStatus;
import cmc.mody.challenge.infrastructure.repository.ChallengeProofRepository;
import cmc.mody.challenge.infrastructure.repository.ChallengeRepository;
import cmc.mody.challenge.infrastructure.repository.GroupChallengeRepository;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.common.upload.UploadProperties;
import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WeeklyChallengeService {
    private final IdGenerator idGenerator;
    private final MemberRepository memberRepository;
    private final ModyGroupRepository modyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ChallengeRepository challengeRepository;
    private final GroupChallengeRepository groupChallengeRepository;
    private final ChallengeProofRepository challengeProofRepository;
    private final UploadProperties uploadProperties;

    @Transactional(readOnly = true)
    public WeeklyChallengeListResult getWeeklyChallenges(Long memberId, Long groupId) {
        validateGroupMembership(memberId, groupId);
        List<Challenge> weeklyChallenges = getWeeklyChallenges();
        List<GroupChallenge> groupChallenges = getCurrentWeeklyGroupChallenges(groupId, weeklyChallenges);
        if (groupChallenges.isEmpty()) {
            return new WeeklyChallengeListResult(List.of());
        }

        Map<Long, Challenge> challengesById = weeklyChallenges.stream()
            .collect(Collectors.toMap(Challenge::getId, Function.identity()));
        Map<Long, List<ChallengeProof>> proofsByGroupChallengeId = challengeProofRepository
            .findByGroupChallengeIdInAndDeletedAtIsNullOrderByUploadedAtAscIdAsc(groupChallengeIds(groupChallenges))
            .stream()
            .collect(Collectors.groupingBy(ChallengeProof::getGroupChallengeId));
        Map<Long, GroupMember> membersById = getJoinedGroupMembersById(groupId);

        return new WeeklyChallengeListResult(groupChallenges.stream()
            .map(groupChallenge -> toWeeklyChallengeSummary(
                groupChallenge,
                challengesById.get(groupChallenge.getChallengeId()),
                proofsByGroupChallengeId.getOrDefault(groupChallenge.getId(), List.of()),
                membersById
            ))
            .toList());
    }

    @Transactional(readOnly = true)
    public WeeklyChallengeDetailResult getWeeklyChallengeDetail(Long memberId, Long challengeId) {
        validateMember(memberId);
        Challenge challenge = getWeeklyChallenge(challengeId);
        return new WeeklyChallengeDetailResult(challenge.getId(), challenge.getTitle(), challenge.getDescription());
    }

    @Transactional(readOnly = true)
    public WeeklyChallengeProofListResult getWeeklyChallengeProofs(
        Long memberId,
        Long groupId,
        Long groupChallengeId
    ) {
        validateGroupMembership(memberId, groupId);
        GroupChallenge groupChallenge = getWeeklyGroupChallenge(groupId, groupChallengeId);
        Map<Long, GroupMember> membersById = getJoinedGroupMembersById(groupId);
        List<WeeklyChallengeProofResult> proofs = challengeProofRepository
            .findByGroupChallengeIdAndDeletedAtIsNullOrderByUploadedAtAscIdAsc(groupChallenge.getId())
            .stream()
            .map(proof -> toWeeklyChallengeProof(proof, membersById.get(proof.getMemberId())))
            .toList();
        return new WeeklyChallengeProofListResult(proofs);
    }

    @Transactional
    public WeeklyChallengeProofCreateResult createWeeklyChallengeProof(
        Long memberId,
        Long groupId,
        Long groupChallengeId,
        WeeklyChallengeProofCreateCommand command
    ) {
        validateGroupMembership(memberId, groupId);
        GroupChallenge groupChallenge = getWeeklyGroupChallenge(groupId, groupChallengeId);
        boolean alreadyProved = challengeProofRepository.existsByGroupChallengeIdAndMemberIdAndDeletedAtIsNull(
            groupChallenge.getId(),
            memberId
        );
        if (alreadyProved) {
            throw new GeneralException(ErrorStatus.CHALLENGE_PROOF_ALREADY_EXISTS);
        }

        ChallengeProof proof = challengeProofRepository.save(new ChallengeProof(
            idGenerator.nextId(),
            groupChallenge.getId(),
            memberId,
            command.imageKey(),
            LocalDateTime.now()
        ));
        return new WeeklyChallengeProofCreateResult(proof.getId(), groupChallenge.getId(), toImageUrl(proof.getImageKey()));
    }

    private List<GroupChallenge> getCurrentWeeklyGroupChallenges(Long groupId, List<Challenge> weeklyChallenges) {
        List<Long> weeklyChallengeIds = challengeIds(weeklyChallenges);
        if (weeklyChallengeIds.isEmpty()) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        return groupChallengeRepository
            .findByGroupIdAndChallengeIdInAndGroupChallengeStatusAndStartsOnLessThanEqualAndEndsOnGreaterThanEqualAndDeletedAtIsNullOrderByEndsOnAscIdAsc(
                groupId,
                weeklyChallengeIds,
                GroupChallengeStatus.IN_PROGRESS,
                today,
                today
            );
    }

    private List<Challenge> getWeeklyChallenges() {
        return challengeRepository.findByChallengeTypeAndDeletedAtIsNull(ChallengeType.PHOTO)
            .stream()
            .toList();
    }

    private List<Long> challengeIds(List<Challenge> challenges) {
        return challenges.stream()
            .map(Challenge::getId)
            .toList();
    }

    private WeeklyChallengeSummaryResult toWeeklyChallengeSummary(
        GroupChallenge groupChallenge,
        Challenge challenge,
        List<ChallengeProof> proofs,
        Map<Long, GroupMember> membersById
    ) {
        if (challenge == null) {
            throw new GeneralException(ErrorStatus.CHALLENGE_NOT_FOUND);
        }
        return new WeeklyChallengeSummaryResult(
            groupChallenge.getId(),
            challenge.getTitle(),
            groupChallenge.getDueDayOfWeek().name(),
            proofs.size(),
            representativeParticipantNickname(proofs, membersById)
        );
    }

    private String representativeParticipantNickname(List<ChallengeProof> proofs, Map<Long, GroupMember> membersById) {
        return proofs.stream()
            .sorted(Comparator.comparing(ChallengeProof::getUploadedAt).thenComparing(ChallengeProof::getId))
            .map(proof -> membersById.get(proof.getMemberId()))
            .filter(member -> member != null)
            .map(GroupMember::getDisplayNickname)
            .findFirst()
            .orElse(null);
    }

    private WeeklyChallengeProofResult toWeeklyChallengeProof(ChallengeProof proof, GroupMember groupMember) {
        if (groupMember == null) {
            throw new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND);
        }
        return new WeeklyChallengeProofResult(
            proof.getId(),
            toImageUrl(proof.getImageKey()),
            groupMember.getMemberId(),
            groupMember.getDisplayNickname(),
            toImageUrl(groupMember.getDisplayProfileImageKey())
        );
    }

    private GroupChallenge getWeeklyGroupChallenge(Long groupId, Long groupChallengeId) {
        GroupChallenge groupChallenge = groupChallengeRepository.findByIdAndGroupIdAndDeletedAtIsNull(
            groupChallengeId,
            groupId
        ).orElseThrow(() -> new GeneralException(ErrorStatus.CHALLENGE_NOT_FOUND));
        getWeeklyChallenge(groupChallenge.getChallengeId());
        return groupChallenge;
    }

    private Challenge getWeeklyChallenge(Long challengeId) {
        return challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(challengeId, ChallengeType.PHOTO)
            .orElseThrow(() -> new GeneralException(ErrorStatus.CHALLENGE_NOT_FOUND));
    }

    private Map<Long, GroupMember> getJoinedGroupMembersById(Long groupId) {
        return groupMemberRepository
            .findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(groupId, GroupMemberStatus.JOINED)
            .stream()
            .collect(Collectors.toMap(GroupMember::getMemberId, Function.identity()));
    }

    private List<Long> groupChallengeIds(List<GroupChallenge> groupChallenges) {
        return groupChallenges.stream()
            .map(GroupChallenge::getId)
            .toList();
    }

    private void validateMember(Long memberId) {
        memberRepository.findById(memberId)
            .filter(Member::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    private void validateGroupMembership(Long memberId, Long groupId) {
        validateMember(memberId);
        modyGroupRepository.findById(groupId)
            .filter(ModyGroup::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));
        boolean joined = groupMemberRepository.existsByMemberIdAndGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            memberId,
            groupId,
            GroupMemberStatus.JOINED
        );
        if (!joined) {
            throw new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND);
        }
    }

    private String toImageUrl(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) {
            return null;
        }
        String baseUrl = uploadProperties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl + imageKey;
        }
        return baseUrl + "/" + imageKey;
    }

    public record WeeklyChallengeListResult(List<WeeklyChallengeSummaryResult> challenges) {
    }

    public record WeeklyChallengeSummaryResult(
        Long groupChallengeId,
        String title,
        String deadlineDayOfWeek,
        int participantCount,
        String randomParticipantNickname
    ) {
    }

    public record WeeklyChallengeDetailResult(Long challengeId, String title, String description) {
    }

    public record WeeklyChallengeProofListResult(List<WeeklyChallengeProofResult> proofs) {
    }

    public record WeeklyChallengeProofResult(
        Long proofId,
        String imageUrl,
        Long memberId,
        String nickname,
        String profileImageUrl
    ) {
    }

    public record WeeklyChallengeProofCreateCommand(String imageKey) {
    }

    public record WeeklyChallengeProofCreateResult(Long proofId, Long groupChallengeId, String imageUrl) {
    }
}
