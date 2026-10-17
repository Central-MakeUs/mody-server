package cmc.mody.admin.presentation;

import cmc.mody.admin.application.AdminReportService;
import cmc.mody.admin.application.AdminSessionService;
import cmc.mody.admin.domain.AdminAccount;
import cmc.mody.common.admin.AdminAccessService;
import cmc.mody.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController {
    private final AdminReportService reports;
    private final AdminAccessService access;

    @GetMapping
    public ApiResponse<AdminReportService.Page> list(
        @RequestHeader(value = "X-Admin-Api-Key", required = false) String key,
        @RequestParam(required = false) String state, @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "20") int size) {
        access.validate(key);
        return ApiResponse.ok(reports.list(state, cursor, size));
    }

    @GetMapping("/summary")
    public ApiResponse<AdminReportService.Summary> summary(
        @RequestHeader(value = "X-Admin-Api-Key", required = false) String key) {
        access.validate(key);
        return ApiResponse.ok(reports.summary());
    }

    @PostMapping("/records/{recordId}/hide")
    public ApiResponse<AdminReportService.Resolution> hide(
        @RequestHeader(value = "X-Admin-Api-Key", required = false) String key, @PathVariable long recordId,
        HttpServletRequest request) {
        access.validate(key);
        return ApiResponse.ok(reports.hide(recordId, actor(request)));
    }

    @PostMapping("/records/{recordId}/dismiss")
    public ApiResponse<AdminReportService.Resolution> dismiss(
        @RequestHeader(value = "X-Admin-Api-Key", required = false) String key, @PathVariable long recordId,
        HttpServletRequest request) {
        access.validate(key);
        return ApiResponse.ok(reports.dismiss(recordId, actor(request)));
    }

    private String actor(HttpServletRequest request) {
        return request.getAttribute(AdminSessionService.PRINCIPAL_ATTRIBUTE) instanceof AdminAccount account
            ? account.getEmail() : "machine";
    }
}
