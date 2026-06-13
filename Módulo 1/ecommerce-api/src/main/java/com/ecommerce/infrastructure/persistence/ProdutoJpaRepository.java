package com.ecommerce.infrastructure.persistence;

import com.ecommerce.application.dto.projection.ProdutoCatalogoProjection;
import com.ecommerce.application.dto.projection.ProdutoResumoProjection;
import com.ecommerce.domain.model.Produto;
import com.ecommerce.domain.repository.ProdutoRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
//import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Implementação JPA do repositório de Produto.
 * Spring Data implementa os métodos automaticamente.
 * Fica na infra – o domínio só conhece a interface ProdutoRepository.
 */
//@Repository
public interface ProdutoJpaRepository
        extends JpaRepository<Produto, Long>, ProdutoRepository {

    // Interface Projection – Spring gera SELECT apenas com id, nome, preco
    List<ProdutoResumoProjection> findByCategoriaNome(String categoriaNome);

    // Class-based Projection com JPQL – mais performático em leitura intensiva
    @Query("""
        SELECT new com.ecommerce.application.dto.projection.ProdutoCatalogoProjection(
            p.id, p.nome, p.preco, c.nome
        )
        FROM Produto p
        JOIN p.categoria c
        WHERE p.estoque > 0 AND p.ativo = true
        ORDER BY p.nome ASC
        """)
    List<ProdutoCatalogoProjection> findProdutosDisponiveis();
}
