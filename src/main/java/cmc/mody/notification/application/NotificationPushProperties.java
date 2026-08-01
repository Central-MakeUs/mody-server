package cmc.mody.notification.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "notification.push")
public class NotificationPushProperties {
    private String provider = "no-op";
    private String title = "Mody";
    private String titlePrefix = "";

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitlePrefix() {
        return titlePrefix;
    }

    public void setTitlePrefix(String titlePrefix) {
        this.titlePrefix = titlePrefix;
    }

    public String applyTitlePrefix(String title) {
        if (!StringUtils.hasText(titlePrefix)) {
            return title;
        }
        if (title != null && title.startsWith(titlePrefix)) {
            return title;
        }
        return titlePrefix + title;
    }
}
