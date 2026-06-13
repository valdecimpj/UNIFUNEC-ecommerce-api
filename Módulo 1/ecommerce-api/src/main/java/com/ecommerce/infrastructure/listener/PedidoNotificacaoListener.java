package com.ecommerce.infrastructure.listener;

import com.ecommerce.application.event.PedidoCanceladoEvent;
import com.ecommerce.application.event.PedidoCriadoEvent;
import com.ecommerce.shared.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener de notificações de pedido.
 * AFTER_COMMIT: só executa se a transação foi confirmada.
 * @Async: executa em thread separada (não bloqueia a transação principal).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PedidoNotificacaoListener {

    private final NotificacaoService notificacaoService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("emailTaskExecutor")
    public void onPedidoCriado(PedidoCriadoEvent event) {
        log.info("[NOTIFICACAO] Pedido criado id={} – enviando confirmação",
                event.pedido().getId());
        notificacaoService.notificarCriacao(event.pedido());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async("emailTaskExecutor")
    public void onPedidoCancelado(PedidoCanceladoEvent event) {
        log.info("[NOTIFICACAO] Pedido cancelado id={} – notificando cliente",
                event.pedido().getId());
        notificacaoService.notificarCancelamento(event.pedido(), event.motivoCancelamento());
    }
}
