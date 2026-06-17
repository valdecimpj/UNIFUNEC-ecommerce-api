package com.ecommerce.infrastructure.persistence;

import com.ecommerce.domain.model.Categoria;
import com.ecommerce.domain.repository.CategoriaRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Implementação JPA do repositório de Categoria.
 * Spring Data implementa os métodos automaticamente.
 * Fica na infra – o domínio só conhece a interface CategoriaRepository.
 */
public interface CategoriaJpaRepository
        extends JpaRepository<Categoria, Long>, CategoriaRepository {

    boolean existsByNome(String nome);

    List<Categoria> findByNomeContainingIgnoreCase(String nome);
}
