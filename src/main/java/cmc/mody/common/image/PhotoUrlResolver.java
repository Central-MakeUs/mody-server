package cmc.mody.common.image;

import cmc.mody.common.image.ImageVariantLookup.ImageVariantUrls;
import cmc.mody.common.upload.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 기록·인증 사진의 응답 URL을 정한다.
 * serve-in-image-url이 켜져 있으면 기존 imageUrl 자리에 축소본을 넣어, 앱 수정 없이 축소본을 내려보낸다.
 * 축소본이 아직 없으면 원본 URL을 쓴다. 프로필 이미지는 대상이 아니다.
 */
@Component
@RequiredArgsConstructor
public class PhotoUrlResolver {
    private final ImageUrlResolver imageUrlResolver;
    private final ImageVariantLookup imageVariantLookup;
    private final ImageVariantProperties properties;

    /** 목록 카드용. list-size 설정에 따라 목록용(480px) 또는 상세용(1440px). */
    public String list(String imageKey, ImageVariantUrls variant) {
        String preferred = properties.getListSize() == ImageVariantProperties.ListSize.THUMBNAIL
            ? variant.thumbnailUrl()
            : variant.displayUrl();
        return choose(imageKey, preferred);
    }

    /** 상세·관리 화면용(1440px). */
    public String display(String imageKey, ImageVariantUrls variant) {
        return choose(imageKey, variant.displayUrl());
    }

    public String list(String imageKey) {
        return list(imageKey, lookup(imageKey));
    }

    public String display(String imageKey) {
        return display(imageKey, lookup(imageKey));
    }

    private ImageVariantUrls lookup(String imageKey) {
        return properties.isServeInImageUrl() ? imageVariantLookup.find(imageKey) : ImageVariantUrls.NONE;
    }

    private String choose(String imageKey, String variantUrl) {
        if (properties.isServeInImageUrl() && variantUrl != null) {
            return variantUrl;
        }
        return imageUrlResolver.resolve(imageKey);
    }
}
