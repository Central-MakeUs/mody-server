package cmc.mody.common.upload;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ImageUrlResolverTest {

    @Test
    void resolveImageKey() {
        ImageUrlResolver resolver = resolver();

        String result = resolver.resolve("profiles/member-1.jpg");

        assertThat(result).isEqualTo("https://storage.example.com/profiles/member-1.jpg");
    }

    @Test
    void resolveExternalUrl() {
        ImageUrlResolver resolver = resolver();

        String result = resolver.resolve("https://example.com/profile.jpg");

        assertThat(result).isEqualTo("https://example.com/profile.jpg");
    }

    @Test
    void resolveBlankImageKey() {
        ImageUrlResolver resolver = resolver();

        String result = resolver.resolve(" ");

        assertThat(result).isNull();
    }

    private ImageUrlResolver resolver() {
        UploadProperties properties = new UploadProperties();
        properties.setBaseUrl("https://storage.example.com");
        return new ImageUrlResolver(properties);
    }
}
