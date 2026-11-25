package cmc.mody.admin.infrastructure;

import cmc.mody.admin.domain.AdminAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, Long> {
    Optional<AdminAccount> findByGoogleSubject(String googleSubject);

    Optional<AdminAccount> findBySlackUserId(String slackUserId);

    Optional<AdminAccount> findFirstByEmailAndSlackUserIdIsNotNull(String email);
}
