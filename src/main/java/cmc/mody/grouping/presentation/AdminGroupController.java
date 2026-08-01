package cmc.mody.grouping.presentation;

import cmc.mody.common.admin.AdminAccessService;
import cmc.mody.common.api.ApiResponse;
import cmc.mody.grouping.application.AdminGroupService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/groups")
public class AdminGroupController {
    private static final String ADMIN_API_KEY_HEADER = "X-Admin-Api-Key";

    private final AdminAccessService adminAccessService;
    private final AdminGroupService adminGroupService;

    @GetMapping
    public ApiResponse<AdminGroupListResponse> getGroups(
        @RequestHeader(name = ADMIN_API_KEY_HEADER, required = false) String adminApiKey
    ) {
        adminAccessService.validate(adminApiKey);
        return ApiResponse.ok(AdminGroupListResponse.from(adminGroupService.getGroups()));
    }

    public record AdminGroupListResponse(List<AdminGroupResponse> groups) {
        public static AdminGroupListResponse from(AdminGroupService.AdminGroupListResult result) {
            return new AdminGroupListResponse(result.groups().stream().map(AdminGroupResponse::from).toList());
        }
    }

    public record AdminGroupResponse(Long groupId, String name, String code, long memberCount) {
        public static AdminGroupResponse from(AdminGroupService.AdminGroupResult result) {
            return new AdminGroupResponse(result.groupId(), result.name(), result.code(), result.memberCount());
        }
    }
}
