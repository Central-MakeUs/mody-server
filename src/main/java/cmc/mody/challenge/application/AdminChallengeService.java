package cmc.mody.challenge.application;

import cmc.mody.challenge.domain.Challenge;
import cmc.mody.challenge.domain.ChallengeType;
import cmc.mody.challenge.domain.GroupChallenge;
import cmc.mody.challenge.domain.GroupChallengeStatus;
import cmc.mody.challenge.infrastructure.repository.ChallengeRepository;
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
public class AdminChallengeService {
    private final IdGenerator idGenerator;
    private final ModyGroupRepository modyGroupRepository;
    private final ChallengeRepository challengeRepository;
    private final GroupChallengeRepository groupChallengeRepository;

    @Transactional
    public WeeklyChallengeCreateResult createWeeklyChallenge(Long groupId, WeeklyChallengeCreateCommand command) {
        modyGroupRepository.findById(groupId)
            .filter(ModyGroup::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));

        Challenge challenge = challengeRepository.save(new Challenge(
            idGenerator.nextId(),
            ChallengeType.PHOTO,
            command.title(),
            command.description()
        ));
        GroupChallenge groupChallenge = groupChallengeRepository.save(new GroupChallenge(
            idGenerator.nextId(),
            groupId,
            challenge.getId(),
            command.startsOn(),
            command.endsOn()
        ));
        return new WeeklyChallengeCreateResult(
            groupChallenge.getId(),
            challenge.getId(),
            challenge.getTitle(),
            challenge.getDescription(),
            groupChallenge.getStartsOn(),
            groupChallenge.getEndsOn()
        );
    }

    @Transactional(readOnly = true)
    public WeeklyChallengeListResult getWeeklyChallenges(Long groupId) {
        validateGroup(groupId);
        List<GroupChallenge> groupChallenges = groupChallengeRepository.findByGroupIdAndDeletedAtIsNull(groupId);
        Map<Long, Challenge> challengesById = challengeRepository.findAllById(
                groupChallenges.stream().map(GroupChallenge::getChallengeId).toList()
            ).stream()
            .filter(Challenge::isActive)
            .collect(Collectors.toMap(Challenge::getId, Function.identity()));

        return new WeeklyChallengeListResult(groupChallenges.stream()
            .filter(groupChallenge -> groupChallenge.getGlobalWeeklyChallengeId() == null)
            .filter(groupChallenge -> challengesById.containsKey(groupChallenge.getChallengeId()))
            .map(groupChallenge -> WeeklyChallengeResult.from(groupChallenge, challengesById.get(groupChallenge.getChallengeId())))
            .toList());
    }

    @Transactional
    public WeeklyChallengeResult updateWeeklyChallenge(
        Long groupId,
        Long groupChallengeId,
        WeeklyChallengeUpdateCommand command
    ) {
        validateGroup(groupId);
        GroupChallenge groupChallenge = groupChallengeRepository.findByIdAndGroupIdAndDeletedAtIsNull(groupChallengeId, groupId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.CHALLENGE_NOT_FOUND));
        validateLegacyWeeklyChallenge(groupChallenge);
        Challenge challenge = challengeRepository.findByIdAndChallengeTypeAndDeletedAtIsNull(
                groupChallenge.getChallengeId(),
                ChallengeType.PHOTO
            )
            .orElseThrow(() -> new GeneralException(ErrorStatus.CHALLENGE_NOT_FOUND));
        challenge.update(command.title(), command.description());
        groupChallenge.updatePeriod(command.startsOn(), command.endsOn());
        return WeeklyChallengeResult.from(groupChallenge, challenge);
    }

    @Transactional
    public void deleteWeeklyChallenge(Long groupId, Long groupChallengeId) {
        validateGroup(groupId);
        GroupChallenge groupChallenge = groupChallengeRepository.findByIdAndGroupIdAndDeletedAtIsNull(groupChallengeId, groupId)
            .orElseThrow(() -> new GeneralException(ErrorStatus.CHALLENGE_NOT_FOUND));
        validateLegacyWeeklyChallenge(groupChallenge);
        groupChallenge.delete();
    }

    private void validateGroup(Long groupId) {
        modyGroupRepository.findById(groupId)
            .filter(ModyGroup::isActive)
            .orElseThrow(() -> new GeneralException(ErrorStatus.GROUP_NOT_FOUND));
    }

    private void validateLegacyWeeklyChallenge(GroupChallenge groupChallenge) {
        if (groupChallenge.getGlobalWeeklyChallengeId() != null) {
            throw new GeneralException(ErrorStatus.CHALLENGE_VALIDATION_FAILED);
        }
    }

    public record WeeklyChallengeCreateCommand(String title, String description, LocalDate startsOn, LocalDate endsOn) {
    }

    public record WeeklyChallengeUpdateCommand(String title, String description, LocalDate startsOn, LocalDate endsOn) {
    }

    public record WeeklyChallengeListResult(List<WeeklyChallengeResult> challenges) {
    }

    public record WeeklyChallengeResult(
        Long groupChallengeId,
        Long challengeId,
        String title,
        String description,
        LocalDate startsOn,
        LocalDate endsOn,
        GroupChallengeStatus status
    ) {
        private static WeeklyChallengeResult from(GroupChallenge groupChallenge, Challenge challenge) {
            return new WeeklyChallengeResult(
                groupChallenge.getId(),
                challenge.getId(),
                challenge.getTitle(),
                challenge.getDescription(),
                groupChallenge.getStartsOn(),
                groupChallenge.getEndsOn(),
                groupChallenge.getGroupChallengeStatus()
            );
        }
    }

    public record WeeklyChallengeCreateResult(
        Long groupChallengeId,
        Long challengeId,
        String title,
        String description,
        LocalDate startsOn,
        LocalDate endsOn
    ) {
    }
}
