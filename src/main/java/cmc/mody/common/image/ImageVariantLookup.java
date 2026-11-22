package cmc.mody.common.image;

import cmc.mody.common.upload.ImageUrlResolver;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 응답에 넣을 축소본 URL을 찾는다. 준비되지 않은 사진은 결과에 없고, 호출 쪽은 원본 URL을 쓴다. */
@Component
@RequiredArgsConstructor
public class ImageVariantLookup {
    private final ImageVariantRepository imageVariantRepository;
    private final ImageUrlResolver imageUrlResolver;

    @Transactional(readOnly = true)
    public Map<String, ImageVariantUrls> findReady(Collection<String> imageKeys) {
        List<String> keys = imageKeys.stream()
            .filter(ImageVariantRequestService::isStoredImageKey)
            .distinct()
            .toList();
        if (keys.isEmpty()) {
            return Map.of();
        }
        return imageVariantRepository.findByImageKeyInAndVariantStatus(keys, ImageVariantStatus.READY).stream()
            .collect(Collectors.toMap(
                ImageVariant::getImageKey,
                variant -> new ImageVariantUrls(
                    imageUrlResolver.resolve(variant.getThumbnailKey()),
                    imageUrlResolver.resolve(variant.getDisplayKey())
                ),
                (first, second) -> first
            ));
    }

    public record ImageVariantUrls(String thumbnailUrl, String displayUrl) {
        public static final ImageVariantUrls NONE = new ImageVariantUrls(null, null);
    }

    public static ImageVariantUrls of(Map<String, ImageVariantUrls> variants, String imageKey) {
        if (imageKey == null) {
            return ImageVariantUrls.NONE;
        }
        return Objects.requireNonNullElse(variants.get(imageKey), ImageVariantUrls.NONE);
    }
}
