package cmc.mody.challenge.domain;

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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "challenge")
public class Challenge extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_type", nullable = false, length = 20)
    private ChallengeType challengeType;

    @Column(nullable = false, length = 50)
    private String title;

    @Column(length = 500)
    private String description;

    public Challenge(Long id, ChallengeType challengeType, String title, String description) {
        super(id);
        this.challengeType = challengeType;
        this.title = title;
        this.description = description;
    }
}
