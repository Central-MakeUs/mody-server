package cmc.mody.notification.infrastructure.fcm;

import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.FileInputStream;
import java.io.IOException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "fcm", name = "enabled", havingValue = "true")
public class FirebaseInitializer {
    private final FcmProperties fcmProperties;

    public FirebaseInitializer(FcmProperties fcmProperties) {
        this.fcmProperties = fcmProperties;
        initialize();
    }

    private void initialize() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        String credentialsPath = fcmProperties.getCredentialsPath();
        if (credentialsPath == null || credentialsPath.isBlank()) {
            throw new GeneralException(ErrorStatus.NOTIFICATION_PUSH_CONFIG_INVALID);
        }

        try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
            FirebaseApp.initializeApp(options);
        } catch (IOException exception) {
            throw new GeneralException(ErrorStatus.NOTIFICATION_PUSH_CONFIG_INVALID);
        }
    }
}
