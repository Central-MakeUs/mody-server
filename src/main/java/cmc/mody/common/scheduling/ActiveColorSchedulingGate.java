package cmc.mody.common.scheduling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 블루그린 배포 중에는 두 인스턴스가 함께 떠 있으므로, 트래픽을 받는 색상의 인스턴스에서만
 * 스케줄 작업을 실행한다. 활성 색상은 호스트가 마운트한 파일에서 매 실행마다 읽는다.
 * 파일 경로나 자기 색상이 설정되지 않은 환경(local, dev, test)에서는 항상 실행한다.
 */
@Slf4j
public class ActiveColorSchedulingGate {
    private final Path activeColorFile;
    private final String serverColor;

    public ActiveColorSchedulingGate(String activeColorFile, String serverColor) {
        this.activeColorFile = StringUtils.hasText(activeColorFile) ? Path.of(activeColorFile) : null;
        this.serverColor = StringUtils.hasText(serverColor) ? serverColor.trim() : null;
    }

    public boolean isActive() {
        if (activeColorFile == null || serverColor == null) {
            return true;
        }
        try {
            String activeColor = Files.readString(activeColorFile, StandardCharsets.UTF_8).trim();
            return serverColor.equalsIgnoreCase(activeColor);
        } catch (IOException exception) {
            // 파일을 못 읽으면 작업이 아예 멈추는 것보다 중복 실행이 낫다.
            log.warn("활성 색상 파일을 읽지 못해 스케줄 작업을 실행합니다. path={}", activeColorFile, exception);
            return true;
        }
    }

    /**
     * 실행 시점마다 활성 여부를 확인하는 작업으로 감싼다.
     * TaskDecorator로 감싸면 반복 작업의 재예약까지 건너뛰어 작업이 영구히 멈추므로,
     * 스케줄러에 등록하기 전의 작업 자체를 감싸야 한다.
     */
    public Runnable wrap(Runnable task) {
        return () -> {
            if (isActive()) {
                task.run();
            }
        };
    }
}
