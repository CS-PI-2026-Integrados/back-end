package br.com.sicape.api.application.common.storage;

public interface FileStorage {
    void save(String key, byte[] content, String contentType);
    byte[] read(String key);
    void delete(String key);
}
