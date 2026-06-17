package com.ecommerce.domain.repository;

import com.ecommerce.domain.model.Categoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Contrato de repositório de Categoria definido no domínio.
 * Use cases dependem desta abstração, não da implementação JPA (DIP - SOLID).
 */
public interface CategoriaRepository {
    Optional<Categoria> findById(Long id);
    Categoria save(Categoria categoria);
    void deleteById(Long id);
    boolean existsById(Long id);
    boolean existsByNome(String nome);
    Page<Categoria> findAll(Pageable pageable);
    List<Categoria> findByNomeContainingIgnoreCase(String nome);
}
