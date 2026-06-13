package com.ecommerce.domain.repository;

import com.ecommerce.domain.model.Produto;
import com.ecommerce.application.dto.projection.ProdutoCatalogoProjection;
import com.ecommerce.application.dto.projection.ProdutoResumoProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.List;
import java.util.Optional;

/**
 * Contrato de repositório de Produto definido no domínio.
 */
public interface ProdutoRepository {
    Optional<Produto> findById(Long id);
    Produto save(Produto produto);
    Page<Produto> findAll(Pageable pageable);
    List<ProdutoResumoProjection> findByCategoriaNome(String categoriaNome);
    List<ProdutoCatalogoProjection> findProdutosDisponiveis();
}
