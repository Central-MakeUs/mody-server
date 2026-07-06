package cmc.mody.common.upload;

public interface ImageObjectStorage {
    boolean exists(String imageKey);

    byte[] read(String imageKey);

    void write(String imageKey, byte[] bytes, String contentType);

    String toUrl(String imageKey);
}
