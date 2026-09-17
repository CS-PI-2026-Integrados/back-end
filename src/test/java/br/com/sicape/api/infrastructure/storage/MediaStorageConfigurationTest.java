package br.com.sicape.api.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import br.com.sicape.api.application.common.storage.FileStorage;

class MediaStorageConfigurationTest {
    @TempDir Path root;

    private ApplicationContextRunner context() {
        return new ApplicationContextRunner().withUserConfiguration(MediaStorageConfiguration.class)
            .withPropertyValues("media.storage.path=" + root.resolve("media"));
    }

    @Test
    void createsGenericLocalAdapter() {
        context().run(context -> assertThat(context).hasSingleBean(FileStorage.class));
    }

    @Test
    void rejectsProvidersNotYetImplemented() {
        context().withPropertyValues("media.storage.provider=s3")
            .run(context -> assertThat(context).hasFailed());
    }
}
