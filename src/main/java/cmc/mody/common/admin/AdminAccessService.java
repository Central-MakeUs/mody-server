package cmc.mody.common.admin;

import cmc.mody.admin.application.AdminSessionService;
import cmc.mody.admin.domain.AdminAccount;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AdminAccessService {
    private final byte[] apiKey;

    public AdminAccessService(@Value("${admin.api-key:}") String apiKey) {
        this.apiKey = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    public void validate(String requestApiKey) {
        if (requestApiKey == null
            && RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
            && attributes.getRequest().getAttribute(AdminSessionService.PRINCIPAL_ATTRIBUTE) instanceof AdminAccount) {
            return;
        }
        if (apiKey.length == 0
            || !StringUtils.hasText(requestApiKey)
            || !MessageDigest.isEqual(apiKey, requestApiKey.getBytes(StandardCharsets.UTF_8))) {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }
    }
}
