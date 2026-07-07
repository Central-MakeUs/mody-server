package cmc.mody.share.presentation;

import io.swagger.v3.oas.annotations.Hidden;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@Hidden
@RestController
public class UniversalLinkController {
    private static final String AASA_RESOURCE_PATH = "universal-link/apple-app-site-association";
    private static final MediaType TEXT_HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);

    private final String appStoreUrl;

    public UniversalLinkController(
        @Value("${invite.app-store-url:https://www.apple.com/kr/app-store/}") String appStoreUrl
    ) {
        this.appStoreUrl = appStoreUrl;
    }

    @GetMapping(value = "/.well-known/apple-app-site-association", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Resource> appleAppSiteAssociation() {
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
            .body(new ClassPathResource(AASA_RESOURCE_PATH));
    }

    @GetMapping(value = "/invite", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> invite(@RequestParam(value = "code", required = false) String code) {
        return ResponseEntity.ok()
            .contentType(TEXT_HTML_UTF8)
            .body(invitePage(code));
    }

    private String invitePage(String code) {
        String escapedAppStoreUrl = HtmlUtils.htmlEscape(appStoreUrl);
        String escapedCode = code == null || code.isBlank()
            ? ""
            : HtmlUtils.htmlEscape(code.strip());
        String codeBlock = escapedCode.isBlank()
            ? ""
            : """
                <p class="code-label">초대 코드</p>
                <p class="code">%s</p>
                """.formatted(escapedCode);

        return """
            <!doctype html>
            <html lang="ko">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <meta http-equiv="refresh" content="1;url=%s">
              <title>Mody 초대</title>
              <style>
                body {
                  margin: 0;
                  min-height: 100vh;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
                  color: #222;
                  background: #f7f7f8;
                }
                main {
                  width: min(420px, calc(100%% - 40px));
                  text-align: center;
                }
                h1 {
                  margin: 0 0 12px;
                  font-size: 24px;
                  line-height: 1.35;
                }
                p {
                  margin: 0;
                  color: #666;
                  line-height: 1.55;
                }
                .code-label {
                  margin-top: 28px;
                  color: #888;
                  font-size: 13px;
                }
                .code {
                  margin-top: 6px;
                  color: #222;
                  font-size: 28px;
                  font-weight: 700;
                  letter-spacing: 2px;
                }
                a {
                  display: inline-flex;
                  align-items: center;
                  justify-content: center;
                  margin-top: 32px;
                  width: 100%%;
                  min-height: 52px;
                  border-radius: 8px;
                  background: #222;
                  color: #fff;
                  font-weight: 700;
                  text-decoration: none;
                }
              </style>
              <script>
                window.setTimeout(function () {
                  window.location.replace("%s");
                }, 300);
              </script>
            </head>
            <body>
              <main>
                <h1>모디 초대를 확인하고 있어요</h1>
                <p>앱이 설치되어 있지 않다면 App Store로 이동합니다.</p>
                %s
                <a href="%s">App Store로 이동</a>
              </main>
            </body>
            </html>
            """.formatted(escapedAppStoreUrl, escapedAppStoreUrl, codeBlock, escapedAppStoreUrl);
    }
}
