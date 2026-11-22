package cmc.mody.common.image;

import cmc.mody.common.upload.ImageUrlResolver;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 사진의 축소본 URL을 찾는다. 준비되지 않은 사진은 결과에 없고, 호출 쪽은 원본 URL을 쓴다.
 * READY가 된 축소본은 다시 바뀌지 않으므로 메모리에 캐시한다. 아직 없는 사진만 DB에서 다시 찾는다.
 */
@Component
@RequiredArgsConstructor
public class ImageVariantLookup {
    private static final int CACHE_SIZE = 20_000;

    private final ImageVariantRepository imageVariantRepository;
    private final ImageUrlResolver imageUrlResolver;
    private final Map<String, ImageVariantUrls> readyCache = Collections.synchronizedMap(
        new LinkedHashMap<>(256, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ImageVariantUrls> eldest) {
                return size() > CACHE_SIZE;
            }
        }
    );

    public Map<String, ImageVariantUrls> findReady(Collection<String> imageKeys) {
        Map<String, ImageVariantUrls> result = new HashMap<>();
        List<String> misses = new ArrayList<>();
        for (String key : imageKeys) {
            if (!ImageVariantRequestService.isStoredImageKey(key) || result.containsKey(key)) {
                continue;
            }
            ImageVariantUrls cached = readyCache.get(key);
            if (cached != null) {
                result.put(key, cached);
            } else if (!misses.contains(key)) {
                misses.add(key);
            }
        }
        if (!misses.isEmpty()) {
            for (ImageVariant variant : imageVariantRepository.findByImageKeyInAndVariantStatus(
                misses,
                ImageVariantStatus.READY
            )) {
                ImageVariantUrls urls = new ImageVariantUrls(
                    imageUrlResolver.resolve(variant.getThumbnailKey()),
                    imageUrlResolver.resolve(variant.getDisplayKey())
                );
                readyCache.put(variant.getImageKey(), urls);
                result.put(variant.getImageKey(), urls);
            }
        }
        return result;
    }

    public ImageVariantUrls find(String imageKey) {
        if (imageKey == null) {
            return ImageVariantUrls.NONE;
        }
        return of(findReady(List.of(imageKey)), imageKey);
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
