package cmc.mody.challenge.presentation;

import cmc.mody.challenge.application.GlobalWeeklyChallengeService;
import cmc.mody.challenge.application.GlobalWeeklyChallengeService.GlobalWeeklyChallengeCommand;
import cmc.mody.challenge.application.GlobalWeeklyChallengeService.GlobalWeeklyChallengeCreateResult;
import cmc.mody.challenge.application.GlobalWeeklyChallengeService.GlobalWeeklyChallengeListResult;
import cmc.mody.challenge.application.GlobalWeeklyChallengeService.GlobalWeeklyChallengeResult;
import cmc.mody.common.admin.AdminAccessService;
import cmc.mody.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/weekly-challenges")
public class GlobalWeeklyChallengeAdminController {
    private static final String ADMIN_API_KEY_HEADER = "X-Admin-Api-Key";

    private final AdminAccessService adminAccessService;
    private final GlobalWeeklyChallengeService globalWeeklyChallengeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GlobalWeeklyChallengeCreateResponse> createWeeklyChallenge(
        @RequestHeader(name = ADMIN_API_KEY_HEADER, required = false) String adminApiKey,
        @Valid @RequestBody GlobalWeeklyChallengeRequest request
    ) {
        adminAccessService.validate(adminApiKey);
        return ApiResponse.created(GlobalWeeklyChallengeCreateResponse.from(globalWeeklyChallengeService.create(request.toCommand())));
    }

    @GetMapping
    public ApiResponse<GlobalWeeklyChallengeListResponse> getWeeklyChallenges(
        @RequestHeader(name = ADMIN_API_KEY_HEADER, required = false) String adminApiKey
    ) {
        adminAccessService.validate(adminApiKey);
        return ApiResponse.ok(GlobalWeeklyChallengeListResponse.from(globalWeeklyChallengeService.getAll()));
    }

    @PutMapping("/{globalWeeklyChallengeId}")
    public ApiResponse<GlobalWeeklyChallengeResponse> updateWeeklyChallenge(
        @RequestHeader(name = ADMIN_API_KEY_HEADER, required = false) String adminApiKey,
        @PathVariable Long globalWeeklyChallengeId,
        @Valid @RequestBody GlobalWeeklyChallengeRequest request
    ) {
        adminAccessService.validate(adminApiKey);
        return ApiResponse.ok(GlobalWeeklyChallengeResponse.from(
            globalWeeklyChallengeService.update(globalWeeklyChallengeId, request.toCommand())
        ));
    }

    @DeleteMapping("/{globalWeeklyChallengeId}")
    public ApiResponse<Void> deleteWeeklyChallenge(
        @RequestHeader(name = ADMIN_API_KEY_HEADER, required = false) String adminApiKey,
        @PathVariable Long globalWeeklyChallengeId
    ) {
        adminAccessService.validate(adminApiKey);
        globalWeeklyChallengeService.delete(globalWeeklyChallengeId);
        return ApiResponse.ok();
    }

    public record GlobalWeeklyChallengeRequest(
        @NotBlank(message = "챌린지 제목은 필수입니다.")
        @Size(max = 50, message = "챌린지 제목은 50자 이하여야 합니다.")
        String title,
        @NotBlank(message = "챌린지 설명은 필수입니다.")
        @Size(max = 500, message = "챌린지 설명은 500자 이하여야 합니다.")
        String description,
        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startsOn,
        @NotNull(message = "마감일은 필수입니다.")
        LocalDate endsOn
    ) {
        @AssertTrue(message = "마감일은 시작일과 같거나 이후여야 합니다.")
        public boolean isPeriodValid() {
            return startsOn == null || endsOn == null || !endsOn.isBefore(startsOn);
        }

        private GlobalWeeklyChallengeCommand toCommand() {
            return new GlobalWeeklyChallengeCommand(title, description, startsOn, endsOn);
        }
    }

    public record GlobalWeeklyChallengeCreateResponse(
        Long globalWeeklyChallengeId,
        Long challengeId,
        String title,
        String description,
        LocalDate startsOn,
        LocalDate endsOn,
        int linkedGroupCount
    ) {
        private static GlobalWeeklyChallengeCreateResponse from(GlobalWeeklyChallengeCreateResult result) {
            return new GlobalWeeklyChallengeCreateResponse(
                result.globalWeeklyChallengeId(),
                result.challengeId(),
                result.title(),
                result.description(),
                result.startsOn(),
                result.endsOn(),
                result.linkedGroupCount()
            );
        }
    }

    public record GlobalWeeklyChallengeListResponse(List<GlobalWeeklyChallengeResponse> challenges) {
        private static GlobalWeeklyChallengeListResponse from(GlobalWeeklyChallengeListResult result) {
            return new GlobalWeeklyChallengeListResponse(
                result.challenges().stream().map(GlobalWeeklyChallengeResponse::from).toList()
            );
        }
    }

    public record GlobalWeeklyChallengeResponse(
        Long globalWeeklyChallengeId,
        Long challengeId,
        String title,
        String description,
        LocalDate startsOn,
        LocalDate endsOn,
        long linkedGroupCount
    ) {
        private static GlobalWeeklyChallengeResponse from(GlobalWeeklyChallengeResult result) {
            return new GlobalWeeklyChallengeResponse(
                result.globalWeeklyChallengeId(),
                result.challengeId(),
                result.title(),
                result.description(),
                result.startsOn(),
                result.endsOn(),
                result.linkedGroupCount()
            );
        }
    }
}
