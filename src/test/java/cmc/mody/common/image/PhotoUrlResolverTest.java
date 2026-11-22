package cmc.mody.common.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import cmc.mody.common.image.ImageVariantLookup.ImageVariantUrls;
import cmc.mody.common.upload.ImageUrlResolver;
import cmc.mody.common.upload.UploadProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PhotoUrlResolverTest {
    private static final String KEY = "records/1/2026/11/100.jpeg";
    private static final ImageVariantUrls READY = new ImageVariantUrls(
        "https://storage.example.com/variants/w480/records/1/2026/11/100.jpg",
        "https://storage.example.com/variants/w1440/records/1/2026/11/100.jpg"
    );

    @Mock
    private ImageVariantRepository imageVariantRepository;

    private ImageVariantProperties properties;
    private PhotoUrlResolver resolver;
    private ImageVariantLookup lookup;

    @BeforeEach
    void setUp() {
        properties = new ImageVariantProperties();
        ImageUrlResolver urls = new ImageUrlResolver(new UploadProperties());
        lookup = new ImageVariantLookup(imageVariantRepository, urls);
        resolver = new PhotoUrlResolver(urls, lookup, properties);
    }

    @Test
    @DisplayName("교체가 꺼져 있으면 축소본이 있어도 원본 URL을 준다.")
    void originalWhenServingDisabled() {
        assertThat(resolver.list(KEY, READY)).isEqualTo("https://storage.example.com/" + KEY);
        assertThat(resolver.display(KEY, READY)).isEqualTo("https://storage.example.com/" + KEY);
    }

    @Test
    @DisplayName("교체가 켜져 있으면 목록은 list-size 크기, 상세는 1440px 축소본을 준다.")
    void variantWhenServingEnabled() {
        properties.setServeInImageUrl(true);

        assertThat(resolver.list(KEY, READY)).isEqualTo(READY.displayUrl());
        properties.setListSize(ImageVariantProperties.ListSize.THUMBNAIL);
        assertThat(resolver.list(KEY, READY)).isEqualTo(READY.thumbnailUrl());
        assertThat(resolver.display(KEY, READY)).isEqualTo(READY.displayUrl());
    }

    @Test
    @DisplayName("축소본이 아직 없으면 원본 URL을 준다.")
    void originalWhenVariantMissing() {
        properties.setServeInImageUrl(true);

        assertThat(resolver.display(KEY, ImageVariantUrls.NONE)).isEqualTo("https://storage.example.com/" + KEY);
    }

    @Test
    @DisplayName("READY 축소본은 캐시해서 같은 사진을 다시 조회하지 않는다.")
    void cachesReadyVariant() {
        properties.setServeInImageUrl(true);
        ImageVariant variant = new ImageVariant(1L, KEY);
        variant.markReady(
            "variants/w480/records/1/2026/11/100.jpg",
            "variants/w1440/records/1/2026/11/100.jpg",
            1, 1, 1, null
        );
        given(imageVariantRepository.findByImageKeyInAndVariantStatus(anyCollection(), eq(ImageVariantStatus.READY)))
            .willReturn(List.of(variant));

        assertThat(resolver.display(KEY)).isEqualTo(READY.displayUrl());
        assertThat(resolver.display(KEY)).isEqualTo(READY.displayUrl());

        then(imageVariantRepository).should(times(1))
            .findByImageKeyInAndVariantStatus(anyCollection(), eq(ImageVariantStatus.READY));
    }
}
