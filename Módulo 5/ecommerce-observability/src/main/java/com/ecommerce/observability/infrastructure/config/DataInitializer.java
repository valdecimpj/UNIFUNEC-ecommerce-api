package com.ecommerce.observability.infrastructure.config;

import com.ecommerce.observability.domain.model.Categoria;
import com.ecommerce.observability.domain.model.Produto;
import com.ecommerce.observability.domain.repository.ProdutoRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ProdutoRepository produtoRepository;
    private final EntityManager em;

    @Override
    @Transactional
    public void run(String... args) {
        if (produtoRepository.count() > 0) return;

        Categoria eletronicos = new Categoria(null, "Eletrônicos");
        Categoria perifericos = new Categoria(null, "Periféricos");
        em.persist(eletronicos);
        em.persist(perifericos);
        em.flush();

        produtoRepository.save(Produto.builder().nome("Notebook Pro 15")
                .preco(BigDecimal.valueOf(8999.90)).estoque(10).ativo(true).categoria(eletronicos).build());
        produtoRepository.save(Produto.builder().nome("Monitor 4K 27\"")
                .preco(BigDecimal.valueOf(3499.90)).estoque(8).ativo(true).categoria(eletronicos).build());
        produtoRepository.save(Produto.builder().nome("Mouse Gamer RGB")
                .preco(BigDecimal.valueOf(299.90)).estoque(50).ativo(true).categoria(perifericos).build());
        produtoRepository.save(Produto.builder().nome("Teclado Mecânico")
                .preco(BigDecimal.valueOf(599.90)).estoque(30).ativo(true).categoria(perifericos).build());
        produtoRepository.save(Produto.builder().nome("Headset 7.1 Surround")
                .preco(BigDecimal.valueOf(449.90)).estoque(20).ativo(true).categoria(perifericos).build());

        log.info("[DATA] 5 produtos criados. Acesse /swagger-ui.html e /actuator");
    }
}
