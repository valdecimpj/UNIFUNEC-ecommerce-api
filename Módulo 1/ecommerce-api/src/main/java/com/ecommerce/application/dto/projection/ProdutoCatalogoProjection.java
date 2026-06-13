package com.ecommerce.application.dto.projection;

import java.math.BigDecimal;

/**
 * Class-based Projection usando Java record.
 * Closed projection – Spring gera query mais eficiente que a interface-based.
 * Usado em consultas JPQL com new().
 */
public record ProdutoCatalogoProjection(
        Long id,
        String nome,
        BigDecimal preco,
        String categoriaNome
) {}
