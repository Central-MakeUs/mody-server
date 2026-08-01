package cmc.mody.challenge.presentation;

import cmc.mody.challenge.application.AdminChallengeService;
import cmc.mody.common.admin.AdminAccessService;
import cmc.mody.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/groups/{groupId}/weekly-challenges")
public class AdminChallengeController {
    private static final String ADMIN_API_KEY_HEADER = "X-Admin-Api-Key";

    private final AdminAccessService adminAccessService;
    private final AdminChallengeService adminChallengeService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WeeklyChallengeCreateResponse> createWeeklyChallenge(
        @RequestHeader(name = ADMIN_API_KEY_HEADER, required = false) String adminApiKey,
        @PathVariable Long groupId,
        @Valid @RequestBody WeeklyChallengeCreateRequest request
    ) {
        adminAccessService.validate(adminApiKey);
        AdminChallengeService.WeeklyChallengeCreateResult result = adminChallengeService.createWeeklyChallenge(
            groupId,
            request.toCommand()
        );
        return ApiResponse.created(WeeklyChallengeCreateResponse.from(result));
    }

    public record WeeklyChallengeCreateRequest(
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

        public AdminChallengeService.WeeklyChallengeCreateCommand toCommand() {
            return new AdminChallengeService.WeeklyChallengeCreateCommand(title, description, startsOn, endsOn);
        }
    }

    public record WeeklyChallengeCreateResponse(
        Long groupChallengeId,
        Long challengeId,
        String title,
        String description,
        LocalDate startsOn,
        LocalDate endsOn
    ) {
        public static WeeklyChallengeCreateResponse from(AdminChallengeService.WeeklyChallengeCreateResult result) {
            return new WeeklyChallengeCreateResponse(
                result.groupChallengeId(),
                result.challengeId(),
                result.title(),
                result.description(),
                result.startsOn(),
                result.endsOn()
            );
        }
    }
}
