package com.ecommerce.application.event;

import com.ecommerce.domain.model.Pedido;

/**
 * Evento publicado após a criação bem-sucedida de um pedido.
 * Consumers: NotificacaoListener (AFTER_COMMIT), EstoqueListener (BEFORE_COMMIT).
 */
public record PedidoCriadoEvent(Pedido pedido) {}
