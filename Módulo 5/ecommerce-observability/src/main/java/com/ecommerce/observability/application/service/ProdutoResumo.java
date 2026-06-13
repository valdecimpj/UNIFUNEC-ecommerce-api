package com.ecommerce.observability.application.service;

import java.math.BigDecimal;

/**
 * Closed Projection como Java record.
 * SELECT apenas id, nome, preco – sem carregar a entity inteira.
 */
public record ProdutoResumo(Long id, String nome, BigDecimal preco) {}
