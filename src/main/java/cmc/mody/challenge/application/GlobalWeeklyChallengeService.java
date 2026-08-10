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
        Challenge challenge = challengeRepository.save(new Challenge(
            idGenerator.nextId(),
            ChallengeType.PHOTO,
            command.title(),
            command.description()
        ));
        GlobalWeeklyChallenge globalWeeklyChallenge = globalWeeklyChallengeRepository.save(new GlobalWeeklyChallenge(
            idGenerator.nextId(),
            challenge.getId(),
            command.startsOn(),
            command.endsOn()
        ));
        int linkedGroupCount = linkToActiveGroups(globalWeeklyChallenge);
        return GlobalWeeklyChallengeCreateResult.from(globalWeeklyChallenge, challenge, linkedGroupCount);
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
        Challenge challenge = getWeeklyChallenge(globalWeeklyChallenge.getChallengeId());
        challenge.update(command.title(), command.description());
        globalWeeklyChallenge.updatePeriod(command.startsOn(), command.endsOn());
        groupChallengeRepository.findByGlobalWeeklyChallengeIdAndDeletedAtIsNull(globalWeeklyChallengeId)
            .forEach(groupChallenge -> groupChallenge.updatePeriod(command.startsOn(), command.endsOn()));
        return toResult(globalWeeklyChallenge);
    }

    @Transactional
    public void delete(Long globalWeeklyChallengeId) {
        GlobalWeeklyChallenge globalWeeklyChallenge = getActiveGlobalWeeklyChallenge(globalWeeklyChallengeId);
        Challenge challenge = getWeeklyChallenge(globalWeeklyChallenge.getChallengeId());
        globalWeeklyChallenge.delete();
        challenge.delete();
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
            .forEach(globalWeeklyChallenge -> groupChallengeRepository.save(new GroupChallenge(
                idGenerator.nextId(),
                groupId,
                globalWeeklyChallenge.getChallengeId(),
                globalWeeklyChallenge.getId(),
                globalWeeklyChallenge.getStartsOn(),
                globalWeeklyChallenge.getEndsOn()
            )));
    }

    private int linkToActiveGroups(GlobalWeeklyChallenge globalWeeklyChallenge) {
        List<ModyGroup> groups = modyGroupRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()
            .stream()
            .filter(ModyGroup::isActive)
            .toList();
        List<GroupChallenge> groupChallenges = groups.stream()
            .map(group -> new GroupChallenge(
                idGenerator.nextId(),
                group.getId(),
                globalWeeklyChallenge.getChallengeId(),
                globalWeeklyChallenge.getId(),
                globalWeeklyChallenge.getStartsOn(),
                globalWeeklyChallenge.getEndsOn()
            ))
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
