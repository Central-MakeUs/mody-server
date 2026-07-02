package cmc.mody.member.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "member")
public class Member extends BaseEntity {
    public static final int MAX_NICKNAME_LENGTH = 14;

    @Column(nullable = false, length = MAX_NICKNAME_LENGTH)
    private String nickname;

    @Column
    private LocalDate birthDate;

    @Column(precision = 5, scale = 2)
    private BigDecimal targetWeightKg;

    @Column(name = "profile_image_key", length = 500)
    private String profileImageKey;

    @Column(name = "group_onboarding_completed", nullable = false)
    private boolean groupOnboardingCompleted = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HealthConnectionStatus healthConnectionStatus = HealthConnectionStatus.DISCONNECTED;

    public Member(Long id, String nickname, LocalDate birthDate, BigDecimal targetWeightKg) {
        super(id);
        this.nickname = nickname;
        this.birthDate = birthDate;
        this.targetWeightKg = targetWeightKg;
    }

    private Member(
        Long id,
        String nickname,
        LocalDate birthDate,
        BigDecimal targetWeightKg,
        String profileImageKey
    ) {
        this(id, nickname, birthDate, targetWeightKg);
        this.profileImageKey = profileImageKey;
    }

    public static Member oauthMember(Long id, String nickname, String profileImageUrl) {
        return new Member(id, nickname, null, null, profileImageUrl);
    }

    public void completeProfile(String nickname, LocalDate birthDate, BigDecimal targetWeightKg) {
        this.nickname = nickname;
        this.birthDate = birthDate;
        this.targetWeightKg = targetWeightKg;
    }

    public void updateTargetWeight(BigDecimal targetWeightKg) {
        this.targetWeightKg = targetWeightKg;
    }

    public void updateProfile(String nickname, LocalDate birthDate) {
        this.nickname = nickname;
        this.birthDate = birthDate;
    }

    public void updateHealthConnection(boolean connected) {
        this.healthConnectionStatus = connected
            ? HealthConnectionStatus.CONNECTED
            : HealthConnectionStatus.DISCONNECTED;
    }

    public void completeGroupOnboarding() {
        this.groupOnboardingCompleted = true;
    }

    public boolean isPersonalInfoCompleted() {
        return birthDate != null && targetWeightKg != null;
    }
}
