package com.ecommerce.domain.exception;

public class PedidoNaoEncontradoException extends RuntimeException {
    public PedidoNaoEncontradoException(Long id) {
        super("Pedido não encontrado com id: " + id);
    }
}
