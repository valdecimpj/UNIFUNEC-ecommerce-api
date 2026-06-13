package com.ecommerce.resilience.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Eventos de domínio publicados via Kafka e RabbitMQ.
 * Records Java 21 – imutáveis, sem boilerplate.
 */
public class PedidoEventos {

    public record PedidoCriadoEvent(
            Long pedidoId,
            Long clienteId,
            List<ItemEvento> itens,
            BigDecimal total,
            String cep,
            LocalDateTime criadoEm
    ) {}

    public record PedidoCanceladoEvent(
            Long pedidoId,
            Long clienteId,
            String motivo,
            LocalDateTime canceladoEm
    ) {}

    public record ItemEvento(
            Long produtoId,
            Integer quantidade,
            BigDecimal precoUnitario
    ) {}

    public record PagamentoConfirmadoEvent(
            Long pedidoId,
            String transacaoId,
            BigDecimal valor,
            LocalDateTime processadoEm
    ) {}
}
