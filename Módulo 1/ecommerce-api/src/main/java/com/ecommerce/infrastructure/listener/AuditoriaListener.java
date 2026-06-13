package com.ecommerce.infrastructure.listener;

import com.ecommerce.application.event.PedidoCanceladoEvent;
import com.ecommerce.application.event.PedidoCriadoEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener de auditoria.
 * @EventListener síncrono – registra log estruturado de todas as operações.
 * Não usa @TransactionalEventListener pois auditoria deve sempre ocorrer,
 * independente do resultado da transação.
 */
@Component
@Slf4j
public class AuditoriaListener {

    @EventListener
    public void onPedidoCriado(PedidoCriadoEvent event) {
        log.info("[AUDITORIA] PEDIDO_CRIADO | id={} | clienteId={} | total={} | itens={}",
                event.pedido().getId(),
                event.pedido().getClienteId(),
                event.pedido().calcularTotal(),
                event.pedido().getItens().size());
    }

    @EventListener
    public void onPedidoCancelado(PedidoCanceladoEvent event) {
        log.warn("[AUDITORIA] PEDIDO_CANCELADO | id={} | clienteId={} | motivo={}",
                event.pedido().getId(),
                event.pedido().getClienteId(),
                event.motivoCancelamento());
    }
}
