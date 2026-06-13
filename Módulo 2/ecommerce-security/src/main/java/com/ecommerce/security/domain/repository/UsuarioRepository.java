package com.ecommerce.security.domain.repository;

import com.ecommerce.security.domain.model.Usuario;

import java.util.Optional;

/**
 * Contrato de repositório de usuário – definido no domínio.
 */
public interface UsuarioRepository {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findById(Long id);
    Usuario save(Usuario usuario);
    boolean existsByEmail(String email);
}
