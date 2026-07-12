package cmc.mody.grouping.domain;

import cmc.mody.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "mody_group",
        indexes = {
                @Index(name = "idx_mody_group_code", columnList = "code", unique = true)
        }
)
public class ModyGroup extends BaseEntity {
    @Column(nullable = false, length = 8)
    private String code;

    @Column(nullable = false, length = 30)
    private String name;

    public ModyGroup(Long id, String code, String name) {
        super(id);
        this.code = code;
        this.name = name;
    }
}
