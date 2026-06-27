package cmc.mody.common.id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SnowflakeIdGeneratorTest {
    @Test
    @DisplayName("Long 타입 ID를 중복 없이 증가 방향으로 생성한다.")
    void generateUniqueIncreasingIds() {
        SnowflakeIdGenerator generator = generator(1L);

        Long first = generator.nextId();
        Long second = generator.nextId();

        assertThat(first).isPositive();
        assertThat(second).isGreaterThan(first);
    }

    @Test
    @DisplayName("같은 초 안에서도 sequence로 중복 없이 ID를 생성한다.")
    void generateUniqueIdsWithinSameSecond() {
        SnowflakeIdGenerator generator = generator(1L);
        Set<Long> ids = new HashSet<>();

        for (int i = 0; i < 512; i++) {
            ids.add(generator.nextId());
        }

        assertThat(ids).hasSize(512);
    }

    @Test
    @DisplayName("node id는 0부터 255까지만 허용한다.")
    void validateNodeIdRange() {
        IdProperties properties = properties(256L);

        assertThatThrownBy(() -> new SnowflakeIdGenerator(properties))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("id.node-id");
    }

    private SnowflakeIdGenerator generator(long nodeId) {
        return new SnowflakeIdGenerator(properties(nodeId));
    }

    private IdProperties properties(long nodeId) {
        IdProperties properties = new IdProperties();
        properties.setEpochSecond(Instant.parse("2026-01-01T00:00:00Z").getEpochSecond());
        properties.setNodeId(nodeId);
        return properties;
    }
}
