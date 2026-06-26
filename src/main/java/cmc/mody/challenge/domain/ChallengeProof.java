package cmc.mody.challenge.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "challenge_proof",
        indexes = {
                @Index(name = "idx_challenge_proof_challenge", columnList = "group_challenge_id"),
                @Index(name = "idx_challenge_proof_member", columnList = "member_id")
        }
)
public class ChallengeProof extends BaseEntity {
    @Column(name = "group_challenge_id", nullable = false)
    private Long groupChallengeId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "image_key", nullable = false, length = 500)
    private String imageKey;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    public ChallengeProof(Long id, Long groupChallengeId, Long memberId, String imageKey, LocalDateTime uploadedAt) {
        super(id);
        this.groupChallengeId = groupChallengeId;
        this.memberId = memberId;
        this.imageKey = imageKey;
        this.uploadedAt = uploadedAt;
    }
}
