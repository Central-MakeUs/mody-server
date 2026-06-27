package cmc.mody.auth.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "refresh_token")
public class RefreshToken extends BaseEntity {
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 500)
    private String token;

    public RefreshToken(Long id, Long memberId, String token) {
        super(id);
        this.memberId = memberId;
        this.token = token;
    }
}
