package com.ecommerce.observability.domain.repository;

import com.ecommerce.observability.domain.model.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * JpaSpecificationExecutor habilita consultas dinâmicas com Specification.
 * Permite compor filtros opcionais via and(), or(), not() sem proliferar métodos.
 */
@Repository
public interface ProdutoRepository
        extends JpaRepository<Produto, Long>, JpaSpecificationExecutor<Produto> {

    // Query com Fetch Join – carrega categoria em uma única query (evita N+1)
    @Query("""
        SELECT p FROM Produto p
        LEFT JOIN FETCH p.categoria
        WHERE p.id = :id
        """)
    java.util.Optional<Produto> findByIdWithCategoria(Long id);

    // Projection query – busca apenas campos necessários (evita over-fetching)
    @Query("""
        SELECT new com.ecommerce.observability.application.service.ProdutoResumo(
            p.id, p.nome, p.preco
        )
        FROM Produto p
        WHERE p.ativo = true
        ORDER BY p.nome
        """)
    List<com.ecommerce.observability.application.service.ProdutoResumo> findAllResumo();

    Page<Produto> findByAtivoTrue(Pageable pageable);
    long countByAtivoTrue();
}
