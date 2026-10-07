package br.com.sicape.api.application.convicted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.com.sicape.api.application.common.media.MediaAssetStore;
import br.com.sicape.api.application.common.media.MediaContent;
import br.com.sicape.api.application.common.validation.PhotoValidator;
import br.com.sicape.api.application.convicted.dto.request.ConvictedProcessRequest;
import br.com.sicape.api.application.convicted.dto.request.UpdateConvictedRequest;
import br.com.sicape.api.application.convicted.service.ConvictedFinder;
import br.com.sicape.api.application.convicted.usecase.GetConvictedPhotoUseCase;
import br.com.sicape.api.application.convicted.usecase.GetConvictedUseCase;
import br.com.sicape.api.application.convicted.usecase.UpdateConvictedPhotoUseCase;
import br.com.sicape.api.application.convicted.usecase.UpdateConvictedStatusUseCase;
import br.com.sicape.api.application.convicted.usecase.UpdateConvictedUseCase;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.ConvictedProcess;
import br.com.sicape.api.domain.entity.JudicialDistrict;
import br.com.sicape.api.domain.entity.JudicialProcess;
import br.com.sicape.api.domain.entity.MediaAsset;
import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.enums.ConvictedStatus;
import br.com.sicape.api.domain.enums.MediaAssetKind;
import br.com.sicape.api.domain.enums.ProcessStatus;
import br.com.sicape.api.domain.enums.UserRole;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.valueobject.Address;
import br.com.sicape.api.domain.valueobject.Cpf;
import br.com.sicape.api.domain.valueobject.Phone;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@DataJpaTest(showSql = false, properties = {
    "spring.datasource.url=jdbc:h2:mem:convicted-status;MODE=MySQL;NON_KEYWORDS=USER,VALUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ConvictedFinder.class, UpdateConvictedStatusUseCase.class, UpdateConvictedUseCase.class,
    UpdateConvictedPhotoUseCase.class, GetConvictedUseCase.class, GetConvictedPhotoUseCase.class, PhotoValidator.class})
class ConvictedStatusIntegrationTest {
    @PersistenceContext private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ConvictedRepository repository;
    @Autowired private ConvictedFinder finder;
    @Autowired private UpdateConvictedStatusUseCase updateStatus;
    @Autowired private UpdateConvictedUseCase update;
    @Autowired private UpdateConvictedPhotoUseCase updatePhoto;
    @Autowired private GetConvictedUseCase get;
    @Autowired private GetConvictedPhotoUseCase getPhoto;
    @MockitoBean private MediaAssetStore media;

    private TransactionTemplate transactions;
    private AuthContext auth;
    private UUID id;

    @BeforeEach
    void setUp() {
        transactions = new TransactionTemplate(transactionManager);
        transactions.executeWithoutResult(ignored -> {
            var district = new JudicialDistrict();
            district.setName("Comarca de teste");
            entityManager.persist(district);
            var user = new User("Operador", Cpf.of("11144477735"), "status@test.local", "hash", UserRole.OPERATOR, district);
            entityManager.persist(user);
            auth = new AuthContext(user, district, null);
            var convicted = new Convicted("Apenado", Cpf.of("52998224725"), LocalDate.of(1990, 1, 1),
                Phone.of("11912345678"), new Address("12345678", "Rua", "10", null, "Centro", "Cidade", "SP"),
                null, district);
            entityManager.persist(convicted);
            entityManager.flush();
            id = convicted.getUuid();
            entityManager.clear();
        });
    }

