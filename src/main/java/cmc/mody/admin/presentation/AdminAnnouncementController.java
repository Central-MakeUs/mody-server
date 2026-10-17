package cmc.mody.admin.presentation;

import cmc.mody.admin.application.AdminAnnouncementService;
import cmc.mody.admin.application.AdminSessionService;
import cmc.mody.admin.domain.AdminAccount;
import cmc.mody.common.admin.AdminAccessService;
import cmc.mody.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/announcements")
public class AdminAnnouncementController {
    private final AdminAnnouncementService announcements;
    private final AdminAccessService access;

    @GetMapping
    public ApiResponse<AdminAnnouncementService.Page> list(
        @RequestHeader(value = "X-Admin-Api-Key", required = false) String key,
        @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "20") int size) {
        access.validate(key);
        return ApiResponse.ok(announcements.list(cursor, size));
    }

    @PostMapping("/preview")
    public ApiResponse<AdminAnnouncementService.Preview> preview(
        @RequestHeader(value = "X-Admin-Api-Key", required = false) String key, @RequestBody TargetRequest request) {
        access.validate(key);
        return ApiResponse.ok(announcements.preview(request.target()));
    }

    @PostMapping
    public ApiResponse<AdminAnnouncementService.Item> send(
        @RequestHeader(value = "X-Admin-Api-Key", required = false) String key,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @RequestBody SendRequest request, HttpServletRequest servletRequest) {
        access.validate(key);
        return ApiResponse.ok(announcements.send(request.target(), request.title(), request.content(),
            Boolean.TRUE.equals(request.allowQuietHours()), idempotencyKey, actor(servletRequest)));
    }

    private String actor(HttpServletRequest request) {
        return request.getAttribute(AdminSessionService.PRINCIPAL_ATTRIBUTE) instanceof AdminAccount account
            ? account.getEmail() : "machine";
    }

    public record TargetRequest(String targetType, Long groupId, List<Long> memberIds) {
        AdminAnnouncementService.Target target() {
            return new AdminAnnouncementService.Target(targetType, groupId, memberIds);
        }
    }

    public record SendRequest(String targetType, Long groupId, List<Long> memberIds, String title, String content,
        Boolean allowQuietHours) {
        AdminAnnouncementService.Target target() {
            return new AdminAnnouncementService.Target(targetType, groupId, memberIds);
        }
    }
}
