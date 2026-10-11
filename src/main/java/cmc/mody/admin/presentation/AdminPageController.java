package cmc.mody.admin.presentation;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {
    @GetMapping({"/admin", "/admin/"})
    public String adminPage() {
        return "forward:/admin/console/index.html";
    }

    @GetMapping("/admin/challenges")
    public String challengeAdminPage() {
        return "forward:/admin/challenges/index.html";
    }
}
