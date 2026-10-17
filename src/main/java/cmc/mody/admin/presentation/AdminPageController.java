package cmc.mody.admin.presentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {
    @GetMapping({"/admin", "/admin/"})
    public String adminPage() {
        return "forward:/admin/console/index.html";
    }

    /** 회원·신고·공지·통계·감사 로그는 한 화면에서 경로로 탭을 고른다. */
    @GetMapping({"/admin/members", "/admin/reports", "/admin/announcements", "/admin/stats", "/admin/audit"})
    public String operationsPage() {
        return "forward:/admin/ops/index.html";
    }

    @GetMapping("/admin/challenges")
    public String challengeAdminPage() {
        return "forward:/admin/challenges/index.html";
    }
}
