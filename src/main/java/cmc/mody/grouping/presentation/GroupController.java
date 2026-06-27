package cmc.mody.grouping.presentation;

import cmc.mody.common.api.ApiResponse;
import java.util.List;
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
@RequestMapping("/api/v1/groups")
public class GroupController {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<GroupCreateResponse> createGroup(@RequestBody GroupCreateRequest request) {
        return ApiResponse.created(new GroupCreateResponse(1L, "ABCDEF", request.name()));
    }

    @PostMapping("/join")
    public ApiResponse<GroupJoinResponse> joinGroup(@RequestBody GroupJoinRequest request) {
        return ApiResponse.ok(new GroupJoinResponse(1L, request.code(), "모디 그룹", 4));
    }

    @GetMapping
    public ApiResponse<GroupListResponse> getMyGroups() {
        return ApiResponse.ok(new GroupListResponse(List.of(
            new GroupSummaryResponse(1L, "모디 그룹", "ABCDEF", 4, true)
        )));
    }

    @GetMapping("/{groupId}/members")
    public ApiResponse<GroupMemberListResponse> getGroupMembers(@PathVariable Long groupId) {
        return ApiResponse.ok(new GroupMemberListResponse(List.of(
            new GroupMemberResponse(1L, "민석", "profiles/member-1.jpg"),
            new GroupMemberResponse(2L, "친구", "profiles/member-2.jpg")
        )));
    }

    @DeleteMapping("/{groupId}/members/me")
    public ApiResponse<Void> leaveGroup(@PathVariable Long groupId) {
        return ApiResponse.ok();
    }

    public record GroupCreateRequest(String name) {
    }

    public record GroupCreateResponse(Long groupId, String code, String name) {
    }

    public record GroupJoinRequest(String code) {
    }

    public record GroupJoinResponse(Long groupId, String code, String name, int memberCount) {
    }

    public record GroupListResponse(List<GroupSummaryResponse> groups) {
    }

    public record GroupSummaryResponse(Long groupId, String name, String code, int memberCount, boolean current) {
    }

    public record GroupMemberListResponse(List<GroupMemberResponse> members) {
    }

    public record GroupMemberResponse(Long memberId, String nickname, String profileImageUrl) {
    }
}
