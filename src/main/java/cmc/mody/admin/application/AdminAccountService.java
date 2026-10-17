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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AdminAccountService {
    private final AdminAccountRepository repository;
    private final IdGenerator idGenerator;
    private final Set<String> bootstrapEmails;
    private final TransactionTemplate transactions;

    public AdminAccountService(AdminAccountRepository repository, IdGenerator idGenerator,
        @Value("${admin.bootstrap-emails:}") String emails, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.transactions = new TransactionTemplate(transactionManager);
        this.bootstrapEmails = Arrays.stream(emails.split(","))
            .map(String::trim).filter(email -> !email.isEmpty())
            .map(email -> email.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 처음 로그인하는 부트스트랩 관리자는 계정을 만든다. 같은 계정의 로그인 요청이 동시에 들어와
     * 한쪽이 유일 키 충돌로 실패하면, 먼저 만들어진 계정을 다시 읽어 로그인시킨다(#228).
     */
    public AdminAccount login(AdminGoogleIdentityVerifier.Identity identity) {
        if (identity.subject() == null || identity.subject().isBlank() || identity.email() == null) {
            throw new GeneralException(ErrorStatus.UNAUTHORIZED);
        }
        return repository.findByGoogleSubject(identity.subject()).map(this::requireActive).orElseGet(() -> {
            if (!identity.authoritativeEmail()
                || !bootstrapEmails.contains(identity.email().toLowerCase(Locale.ROOT))) {
                throw new GeneralException(ErrorStatus.FORBIDDEN);
            }
            try {
                return transactions.execute(status -> repository.saveAndFlush(new AdminAccount(
                    idGenerator.nextId(), identity.subject(), identity.email(), AdminRole.ADMIN)));
            } catch (DataIntegrityViolationException duplicate) {
                return repository.findByGoogleSubject(identity.subject()).map(this::requireActive)
                    .orElseThrow(() -> duplicate);
            }
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
