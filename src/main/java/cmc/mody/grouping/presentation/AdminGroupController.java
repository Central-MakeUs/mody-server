package cmc.mody.grouping.presentation;

import cmc.mody.common.admin.AdminAccessService;
import cmc.mody.common.api.ApiResponse;
import cmc.mody.grouping.application.AdminGroupService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @GetMapping("/{groupId}")
    public ApiResponse<AdminGroupDetailResponse> getGroup(
        @RequestHeader(name = ADMIN_API_KEY_HEADER, required = false) String adminApiKey,
        @PathVariable Long groupId
    ) {
        adminAccessService.validate(adminApiKey);
        return ApiResponse.ok(AdminGroupDetailResponse.from(adminGroupService.getGroup(groupId)));
    }

    @PutMapping("/{groupId}")
    public ApiResponse<AdminGroupResponse> updateGroup(
        @RequestHeader(name = ADMIN_API_KEY_HEADER, required = false) String adminApiKey,
        @PathVariable Long groupId,
        @Valid @RequestBody AdminGroupUpdateRequest request
    ) {
        adminAccessService.validate(adminApiKey);
        return ApiResponse.ok(AdminGroupResponse.from(adminGroupService.updateGroupName(groupId, request.name())));
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

    public record AdminGroupDetailResponse(Long groupId, String name, String code, List<AdminGroupMemberResponse> members) {
        public static AdminGroupDetailResponse from(AdminGroupService.AdminGroupDetailResult result) {
            return new AdminGroupDetailResponse(
                result.groupId(),
                result.name(),
                result.code(),
                result.members().stream().map(AdminGroupMemberResponse::from).toList()
            );
        }
    }

    public record AdminGroupMemberResponse(Long memberId, String nickname, String profileImageKey, LocalDateTime joinedAt) {
        public static AdminGroupMemberResponse from(AdminGroupService.AdminGroupMemberResult result) {
            return new AdminGroupMemberResponse(result.memberId(), result.nickname(), result.profileImageKey(), result.joinedAt());
        }
    }

    public record AdminGroupUpdateRequest(
        @NotBlank(message = "그룹명은 필수입니다.")
        @Size(max = 30, message = "그룹명은 30자 이하여야 합니다.")
        String name
    ) {
    }
}
