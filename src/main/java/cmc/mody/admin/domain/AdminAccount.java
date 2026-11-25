package cmc.mody.admin.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "admin_account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminAccount extends BaseEntity {
    @Column(nullable = false, unique = true, length = 255)
    private String googleSubject;

    @Column(nullable = false, length = 320)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminRole role;

    /** Slack에서 신고를 처리할 때 이 계정으로 볼 Slack 사용자 ID. 연결은 SQL로 한다(LLD-0040). */
    @Column(name = "slack_user_id", unique = true, length = 32)
    private String slackUserId;

    public AdminAccount(Long id, String googleSubject, String email, AdminRole role) {
        super(id);
        this.googleSubject = googleSubject;
        this.email = email;
        this.role = role;
    }

    public void linkSlackUser(String slackUserId) {
        this.slackUserId = slackUserId;
    }
}
