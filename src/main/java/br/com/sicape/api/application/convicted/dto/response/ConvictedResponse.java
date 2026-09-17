package br.com.sicape.api.application.convicted.dto.response;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import br.com.sicape.api.application.common.dto.response.AddressResponse;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.ConvictedProcess;
import br.com.sicape.api.domain.enums.ConvictedStatus;
import br.com.sicape.api.domain.enums.EmploymentStatus;

public record ConvictedResponse(
    UUID id,
    String name,
    String cpf,
    LocalDate birthDate,
    String phone,
    AddressResponse address,
    EmploymentStatus employmentStatus,
    ConvictedStatus status,
    List<ProcessResponse> processes
) {
    public static ConvictedResponse from(Convicted convicted) {
        return new ConvictedResponse(
            convicted.getUuid(),
            convicted.getName(),
            convicted.getCpf().value(),
            convicted.getBirthDate(),
            convicted.getPhone().value(),
            AddressResponse.from(convicted.getAddress()),
            convicted.getEmploymentStatus(),
            convicted.getStatus(),
            convicted.getProcesses().stream()
                .sorted(Comparator.comparing(ConvictedProcess::isPrincipal).reversed())
                .map(link -> new ProcessResponse(
                    link.getProcess().getUuid(),
                    link.getProcess().getNumber(),
                    link.getProcess().getStatus(),
                    link.isPrincipal()
                ))
                .toList()
        );
    }
}
