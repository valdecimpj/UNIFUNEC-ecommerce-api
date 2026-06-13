package com.ecommerce.resilience.shared.exception;

/**
 * Exceção de negócio – NÃO deve acionar retry nem abrir Circuit Breaker.
 * Configurada em ignore-exceptions no application.yml.
 */
public class EstoqueInsuficienteException extends RuntimeException {
    public EstoqueInsuficienteException(Long produtoId, int disponivel, int solicitado) {
        super(String.format("Estoque insuficiente para produto %d. Disponível: %d, Solicitado: %d",
                produtoId, disponivel, solicitado));
    }
}
