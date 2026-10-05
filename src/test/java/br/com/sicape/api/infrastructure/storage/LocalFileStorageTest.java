package br.com.sicape.api.infrastructure.storage;

import static org.assertj.core.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import br.com.sicape.api.application.common.storage.FileStorageException;

class LocalFileStorageTest {
    @TempDir Path root;

    @Test
    void savesExactBytesAndDeletesIdempotently() {
        var storage = new LocalFileStorageImpl(root);
        String key = "photo/" + UUID.randomUUID();
        byte[] bytes = {1, 3, 5, 7};
        storage.save(key, bytes, "image/jpeg");
        assertThat(storage.read(key)).containsExactly(bytes);
        storage.delete(key);
        storage.delete(key);
        assertThat(Files.exists(root.resolve(key))).isFalse();
    }

    @Test
    void neverOverwrites() {
        String key = "receipt/" + UUID.randomUUID();
        var first = new LocalFileStorageImpl(root);
        var second = new LocalFileStorageImpl(root);
        first.save(key, new byte[]{1}, "image/jpeg");
        assertThatThrownBy(() -> second.save(key, new byte[]{2}, "image/jpeg"))
            .isInstanceOf(FileStorageException.class);
        assertThat(first.read(key)).containsExactly((byte) 1);
    }

    @Test
    void concurrentWritersCannotReplaceTheWinner() throws Exception {
        String key = "photo/" + UUID.randomUUID();
        var first = new LocalFileStorageImpl(root);
        var second = new LocalFileStorageImpl(root);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> saveAfter(start, first, key, (byte) 1));
            var b = pool.submit(() -> saveAfter(start, second, key, (byte) 2));
            start.countDown();
            boolean firstWon = a.get(5, TimeUnit.SECONDS);
            boolean secondWon = b.get(5, TimeUnit.SECONDS);
            assertThat(firstWon).isNotEqualTo(secondWon);
            assertThat(first.read(key)).containsExactly(firstWon ? (byte) 1 : (byte) 2);
        }
    }

    private boolean saveAfter(CountDownLatch start, LocalFileStorageImpl storage, String key, byte value)
        throws InterruptedException {
        start.await();
        try {
            storage.save(key, new byte[]{value}, "image/jpeg");
            return true;
        } catch (FileStorageException exception) {
            return false;
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"../photo.jpg", "/tmp/photo.jpg", "C:\\photo.jpg", "attendance/../receipt.pdf",
        "attendance/%2e%2e/photo.jpg", "attendance\\fake\\photo.jpg", "", "invalid",
        "media/00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000001"})
    void rejectsUnsafeKeysForAllOperations(String key) {
        var storage = new LocalFileStorageImpl(root);
        assertThatIllegalArgumentException().isThrownBy(() -> storage.save(key, new byte[]{1}, "image/jpeg"));
        assertThatIllegalArgumentException().isThrownBy(() -> storage.delete(key));
        assertThatIllegalArgumentException().isThrownBy(() -> storage.read(key));
    }
}
