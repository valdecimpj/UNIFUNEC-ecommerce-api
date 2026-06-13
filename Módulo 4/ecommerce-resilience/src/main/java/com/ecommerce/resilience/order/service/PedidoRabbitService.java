package com.ecommerce.resilience.order.service;

import com.ecommerce.resilience.order.dto.PedidoEventos;
import com.ecommerce.resilience.shared.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Producer e Consumer RabbitMQ para eventos de pedidos.
 *
 * RabbitMQ vs Kafka:
 *   RabbitMQ: push-based, routing complexo, bom para tarefas e RPC
 *   Kafka: pull-based, log distribuído, bom para Event Sourcing e replay
 *
 * Use RabbitMQ quando precisar de:
 *   - Routing por tipo de mensagem (Exchange + RoutingKey)
 *   - Prioridade de mensagens
 *   - Timeout de mensagens (TTL por fila)
 *   - Confirmação de processamento (ack/nack individual)
 *
 * Use Kafka quando precisar de:
 *   - Alta vazão (milhões de msg/s)
 *   - Replay de eventos (reprocessamento)
 *   - Event Sourcing
 *   - Stream processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoRabbitService {

    private final RabbitTemplate rabbitTemplate;

    // =========================================================================
    // PRODUCER
    // =========================================================================

    /**
     * Publica no Topic Exchange com routingKey específica.
     * O Exchange roteia para as filas com binding correspondente.
     *
     * Fluxo: PedidoCriadoEvent
     *   → pedidos.exchange (topic)
     *   → routingKey "pedido.criado"
     *   → pedidos.criados.queue (binding exato)
     *   → pedidos.todos.queue (binding wildcard "pedido.*")
     */
    public void publicarPedidoCriado(PedidoEventos.PedidoCriadoEvent evento) {
        log.info("[RABBIT] Publicando PedidoCriado: pedidoId={}", evento.pedidoId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_PEDIDOS,
                RabbitMQConfig.RK_PEDIDO_CRIADO,
                evento
        );
    }

    public void publicarPedidoCancelado(PedidoEventos.PedidoCanceladoEvent evento) {
        log.info("[RABBIT] Publicando PedidoCancelado: pedidoId={}", evento.pedidoId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_PEDIDOS,
                RabbitMQConfig.RK_PEDIDO_CANCELADO,
                evento
        );
    }

    // =========================================================================
    // CONSUMERS com ack manual
    // =========================================================================

    /**
     * @RabbitListener com ack manual via Channel.
     *
     * Fluxo de ack/nack:
     *   channel.basicAck(tag, false)       → confirma, remove da fila
     *   channel.basicNack(tag, false, true) → rejeita, recoloca na fila (requeue)
     *   channel.basicNack(tag, false, false) → rejeita, vai para DLQ (sem requeue)
     *
     * Em produção, use basicNack com requeue=false após N falhas
     * para evitar loop infinito de retry.
     */
    @RabbitListener(
        queues = RabbitMQConfig.QUEUE_PEDIDOS_CRIADOS,
        ackMode = "MANUAL"
    )
    public void onPedidoCriado(
            PedidoEventos.PedidoCriadoEvent evento,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("[RABBIT] Pedido criado recebido: pedidoId={}", evento.pedidoId());

        try {
            processarPedidoCriado(evento);

            // ACK – confirma processamento, remove da fila
            channel.basicAck(deliveryTag, false);
            log.debug("[RABBIT] ACK enviado para deliveryTag={}", deliveryTag);

        } catch (Exception ex) {
            log.error("[RABBIT] Erro ao processar pedido {}: {}", evento.pedidoId(), ex.getMessage());

            // NACK sem requeue → mensagem vai automaticamente para a DLQ
            // (configurada via x-dead-letter-exchange na fila)
            channel.basicNack(deliveryTag, false, false);
            log.warn("[RABBIT] NACK enviado – mensagem roteada para DLQ");
        }
    }

    @RabbitListener(
        queues = RabbitMQConfig.QUEUE_PEDIDOS_CANC,
        ackMode = "MANUAL"
    )
    public void onPedidoCancelado(
            PedidoEventos.PedidoCanceladoEvent evento,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.info("[RABBIT] Pedido cancelado: pedidoId={} motivo={}", evento.pedidoId(), evento.motivo());

        try {
            // Lógica: restaurar estoque, notificar cliente etc.
            log.info("[RABBIT] Processando cancelamento do pedido {}", evento.pedidoId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception ex) {
            log.error("[RABBIT] Erro no cancelamento: {}", ex.getMessage());
            channel.basicNack(deliveryTag, false, false); // → DLQ
        }
    }

    /**
     * Consumer da DLQ – monitora mensagens que falharam.
     * Deve fazer ACK para evitar reentrega infinita com acknowledge-mode: manual.
     */
    @RabbitListener(queues = RabbitMQConfig.DLQ_PEDIDOS_CRIADOS, ackMode = "MANUAL")
    public void onDlqPedidosCriados(
            PedidoEventos.PedidoCriadoEvent evento,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {

        log.error("[RABBIT DLQ] Mensagem na DLQ: pedidoId={}", evento.pedidoId());
        // Em produção: salva para análise, envia alerta, disponibiliza reprocessamento
        channel.basicAck(deliveryTag, false);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void processarPedidoCriado(PedidoEventos.PedidoCriadoEvent evento) {
        log.info("[RABBIT] Processando pedido criado: {} itens, total: {}",
                evento.itens().size(), evento.total());
    }
}
