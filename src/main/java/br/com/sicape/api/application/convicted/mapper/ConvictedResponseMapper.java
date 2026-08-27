package br.com.sicape.api.application.convicted.mapper;

import java.util.Comparator;

import org.springframework.stereotype.Component;

import br.com.sicape.api.application.convicted.dto.response.AddressResponse;
import br.com.sicape.api.application.convicted.dto.response.ConvictedResponse;
import br.com.sicape.api.application.convicted.dto.response.ProcessResponse;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.entity.ConvictedProcess;
import br.com.sicape.api.domain.valueobject.Address;

@Component
public class ConvictedResponseMapper {
    public ConvictedResponse toResponse(Convicted convicted) {
        return new ConvictedResponse(
            convicted.getUuid(),
            convicted.getName(),
            convicted.getCpf().value(),
            convicted.getBirthDate(),
            convicted.getPhone(),
            toAddressResponse(convicted.getAddress()),
            convicted.getEmploymentStatus(),
            convicted.getStatus(),
            convicted.getPhotoUrl(),
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

    private AddressResponse toAddressResponse(Address address) {
        return new AddressResponse(
            address.getZipCode(),
            address.getStreet(),
            address.getNumber(),
            address.getComplement(),
            address.getNeighborhood(),
            address.getCity(),
            address.getState()
        );
    }
}
