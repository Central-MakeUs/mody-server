package cmc.mody.challenge.presentation;

import cmc.mody.common.api.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ChallengeController {
    @GetMapping("/groups/{groupId}/challenges/summary")
    public ApiResponse<ChallengeSummaryResponse> getChallengeSummary(@PathVariable Long groupId) {
        return ApiResponse.ok(new ChallengeSummaryResponse(12, 7, 360, 2));
    }

    @GetMapping("/groups/{groupId}/challenges/step/current")
    public ApiResponse<StepChallengeStatusResponse> getCurrentStepChallenge(@PathVariable Long groupId) {
        return ApiResponse.ok(new StepChallengeStatusResponse(1L, "서울-인천", 150_000, 34_000));
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
    public ApiResponse<WalkedRegionListResponse> getWalkedRegions(@PathVariable Long groupId) {
        return ApiResponse.ok(new WalkedRegionListResponse(List.of(
            new WalkedRegionResponse("인천", "regions/incheon.png")
        )));
    }

    @GetMapping("/groups/{groupId}/challenges/step/rankings")
    public ApiResponse<StepRankingListResponse> getStepRankings(@PathVariable Long groupId) {
        return ApiResponse.ok(new StepRankingListResponse(List.of(
            new StepRankingResponse(1, 1L, "민석", "profiles/member-1.jpg", 18_000)
        )));
    }

    @PatchMapping("/groups/{groupId}/challenges/step/current")
    public ApiResponse<StepChallengeChangeResponse> changeStepChallenge(
        @PathVariable Long groupId,
        @RequestBody StepChallengeChangeRequest request
    ) {
        return ApiResponse.ok(new StepChallengeChangeResponse(2L, request.challengeId(), "서울-천안", 200_000, 0));
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

    public record StepChallengeStatusResponse(Long groupChallengeId, String title, int targetStepCount, int currentStepCount) {
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
    }

    public record WalkedRegionResponse(String regionName, String regionImageUrl) {
    }

    public record StepRankingListResponse(List<StepRankingResponse> rankings) {
    }

    public record StepRankingResponse(int rank, Long memberId, String nickname, String profileImageUrl, int stepCount) {
    }

    public record StepChallengeChangeRequest(Long challengeId) {
    }

    public record StepChallengeChangeResponse(
        Long groupChallengeId,
        Long challengeId,
        String title,
        int targetStepCount,
        int currentStepCount
    ) {
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
