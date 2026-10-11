package cmc.mody.admin.presentation;

import cmc.mody.admin.application.AdminChallengeOverviewService;
import cmc.mody.common.admin.AdminAccessService;
import cmc.mody.common.api.ApiResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminChallengeOverviewController {
    private final AdminChallengeOverviewService overview;
    private final AdminAccessService access;

    @GetMapping("/api/v1/admin/challenge-overview")
    public ApiResponse<AdminChallengeOverviewService.Page> list(
        @RequestHeader(value = "X-Admin-Api-Key", required = false) String key,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) Long groupId, @RequestParam(defaultValue = "ALL") String phase,
        @RequestParam(required = false) String keyword, @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "24") int size) {
        access.validate(key);
        return ApiResponse.ok(overview.list(from, to, groupId, phase, keyword, cursor, size));
    }
}
