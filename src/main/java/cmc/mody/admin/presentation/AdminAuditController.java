package cmc.mody.admin.presentation;

import cmc.mody.admin.application.AdminAuditService;
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
@RequestMapping("/api/v1/admin/audit-logs")
public class AdminAuditController {
    private final AdminAuditService audit;
    private final AdminAccessService access;

    @GetMapping
    public ApiResponse<AdminAuditService.Page> list(
        @RequestHeader(value = "X-Admin-Api-Key", required = false) String key,
        @RequestParam(required = false) String actor, @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "50") int size) {
        access.validate(key);
        return ApiResponse.ok(audit.list(actor, cursor, size));
    }
}
