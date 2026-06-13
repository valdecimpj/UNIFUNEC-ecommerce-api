package com.ecommerce.infrastructure.listener;

import com.ecommerce.application.event.PedidoCanceladoEvent;
import com.ecommerce.application.event.PedidoCriadoEvent;
import com.ecommerce.shared.service.EstoqueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener de estoque.
 * BEFORE_COMMIT: executa DENTRO da transação – se falhar, o pedido é revertido.
 * Garante consistência entre pedido e estoque.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EstoqueListener {

    private final EstoqueService estoqueService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onPedidoCriado(PedidoCriadoEvent event) {
        log.info("[ESTOQUE] Decrementando estoque para pedido id={}", event.pedido().getId());
        event.pedido().getItens().forEach(item ->
            estoqueService.decrementar(item.getProduto().getId(), item.getQuantidade())
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onPedidoCancelado(PedidoCanceladoEvent event) {
        log.info("[ESTOQUE] Restaurando estoque para pedido cancelado id={}", event.pedido().getId());
        event.pedido().getItens().forEach(item ->
            estoqueService.incrementar(item.getProduto().getId(), item.getQuantidade())
        );
    }
}
