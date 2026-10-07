package br.com.sicape.api.infrastructure.rest.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sicape.api.application.attendance.usecase.CreateAttendanceUseCase;
import br.com.sicape.api.application.attendance.usecase.GetAttendancePhotoUseCase;
import br.com.sicape.api.application.attendance.usecase.GetAttendanceReceiptUseCase;
import br.com.sicape.api.application.attendance.usecase.GetAttendanceUseCase;
import br.com.sicape.api.application.attendance.usecase.ListAttendanceUseCase;
import br.com.sicape.api.application.common.dto.response.PageResponse;
import br.com.sicape.api.application.convicted.usecase.CreateConvictedUseCase;
import br.com.sicape.api.application.convicted.usecase.GetConvictedPhotoUseCase;
import br.com.sicape.api.application.convicted.usecase.GetConvictedUseCase;
import br.com.sicape.api.application.convicted.usecase.ListConvictedUseCase;
import br.com.sicape.api.application.convicted.usecase.UpdateConvictedStatusUseCase;
import br.com.sicape.api.application.convicted.dto.response.ConvictedResponse;
import br.com.sicape.api.application.convicted.usecase.UpdateConvictedPhotoUseCase;
import br.com.sicape.api.application.convicted.usecase.UpdateConvictedUseCase;
import br.com.sicape.api.application.process.usecase.ListProcessUseCase;
import br.com.sicape.api.application.user.usecase.CreateUserUseCase;
import br.com.sicape.api.application.user.usecase.DeleteUserUseCase;
import br.com.sicape.api.application.user.usecase.GetUserUseCase;
import br.com.sicape.api.application.user.usecase.ListUsersUseCase;
import br.com.sicape.api.application.user.usecase.UpdateUserUseCase;
import br.com.sicape.api.infrastructure.security.JwtAuthenticationFilter;
import br.com.sicape.api.infrastructure.settings.Settings;
import br.com.sicape.api.domain.enums.ConvictedStatus;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.exception.ResourceNotFoundException;

