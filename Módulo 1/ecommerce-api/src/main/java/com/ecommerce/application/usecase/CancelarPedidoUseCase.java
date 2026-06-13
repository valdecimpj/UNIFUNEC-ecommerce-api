package com.ecommerce.application.usecase;

import com.ecommerce.application.event.PedidoCanceladoEvent;
import com.ecommerce.domain.exception.PedidoNaoEncontradoException;
import com.ecommerce.domain.model.Pedido;
import com.ecommerce.domain.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CancelarPedidoUseCase {

    private final PedidoRepository pedidoRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void executar(Long pedidoId, String motivo) {
        log.info("Cancelando pedido id={}, motivo={}", pedidoId, motivo);

        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new PedidoNaoEncontradoException(pedidoId));

        // Delega a regra de estado para o domínio
        pedido.cancelar();
        pedidoRepository.save(pedido);

        // Publica evento – EstoqueListener restaura o estoque
        eventPublisher.publishEvent(new PedidoCanceladoEvent(pedido, motivo));
        log.info("Pedido {} cancelado com sucesso", pedidoId);
    }
}
