package cmc.mody.admin.presentation;

import cmc.mody.admin.application.AdminMemberService;
import cmc.mody.common.admin.AdminAccessService;
import cmc.mody.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/members")
public class AdminMemberController {
    private final AdminMemberService members;
    private final AdminAccessService access;

    @GetMapping
    public ApiResponse<AdminMemberService.Page> list(
        @RequestHeader(value = "X-Admin-Api-Key", required = false) String key,
        @RequestParam(required = false) String keyword, @RequestParam(required = false) String state,
        @RequestParam(required = false) String loginType, @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "30") int size) {
        access.validate(key);
        return ApiResponse.ok(members.list(keyword, state, loginType, cursor, size));
    }

    @GetMapping("/summary")
    public ApiResponse<AdminMemberService.Summary> summary(
        @RequestHeader(value = "X-Admin-Api-Key", required = false) String key) {
        access.validate(key);
        return ApiResponse.ok(members.summary());
    }

    @GetMapping("/{memberId}")
    public ApiResponse<AdminMemberService.Detail> detail(
        @RequestHeader(value = "X-Admin-Api-Key", required = false) String key, @PathVariable long memberId) {
        access.validate(key);
        return ApiResponse.ok(members.detail(memberId));
    }
}
