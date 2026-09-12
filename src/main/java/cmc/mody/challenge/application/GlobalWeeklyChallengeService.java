package cmc.mody.challenge.application;

import cmc.mody.challenge.domain.Challenge;
import cmc.mody.challenge.domain.ChallengeType;
import cmc.mody.challenge.domain.GlobalWeeklyChallenge;
import cmc.mody.challenge.domain.GroupChallenge;
import cmc.mody.challenge.infrastructure.repository.ChallengeRepository;
import cmc.mody.challenge.infrastructure.repository.GlobalWeeklyChallengeRepository;
import cmc.mody.challenge.infrastructure.repository.GroupChallengeRepository;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GlobalWeeklyChallengeService {
    private final IdGenerator idGenerator;
    private final ChallengeRepository challengeRepository;
    private final GlobalWeeklyChallengeRepository globalWeeklyChallengeRepository;
    private final GroupChallengeRepository groupChallengeRepository;
    private final ModyGroupRepository modyGroupRepository;

    @Transactional
    public GlobalWeeklyChallengeCreateResult create(GlobalWeeklyChallengeCommand command) {
        return create(null, command);
    }

    @Transactional
    public GlobalWeeklyChallengeCreateResult create(String idempotencyKey, GlobalWeeklyChallengeCommand command) {
        String normalizedIdempotencyKey = normalizeIdempotencyKey(idempotencyKey);
        if (normalizedIdempotencyKey != null) {
            GlobalWeeklyChallenge existing = globalWeeklyChallengeRepository
                .findByIdempotencyKeyAndDeletedAtIsNull(normalizedIdempotencyKey)
                .orElse(null);
            if (existing != null) {
                return toCreateResult(existing);
            }
        }
        Challenge sourceChallenge = challengeRepository.save(new Challenge(
            idGenerator.nextId(),
            ChallengeType.PHOTO,
            command.title(),
            command.description()
        ));
        GlobalWeeklyChallenge globalWeeklyChallenge = globalWeeklyChallengeRepository.save(new GlobalWeeklyChallenge(
            idGenerator.nextId(),
            sourceChallenge.getId(),
            normalizedIdempotencyKey,
            command.startsOn(),
            command.endsOn()
        ));
        int linkedGroupCount = linkToActiveGroups(globalWeeklyChallenge, sourceChallenge);
        return GlobalWeeklyChallengeCreateResult.from(globalWeeklyChallenge, sourceChallenge, linkedGroupCount);
    }

    @Transactional
    public GlobalWeeklyChallengeSyncResult syncGroups(Long globalWeeklyChallengeId) {
        GlobalWeeklyChallenge globalWeeklyChallenge = getActiveGlobalWeeklyChallenge(globalWeeklyChallengeId);
        Challenge sourceChallenge = getWeeklyChallenge(globalWeeklyChallenge.getChallengeId());
        int linkedGroupCount = linkToActiveGroups(globalWeeklyChallenge, sourceChallenge, true);
        return new GlobalWeeklyChallengeSyncResult(globalWeeklyChallengeId, linkedGroupCount);
    }

    @Transactional(readOnly = true)
    public GlobalWeeklyChallengeListResult getAll() {
        List<GlobalWeeklyChallengeResult> challenges = globalWeeklyChallengeRepository
            .findByDeletedAtIsNullOrderByStartsOnDescIdDesc()
            .stream()
            .filter(GlobalWeeklyChallenge::isActive)
            .map(this::toResult)
            .toList();
        return new GlobalWeeklyChallengeListResult(challenges);
    }

    @Transactional
    public GlobalWeeklyChallengeResult update(Long globalWeeklyChallengeId, GlobalWeeklyChallengeCommand command) {
        GlobalWeeklyChallenge globalWeeklyChallenge = getActiveGlobalWeeklyChallenge(globalWeeklyChallengeId);
        Challenge sourceChallenge = getWeeklyChallenge(globalWeeklyChallenge.getChallengeId());
        sourceChallenge.update(command.title(), command.description());
        globalWeeklyChallenge.updatePeriod(command.startsOn(), command.endsOn());
        List<GroupChallenge> groupChallenges = groupChallengeRepository
            .findByGlobalWeeklyChallengeIdAndDeletedAtIsNull(globalWeeklyChallengeId);
        Map<Long, Challenge> groupChallengesByChallengeId = challengeRepository.findAllById(
                groupChallenges.stream().map(GroupChallenge::getChallengeId).distinct().toList()
            ).stream()
            .collect(Collectors.toMap(Challenge::getId, Function.identity()));
        groupChallenges.forEach(groupChallenge -> {
            Challenge challenge = groupChallengesByChallengeId.get(groupChallenge.getChallengeId());
            if (challenge != null) {
                challenge.update(command.title(), command.description());
            }
            groupChallenge.updatePeriod(command.startsOn(), command.endsOn());
        });
        return toResult(globalWeeklyChallenge);
    }

    @Transactional
    public void delete(Long globalWeeklyChallengeId) {
        GlobalWeeklyChallenge globalWeeklyChallenge = getActiveGlobalWeeklyChallenge(globalWeeklyChallengeId);
        Challenge sourceChallenge = getWeeklyChallenge(globalWeeklyChallenge.getChallengeId());
        globalWeeklyChallenge.delete();
        sourceChallenge.delete();
        groupChallengeRepository.findByGlobalWeeklyChallengeIdAndDeletedAtIsNull(globalWeeklyChallengeId)
            .stream()
            .map(GroupChallenge::getChallengeId)
            .filter(challengeId -> !challengeId.equals(sourceChallenge.getId()))
            .distinct()
            .map(this::findWeeklyChallenge)
            .forEach(Challenge::delete);
    }

    @Transactional
    public void initializeForNewGroup(Long groupId) {
        LocalDate today = LocalDate.now();
        globalWeeklyChallengeRepository
            .findByStartsOnLessThanEqualAndEndsOnGreaterThanEqualAndDeletedAtIsNull(today, today)
            .stream()
            .filter(GlobalWeeklyChallenge::isActive)
            .filter(globalWeeklyChallenge -> !groupChallengeRepository
                .existsByGroupIdAndGlobalWeeklyChallengeIdAndDeletedAtIsNull(groupId, globalWeeklyChallenge.getId()))
            .forEach(globalWeeklyChallenge -> {
                Challenge sourceChallenge = getWeeklyChallenge(globalWeeklyChallenge.getChallengeId());
                Challenge groupChallengeDefinition = copyChallenge(sourceChallenge);
                groupChallengeRepository.save(new GroupChallenge(
                    idGenerator.nextId(),
                    groupId,
                    groupChallengeDefinition.getId(),
                    globalWeeklyChallenge.getId(),
                    globalWeeklyChallenge.getStartsOn(),
                    globalWeeklyChallenge.getEndsOn()
                ));
            });
    }

    private int linkToActiveGroups(GlobalWeeklyChallenge globalWeeklyChallenge, Challenge sourceChallenge) {
        return linkToActiveGroups(globalWeeklyChallenge, sourceChallenge, false);
    }

    private int linkToActiveGroups(
        GlobalWeeklyChallenge globalWeeklyChallenge,
        Challenge sourceChallenge,
        boolean skipAlreadyLinkedGroups
    ) {
        List<ModyGroup> groups = modyGroupRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()
            .stream()
            .filter(ModyGroup::isActive)
            .filter(group -> !skipAlreadyLinkedGroups || !groupChallengeRepository
                .existsByGroupIdAndGlobalWeeklyChallengeIdAndDeletedAtIsNull(group.getId(), globalWeeklyChallenge.getId()))
            .toList();
        List<GroupChallenge> groupChallenges = groups.stream()
            .map(group -> {
                Challenge groupChallengeDefinition = copyChallenge(sourceChallenge);
                return new GroupChallenge(
                    idGenerator.nextId(),
                    group.getId(),
                    groupChallengeDefinition.getId(),
                    globalWeeklyChallenge.getId(),
                    globalWeeklyChallenge.getStartsOn(),
                    globalWeeklyChallenge.getEndsOn()
                );
            })
            .toList();
        groupChallengeRepository.saveAll(groupChallenges);
        return groupChallenges.size();
    }

    private GlobalWeeklyChallengeResult toResult(GlobalWeeklyChallenge globalWeeklyChallenge) {
        Challenge challenge = getWeeklyChallenge(globalWeeklyChallenge.getChallengeId());
        long linkedGroupCount = groupChallengeRepository
            .countByGlobalWeeklyChallengeIdAndDeletedAtIsNull(globalWeeklyChallenge.getId());
        return GlobalWeeklyChallengeResult.from(globalWeeklyChallenge, challenge, linkedGroupCount);
    }

    private GlobalWeeklyChallengeCreateResult toCreateResult(GlobalWeeklyChallenge globalWeeklyChallenge) {
        Challenge challenge = getWeeklyChallenge(globalWeeklyChallenge.getChallengeId());
        return GlobalWeeklyChallengeCreateResult.from(
            globalWeeklyChallenge,
            challenge,
            (int) groupChallengeRepository.countByGlobalWeeklyChallengeIdAndDeletedAtIsNull(globalWeeklyChallenge.getId())
        );
    }

    private GlobalWeeklyChallenge getActiveGlobalWeeklyChallenge(Long globalWeeklyChallengeId) {
        return globalWeeklyChallengeRepository.findByIdAndDeletedAtIsNull(globalWeeklyChallengeId)
            .filter(GlobalWeeklyChallenge::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.CHALLENGE_NOT_FOUND));
    }

    private Challenge getWeeklyChallenge(Long challengeId) {
        return challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(challengeId, ChallengeType.PHOTO)
            .filter(Challenge::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.CHALLENGE_NOT_FOUND));
    }

    private Challenge findWeeklyChallenge(Long challengeId) {
        return challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(challengeId, ChallengeType.PHOTO)
            .orElseThrow(() -> new GeneralException(ErrorStatus.CHALLENGE_NOT_FOUND));
    }

    private Challenge copyChallenge(Challenge sourceChallenge) {
        return challengeRepository.save(new Challenge(
            idGenerator.nextId(),
            ChallengeType.PHOTO,
            sourceChallenge.getTitle(),
            sourceChallenge.getDescription()
        ));
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        if (idempotencyKey.length() > 100) {
            throw new GeneralException(ErrorStatus.CHALLENGE_VALIDATION_FAILED);
        }
        return idempotencyKey;
    }

    public record GlobalWeeklyChallengeCommand(
        String title,
        String description,
        LocalDate startsOn,
        LocalDate endsOn
    ) {
    }

    public record GlobalWeeklyChallengeCreateResult(
        Long globalWeeklyChallengeId,
        Long challengeId,
        String title,
        String description,
        LocalDate startsOn,
        LocalDate endsOn,
        int linkedGroupCount
    ) {
        private static GlobalWeeklyChallengeCreateResult from(
            GlobalWeeklyChallenge globalWeeklyChallenge,
            Challenge challenge,
            int linkedGroupCount
        ) {
            return new GlobalWeeklyChallengeCreateResult(
                globalWeeklyChallenge.getId(),
                challenge.getId(),
                challenge.getTitle(),
                challenge.getDescription(),
                globalWeeklyChallenge.getStartsOn(),
                globalWeeklyChallenge.getEndsOn(),
                linkedGroupCount
            );
        }
    }

    public record GlobalWeeklyChallengeListResult(List<GlobalWeeklyChallengeResult> challenges) {
    }

    public record GlobalWeeklyChallengeSyncResult(Long globalWeeklyChallengeId, int linkedGroupCount) {
    }

    public record GlobalWeeklyChallengeResult(
        Long globalWeeklyChallengeId,
        Long challengeId,
        String title,
        String description,
        LocalDate startsOn,
        LocalDate endsOn,
        long linkedGroupCount
    ) {
        private static GlobalWeeklyChallengeResult from(
            GlobalWeeklyChallenge globalWeeklyChallenge,
            Challenge challenge,
            long linkedGroupCount
        ) {
            return new GlobalWeeklyChallengeResult(
                globalWeeklyChallenge.getId(),
                challenge.getId(),
                challenge.getTitle(),
                challenge.getDescription(),
                globalWeeklyChallenge.getStartsOn(),
                globalWeeklyChallenge.getEndsOn(),
                linkedGroupCount
            );
        }
    }
}
