package cmc.mody.share.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UniversalLinkController.class)
@TestPropertySource(properties = "invite.aasa-resource-path=universal-link/apple-app-site-association-prod")
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
}