    @AfterEach
    void cleanCommittedConcurrencyFixture() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            transactions.executeWithoutResult(ignored -> {
                repository.findByUuid(id).ifPresent(entityManager::remove);
                entityManager.flush();
                entityManager.remove(entityManager.find(User.class, auth.user().getId()));
                entityManager.flush();
                entityManager.remove(entityManager.find(JudicialDistrict.class, auth.district().getId()));
            });
        }
    }

    @Test
    void persistsStatusAuditAndAllowsEditingAfterReactivation() {
        assertThat(updateStatus.execute(id, ConvictedStatus.INACTIVE, auth).status()).isEqualTo(ConvictedStatus.INACTIVE);
        entityManager.flush();
        entityManager.clear();
        var inactive = repository.findByUuid(id).orElseThrow();
        var timestamp = inactive.getDeactivatedAt();
        var updatedAt = inactive.getUpdatedAt();
        assertThat(timestamp).isNotNull();
        assertThat(inactive.getDeactivatedBy().getUuid()).isEqualTo(auth.user().getUuid());

        updateStatus.execute(id, ConvictedStatus.INACTIVE, auth);
        entityManager.flush();
        entityManager.clear();
        inactive = repository.findByUuid(id).orElseThrow();
        assertThat(inactive.getDeactivatedAt()).isEqualTo(timestamp);
        assertThat(inactive.getUpdatedAt()).isEqualTo(updatedAt);

        updateStatus.execute(id, ConvictedStatus.ACTIVE, auth);
        var request = new UpdateConvictedRequest();
        request.setName("Nome atualizado");
        update.execute(id, request, auth);
        entityManager.flush();
        entityManager.clear();
        var active = repository.findByUuid(id).orElseThrow();
        assertThat(active.getStatus()).isEqualTo(ConvictedStatus.ACTIVE);
        assertThat(active.getDeactivatedAt()).isNull();
        assertThat(active.getDeactivatedBy()).isNull();
        assertThat(active.getName()).isEqualTo("Nome atualizado");
    }

    @Test
    void readsInactiveDetailsAndPhotoButKeepsActiveOnlyLookupRestricted() {
        var convicted = repository.findByUuid(id).orElseThrow();
        UUID photoId = UUID.randomUUID();
        var photo = new MediaAsset(photoId, photoId.toString(), "image/jpeg", 3, MediaAssetKind.PHOTO);
        entityManager.persist(photo);
        convicted.completePhoto(photo);
        updateStatus.execute(id, ConvictedStatus.INACTIVE, auth);
        var content = new MediaContent(new byte[]{1, 2, 3}, "image/jpeg");
        when(media.read(photo)).thenReturn(content);

        assertThat(get.execute(id, auth).status()).isEqualTo(ConvictedStatus.INACTIVE);
        assertThat(getPhoto.execute(id, auth)).isSameAs(content);
        assertThatThrownBy(() -> finder.findActive(id, auth)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsInactiveDataProcessesAndPhotoWithoutWrites() {
        updateStatus.execute(id, ConvictedStatus.INACTIVE, auth);
        var dataRequest = new UpdateConvictedRequest();
        dataRequest.setName("Nome indevido");
        var processesRequest = new UpdateConvictedRequest();
        processesRequest.setProcesses(List.of(new ConvictedProcessRequest(UUID.randomUUID(), true)));

        for (var request : List.of(new UpdateConvictedRequest(), dataRequest, processesRequest)) {
            assertThatThrownBy(() -> update.execute(id, request, auth)).isInstanceOf(ConflictException.class);
        }
        assertThatThrownBy(() -> updatePhoto.execute(id, new byte[]{1, 2, 3}, "image/jpeg", auth))
            .isInstanceOf(ConflictException.class);
        verifyNoInteractions(media);
        entityManager.flush();
        entityManager.clear();
        var convicted = repository.findByUuid(id).orElseThrow();
        assertThat(convicted.getName()).isEqualTo("Apenado");
        assertThat(convicted.getProcesses()).isEmpty();
        assertThat(convicted.getPhoto()).isNull();
    }

    @Test
    void rejectsDeactivationWithActiveProcessWithoutPersistingChanges() {
        var convicted = repository.findByUuid(id).orElseThrow();
        var process = new JudicialProcess("0001234-56.2026.8.26.0001", ProcessStatus.ACTIVE, auth.district());
        entityManager.persist(process);
        convicted.replaceProcesses(List.of(new ConvictedProcess(convicted, process, true)));
        entityManager.flush();

        assertThatThrownBy(() -> updateStatus.execute(id, ConvictedStatus.INACTIVE, auth))
            .isInstanceOf(ConflictException.class);
        entityManager.clear();
        convicted = repository.findByUuid(id).orElseThrow();
        assertThat(convicted.getStatus()).isEqualTo(ConvictedStatus.ACTIVE);
        assertThat(convicted.getDeactivatedAt()).isNull();
        assertThat(convicted.getDeactivatedBy()).isNull();
    }

    @Test
    void doesNotExposeOrChangeConvictedFromAnotherDistrictOrMissingUuid() {
        var foreignDistrict = new JudicialDistrict();
        foreignDistrict.setName("Outra comarca");
        entityManager.persist(foreignDistrict);
        var foreignAuth = new AuthContext(auth.user(), foreignDistrict, null);
        for (var context : List.of(auth, foreignAuth)) {
            UUID requestedId = context == auth ? UUID.randomUUID() : id;
            assertThatThrownBy(() -> get.execute(requestedId, context)).isInstanceOf(ResourceNotFoundException.class);
            assertThatThrownBy(() -> getPhoto.execute(requestedId, context)).isInstanceOf(ResourceNotFoundException.class);
            assertThatThrownBy(() -> updateStatus.execute(requestedId, ConvictedStatus.INACTIVE, context))
                .isInstanceOf(ResourceNotFoundException.class);
            assertThatThrownBy(() -> update.execute(requestedId, new UpdateConvictedRequest(), context))
                .isInstanceOf(ResourceNotFoundException.class);
        }
        verifyNoInteractions(media);
        assertThat(repository.findByUuid(id).orElseThrow().getStatus()).isEqualTo(ConvictedStatus.ACTIVE);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Timeout(30)
    void serializesEditsAndDeactivationInBothOrders() throws Exception {
        assertSerializes(false);
        updateStatus.execute(id, ConvictedStatus.ACTIVE, auth);
        assertSerializes(true);
        assertThat(get.execute(id, auth).name()).isEqualTo("Edição anterior à inativação");
    }

    private void assertSerializes(boolean deactivateFirst) throws Exception {
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var waiting = new CountDownLatch(1);
        var request = new UpdateConvictedRequest();
        request.setName(deactivateFirst ? "Edição proibida" : "Edição anterior à inativação");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var holder = executor.submit(() -> transactions.executeWithoutResult(ignored -> {
                finder.findForUpdate(id, auth);
                if (deactivateFirst) {
                    updateStatus.execute(id, ConvictedStatus.INACTIVE, auth);
                } else {
                    update.execute(id, request, auth);
                }
                locked.countDown();
                await(release);
            }));
            try {
                assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
                var waiter = executor.submit(() -> {
                    waiting.countDown();
                    return deactivateFirst ? update.execute(id, request, auth)
                        : updateStatus.execute(id, ConvictedStatus.INACTIVE, auth);
                });
                assertThat(waiting.await(5, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> waiter.get(250, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                release.countDown();
                holder.get(5, TimeUnit.SECONDS);
                if (deactivateFirst) {
                    assertThatThrownBy(() -> waiter.get(5, TimeUnit.SECONDS)).hasRootCauseInstanceOf(ConflictException.class);
                } else {
                    assertThat(waiter.get(5, TimeUnit.SECONDS).status()).isEqualTo(ConvictedStatus.INACTIVE);
                }
            } finally {
                release.countDown();
            }
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Tempo esgotado aguardando liberação da transação");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
