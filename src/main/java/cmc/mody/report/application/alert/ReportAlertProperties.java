package cmc.mody.report.application.alert;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** 신고 접수 Slack 알림 설정(LLD-0040). 기본값은 모두 꺼짐이다. */
@Getter
@Setter
@ConfigurationProperties(prefix = "report-alert.slack")
public class ReportAlertProperties {
    private boolean enabled = false;
    private String webhookUrl = "";
    /** 콘솔 링크 앞부분(예: https://dev-mody.store). 비어 있으면 링크를 넣지 않는다. */
    private String consoleBaseUrl = "";
    /** 같은 대상의 알림을 한 번으로 묶는 시간. */
    private Duration groupWindow = Duration.ofMinutes(10);
    /** Slack 봇이 버튼을 받을 준비가 됐을 때만 켠다. */
    private boolean buttonsEnabled = false;

    public boolean isActive() {
        return enabled && StringUtils.hasText(webhookUrl);
    }
}
