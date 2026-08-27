package br.com.sicape.api.application.convicted.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sicape.api.application.convicted.dto.request.CreateConvictedRequest;
import br.com.sicape.api.application.convicted.dto.response.ConvictedResponse;
import br.com.sicape.api.application.convicted.mapper.ConvictedResponseMapper;
import br.com.sicape.api.application.oauth.AuthContext;
import br.com.sicape.api.domain.entity.Convicted;
import br.com.sicape.api.domain.exception.ConflictException;
import br.com.sicape.api.domain.exception.ValidationException;
import br.com.sicape.api.domain.repository.ConvictedRepository;
import br.com.sicape.api.domain.valueobject.Address;
import br.com.sicape.api.domain.valueobject.Cpf;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateConvictedUseCase {
    private final ConvictedRepository repository;
    private final ConvictedResponseMapper mapper;

    @Transactional
    public ConvictedResponse execute(CreateConvictedRequest request, AuthContext authContext) {
        Cpf cpf = toCpf(request.cpf());
        if (repository.existsByCpf(cpf)) {
            throw new ConflictException("Já existe um condenado cadastrado com este CPF.");
        }

        Address address;
        try {
            address = request.address().toValueObject();
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("address", exception.getMessage());
        }

        Convicted convicted = new Convicted(
            request.name(),
            cpf,
            request.birthDate(),
            request.phone(),
            address,
            request.employmentStatus(),
            authContext.district()
        );

        return mapper.toResponse(repository.save(convicted));
    }

    private Cpf toCpf(String value) {
        try {
            return Cpf.of(value);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("cpf", exception.getMessage());
        }
    }
}
