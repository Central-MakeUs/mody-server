package cmc.mody.challenge.presentation;

import cmc.mody.auth.presentation.support.CurrentMember;
import cmc.mody.challenge.application.StepChallengeService;
import cmc.mody.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class ChallengeController {
    private final StepChallengeService stepChallengeService;

    @GetMapping("/groups/{groupId}/challenges/summary")
    public ApiResponse<ChallengeSummaryResponse> getChallengeSummary(@PathVariable Long groupId) {
        return ApiResponse.ok(new ChallengeSummaryResponse(12, 7, 360, 2));
    }

    @GetMapping("/groups/{groupId}/challenges/step/current")
    public ApiResponse<StepChallengeStatusResponse> getCurrentStepChallenge(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long groupId
    ) {
        StepChallengeService.StepChallengeStatusResult result = stepChallengeService.getCurrentStepChallenge(
            memberId,
            groupId
        );
        return ApiResponse.ok(StepChallengeStatusResponse.from(result));
    }

    @GetMapping("/groups/{groupId}/challenges/weekly")
    public ApiResponse<WeeklyChallengeListResponse> getWeeklyChallenges(@PathVariable Long groupId) {
        return ApiResponse.ok(new WeeklyChallengeListResponse(List.of(
            new WeeklyChallengeSummaryResponse(1L, "물 2L 마시기", "SUNDAY", 3, "민석")
        )));
    }

    @GetMapping("/groups/{groupId}/challenges/nudges")
    public ApiResponse<NudgeTargetListResponse> getNudgeTargets(@PathVariable Long groupId) {
        return ApiResponse.ok(new NudgeTargetListResponse(List.of(
            new NudgeTargetResponse(2L, "친구", "profiles/member-2.jpg", false)
        )));
    }

    @PostMapping("/groups/{groupId}/challenges/nudges/{memberId}")
    public ApiResponse<Void> nudgeMember(@PathVariable Long groupId, @PathVariable Long memberId) {
        return ApiResponse.ok();
    }

    @GetMapping("/groups/{groupId}/challenges/step/regions")
    public ApiResponse<WalkedRegionListResponse> getWalkedRegions(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long groupId
    ) {
        StepChallengeService.WalkedRegionListResult result = stepChallengeService.getWalkedRegions(memberId, groupId);
        return ApiResponse.ok(WalkedRegionListResponse.from(result));
    }

    @GetMapping("/groups/{groupId}/challenges/step/options")
    public ApiResponse<StepChallengeOptionListResponse> getStepChallengeOptions(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long groupId
    ) {
        StepChallengeService.StepChallengeOptionListResult result = stepChallengeService.getStepChallengeOptions(
            memberId,
            groupId
        );
        return ApiResponse.ok(StepChallengeOptionListResponse.from(result));
    }

    @GetMapping("/groups/{groupId}/challenges/step/rankings")
    public ApiResponse<StepRankingListResponse> getStepRankings(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long groupId
    ) {
        StepChallengeService.StepRankingListResult result = stepChallengeService.getStepRankings(memberId, groupId);
        return ApiResponse.ok(StepRankingListResponse.from(result));
    }

    @PatchMapping("/groups/{groupId}/challenges/step/current")
    public ApiResponse<StepChallengeChangeResponse> changeStepChallenge(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long groupId,
        @Valid @RequestBody StepChallengeChangeRequest request
    ) {
        StepChallengeService.StepChallengeChangeResult result = stepChallengeService.changeStepChallenge(
            memberId,
            groupId,
            request.toCommand()
        );
        return ApiResponse.ok(StepChallengeChangeResponse.from(result));
    }

    @GetMapping("/weekly-challenges/{challengeId}")
    public ApiResponse<WeeklyChallengeDetailResponse> getWeeklyChallengeDetail(@PathVariable Long challengeId) {
        return ApiResponse.ok(new WeeklyChallengeDetailResponse(challengeId, "물 2L 마시기", "하루 동안 물 2L를 마시고 사진으로 인증한다."));
    }

    @GetMapping("/groups/{groupId}/weekly-challenges/{groupChallengeId}/proofs")
    public ApiResponse<WeeklyChallengeProofListResponse> getWeeklyChallengeProofs(
        @PathVariable Long groupId,
        @PathVariable Long groupChallengeId
    ) {
        return ApiResponse.ok(new WeeklyChallengeProofListResponse(List.of(
            new WeeklyChallengeProofResponse(1L, "proofs/proof-1.jpg", 1L, "민석", "profiles/member-1.jpg")
        )));
    }

    @PostMapping("/groups/{groupId}/weekly-challenges/{groupChallengeId}/share")
    public ApiResponse<WeeklyChallengeShareResponse> shareWeeklyChallenge(
        @PathVariable Long groupId,
        @PathVariable Long groupChallengeId
    ) {
        return ApiResponse.ok(new WeeklyChallengeShareResponse("shares/group-1-weekly-1.png", 2, 2));
    }

    public record ChallengeSummaryResponse(
        int daysTogether,
        int allMemberRecordedDays,
        int monthlyExerciseMinutes,
        int monthlyCompletedChallengeCount
    ) {
    }

    public record StepChallengeStatusResponse(
        Long groupChallengeId,
        String title,
        int targetStepCount,
        int currentStepCount
    ) {
        public static StepChallengeStatusResponse from(StepChallengeService.StepChallengeStatusResult result) {
            return new StepChallengeStatusResponse(
                result.groupChallengeId(),
                result.title(),
                result.targetStepCount(),
                result.currentStepCount()
            );
        }
    }

    public record WeeklyChallengeListResponse(List<WeeklyChallengeSummaryResponse> challenges) {
    }

    public record WeeklyChallengeSummaryResponse(
        Long groupChallengeId,
        String title,
        String deadlineDayOfWeek,
        int participantCount,
        String randomParticipantNickname
    ) {
    }

    public record NudgeTargetListResponse(List<NudgeTargetResponse> members) {
    }

    public record NudgeTargetResponse(Long memberId, String nickname, String profileImageUrl, boolean recordedToday) {
    }

    public record WalkedRegionListResponse(List<WalkedRegionResponse> regions) {
        public static WalkedRegionListResponse from(StepChallengeService.WalkedRegionListResult result) {
            return new WalkedRegionListResponse(result.regions().stream()
                .map(WalkedRegionResponse::from)
                .toList());
        }
    }

    public record WalkedRegionResponse(String regionName, String regionImageUrl) {
        public static WalkedRegionResponse from(StepChallengeService.WalkedRegionResult result) {
            return new WalkedRegionResponse(result.regionName(), result.regionImageUrl());
        }
    }

    public record StepChallengeOptionListResponse(List<StepChallengeOptionResponse> options) {
        public static StepChallengeOptionListResponse from(StepChallengeService.StepChallengeOptionListResult result) {
            return new StepChallengeOptionListResponse(result.options().stream()
                .map(StepChallengeOptionResponse::from)
                .toList());
        }
    }

    public record StepChallengeOptionResponse(
        Long challengeId,
        String title,
        String departure,
        String destination,
        double distanceKm,
        int targetStepCount,
        boolean selected
    ) {
        public static StepChallengeOptionResponse from(StepChallengeService.StepChallengeOptionResult result) {
            return new StepChallengeOptionResponse(
                result.challengeId(),
                result.title(),
                result.departure(),
                result.destination(),
                result.distanceKm(),
                result.targetStepCount(),
                result.selected()
            );
        }
    }

    public record StepRankingListResponse(List<StepRankingResponse> rankings) {
        public static StepRankingListResponse from(StepChallengeService.StepRankingListResult result) {
            return new StepRankingListResponse(result.rankings().stream()
                .map(StepRankingResponse::from)
                .toList());
        }
    }

    public record StepRankingResponse(int rank, Long memberId, String nickname, String profileImageUrl, int stepCount) {
        public static StepRankingResponse from(StepChallengeService.StepRankingResult result) {
            return new StepRankingResponse(
                result.rank(),
                result.memberId(),
                result.nickname(),
                result.profileImageUrl(),
                result.stepCount()
            );
        }
    }

    public record StepChallengeChangeRequest(
        @NotNull(message = "챌린지 id는 필수입니다.")
        Long challengeId
    ) {
        public StepChallengeService.StepChallengeChangeCommand toCommand() {
            return new StepChallengeService.StepChallengeChangeCommand(challengeId);
        }
    }

    public record StepChallengeChangeResponse(
        Long groupChallengeId,
        Long challengeId,
        String title,
        int targetStepCount,
        int currentStepCount
    ) {
        public static StepChallengeChangeResponse from(StepChallengeService.StepChallengeChangeResult result) {
            return new StepChallengeChangeResponse(
                result.groupChallengeId(),
                result.challengeId(),
                result.title(),
                result.targetStepCount(),
                result.currentStepCount()
            );
        }
    }

    public record WeeklyChallengeDetailResponse(Long challengeId, String title, String description) {
    }

    public record WeeklyChallengeProofListResponse(List<WeeklyChallengeProofResponse> proofs) {
    }

    public record WeeklyChallengeProofResponse(
        Long proofId,
        String imageUrl,
        Long memberId,
        String nickname,
        String profileImageUrl
    ) {
    }

    public record WeeklyChallengeShareResponse(String imageUrl, int rows, int columns) {
    }
}
