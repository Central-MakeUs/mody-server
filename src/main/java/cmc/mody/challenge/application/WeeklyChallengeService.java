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
import cmc.mody.common.upload.ImageObjectStorage;
import cmc.mody.common.upload.ImageUrlResolver;
import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import cmc.mody.notification.application.NotificationRequestService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
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
    private final NotificationRequestService notificationRequestService;
    private final ImageUrlResolver imageUrlResolver;
    private final ImageObjectStorage imageObjectStorage;
    private final WeeklyChallengeShareImageGenerator shareImageGenerator;

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
        ModyGroup group = validateGroupMembership(memberId, groupId);
        GroupChallenge groupChallenge = getWeeklyGroupChallenge(groupId, groupChallengeId);
        if (groupChallenge.getGroupChallengeStatus() == GroupChallengeStatus.COMPLETED) {
            throw new GeneralException(ErrorStatus.CHALLENGE_ALREADY_COMPLETED);
        }
        LocalDate today = LocalDate.now();
        if (groupChallenge.getGroupChallengeStatus() != GroupChallengeStatus.IN_PROGRESS
            || today.isBefore(groupChallenge.getStartsOn())
            || today.isAfter(groupChallenge.getEndsOn())) {
            throw new GeneralException(ErrorStatus.CHALLENGE_VALIDATION_FAILED);
        }
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
            command.imageCropRegion() == null ? null : command.imageCropRegion().x(),
            command.imageCropRegion() == null ? null : command.imageCropRegion().y(),
            command.imageCropRegion() == null ? null : command.imageCropRegion().width(),
            command.imageCropRegion() == null ? null : command.imageCropRegion().height(),
            LocalDateTime.now()
        ));
        completeIfAllMembersProved(groupChallenge, group.getName());
        return new WeeklyChallengeProofCreateResult(
            proof.getId(),
            groupChallenge.getId(),
            imageUrlResolver.resolve(proof.getImageKey()),
            ImageCropRegionResult.from(proof)
        );
    }

    @Transactional(readOnly = true)
    public WeeklyChallengeShareResult shareWeeklyChallenge(Long memberId, Long groupId, Long groupChallengeId) {
        validateGroupMembership(memberId, groupId);
        GroupChallenge groupChallenge = getWeeklyGroupChallenge(groupId, groupChallengeId);
        Challenge challenge = getWeeklyChallenge(groupChallenge.getChallengeId());
        if (groupChallenge.getGroupChallengeStatus() != GroupChallengeStatus.COMPLETED) {
            throw new GeneralException(ErrorStatus.CHALLENGE_NOT_COMPLETED);
        }

        List<ChallengeProof> proofs = challengeProofRepository
            .findByGroupChallengeIdAndDeletedAtIsNullOrderByUploadedAtAscIdAsc(groupChallenge.getId());
        if (proofs.isEmpty()) {
            throw new GeneralException(ErrorStatus.CHALLENGE_PROOF_NOT_FOUND);
        }

        WeeklyChallengeShareImageGenerator.GridSize gridSize = shareImageGenerator.calculateGridSize(proofs.size());
        String shareImageKey = shareImageKey(groupId, groupChallengeId);
        if (!imageObjectStorage.exists(shareImageKey)) {
            Map<Long, GroupMember> membersById = getJoinedGroupMembersById(groupId);
            List<WeeklyChallengeShareImageGenerator.ShareImageSource> sources = proofs.stream()
                .map(proof -> toShareImageSource(proof, membersById.get(proof.getMemberId())))
                .toList();
            byte[] sharedImage = shareImageGenerator.generate(
                challenge.getTitle(),
                challenge.getDescription(),
                sources,
                gridSize
            );
            imageObjectStorage.write(shareImageKey, sharedImage, "image/jpeg");
        }
        return new WeeklyChallengeShareResult(
            imageObjectStorage.toUrl(shareImageKey),
            null,
            gridSize.rows(),
            gridSize.columns()
        );
    }

    private WeeklyChallengeShareImageGenerator.ShareImageSource toShareImageSource(
        ChallengeProof proof,
        GroupMember groupMember
    ) {
        if (groupMember == null) {
            throw new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND);
        }
        return new WeeklyChallengeShareImageGenerator.ShareImageSource(
            imageObjectStorage.read(proof.getImageKey()),
            toGeneratorCropRegion(proof),
            groupMember.getDisplayNickname(),
            readProfileImage(groupMember.getDisplayProfileImageKey())
        );
    }

    private WeeklyChallengeShareImageGenerator.ImageCropRegion toGeneratorCropRegion(ChallengeProof proof) {
        if (proof.getCropX() == null
            || proof.getCropY() == null
            || proof.getCropWidth() == null
            || proof.getCropHeight() == null) {
            return null;
        }
        return new WeeklyChallengeShareImageGenerator.ImageCropRegion(
            proof.getCropX(),
            proof.getCropY(),
            proof.getCropWidth(),
            proof.getCropHeight()
        );
    }

    private byte[] readProfileImage(String imageKey) {
        if (imageKey == null || imageKey.isBlank() || imageKey.startsWith("http://") || imageKey.startsWith("https://")) {
            return null;
        }
        try {
            if (!imageObjectStorage.exists(imageKey)) {
                return null;
            }
            return imageObjectStorage.read(imageKey);
        } catch (GeneralException e) {
            return null;
        }
    }

    private void completeIfAllMembersProved(GroupChallenge groupChallenge, String groupName) {
        long joinedMemberCount = groupMemberRepository.countByGroupIdAndGroupMemberStatusAndDeletedAtIsNull(
            groupChallenge.getGroupId(),
            GroupMemberStatus.JOINED
        );
        if (joinedMemberCount == 0) {
            return;
        }

        long proofCount = challengeProofRepository.countByGroupChallengeIdAndDeletedAtIsNull(groupChallenge.getId());
        if (proofCount < joinedMemberCount) {
            return;
        }

        groupChallenge.complete(LocalDateTime.now());
        notificationRequestService.requestWeeklyChallengeCompleted(
            groupChallenge.getGroupId(),
            groupName,
            groupChallenge.getId()
        );
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
        List<GroupMember> participants = randomizedParticipants(proofs, membersById);
        return new WeeklyChallengeSummaryResult(
            groupChallenge.getId(),
            challenge.getTitle(),
            groupChallenge.getDueDayOfWeek().name(),
            groupChallenge.getStartsOn(),
            groupChallenge.getEndsOn(),
            Math.toIntExact(ChronoUnit.DAYS.between(LocalDate.now(), groupChallenge.getEndsOn())),
            proofs.size(),
            representativeParticipantNickname(participants),
            representativeParticipants(participants)
        );
    }

    private List<GroupMember> randomizedParticipants(List<ChallengeProof> proofs, Map<Long, GroupMember> membersById) {
        List<GroupMember> participants = new ArrayList<>(proofs.stream()
            .map(proof -> membersById.get(proof.getMemberId()))
            .filter(member -> member != null)
            .toList());
        Collections.shuffle(participants);
        return participants;
    }

    private String representativeParticipantNickname(List<GroupMember> participants) {
        return participants.stream()
            .map(GroupMember::getDisplayNickname)
            .findFirst()
            .orElse(null);
    }

    private List<WeeklyChallengeParticipantResult> representativeParticipants(List<GroupMember> participants) {
        return participants.stream()
            .limit(3)
            .map(member -> new WeeklyChallengeParticipantResult(
                member.getMemberId(),
                member.getDisplayNickname(),
                imageUrlResolver.resolve(member.getDisplayProfileImageKey())
            ))
            .toList();
    }

    private WeeklyChallengeProofResult toWeeklyChallengeProof(ChallengeProof proof, GroupMember groupMember) {
        if (groupMember == null) {
            throw new GeneralException(ErrorStatus.GROUP_MEMBER_NOT_FOUND);
        }
        return new WeeklyChallengeProofResult(
            proof.getId(),
            imageUrlResolver.resolve(proof.getImageKey()),
            ImageCropRegionResult.from(proof),
            groupMember.getMemberId(),
            groupMember.getDisplayNickname(),
            imageUrlResolver.resolve(groupMember.getDisplayProfileImageKey())
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

    private ModyGroup validateGroupMembership(Long memberId, Long groupId) {
        validateMember(memberId);
        ModyGroup group = modyGroupRepository.findById(groupId)
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
        return group;
    }

    private String shareImageKey(Long groupId, Long groupChallengeId) {
        return "weekly-challenge-shares/" + groupId + "/" + groupChallengeId + ".jpg";
    }

    public record WeeklyChallengeListResult(List<WeeklyChallengeSummaryResult> challenges) {
    }

    public record WeeklyChallengeSummaryResult(
        Long groupChallengeId,
        String title,
        String deadlineDayOfWeek,
        LocalDate startsOn,
        LocalDate endsOn,
        int remainingDays,
        int participantCount,
        String randomParticipantNickname,
        List<WeeklyChallengeParticipantResult> participants
    ) {
    }

    public record WeeklyChallengeParticipantResult(Long memberId, String nickname, String profileImageUrl) {
    }

    public record WeeklyChallengeDetailResult(Long challengeId, String title, String description) {
    }

    public record WeeklyChallengeProofListResult(List<WeeklyChallengeProofResult> proofs) {
    }

    public record WeeklyChallengeProofResult(
        Long proofId,
        String imageUrl,
        ImageCropRegionResult imageCropRegion,
        Long memberId,
        String nickname,
        String profileImageUrl
    ) {
    }

    public record WeeklyChallengeProofCreateCommand(String imageKey, ImageCropRegionCommand imageCropRegion) {
    }

    public record WeeklyChallengeProofCreateResult(
        Long proofId,
        Long groupChallengeId,
        String imageUrl,
        ImageCropRegionResult imageCropRegion
    ) {
    }

    public record WeeklyChallengeShareResult(
        String imageUrl,
        ImageCropRegionResult imageCropRegion,
        int rows,
        int columns
    ) {
    }

    public record ImageCropRegionCommand(
        BigDecimal x,
        BigDecimal y,
        BigDecimal width,
        BigDecimal height
    ) {
    }

    public record ImageCropRegionResult(
        BigDecimal x,
        BigDecimal y,
        BigDecimal width,
        BigDecimal height
    ) {
        public static ImageCropRegionResult from(ChallengeProof proof) {
            if (proof.getCropX() == null
                || proof.getCropY() == null
                || proof.getCropWidth() == null
                || proof.getCropHeight() == null) {
                return null;
            }
            return new ImageCropRegionResult(
                proof.getCropX(),
                proof.getCropY(),
                proof.getCropWidth(),
                proof.getCropHeight()
            );
        }
    }
}
