package cmc.mody.share.presentation;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UniversalLinkController.class)
@TestPropertySource(properties = {
    "invite.aasa-resource-path=universal-link/apple-app-site-association-prod",
    "invite.app-store-url=https://apps.apple.com/kr/app/id6785124295"
})
class UniversalLinkControllerProdTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void appleAppSiteAssociationForProd() throws Exception {
        mockMvc.perform(get("/.well-known/apple-app-site-association"))
            .andExpect(status().isOk())
            .andExpect(redirectedUrl(null))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.applinks.details[0].appIDs[0]").value("BLRYMXGV5K.com.jagsim.mody"))
            .andExpect(jsonPath("$.applinks.details[0].components[0].['/']").value("/invite"))
            .andExpect(jsonPath("$.applinks.details[0].components[1].['/']").value("/invite/*"));
    }

    @Test
    void inviteForIosUsesProdAppStoreUrl() throws Exception {
        mockMvc.perform(get("/invite")
                .param("code", "ABCD2345")
                .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 (iPhone; CPU iPhone OS 18_0 like Mac OS X)"))
            .andExpect(status().isOk())
            .andExpect(redirectedUrl(null))
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString("text/html")))
            .andExpect(content().string(containsString("ABCD2345")))
            .andExpect(content().string(containsString("App Store로 이동")))
            .andExpect(content().string(containsString("https://apps.apple.com/kr/app/id6785124295")));
    }
}
