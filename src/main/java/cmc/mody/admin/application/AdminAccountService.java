package cmc.mody.admin.application;

import cmc.mody.admin.domain.AdminAccount;
import cmc.mody.admin.domain.AdminRole;
import cmc.mody.admin.infrastructure.AdminAccountRepository;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.api.status.ErrorStatus;
import cmc.mody.common.id.IdGenerator;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAccountService {
    private final AdminAccountRepository repository;
    private final IdGenerator idGenerator;
    private final Set<String> bootstrapEmails;

    public AdminAccountService(AdminAccountRepository repository, IdGenerator idGenerator,
        @Value("${admin.bootstrap-emails:}") String emails) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.bootstrapEmails = Arrays.stream(emails.split(","))
            .map(String::trim).filter(email -> !email.isEmpty())
            .map(email -> email.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }

    @Transactional
    public AdminAccount login(AdminGoogleIdentityVerifier.Identity identity) {
        if (identity.subject() == null || identity.subject().isBlank() || identity.email() == null) {
            throw new GeneralException(ErrorStatus.UNAUTHORIZED);
        }
        return repository.findByGoogleSubject(identity.subject()).map(this::requireActive).orElseGet(() -> {
            if (!identity.authoritativeEmail()
                || !bootstrapEmails.contains(identity.email().toLowerCase(Locale.ROOT))) {
                throw new GeneralException(ErrorStatus.FORBIDDEN);
            }
            return repository.save(new AdminAccount(idGenerator.nextId(), identity.subject(),
                identity.email(), AdminRole.ADMIN));
        });
    }

    @Transactional(readOnly = true)
    public AdminAccount getActive(Long id) {
        return requireActive(repository.findById(id)
            .orElseThrow(() -> new GeneralException(ErrorStatus.UNAUTHORIZED)));
    }

    private AdminAccount requireActive(AdminAccount account) {
        if (!account.isActive() || account.getDeletedAt() != null) {
            throw new GeneralException(ErrorStatus.FORBIDDEN);
        }
        return account;
    }
}
