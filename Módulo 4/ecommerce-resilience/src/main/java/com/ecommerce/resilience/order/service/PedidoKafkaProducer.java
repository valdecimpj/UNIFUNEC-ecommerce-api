package com.ecommerce.resilience.order.service;

import com.ecommerce.resilience.order.dto.PedidoEventos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Producer Kafka para eventos de pedidos.
 *
 * KafkaTemplate é thread-safe e pode ser injetado em qualquer bean.
 *
 * Garantias configuradas no application.yml:
 *   acks=all       → aguarda confirmação de todos os replicas
 *   enable.idempotence=true → exatamente uma entrega por mensagem
 *   retries=3      → tenta reenviar em caso de falha transitória
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // =========================================================================
    // PUBLICAÇÃO COM ACK ASSÍNCRONO
    // =========================================================================

    /**
     * Publica evento de pedido criado.
     *
     * A chave (key) garante que eventos do mesmo pedido vão para
     * a mesma partição – preservando ordem por pedidoId.
     *
     * CompletableFuture: Kafka confirma quando a mensagem foi persistida
     * em todos os brokers configurados (acks=all).
     */
    public void publicarPedidoCriado(PedidoEventos.PedidoCriadoEvent evento) {
        String key = String.valueOf(evento.pedidoId()); // mesma partição por pedidoId

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send("pedido-criado", key, evento);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("[KAFKA] Falha ao publicar PedidoCriadoEvent: pedidoId={} | erro={}",
                        evento.pedidoId(), ex.getMessage());
                // Em produção: salve em outbox para retry transacional
            } else {
                log.info("[KAFKA] PedidoCriadoEvent publicado: pedidoId={} | partition={} | offset={}",
                        evento.pedidoId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }

    /**
     * Publica evento de pedido cancelado.
     * Mesmo key = mesma partição = ordem garantida com PedidoCriadoEvent.
     */
    public void publicarPedidoCancelado(PedidoEventos.PedidoCanceladoEvent evento) {
        String key = String.valueOf(evento.pedidoId());

        kafkaTemplate.send("pedido-cancelado", key, evento)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[KAFKA] Falha PedidoCanceladoEvent: {}", ex.getMessage());
                    } else {
                        log.info("[KAFKA] PedidoCanceladoEvent: pedidoId={}", evento.pedidoId());
                    }
                });
    }
}
