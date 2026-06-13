package com.ecommerce.shared.service;

import com.ecommerce.domain.exception.ProdutoNaoEncontradoException;
import com.ecommerce.domain.model.Produto;
import com.ecommerce.domain.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável exclusivamente por operações de estoque.
 * Responsabilidade única – não mistura com lógica de pedido ou notificação.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EstoqueService {

    private final ProdutoRepository produtoRepository;

    @Transactional
    public void decrementar(Long produtoId, int quantidade) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(produtoId));

        produto.decrementarEstoque(quantidade); // regra no domínio
        produtoRepository.save(produto);
        log.info("[ESTOQUE] Produto id={} decrementado em {}. Novo estoque: {}",
                produtoId, quantidade, produto.getEstoque());
    }

    @Transactional
    public void incrementar(Long produtoId, int quantidade) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(produtoId));

        produto.incrementarEstoque(quantidade);
        produtoRepository.save(produto);
        log.info("[ESTOQUE] Produto id={} incrementado em {}. Novo estoque: {}",
                produtoId, quantidade, produto.getEstoque());
    }
}
