package cmc.mody.admin.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminPageController.class)
class AdminPageControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void 인증_피드_페이지를_정적_화면으로_전달한다() throws Exception {
        mockMvc.perform(get("/admin"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/admin/console/index.html"));
        assertThat(new ClassPathResource("static/admin/console/index.html").exists()).isTrue();
    }

    @Test
    void 챌린지_관리_페이지를_정적_화면으로_전달한다() throws Exception {
        mockMvc.perform(get("/admin/challenges"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/admin/challenges/index.html"));

        assertThat(new ClassPathResource("static/admin/challenges/index.html").exists()).isTrue();
    }
}
