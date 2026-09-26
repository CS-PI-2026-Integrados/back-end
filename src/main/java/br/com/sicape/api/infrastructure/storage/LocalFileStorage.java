package br.com.sicape.api.infrastructure.storage;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

import br.com.sicape.api.application.common.storage.FileStorage;
import br.com.sicape.api.application.common.storage.FileStorageException;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class LocalFileStorage implements FileStorage {
    private static final Pattern KEYS = Pattern.compile(
        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
    );

    private final Path root;

    public LocalFileStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
        try {
            rejectSymlinks(this.root);
            Files.createDirectories(this.root);
        } catch (IOException exception) {
            throw failure(exception);
        }
    }

    public Path root() {
        return root;
    }

    @Override
    public void save(String key, byte[] content, String contentType) {
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Arquivo vazio");
        }
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Tipo do arquivo é obrigatório");
        }

        Path target = resolve(key);
        Path temporary = null;
        Path lock = target.resolveSibling(target.getFileName() + ".lock");
        boolean ownsLock = false;
        try {
            rejectSymlinks(target);
            Files.createFile(lock);
            ownsLock = true;
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new FileAlreadyExistsException("Arquivo já existe");
            }
            temporary = Files.createTempFile(root, ".upload-", ".tmp");
            Files.write(temporary, content);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target);
            }
        } catch (IOException exception) {
            throw failure(exception);
        } finally {
            cleanup(temporary);
            if (ownsLock) {
                cleanup(lock);
            }
        }
    }

    @Override
    public byte[] read(String key) {
        try {
            return Files.readAllBytes(resolve(key));
        } catch (NoSuchFileException exception) {
            throw new ResourceNotFoundException("Arquivo não encontrado.");
        } catch (IOException exception) {
            throw failure(exception);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException exception) {
            throw failure(exception);
        }
    }

    private Path resolve(String key) {
        if (key == null || !KEYS.matcher(key).matches()) {
            throw new IllegalArgumentException("Referência de arquivo inválida");
        }
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("Referência fora do diretório de storage");
        }
        rejectSymlinks(target);
        return target;
    }

    private static void rejectSymlinks(Path path) {
        for (Path current = path; current != null; current = current.getParent()) {
            if (Files.isSymbolicLink(current)) {
                throw new IllegalArgumentException("Links simbólicos não são permitidos no storage");
            }
        }
    }

    private static void cleanup(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.error("Falha ao limpar arquivo temporário; errorType={}", exception.getClass().getSimpleName());
        }
    }

    private static FileStorageException failure(IOException cause) {
        return new FileStorageException("Falha no armazenamento de arquivo", cause);
    }
}
