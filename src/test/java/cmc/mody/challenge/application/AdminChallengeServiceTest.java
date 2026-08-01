package cmc.mody.challenge.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import cmc.mody.challenge.application.AdminChallengeService.WeeklyChallengeCreateCommand;
import cmc.mody.challenge.application.AdminChallengeService.WeeklyChallengeCreateResult;
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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminChallengeServiceTest {
    @Mock
    private IdGenerator idGenerator;

    @Mock
    private ModyGroupRepository modyGroupRepository;

    @Mock
    private ChallengeRepository challengeRepository;

    @Mock
    private GroupChallengeRepository groupChallengeRepository;

    @Captor
    private ArgumentCaptor<Challenge> challengeCaptor;

    @Captor
    private ArgumentCaptor<GroupChallenge> groupChallengeCaptor;

    @Test
    @DisplayName("운영자가 그룹에 임의의 주간 사진 챌린지를 생성한다.")
    void createWeeklyChallenge() {
        AdminChallengeService service = service();
        LocalDate startsOn = LocalDate.of(2026, 8, 3);
        LocalDate endsOn = LocalDate.of(2026, 8, 9);
        given(modyGroupRepository.findById(10L)).willReturn(Optional.of(new ModyGroup(10L, "ABCD2345", "모디")));
        given(idGenerator.nextId()).willReturn(100L, 200L);
        given(challengeRepository.save(any(Challenge.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(groupChallengeRepository.save(any(GroupChallenge.class))).willAnswer(invocation -> invocation.getArgument(0));

        WeeklyChallengeCreateResult result = service.createWeeklyChallenge(
            10L,
            new WeeklyChallengeCreateCommand("엘리베이터 안 타고 올라가기", "계단으로 이동한 사진을 인증해주세요.", startsOn, endsOn)
        );

        then(challengeRepository).should().save(challengeCaptor.capture());
        assertThat(challengeCaptor.getValue().getChallengeType()).isEqualTo(ChallengeType.PHOTO);
        assertThat(challengeCaptor.getValue().getTitle()).isEqualTo("엘리베이터 안 타고 올라가기");
        then(groupChallengeRepository).should().save(groupChallengeCaptor.capture());
        assertThat(groupChallengeCaptor.getValue().getGroupId()).isEqualTo(10L);
        assertThat(groupChallengeCaptor.getValue().getChallengeId()).isEqualTo(100L);
        assertThat(result).isEqualTo(new WeeklyChallengeCreateResult(
            200L,
            100L,
            "엘리베이터 안 타고 올라가기",
            "계단으로 이동한 사진을 인증해주세요.",
            startsOn,
            endsOn
        ));
    }

    @Test
    @DisplayName("존재하지 않는 그룹에는 주간 챌린지를 생성할 수 없다.")
    void throwGroupNotFound() {
        AdminChallengeService service = service();
        given(modyGroupRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createWeeklyChallenge(
            10L,
            new WeeklyChallengeCreateCommand(
                "엘리베이터 안 타고 올라가기",
                "계단으로 이동한 사진을 인증해주세요.",
                LocalDate.of(2026, 8, 3),
                LocalDate.of(2026, 8, 9)
            )
        ))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getStatus()).isEqualTo(ErrorStatus.GROUP_NOT_FOUND));
    }

    private AdminChallengeService service() {
        return new AdminChallengeService(idGenerator, modyGroupRepository, challengeRepository, groupChallengeRepository);
    }
}
