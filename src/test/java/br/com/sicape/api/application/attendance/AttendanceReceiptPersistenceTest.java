package br.com.sicape.api.application.attendance;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.attendance.receipt.*;
import br.com.sicape.api.application.attendance.usecase.GetAttendanceReceiptUseCase;
import br.com.sicape.api.application.common.media.MediaAssetStore;
import br.com.sicape.api.application.common.media.MediaContent;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.*;
import br.com.sicape.api.domain.enums.*;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;
import br.com.sicape.api.domain.repository.*;
import br.com.sicape.api.domain.valueobject.*;
import jakarta.persistence.EntityManager;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:receipt-integration;MODE=MySQL;NON_KEYWORDS=USER,VALUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=chave-de-teste-com-pelo-menos-32-bytes",
    "jwt.issuer=sicape-api",
    "jwt.access-token-duration=15m"
})
@ActiveProfiles("development")
class AttendanceReceiptPersistenceTest {
    @Autowired private GetAttendanceReceiptUseCase useCase;
    @Autowired private AttendanceRepository attendances;
    @Autowired private JudicialDistrictRepository districts;
    @Autowired private JudicialProcessRepository processes;
    @Autowired private ConvictedRepository convicteds;
    @Autowired private UserRepository users;
    @Autowired private MediaAssetRepository assets;
    @Autowired private EntityManager entityManager;

    @MockitoBean private MediaAssetStore media;
    @MockitoBean private ReceiptPdfRenderer renderer;

    @Test
    @Transactional
    void persistsReceiptAndServesOnlySameDistrictWithoutRegeneration() {
        var district = new JudicialDistrict();
        district.setName("Comarca Central");
        district = districts.saveAndFlush(district);
        var otherDistrict = new JudicialDistrict();
        otherDistrict.setName("Outra Comarca");
        otherDistrict = districts.saveAndFlush(otherDistrict);
        var user = users.saveAndFlush(new User("Operador", Cpf.of("52998224725"),
            "operador-receipt@test.local", "hash", UserRole.OPERATOR, district));
        var address = new Address("12345678", "Rua Central", "10", null, "Centro", "Cidade", "SP");
        var convicted = convicteds.saveAndFlush(new Convicted("Arthur Morgan", Cpf.of("11144477735"),
            LocalDate.of(1990, 1, 1), Phone.of("11988881001"), address,
            EmploymentStatus.FORMAL_WORK, district));
        var process = processes.saveAndFlush(new JudicialProcess("TEST-" + UUID.randomUUID(),
            ProcessStatus.ACTIVE, district));
        var photo = assets.saveAndFlush(new MediaAsset(UUID.randomUUID(), UUID.randomUUID().toString(),
            "image/png", 3, MediaAssetKind.PHOTO));
        var convictedPhoto = assets.saveAndFlush(new MediaAsset(UUID.randomUUID(), UUID.randomUUID().toString(),
            "image/png", 3, MediaAssetKind.PHOTO));
        convicted.completePhoto(convictedPhoto);
        convicteds.saveAndFlush(convicted);
        var receipt = assets.saveAndFlush(new MediaAsset(UUID.randomUUID(), UUID.randomUUID().toString(),
            "application/pdf", 8, MediaAssetKind.RECEIPT));
        var attendance = attendances.saveAndFlush(new Attendance(convicted, process, district, user,
            address, Phone.of("11988881001"), EmploymentStatus.FORMAL_WORK, photo));
        var id = attendance.getUuid();
        byte[] pdf = "%PDF-1.7".getBytes();
        when(media.read(photo)).thenReturn(new MediaContent(new byte[]{1, 2, 3}, "image/png"));
        when(media.read(convictedPhoto)).thenReturn(new MediaContent(new byte[]{4, 5, 6}, "image/png"));
        when(media.read(argThat(asset -> asset != null && receipt.getUuid().equals(asset.getUuid()))))
            .thenReturn(new MediaContent(pdf, "application/pdf"));
        when(media.save(pdf, "application/pdf", MediaAssetKind.RECEIPT)).thenReturn(receipt);
        when(renderer.template()).thenReturn("<html>" + "x".repeat(2048) + "</html>");
        when(renderer.logoSha256()).thenReturn("logo-hash");
        when(renderer.render(any(), any(), any())).thenReturn(pdf);

        var first = useCase.execute(id, new AuthContext(user, district, null));
        assertThat(first.bytes()).isEqualTo(pdf);
        entityManager.clear();
        var persisted = attendances.findByUuidAndDistrict(id, district).orElseThrow();
        assertThat(persisted.getReceipt().getUuid()).isEqualTo(receipt.getUuid());
        assertThat(useCase.execute(id, new AuthContext(user, district, null)).bytes()).isEqualTo(pdf);
        var forbiddenDistrict = otherDistrict;
        assertThatThrownBy(() -> useCase.execute(id, new AuthContext(user, forbiddenDistrict, null)))
            .isInstanceOf(ResourceNotFoundException.class);
        var snapshotCaptor = ArgumentCaptor.forClass(ReceiptSnapshot.class);
        verify(renderer, times(1)).render(snapshotCaptor.capture(), any(), any());
        assertThat(snapshotCaptor.getValue().templateXhtml()).hasSizeGreaterThan(2048);
        assertThat(snapshotCaptor.getValue().logoSha256()).isEqualTo("logo-hash");
        verify(media, times(1)).save(pdf, "application/pdf", MediaAssetKind.RECEIPT);
    }
}
