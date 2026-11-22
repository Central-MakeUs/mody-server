package cmc.mody.admin.presentation;

import cmc.mody.admin.application.AdminGroupPageService;
import cmc.mody.common.admin.AdminAccessService;
import cmc.mody.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 그룹 페이지(/admin/groups)용 조회 API. 그룹명 수정 같은 변경은 기존 /api/v1/admin/groups 를 쓴다. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/group-pages")
public class AdminGroupPageController {
    private final AdminGroupPageService groups;
    private final AdminAccessService access;

    @GetMapping
    public ApiResponse<AdminGroupPageService.Page> list(
        @RequestHeader(value = "X-Admin-Api-Key", required = false) String key,
        @RequestParam(required = false) String keyword, @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "30") int size) {
        access.validate(key);
        return ApiResponse.ok(groups.list(keyword, cursor, size));
    }

    @GetMapping("/{groupId}")
    public ApiResponse<AdminGroupPageService.Detail> detail(
        @RequestHeader(value = "X-Admin-Api-Key", required = false) String key, @PathVariable long groupId) {
        access.validate(key);
        return ApiResponse.ok(groups.detail(groupId));
    }
}
