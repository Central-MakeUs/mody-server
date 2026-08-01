package cmc.mody.challenge.application;

import cmc.mody.challenge.domain.Challenge;
import cmc.mody.challenge.domain.ChallengeType;
import cmc.mody.challenge.domain.GroupChallenge;
import cmc.mody.challenge.infrastructure.repository.ChallengeRepository;
import cmc.mody.challenge.infrastructure.repository.GroupChallengeRepository;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import cmc.mody.grouping.domain.ModyGroup;
import cmc.mody.grouping.infrastructure.repository.ModyGroupRepository;
import java.time.LocalDate;
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

    public record WeeklyChallengeCreateCommand(String title, String description, LocalDate startsOn, LocalDate endsOn) {
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
