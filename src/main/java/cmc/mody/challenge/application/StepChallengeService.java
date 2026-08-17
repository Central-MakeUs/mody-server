package cmc.mody.challenge.application;

import cmc.mody.challenge.domain.Challenge;
import cmc.mody.challenge.domain.ChallengeType;
import cmc.mody.challenge.domain.GroupChallenge;
import cmc.mody.challenge.domain.GroupChallengeStatus;
import cmc.mody.challenge.domain.StepChallengeDetail;
import cmc.mody.challenge.domain.StepRecord;
import cmc.mody.challenge.domain.StepSource;
import cmc.mody.challenge.infrastructure.repository.ChallengeRepository;
import cmc.mody.challenge.infrastructure.repository.GroupChallengeRepository;
import cmc.mody.challenge.infrastructure.repository.StepChallengeDetailRepository;
import cmc.mody.challenge.infrastructure.repository.StepRecordRepository;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.common.upload.ImageUrlResolver;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
    private final ImageUrlResolver imageUrlResolver;

    @Transactional(readOnly = true)
    public StepChallengeStatusResult getCurrentStepChallenge(Long memberId, Long groupId) {
        validateGroupMembership(memberId, groupId);
        GroupChallenge groupChallenge = getDisplayedStepGroupChallenge(groupId);
        Challenge challenge = getStepChallenge(groupChallenge.getChallengeId());
        StepChallengeDetail detail = getStepChallengeDetail(challenge.getId());
        return toStatusResult(groupChallenge, challenge, detail, currentStepCount(groupChallenge));
    }

    @Transactional
    public void initializeDefaultStepChallenge(Long groupId) {
        List<Challenge> challenges = getStepChallenges();
        if (challenges.isEmpty() || getCurrentStepGroupChallengeOrNull(groupId, challengeIds(challenges)).isPresent()) {
            return;
        }
        Map<Long, Challenge> challengesById = challenges.stream()
            .collect(Collectors.toMap(Challenge::getId, Function.identity()));
        stepChallengeDetailRepository.findByChallengeIdInAndDeletedAtIsNull(challengeIds(challenges))
            .stream()
            .min(Comparator.comparingInt(StepChallengeDetail::getDisplayOrder))
            .map(detail -> challengesById.get(detail.getChallengeId()))
            .filter(java.util.Objects::nonNull)
            .ifPresent(challenge -> groupChallengeRepository.save(new GroupChallenge(
                idGenerator.nextId(),
                groupId,
                challenge.getId(),
                LocalDate.now(),
                OPEN_ENDED_DATE
            )));
    }

    @Transactional(readOnly = true)
    public StepChallengeOptionListResult getStepChallengeOptions(Long memberId, Long groupId) {
        validateGroupMembership(memberId, groupId);
        List<Challenge> challenges = getStepChallenges();
        if (challenges.isEmpty()) {
            return new StepChallengeOptionListResult(List.of());
        }

        Map<Long, Challenge> challengesById = challenges.stream()
            .collect(Collectors.toMap(Challenge::getId, Function.identity()));
        Long currentChallengeId = getCurrentStepGroupChallengeOrNull(groupId, challengeIds(challenges))
            .map(GroupChallenge::getChallengeId)
            .orElse(null);
        Set<Long> completedChallengeIds = groupChallengeRepository
            .findAllByGroupIdAndChallengeIdInAndGroupChallengeStatusAndDeletedAtIsNull(
                groupId,
                challengeIds(challenges),
                GroupChallengeStatus.COMPLETED
            )
            .stream()
            .map(GroupChallenge::getChallengeId)
            .collect(Collectors.toSet());

        List<StepChallengeOptionResult> options = stepChallengeDetailRepository
            .findByChallengeIdInAndDeletedAtIsNull(challengeIds(challenges))
            .stream()
            .sorted(Comparator.comparingInt(StepChallengeDetail::getDisplayOrder))
            .map(detail -> toStepChallengeOption(
                detail,
                challengesById.get(detail.getChallengeId()),
                currentChallengeId,
                completedChallengeIds
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
            .map(this::toWalkedRegion)
            .toList();
        return new WalkedRegionListResult(regions);
    }

    @Transactional(readOnly = true)
    public StepRankingListResult getStepRankings(Long memberId, Long groupId) {
        validateGroupMembership(memberId, groupId);
        GroupChallenge groupChallenge = getDisplayedStepGroupChallenge(groupId);
        Map<Long, Integer> stepCounts = getStepCountsByMember(groupChallenge);

        List<GroupMember> members = groupMemberRepository
            .findByGroupIdAndGroupMemberStatusAndDeletedAtIsNullOrderByJoinedAtAsc(groupId, GroupMemberStatus.JOINED)
            .stream()
            .sorted(Comparator
                .comparingInt((GroupMember groupMember) -> stepCounts.getOrDefault(groupMember.getMemberId(), 0))
                .reversed()
                .thenComparing(GroupMember::getJoinedAt)
                .thenComparing(GroupMember::getMemberId))
            .toList();
        return new StepRankingListResult(IntStream.range(0, members.size())
            .mapToObj(index -> toStepRanking(index + 1, members.get(index), stepCounts))
            .toList());
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
        return currentGroupChallenge
            .filter(groupChallenge -> groupChallenge.getChallengeId().equals(challenge.getId()))
            .map(groupChallenge -> toChangeResult(groupChallenge, challenge, detail, currentStepCount(groupChallenge)))
            .orElseGet(() -> changeToNewChallenge(groupId, challenge, detail, currentGroupChallenge));
    }

    @Transactional
    public StepRecordUpsertResult upsertDailyStepRecord(
        Long memberId,
        Long groupId,
        StepRecordUpsertCommand command
    ) {
        validateGroupMembership(memberId, groupId);
        GroupChallenge groupChallenge = getCurrentStepGroupChallenge(groupId);
        if (command.recordedOn().isBefore(groupChallenge.getStartsOn())) {
            throw new GeneralException(ErrorStatus.CHALLENGE_VALIDATION_FAILED);
        }

        StepRecord stepRecord = stepRecordRepository
            .findByGroupChallengeIdAndMemberIdAndRecordedOnAndDeletedAtIsNull(
                groupChallenge.getId(),
                memberId,
                command.recordedOn()
            )
            .map(existing -> {
                existing.updateStepCountIfGreater(command.stepCount());
                return existing;
            })
            .orElseGet(() -> stepRecordRepository.save(new StepRecord(
                idGenerator.nextId(),
                groupChallenge.getId(),
                memberId,
                command.recordedOn(),
                command.stepCount(),
                StepSource.API
            )));

        Challenge challenge = getStepChallenge(groupChallenge.getChallengeId());
        StepChallengeDetail detail = getStepChallengeDetail(challenge.getId());
        int currentStepCount = currentStepCount(groupChallenge);
        boolean completed = currentStepCount >= detail.getTargetStepCount();
        if (completed) {
            groupChallenge.complete(LocalDateTime.now());
        }
        return new StepRecordUpsertResult(
            groupChallenge.getId(),
            stepRecord.getRecordedOn(),
            stepRecord.getStepCount(),
            currentStepCount,
            detail.getTargetStepCount(),
            completed
        );
    }

    private StepChallengeChangeResult changeToNewChallenge(
        Long groupId,
        Challenge challenge,
        StepChallengeDetail detail,
        Optional<GroupChallenge> currentGroupChallenge
    ) {
        currentGroupChallenge.ifPresent(groupChallenge -> groupChallenge.reset(LocalDateTime.now()));
        GroupChallenge groupChallenge = groupChallengeRepository.save(new GroupChallenge(
            idGenerator.nextId(),
            groupId,
            challenge.getId(),
            LocalDate.now(),
            OPEN_ENDED_DATE
        ));
        return toChangeResult(groupChallenge, challenge, detail, 0);
    }

    private StepChallengeStatusResult toStatusResult(
        GroupChallenge groupChallenge,
        Challenge challenge,
        StepChallengeDetail detail,
        int currentStepCount
    ) {
        return new StepChallengeStatusResult(
            groupChallenge.getId(),
            challenge.getTitle(),
            detail.getTargetStepCount(),
            currentStepCount,
            groupChallenge.getCreatedAt(),
            groupChallenge.getGroupChallengeStatus()
        );
    }

    private StepChallengeChangeResult toChangeResult(
        GroupChallenge groupChallenge,
        Challenge challenge,
        StepChallengeDetail detail,
        int currentStepCount
    ) {
        return new StepChallengeChangeResult(
            groupChallenge.getId(),
            challenge.getId(),
            challenge.getTitle(),
            detail.getTargetStepCount(),
            currentStepCount,
            groupChallenge.getCreatedAt()
        );
    }

    private int currentStepCount(GroupChallenge groupChallenge) {
        return Math.toIntExact(stepRecordRepository.sumStepCountByGroupChallengeId(groupChallenge.getId()));
    }

    private Map<Long, Integer> getStepCountsByMember(GroupChallenge groupChallenge) {
        return stepRecordRepository.sumStepCountByMember(groupChallenge.getId())
            .stream()
            .collect(Collectors.toMap(
                row -> ((Number) row[0]).longValue(),
                row -> Math.toIntExact(((Number) row[1]).longValue())
            ));
    }

    private StepRankingResult toStepRanking(
        int rank,
        GroupMember groupMember,
        Map<Long, Integer> stepCounts
    ) {
        return new StepRankingResult(
            rank,
            groupMember.getMemberId(),
            groupMember.getDisplayNickname(),
            imageUrlResolver.resolve(groupMember.getDisplayProfileImageKey()),
            stepCounts.getOrDefault(groupMember.getMemberId(), 0)
        );
    }

    private WalkedRegionResult toWalkedRegion(StepChallengeDetail detail) {
        return new WalkedRegionResult(detail.getDestination(), "regions/" + detail.getDestination() + ".png");
    }

    private List<Long> challengeIds(List<Challenge> challenges) {
        return challenges.stream()
            .map(Challenge::getId)
            .toList();
    }

    private List<Challenge> getStepChallenges() {
        return challengeRepository.findByChallengeTypeAndDeletedAtIsNull(ChallengeType.STEP);
    }

    private List<Long> getStepChallengeIds() {
        return challengeIds(getStepChallenges());
    }

    private GroupChallenge getCurrentStepGroupChallenge(Long groupId) {
        return getCurrentStepGroupChallengeOrNull(groupId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.CHALLENGE_IN_PROGRESS_NOT_FOUND));
    }

    private GroupChallenge getDisplayedStepGroupChallenge(Long groupId) {
        List<Long> stepChallengeIds = getStepChallengeIds();
        return getCurrentStepGroupChallengeOrNull(groupId, stepChallengeIds)
            .or(() -> getLatestCompletedStepGroupChallengeOrNull(groupId, stepChallengeIds))
            .orElseThrow(() -> new GeneralException(ErrorStatus.CHALLENGE_IN_PROGRESS_NOT_FOUND));
    }

    private Optional<GroupChallenge> getCurrentStepGroupChallengeOrNull(Long groupId) {
        return getCurrentStepGroupChallengeOrNull(groupId, getStepChallengeIds());
    }

    private Optional<GroupChallenge> getCurrentStepGroupChallengeOrNull(Long groupId, List<Long> stepChallengeIds) {
        if (stepChallengeIds.isEmpty()) {
            return Optional.empty();
        }
        return groupChallengeRepository.findByGroupIdAndChallengeIdInAndGroupChallengeStatusAndDeletedAtIsNull(
            groupId,
            stepChallengeIds,
            GroupChallengeStatus.IN_PROGRESS
        );
    }

    private Optional<GroupChallenge> getLatestCompletedStepGroupChallengeOrNull(Long groupId, List<Long> stepChallengeIds) {
        if (stepChallengeIds.isEmpty()) {
            return Optional.empty();
        }
        return groupChallengeRepository
            .findFirstByGroupIdAndChallengeIdInAndGroupChallengeStatusAndDeletedAtIsNullOrderByCompletedAtDescIdDesc(
                groupId,
                stepChallengeIds,
                GroupChallengeStatus.COMPLETED
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
        Long currentChallengeId,
        Set<Long> completedChallengeIds
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
            challenge.getId().equals(currentChallengeId),
            completedChallengeIds.contains(challenge.getId())
        );
    }

    public record StepChallengeStatusResult(
        Long groupChallengeId,
        String title,
        int targetStepCount,
        int currentStepCount,
        LocalDateTime stepCountFetchFromAt,
        GroupChallengeStatus challengeStatus
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
        boolean selected,
        boolean completed
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
        int currentStepCount,
        LocalDateTime stepCountFetchFromAt
    ) {
    }

    public record StepRecordUpsertCommand(LocalDate recordedOn, int stepCount) {
    }

    public record StepRecordUpsertResult(
        Long groupChallengeId,
        LocalDate recordedOn,
        int stepCount,
        int currentStepCount,
        int targetStepCount,
        boolean completed
    ) {
    }
}
