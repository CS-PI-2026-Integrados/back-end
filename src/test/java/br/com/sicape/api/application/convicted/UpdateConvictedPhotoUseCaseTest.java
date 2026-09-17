package br.com.sicape.api.application.convicted;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.sicape.api.application.common.media.MediaAssetStore;
import br.com.sicape.api.application.common.validation.PhotoValidator;
import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.convicted.usecase.UpdateConvictedPhotoUseCase;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.MediaAsset;
import br.com.sicape.api.domain.enums.EmploymentStatus;
import br.com.sicape.api.domain.enums.MediaAssetKind;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.valueobject.Address;
import br.com.sicape.api.domain.valueobject.Cpf;
import br.com.sicape.api.domain.valueobject.Phone;

class UpdateConvictedPhotoUseCaseTest {
    private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff};

    private final ConvictedFinder finder = mock(ConvictedFinder.class);
    private final ConvictedRepository repository = mock(ConvictedRepository.class);
    private final MediaAssetStore media = mock(MediaAssetStore.class);
    private final MediaAsset replaced = photo();
    private final MediaAsset created = photo();
    private final UUID convictedId = UUID.randomUUID();
    private final AuthContext auth = new AuthContext(null, new JudicialDistrict(), null);
    private Convicted convicted;
    private UpdateConvictedPhotoUseCase useCase;

    @BeforeEach
    void setUp() {
        convicted = convicted();
        convicted.completePhoto(replaced);

        when(finder.find(convictedId, auth)).thenReturn(convicted);
        when(media.save(any(), eq("image/jpeg"), eq(MediaAssetKind.PHOTO))).thenReturn(created);
        when(repository.saveAndFlush(convicted)).thenReturn(convicted);
        useCase = new UpdateConvictedPhotoUseCase(
            finder,
            repository,
            new PhotoValidator(),
            media
        );
    }

    @Test
    void delegatesReplacedMediaRemovalAfterOwnerFlush() {
        execute();

        var order = inOrder(media, repository);
        order.verify(media).save(any(), eq("image/jpeg"), eq(MediaAssetKind.PHOTO));
        order.verify(repository).saveAndFlush(convicted);
        order.verify(media).removeAfterCommit(replaced);
        verify(media, never()).deleteContent(any());
    }

    @Test
    void leavesRollbackCompensationToStoreWhenOwnerWriteFails() {
        var failure = new IllegalStateException("database failure");
        when(repository.saveAndFlush(any())).thenThrow(failure);

        assertThatThrownBy(this::execute).isSameAs(failure);
        verify(media, never()).removeAfterCommit(any());
        verify(media, never()).deleteContent(any());
    }

    @Test
    void propagatesFailureWhileSchedulingReplacedMediaRemoval() {
        var failure = new IllegalStateException("media metadata failure");
        doThrow(failure).when(media).removeAfterCommit(replaced);

        assertThatThrownBy(this::execute).isSameAs(failure);
        verify(repository).saveAndFlush(convicted);
    }

    @Test
    void doesNotScheduleRemovalWhenThereWasNoPreviousPhoto() {
        convicted = convicted();
        when(finder.find(convictedId, auth)).thenReturn(convicted);
        when(repository.saveAndFlush(convicted)).thenReturn(convicted);

        execute();

        verify(media, never()).removeAfterCommit(any());
    }

    private void execute() {
        useCase.execute(convictedId, JPEG, "image/jpeg", auth);
    }

    private Convicted convicted() {
        return new Convicted(
            "Apenado",
            Cpf.of("52998224725"),
            LocalDate.of(1990, 1, 1),
            Phone.of("(11) 91234-5678"),
            new Address("12345-678", "Rua", "10", null, "Centro", "Cidade", "SP"),
            EmploymentStatus.UNEMPLOYED,
            auth.district()
        );
    }

    private static MediaAsset photo() {
        UUID uuid = UUID.randomUUID();
        return new MediaAsset(uuid, uuid.toString(), "image/jpeg", JPEG.length, MediaAssetKind.PHOTO);
    }
}
