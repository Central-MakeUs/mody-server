package cmc.mody.grouping.presentation;

import cmc.mody.auth.presentation.support.CurrentMember;
import cmc.mody.common.api.ApiResponse;
import cmc.mody.grouping.application.GroupService;
import cmc.mody.grouping.application.GroupService.GroupCreateCommand;
import cmc.mody.grouping.application.GroupService.GroupJoinCommand;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
public class GroupController {
    private final GroupService groupService;

    @GetMapping("/code")
    public ApiResponse<GroupCodeResponse> generateGroupCode(@Parameter(hidden = true) @CurrentMember Long memberId) {
        GroupService.GroupCodeResult result = groupService.generateCode(memberId);
        return ApiResponse.ok(new GroupCodeResponse(result.code()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupCreateResponse> createGroup(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @Valid @RequestBody GroupCreateRequest request
    ) {
        GroupService.GroupCreateResult result = groupService.createGroup(memberId, request.toCommand());
        return ApiResponse.created(GroupCreateResponse.from(result));
    }

    @PostMapping("/join")
    public ApiResponse<GroupJoinResponse> joinGroup(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @Valid @RequestBody GroupJoinRequest request
    ) {
        GroupService.GroupJoinResult result = groupService.joinGroup(memberId, request.toCommand());
        return ApiResponse.ok(GroupJoinResponse.from(result));
    }

    @GetMapping
    public ApiResponse<GroupListResponse> getMyGroups(@Parameter(hidden = true) @CurrentMember Long memberId) {
        GroupService.GroupListResult result = groupService.getMyGroups(memberId);
        return ApiResponse.ok(GroupListResponse.from(result));
    }

    @GetMapping("/{groupId}/members")
    public ApiResponse<GroupMemberListResponse> getGroupMembers(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long groupId
    ) {
        GroupService.GroupMemberListResult result = groupService.getGroupMembers(memberId, groupId);
        return ApiResponse.ok(GroupMemberListResponse.from(result));
    }

    @DeleteMapping("/{groupId}/members/me")
    public ApiResponse<Void> leaveGroup(
        @Parameter(hidden = true) @CurrentMember Long memberId,
        @PathVariable Long groupId
    ) {
        groupService.leaveGroup(memberId, groupId);
        return ApiResponse.ok();
    }

    public record GroupCodeResponse(String code) {
    }

    public record GroupCreateRequest(
        @NotBlank(message = "그룹명은 필수입니다.")
        @Size(max = 30, message = "그룹명은 30자 이하로 입력해주세요.")
        String name
    ) {
        public GroupCreateCommand toCommand() {
            return new GroupCreateCommand(name);
        }
    }

    public record GroupCreateResponse(Long groupId, String code, String name) {
        public static GroupCreateResponse from(GroupService.GroupCreateResult result) {
            return new GroupCreateResponse(result.groupId(), result.code(), result.name());
        }
    }

    public record GroupJoinRequest(
        @NotBlank(message = "그룹 코드는 필수입니다.")
        @Pattern(
            regexp = "^[A-Za-z0-9]{6}$",
            message = "그룹 코드는 영문 또는 숫자 6자리여야 합니다."
        )
        String code
    ) {
        public GroupJoinCommand toCommand() {
            return new GroupJoinCommand(code.toUpperCase(Locale.ROOT));
        }
    }

    public record GroupJoinResponse(Long groupId, String code, String name, int memberCount) {
        public static GroupJoinResponse from(GroupService.GroupJoinResult result) {
            return new GroupJoinResponse(result.groupId(), result.code(), result.name(), result.memberCount());
        }
    }

    public record GroupListResponse(List<GroupSummaryResponse> groups) {
        public static GroupListResponse from(GroupService.GroupListResult result) {
            return new GroupListResponse(result.groups().stream()
                .map(GroupSummaryResponse::from)
                .toList());
        }
    }

    public record GroupSummaryResponse(Long groupId, String name, String code, int memberCount) {
        public static GroupSummaryResponse from(GroupService.GroupSummaryResult result) {
            return new GroupSummaryResponse(result.groupId(), result.name(), result.code(), result.memberCount());
        }
    }

    public record GroupMemberListResponse(List<GroupMemberResponse> members) {
        public static GroupMemberListResponse from(GroupService.GroupMemberListResult result) {
            return new GroupMemberListResponse(result.members().stream()
                .map(GroupMemberResponse::from)
                .toList());
        }
    }

    public record GroupMemberResponse(Long memberId, String nickname, String profileImageUrl) {
        public static GroupMemberResponse from(GroupService.GroupMemberResult result) {
            return new GroupMemberResponse(result.memberId(), result.nickname(), result.profileImageUrl());
        }
    }
}
