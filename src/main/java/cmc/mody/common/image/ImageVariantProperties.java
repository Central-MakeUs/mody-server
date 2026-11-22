package cmc.mody.common.image;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "image.variant")
public class ImageVariantProperties {
    /** 생성 스케줄러 실행 여부. 꺼져 있어도 생성 요청은 쌓이고, 켜면 그때부터 처리한다. */
    private boolean enabled = false;
    private long fixedDelayMs = 5_000L;
    /** 한 번에 선점할 작업 수. 사진 디코딩은 메모리를 많이 써서 작게 둔다. */
    private int batchSize = 4;
    /** 동시에 처리할 사진 수. 힙이 작은 서버(dev 320MB)는 1로 둔다. */
    private int concurrency = 2;
    private int maxAttempts = 3;
    private long retryDelaySeconds = 60L;
    /** PROCESSING으로 남은 작업을 다시 가져갈 기준. 처리 중 서버가 재시작된 경우를 위한 것. */
    private long staleProcessingMinutes = 10L;
    private int thumbnailMaxSize = 480;
    private int displayMaxSize = 1440;
    private float jpegQuality = 0.85f;
    /** 기존 사진 중 생성 요청이 없는 것을 조금씩 채운다. 처리할 신규 작업이 없을 때만 돈다. */
    private boolean backfillEnabled = false;
    private int backfillBatchSize = 20;
    /** 생성 방식이 바뀌면 버전을 올린다. 앱이 같은 URL의 예전 축소본을 캐시해 두고 계속 보여 주는 것을 막는다. */
    private String keyPrefix = "variants/v2";
    /** 기존 imageUrl 자리에 축소본 URL을 넣는다. 앱 수정 없이 축소본을 쓰게 하는 스위치다. */
    private boolean serveInImageUrl = false;
    /** serveInImageUrl일 때 목록 카드에 줄 크기. */
    private ListSize listSize = ListSize.DISPLAY;

    public enum ListSize {
        THUMBNAIL,
        DISPLAY
    }
}