@WebMvcTest({AttendanceController.class, ConvictedController.class, ProcessController.class, UserController.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("development")
@Import(Settings.class)
@MockitoBean(types = {
    GetAttendancePhotoUseCase.class, GetAttendanceReceiptUseCase.class, GetAttendanceUseCase.class,
    GetConvictedPhotoUseCase.class, GetConvictedUseCase.class,
    UpdateConvictedPhotoUseCase.class, UpdateConvictedUseCase.class,
    GetUserUseCase.class, DeleteUserUseCase.class, JwtAuthenticationFilter.class
})
class ListControllersTest {
    private static final List<String> PATHS = List.of("/attendance", "/convicted", "/processes", "/users", "/usuarios");

    @Autowired private MockMvc mvc;
    @MockitoBean private ListAttendanceUseCase listAttendance;
    @MockitoBean private ListConvictedUseCase listConvicted;
    @MockitoBean private ListProcessUseCase listProcess;
    @MockitoBean private ListUsersUseCase listUsers;
    @MockitoBean private CreateAttendanceUseCase createAttendance;
    @MockitoBean private CreateConvictedUseCase createConvicted;
    @MockitoBean private CreateUserUseCase createUser;
    @MockitoBean private UpdateUserUseCase updateUser;
    @MockitoBean private UpdateConvictedStatusUseCase updateConvictedStatus;

    @BeforeEach
    void stubLists() {
        when(listAttendance.execute(nullable(String.class), anyInt(), anyInt(), any()))
            .thenAnswer(invocation -> emptyPage(invocation.getArgument(1), invocation.getArgument(2)));
        when(listConvicted.execute(nullable(String.class), nullable(ConvictedStatus.class), anyInt(), anyInt(), any()))
            .thenAnswer(invocation -> emptyPage(invocation.getArgument(2), invocation.getArgument(3)));
        when(listProcess.execute(nullable(String.class), anyInt(), anyInt(), any()))
            .thenAnswer(invocation -> emptyPage(invocation.getArgument(1), invocation.getArgument(2)));
        when(listUsers.execute(nullable(String.class), anyInt(), anyInt(), any()))
            .thenAnswer(invocation -> emptyPage(invocation.getArgument(1), invocation.getArgument(2)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/attendance", "/convicted", "/processes", "/users", "/usuarios"})
    void usesSamePaginationDefaultsAndResponse(String path) throws Exception {
        mvc.perform(get(path))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.total_elements").value(0))
            .andExpect(jsonPath("$.total_pages").value(0));
        verifyList(path, null, 0, 20);
    }

    @ParameterizedTest
    @MethodSource("invalidPagination")
    void rejectsInvalidPaginationWithFieldErrorBeforeCallingUseCase(String path, String field, String value) throws Exception {
        mvc.perform(get(path).param(field, value))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(422))
            .andExpect(jsonPath("$.fields[*].field", hasItem(field)));
        verifyNoInteractions(listAttendance, listConvicted, listProcess, listUsers);
    }

    static Stream<Arguments> invalidPagination() {
        return PATHS.stream().flatMap(path -> Stream.of(
            Arguments.of(path, "page", "-1"), Arguments.of(path, "size", "0"),
            Arguments.of(path, "size", "-1"), Arguments.of(path, "size", "101"),
            Arguments.of(path, "page", "abc"), Arguments.of(path, "size", "abc")
        ));
    }

    @ParameterizedTest
    @MethodSource("paginationBounds")
    void acceptsPaginationBoundsAndForwardsSearch(String path, int page, int size) throws Exception {
        mvc.perform(get(path).param("search", "Silva").param("page", Integer.toString(page)).param("size", Integer.toString(size)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(page))
            .andExpect(jsonPath("$.size").value(size));
        verifyList(path, "Silva", page, size);
    }

    static Stream<Arguments> paginationBounds() {
        return PATHS.stream().flatMap(path -> Stream.of(Arguments.of(path, 0, 1), Arguments.of(path, 1, 100)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "INACTIVE"})
    void forwardsConvictedStatusAndSearch(String convictedStatus) throws Exception {
        mvc.perform(get("/convicted").param("status", convictedStatus).param("search", "Silva"))
            .andExpect(status().isOk());
        verify(listConvicted).execute("Silva", ConvictedStatus.valueOf(convictedStatus), 0, 20, null);
    }

    @Test
    void rejectsInvalidConvictedStatus() throws Exception {
        mvc.perform(get("/convicted").param("status", "INVALID"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.fields[*].field", hasItem("status")));
        verifyNoInteractions(listConvicted);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACTIVE", "INACTIVE"})
    void updatesConvictedStatusUsingOnlyUrl(String requestedStatus) throws Exception {
        UUID id = UUID.randomUUID();
        var convictedStatus = ConvictedStatus.valueOf(requestedStatus);
        when(updateConvictedStatus.execute(id, convictedStatus, null)).thenReturn(
            new ConvictedResponse(id, "Apenado", "52998224725", null, null, null, null, convictedStatus, List.of()));

        mvc.perform(put("/convicted/{uuid}/status/{status}", id, requestedStatus))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.status").value(requestedStatus));
        verify(updateConvictedStatus).execute(id, convictedStatus, null);
    }

    @Test
    void rejectsInvalidStatusInUrl() throws Exception {
        mvc.perform(put("/convicted/{uuid}/status/INVALID", UUID.randomUUID()))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.fields[*].field", hasItem("status")));
        verifyNoInteractions(updateConvictedStatus);
    }

    @Test
    void rejectsRemovedConvictedDeleteRoute() throws Exception {
        mvc.perform(delete("/convicted/{uuid}", UUID.randomUUID()))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(header().string("Allow", containsString("PUT")));
        verifyNoInteractions(updateConvictedStatus);
    }

    @Test
    void returnsConflictWhenDeactivationIsBlocked() throws Exception {
        UUID id = UUID.randomUUID();
        when(updateConvictedStatus.execute(id, ConvictedStatus.INACTIVE, null))
            .thenThrow(new ConflictException("status", "O apenado possui processo ativo."));
        mvc.perform(put("/convicted/{uuid}/status/INACTIVE", id))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.fields[*].field", hasItem("status")));
    }

    @Test
    void returnsNotFoundForUnavailableConvicted() throws Exception {
        UUID id = UUID.randomUUID();
        when(updateConvictedStatus.execute(id, ConvictedStatus.ACTIVE, null))
            .thenThrow(new ResourceNotFoundException("Condenado não encontrado."));
        mvc.perform(put("/convicted/{uuid}/status/ACTIVE", id))
            .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/attendance", "/convicted", "/processes", "/users", "/usuarios"})
    void reportsBothInvalidPaginationFields(String path) throws Exception {
        mvc.perform(get(path).param("page", "-1").param("size", "101"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.fields[*].field", hasItems("page", "size")));
        verifyNoInteractions(listAttendance, listConvicted, listProcess, listUsers);
    }

    @Test
    void preservesUserRequestBodyValidation() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.fields[*].field", hasItems("name", "cpf", "email", "password", "role")));
        mvc.perform(put("/users/00000000-0000-0000-0000-000000000001")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.fields[*].field", hasItems("name", "email", "role")));
        verifyNoInteractions(createUser, updateUser);
    }

    @Test
    void preservesNestedConvictedRequestBodyValidation() throws Exception {
        mvc.perform(post("/convicted").contentType(MediaType.APPLICATION_JSON).content("""
            {"name":"Arthur", "cpf":"11144477735", "birth_date":"1990-01-01", "phone":"11988881001",
             "address":{"zip_code":"12345678", "street":"", "number":"10", "neighborhood":"Centro",
                        "city":"Cidade", "state":"SP"}}
            """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.fields[*].field", hasItem("address.street")));
        verifyNoInteractions(createConvicted);
    }

    @Test
    void preservesNestedAttendanceMultipartValidation() throws Exception {
        var data = new MockMultipartFile("data", "data.json", "application/json", """
            {"convicted_id":"00000000-0000-0000-0000-000000000001",
             "process_id":"00000000-0000-0000-0000-000000000002", "phone":"11988881001",
             "employment_status":"FORMAL_WORK",
             "address":{"zip_code":"12345678", "street":"", "number":"10", "neighborhood":"Centro",
                        "city":"Cidade", "state":"SP"}}
            """.getBytes(StandardCharsets.UTF_8));
        var photo = new MockMultipartFile("photo", "photo.png", "image/png", new byte[]{1});
        mvc.perform(multipart("/attendance").file(data).file(photo))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.fields[*].field", hasItem("address.street")));
        verifyNoInteractions(createAttendance);
    }

    private static <T> PageResponse<T> emptyPage(int page, int size) {
        return new PageResponse<>(List.of(), page, size, 0, 0);
    }

    private void verifyList(String path, String search, int page, int size) {
        switch (path) {
            case "/attendance" -> verify(listAttendance).execute(search, page, size, null);
            case "/convicted" -> verify(listConvicted).execute(search, null, page, size, null);
            case "/processes" -> verify(listProcess).execute(search, page, size, null);
            case "/users", "/usuarios" -> verify(listUsers).execute(search, page, size, null);
            default -> throw new IllegalArgumentException("Rota não coberta: " + path);
        }
    }
}
