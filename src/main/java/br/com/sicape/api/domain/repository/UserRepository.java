package br.com.sicape.api.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.sicape.api.domain.entity.User;
import br.com.sicape.api.domain.valueobject.Cpf;

public interface UserRepository extends BaseRepository<User> {
    Optional<User> findByCpf(Cpf cpf);
    Optional<User> findByEmail(String email);
    boolean existsByCpf(Cpf cpf);
    boolean existsByEmail(String email);
    boolean existsByEmailAndUuidNot(String email, java.util.UUID uuid);

    @Query("SELECT u FROM User u WHERE " +
           "(:searchTerm IS NULL OR " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);
}
