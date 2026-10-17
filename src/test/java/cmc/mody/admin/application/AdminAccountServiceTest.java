package cmc.mody.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cmc.mody.admin.domain.AdminAccount;
import cmc.mody.admin.domain.AdminRole;
import cmc.mody.admin.infrastructure.AdminAccountRepository;
import cmc.mody.common.api.exception.GeneralException;
import cmc.mody.common.id.IdGenerator;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

class AdminAccountServiceTest {
    private static final String SUBJECT = "113337070957860701927";
    private final AdminAccountRepository repository = mock(AdminAccountRepository.class);
    private final IdGenerator ids = () -> 1L;
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final AdminAccountService service = new AdminAccountService(repository, ids, "ops@example.com",
        transactions);

    @Test
    void concurrentFirstLoginReusesAccountCreatedByTheOtherRequest() {
        when(transactions.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        AdminAccount created = new AdminAccount(2L, SUBJECT, "ops@example.com", AdminRole.ADMIN);
        when(repository.findByGoogleSubject(SUBJECT)).thenReturn(Optional.empty(), Optional.of(created));
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        assertThat(service.login(identity("ops@example.com"))).isSameAs(created);
    }

    @Test
    void duplicateWithoutVisibleAccountStillFailsAndUnknownEmailIsForbidden() {
        when(transactions.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(repository.findByGoogleSubject(SUBJECT)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        assertThatThrownBy(() -> service.login(identity("ops@example.com")))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> service.login(identity("stranger@example.com")))
            .isInstanceOf(GeneralException.class);
    }

    private AdminGoogleIdentityVerifier.Identity identity(String email) {
        return new AdminGoogleIdentityVerifier.Identity(SUBJECT, email, true);
    }
}
