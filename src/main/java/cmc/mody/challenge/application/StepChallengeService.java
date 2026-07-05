package cmc.mody.challenge.application;

import cmc.mody.challenge.domain.Challenge;
import cmc.mody.challenge.domain.ChallengeType;
import cmc.mody.challenge.domain.GroupChallenge;
import cmc.mody.challenge.domain.GroupChallengeStatus;
import cmc.mody.challenge.domain.StepChallengeDetail;
import cmc.mody.challenge.infrastructure.repository.ChallengeRepository;
import cmc.mody.challenge.infrastructure.repository.GroupChallengeRepository;
import cmc.mody.challenge.infrastructure.repository.StepChallengeDetailRepository;
import cmc.mody.challenge.infrastructure.repository.StepRecordRepository;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.grouping.domain.GroupMember;
import cmc.mody.grouping.domain.GroupMemberStatus;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.GroupMemberRepository;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import cmc.mody.member.domain.Member;
import cmc.mody.member.infrastructure.repository.MemberRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StepChallengeService {
    private static final LocalDate OPEN_ENDED_DATE = LocalDate.of(9999, 12, 31);

    private final IdGenerator idGenerator;
    private final MemberRepository memberRepository;
    private final ModyGroupRepository modyGroupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ChallengeRepository challengeRepository;
    private final StepChallengeDetailRepository stepChallengeDetailRepository;
    private final GroupChallengeRepository groupChallengeRepository;
    private final StepRecordRepository stepRecordRepository;

    @Transactional(readOnly = true)
    public StepChallengeStatusResult getCurrentStepChallenge(Long memberId, Long groupId) {
        validateGroupMembership(memberId, groupId);
        GroupChallenge groupChallenge = getCurrentStepGroupChallenge(groupId);
        Challenge challenge = getStepChallenge(groupChallenge.getChallengeId());
        StepChallengeDetail detail = getStepChallengeDetail(challenge.getId());
        long currentStepCount = stepRecordRepository.sumStepCountByGroupChallengeId(groupChallenge.getId());
        return new StepChallengeStatusResult(
            groupChallenge.getId(),
            challenge.getTitle(),
            detail.getTargetStepCount(),
            Math.toIntExact(currentStepCount)
        );
    }

    @Transactional(readOnly = true)
    public StepChallengeOptionListResult getStepChallengeOptions(Long memberId, Long groupId) {
        validateGroupMembership(memberId, groupId);
        List<Challenge> challenges = challengeRepository.findByChallengeTypeAndDeletedAtIsNull(ChallengeType.STEP);
        if (challenges.isEmpty()) {
            return new StepChallengeOptionListResult(List.of());
        }
        Map<Long, Challenge> challengesById = challenges.stream()
            .collect(Collectors.toMap(Challenge::getId, Function.identity()));
        Long currentChallengeId = getCurrentStepGroupChallengeOrNull(groupId)
            .map(GroupChallenge::getChallengeId)
            .orElse(null);

        List<StepChallengeOptionResult> options = stepChallengeDetailRepository
            .findByChallengeIdInAndDeletedAtIsNull(challenges.stream()
                .map(Challenge::getId)
                .toList())
            .stream()
            .sorted(Comparator.comparingInt(StepChallengeDetail::getDisplayOrder))
            .map(detail -> toStepChallengeOption(
                detail,
                challengesById.get(detail.getChallengeId()),
                currentChallengeId
            ))
            .toList();
        return new StepChallengeOptionListResult(options);
    }

    @Transactional(readOnly = true)
    public WalkedRegionListResult getWalkedRegions(Long memberId, Long groupId) {
        validateGroupMembership(memberId, groupId);
        List<Long> stepChallengeIds = getStepChallengeIds();
        if (stepChallengeIds.isEmpty()) {
            return new WalkedRegionListResult(List.of());
        }

        Map<Long, StepChallengeDetail> detailsByChallengeId = stepChallengeDetailRepository
            .findByChallengeIdInAndDeletedAtIsNull(stepChallengeIds)
            .stream()
            .collect(Collectors.toMap(StepChallengeDetail::getChallengeId, Function.identity()));

        List<WalkedRegionResult> regions = groupChallengeRepository
            .findByGroupIdAndChallengeIdInAndGroupChallengeStatusInAndDeletedAtIsNullOrderByEndedAtAscIdAsc(
                groupId,
                stepChallengeIds,
                List.of(GroupChallengeStatus.COMPLETED)
            )
            .stream()
            .map(groupChallenge -> detailsByChallengeId.get(groupChallenge.getChallengeId()))
            .filter(detail -> detail != null)
            .map(detail -> new WalkedRegionResult(
                detail.getDestination(),
                "regions/" + detail.getDestination() + ".png"
            ))
            .toList();
        return new WalkedRegionListResult(regions);
    }

    @Transactional(readOnly = true)
    public StepRankingListResult getStepRankings(Long memberId, Long groupId) {
        validateGroupMembership(memberId, groupId);
        GroupChallenge groupChallenge = getCurrentStepGroupChallenge(groupId);
        Map<Long, Integer> stepCounts = stepRecordRepository.sumStepCountByMember(groupChallenge.getId())
            .stream()
            .collect(Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                row -> Math.toIntExact(((Number) row[1]).longValue())
            ));

        List<StepRankingResult> rankings = groupMemberRepository
            .findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(groupId, GroupMemberStatus.JOINED)
            .stream()
            .sorted(Comparator
                .comparingInt((GroupMember groupMember) -> stepCounts.getOrDefault(groupMember.getMemberId(), 0))
                .reversed()
                .thenComparing(GroupMember::getJoinedAt)
                .thenComparing(GroupMember::getMemberId))
            .map(groupMember -> new StepRankingResult(
                0,
                groupMember.getMemberId(),
                groupMember.getDisplayNickname(),
                groupMember.getDisplayProfileImageKey(),
                stepCounts.getOrDefault(groupMember.getMemberId(), 0)
            ))
            .toList();

        List<StepRankingResult> ranked = new ArrayList<>();
        for (int index = 0; index < rankings.size(); index++) {
            StepRankingResult result = rankings.get(index);
            ranked.add(new StepRankingResult(
                index + 1,
                result.memberId(),
                result.nickname(),
                result.profileImageUrl(),
                result.stepCount()
            ));
        }
        return new StepRankingListResult(ranked);
    }

    @Transactional
    public StepChallengeChangeResult changeStepChallenge(
        Long memberId,
        Long groupId,
        StepChallengeChangeCommand command
    ) {
        validateGroupMembership(memberId, groupId);
        Challenge challenge = getStepChallenge(command.challengeId());
        StepChallengeDetail detail = getStepChallengeDetail(challenge.getId());
        Optional<GroupChallenge> currentGroupChallenge = getCurrentStepGroupChallengeOrNull(groupId);
        if (currentGroupChallenge
            .map(GroupChallenge::getChallengeId)
            .filter(challenge.getId()::equals)
            .isPresent()) {
            GroupChallenge groupChallenge = currentGroupChallenge.get();
            long currentStepCount = stepRecordRepository.sumStepCountByGroupChallengeId(groupChallenge.getId());
            return new StepChallengeChangeResult(
                groupChallenge.getId(),
                challenge.getId(),
                challenge.getTitle(),
                detail.getTargetStepCount(),
                Math.toIntExact(currentStepCount)
            );
        }

        currentGroupChallenge.ifPresent(groupChallenge -> groupChallenge.reset(LocalDateTime.now()));

        GroupChallenge groupChallenge = groupChallengeRepository.save(new GroupChallenge(
            idGenerator.nextId(),
            groupId,
            challenge.getId(),
            LocalDate.now(),
            OPEN_ENDED_DATE
        ));

        return new StepChallengeChangeResult(
            groupChallenge.getId(),
            challenge.getId(),
            challenge.getTitle(),
            detail.getTargetStepCount(),
            0
        );
    }

    private void validateGroupMembership(Long memberId, Long groupId) {
        memberRepository.findById(memberId)
            .filter(Member::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
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

    private GroupChallenge getCurrentStepGroupChallenge(Long groupId) {
        return getCurrentStepGroupChallengeOrNull(groupId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.CHALLENGE_IN_PROGRESS_NOT_FOUND));
    }

    private Optional<GroupChallenge> getCurrentStepGroupChallengeOrNull(Long groupId) {
        List<Long> stepChallengeIds = getStepChallengeIds();
        if (stepChallengeIds.isEmpty()) {
            return Optional.empty();
        }
        return groupChallengeRepository.findByGroupIdAndChallengeIdInAndGroupChallengeStatusAndDeletedAtIsNull(
            groupId,
            stepChallengeIds,
            GroupChallengeStatus.IN_PROGRESS
        );
    }

    private List<Long> getStepChallengeIds() {
        return challengeRepository.findByChallengeTypeAndDeletedAtIsNull(ChallengeType.STEP)
            .stream()
            .map(Challenge::getId)
            .toList();
    }

    private Challenge getStepChallenge(Long challengeId) {
        return challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(challengeId, ChallengeType.STEP)
            .orElseThrow(() -> new GeneralException(ErrorStatus.CHALLENGE_NOT_FOUND));
    }

    private StepChallengeDetail getStepChallengeDetail(Long challengeId) {
        return stepChallengeDetailRepository.findByChallengeIdAndDeletedAtIsNull(challengeId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.CHALLENGE_NOT_FOUND));
    }

    private StepChallengeOptionResult toStepChallengeOption(
        StepChallengeDetail detail,
        Challenge challenge,
        Long currentChallengeId
    ) {
        if (challenge == null) {
            throw new GeneralException(ErrorStatus.CHALLENGE_NOT_FOUND);
        }
        return new StepChallengeOptionResult(
            challenge.getId(),
            challenge.getTitle(),
            detail.getDeparture(),
            detail.getDestination(),
            detail.getDistanceKm().doubleValue(),
            detail.getTargetStepCount(),
            challenge.getId().equals(currentChallengeId)
        );
    }

    public record StepChallengeStatusResult(
        Long groupChallengeId,
        String title,
        int targetStepCount,
        int currentStepCount
    ) {
    }

    public record WalkedRegionListResult(List<WalkedRegionResult> regions) {
    }

    public record WalkedRegionResult(String regionName, String regionImageUrl) {
    }

    public record StepChallengeOptionListResult(List<StepChallengeOptionResult> options) {
    }

    public record StepChallengeOptionResult(
        Long challengeId,
        String title,
        String departure,
        String destination,
        double distanceKm,
        int targetStepCount,
        boolean selected
    ) {
    }

    public record StepRankingListResult(List<StepRankingResult> rankings) {
    }

    public record StepRankingResult(
        int rank,
        Long memberId,
        String nickname,
        String profileImageUrl,
        int stepCount
    ) {
    }

    public record StepChallengeChangeCommand(Long challengeId) {
    }

    public record StepChallengeChangeResult(
        Long groupChallengeId,
        Long challengeId,
        String title,
        int targetStepCount,
        int currentStepCount
    ) {
    }
}
