package com.ecommerce.application.dto.projection;

import org.springframework.beans.factory.annotation.Value;
import java.math.BigDecimal;

/**
 * Interface Projection – Spring Data gera SELECT otimizado com apenas as colunas mapeadas.
 * Mais performático que buscar a Entity completa.
 */
public interface ProdutoResumoProjection {
    Long getId();
    String getNome();
    BigDecimal getPreco();

    // SpEL: campo calculado no Java sem precisar de SQL
    @Value("#{target.preco.multiply(0.9)}")
    BigDecimal getPrecoComDescontoFidelidade();
}
