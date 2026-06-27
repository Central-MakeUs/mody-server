package cmc.mody.common.id;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "id")
public class IdProperties {
    private static final long DEFAULT_EPOCH_SECOND = 1_767_225_600L;

    private long epochSecond = DEFAULT_EPOCH_SECOND;
    private long nodeId = 1L;

    public long getEpochSecond() {
        return epochSecond;
    }

    public void setEpochSecond(long epochSecond) {
        this.epochSecond = epochSecond;
    }

    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }
}
