package com.ecommerce.domain.exception;

import lombok.Getter;

@Getter
public class EstoqueInsuficienteException extends RuntimeException {

    private final Long produtoId;
    private final int estoqueDisponivel;
    private final int quantidadeSolicitada;

    public EstoqueInsuficienteException(Long produtoId, int estoqueDisponivel, int quantidadeSolicitada) {
        super(String.format(
                "Estoque insuficiente para produto %d. Disponível: %d, Solicitado: %d",
                produtoId, estoqueDisponivel, quantidadeSolicitada));
        this.produtoId = produtoId;
        this.estoqueDisponivel = estoqueDisponivel;
        this.quantidadeSolicitada = quantidadeSolicitada;
    }
}
