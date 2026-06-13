package com.ecommerce.application.event;

import com.ecommerce.domain.model.Pedido;

/**
 * Evento publicado após cancelamento de pedido.
 * Consumers: NotificacaoListener, EstoqueListener (restaura estoque).
 */
public record PedidoCanceladoEvent(Pedido pedido, String motivoCancelamento) {}
