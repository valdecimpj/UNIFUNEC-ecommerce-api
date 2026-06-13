package com.ecommerce.security.infrastructure.persistence;

import com.ecommerce.security.domain.model.Usuario;
import com.ecommerce.security.domain.repository.UsuarioRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Implementação JPA do repositório de usuários.
 * Usa Prepared Statements internamente – imune a SQL Injection por padrão.
 */

public interface UsuarioJpaRepository
        extends JpaRepository<Usuario, Long>, UsuarioRepository {

    // Spring Data gera: SELECT * FROM usuarios WHERE email = ? (parametrizado)
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
