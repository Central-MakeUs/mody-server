package cmc.mody.common.image;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ImageVariantClaimService {
    private final ImageVariantRepository imageVariantRepository;
    private final ImageVariantProperties properties;

    @Transactional
    public List<Long> claimDue(String serverId, int batchSize) {
        LocalDateTime now = LocalDateTime.now();
        List<ImageVariant> variants = imageVariantRepository.findDueForUpdateSkipLocked(
            now,
            now.minusMinutes(properties.getStaleProcessingMinutes()),
            batchSize
        );
        variants.forEach(variant -> variant.markProcessing(serverId, now));
        return variants.stream()
            .map(ImageVariant::getId)
            .toList();
    }
}
