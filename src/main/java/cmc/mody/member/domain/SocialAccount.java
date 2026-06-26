package cmc.mody.member.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "social_account",
        indexes = {
                @Index(name = "idx_social_account_provider_user", columnList = "login_type, provider_user_id")
        }
)
public class SocialAccount extends BaseEntity {
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "login_type", nullable = false, length = 20)
    private LoginType loginType;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    public SocialAccount(Long id, Long memberId, LoginType loginType, String providerUserId) {
        super(id);
        this.memberId = memberId;
        this.loginType = loginType;
        this.providerUserId = providerUserId;
    }
}
