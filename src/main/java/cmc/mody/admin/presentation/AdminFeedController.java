package cmc.mody.admin.presentation;

import cmc.mody.admin.application.AdminFeedService;
import cmc.mody.common.admin.AdminAccessService;
import cmc.mody.common.api.ApiResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/feed")
public class AdminFeedController {
    private final AdminFeedService feed;
    private final AdminAccessService access;

    @GetMapping
    public ApiResponse<AdminFeedService.Page> list(
        @RequestHeader(value = "X-Admin-Api-Key", required = false) String key,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @RequestParam(required = false) String type, @RequestParam(required = false) Long memberId,
        @RequestParam(required = false) Long groupId, @RequestParam(required = false) Long groupChallengeId,
        @RequestParam(required = false) String keyword, @RequestParam(required = false) String cursor,
        @RequestParam(defaultValue = "24") int size) {
        access.validate(key);
        return ApiResponse.ok(feed.list(from, to, type, memberId, groupId, groupChallengeId, keyword, cursor, size));
    }

    @GetMapping("/{type}/{id}")
    public ApiResponse<AdminFeedService.Detail> detail(
        @RequestHeader(value = "X-Admin-Api-Key", required = false) String key,
        @PathVariable String type, @PathVariable long id) {
        access.validate(key);
        return ApiResponse.ok(feed.detail(type, id));
    }
}
