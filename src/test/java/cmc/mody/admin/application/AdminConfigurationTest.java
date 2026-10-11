package cmc.mody.admin.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.FileSystemResource;

class AdminConfigurationTest {
    @Test
    void mainConfigurationPreservesSingleInstanceShutdownAndSecureSessionSettings() throws Exception {
        var resource = new FileSystemResource(Path.of("src/main/resources/application.yaml"));
        var properties = new YamlPropertySourceLoader().load("main-config", resource).getFirst();
        assertThat(properties.getProperty("server.shutdown")).isEqualTo("graceful");
        assertThat(properties.getProperty("server.servlet.session.cookie.http-only")).isEqualTo(true);
        assertThat(properties.getProperty("server.servlet.session.tracking-modes")).isEqualTo("cookie");
        assertThat(properties.getProperty("scheduling.active-color-file")).isNull();
    }
}
