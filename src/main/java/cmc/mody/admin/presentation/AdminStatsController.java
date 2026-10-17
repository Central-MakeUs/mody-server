package cmc.mody.admin.presentation;

import cmc.mody.admin.application.AdminStatsService;
import cmc.mody.common.admin.AdminAccessService;
import cmc.mody.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/stats")
public class AdminStatsController {
    private final AdminStatsService stats;
    private final AdminAccessService access;

    @GetMapping
    public ApiResponse<AdminStatsService.Stats> stats(
        @RequestHeader(value = "X-Admin-Api-Key", required = false) String key,
        @RequestParam(defaultValue = "8") int weeks) {
        access.validate(key);
        return ApiResponse.ok(stats.stats(weeks));
    }
}
