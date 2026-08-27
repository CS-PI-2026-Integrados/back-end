package br.com.sicape.api.domain.repository;

import java.util.Optional;

import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.valueobject.Cpf;

public interface UserRepository extends BaseRepository<User> {
    Optional<User> findByCpf(Cpf cpf);
    Optional<User> findByEmail(String email);
    boolean existsByCpf(Cpf cpf);
    boolean existsByEmail(String email);
}
