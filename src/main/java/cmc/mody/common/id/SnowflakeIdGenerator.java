package cmc.mody.common.id;

import org.springframework.stereotype.Component;

@Component
public class SnowflakeIdGenerator implements IdGenerator {
    private static final int NODE_BITS = 8;
    private static final int SEQUENCE_BITS = 10;
    private static final long MAX_NODE_ID = (1L << NODE_BITS) - 1;
    private static final long MAX_SEQUENCE = (1L << SEQUENCE_BITS) - 1;

    private final long epochSecond;
    private final long nodeId;

    private long lastTimestampSecond = -1L;
    private long sequence = 0L;

    public SnowflakeIdGenerator(IdProperties properties) {
        validate(properties);
        this.epochSecond = properties.getEpochSecond();
        this.nodeId = properties.getNodeId();
    }

    @Override
    public synchronized Long nextId() {
        long timestampSecond = currentTimestampSecond();
        if (timestampSecond < lastTimestampSecond) {
            throw new IllegalStateException("Clock moved backwards while generating id.");
        }

        if (timestampSecond == lastTimestampSecond) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestampSecond = waitNextSecond(timestampSecond);
            }
        } else {
            sequence = 0;
        }

        lastTimestampSecond = timestampSecond;
        return ((timestampSecond - epochSecond) << (NODE_BITS + SEQUENCE_BITS))
            | (nodeId << SEQUENCE_BITS)
            | sequence;
    }

    private long currentTimestampSecond() {
        return System.currentTimeMillis() / 1000;
    }

    private long waitNextSecond(long timestampSecond) {
        long nextTimestampSecond = currentTimestampSecond();
        while (nextTimestampSecond <= timestampSecond) {
            nextTimestampSecond = currentTimestampSecond();
        }
        return nextTimestampSecond;
    }

    private void validate(IdProperties properties) {
        if (properties.getNodeId() < 0 || properties.getNodeId() > MAX_NODE_ID) {
            throw new IllegalArgumentException("id.node-id must be between 0 and " + MAX_NODE_ID + ".");
        }
        if (properties.getEpochSecond() < 0) {
            throw new IllegalArgumentException("id.epoch-second must be positive.");
        }
    }
}
