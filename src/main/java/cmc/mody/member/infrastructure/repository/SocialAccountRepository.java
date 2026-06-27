package cmc.mody.member.infrastructure.repository;

import cmc.mody.member.domain.LoginType;
import cmc.mody.member.domain.SocialAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    Optional<SocialAccount> findByLoginTypeAndProviderUserId(LoginType loginType, String providerUserId);
}
