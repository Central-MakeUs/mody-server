package cmc.mody.admin.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    void 운영_로그인_페이지는_Google_스크립트에_원본을_전달한다() throws Exception {
        mockMvc.perform(get("https://prod-mody.shop/admin").secure(true))
            .andExpect(status().isOk())
            .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
            .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void 로컬_HTTP_로그인은_Google_권장_참조자_정책을_사용한다() throws Exception {
        mockMvc.perform(get("http://localhost:8080/admin"))
            .andExpect(status().isOk())
            .andExpect(header().string("Referrer-Policy", "no-referrer-when-downgrade"));
    }

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
