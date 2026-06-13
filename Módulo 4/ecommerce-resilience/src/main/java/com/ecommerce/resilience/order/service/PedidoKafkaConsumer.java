package com.ecommerce.resilience.order.service;

import com.ecommerce.resilience.order.dto.PedidoEventos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Consumer Kafka para eventos de pagamento e estoque.
 *
 * Consumer Groups:
 *   Múltiplas instâncias do order-service formam um grupo.
 *   Kafka distribui as partições entre as instâncias do grupo.
 *   Cada mensagem é processada por APENAS UMA instância do grupo.
 *
 *   Grupo "notificacao-group" recebe as MESMAS mensagens em paralelo
 *   (consumer groups diferentes são independentes).
 *
 * Idempotência:
 *   Em Kafka, at-least-once = a mesma mensagem pode chegar 2x em caso de
 *   rebalanceamento. O consumer deve ser IDEMPOTENTE:
 *   processar a mesma mensagem 2x deve ter o mesmo efeito que processar 1x.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PedidoKafkaConsumer {

    // Cache simples de offsets processados – em produção use Redis ou DB
    private final Set<String> offsetsProcessados = ConcurrentHashMap.newKeySet();

    // =========================================================================
    // CONSUMER DE PAGAMENTO CONFIRMADO
    // =========================================================================

    /**
     * @KafkaListener com:
     *   - topics: tópico a consumir
     *   - groupId: Consumer Group (compartilhado com outras instâncias)
     *   - containerFactory: usa o factory com DLT error handler
     *
     * Acknowledgment manual:
     *   Só faz commit do offset DEPOIS de processar com sucesso.
     *   Se falhar, a mensagem é reprocessada (at-least-once delivery).
     */
    @KafkaListener(
        topics = "pagamento-confirmado",
        groupId = "order-service-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onPagamentoConfirmado(
            @Payload PedidoEventos.PagamentoConfirmadoEvent evento,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment ack) {

        // ── Idempotência ─────────────────────────────────────────────────────
        // Gera chave única: tópico + partição + offset
        String chaveIdempotencia = "pagamento-confirmado:" + partition + ":" + offset;

        if (offsetsProcessados.contains(chaveIdempotencia)) {
            log.warn("[KAFKA] Mensagem duplicada ignorada: {}", chaveIdempotencia);
            ack.acknowledge(); // commita mesmo assim para não reprocessar
            return;
        }

        try {
            log.info("[KAFKA] Pagamento confirmado: pedidoId={} transacaoId={} partition={} offset={}",
                    evento.pedidoId(), evento.transacaoId(), partition, offset);

            // Lógica de negócio: confirma o pedido após pagamento
            processarPagamentoConfirmado(evento);

            // Registra como processado (idempotência)
            offsetsProcessados.add(chaveIdempotencia);

            // Commit manual do offset – SOMENTE após processamento com sucesso
            ack.acknowledge();

        } catch (Exception ex) {
            log.error("[KAFKA] Erro ao processar pagamento: {} | erro={}",
                    evento.pedidoId(), ex.getMessage());
            // NÃO faz ack → Kafka reentrega → error handler → DLT após N tentativas
            // ack.nack(Duration.ofSeconds(5)); // opcional: aguarda 5s antes de retentar
        }
    }

    // =========================================================================
    // CONSUMER DE DLT – mensagens que falharam N vezes
    // =========================================================================

    /**
     * Consumer da Dead Letter Topic.
     * Processa mensagens que falharam após todos os retries.
     * Ação típica: logar, alertar, salvar para análise manual.
     */
    @KafkaListener(
        topics = "pedido-criado.DLT",
        groupId = "order-service-dlt-group"
    )
    public void onPedidoCriadoDLT(ConsumerRecord<String, Object> record) {
        log.error("[KAFKA DLT] Mensagem na Dead Letter Topic:" +
                " topic={} partition={} offset={} key={} value={}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                record.value());

        // Em produção:
        // 1. Salva no banco para análise manual
        // 2. Envia alerta no Slack/PagerDuty
        // 3. Disponibiliza endpoint para reprocessamento manual
    }

    // =========================================================================
    // CONSUMER BATCH – processa múltiplas mensagens de uma vez
    // =========================================================================

    /**
     * Listener que recebe um lote de mensagens.
     * Mais eficiente para processamento em bulk (ex: geração de relatórios).
     */
    @KafkaListener(
        topics = "estoque-reservado",
        groupId = "order-service-batch-group",
        batch = "true"   // habilita modo batch
    )
    public void onEstoqueReservadoBatch(
            java.util.List<PedidoEventos.PedidoCriadoEvent> eventos,
            Acknowledgment ack) {

        log.info("[KAFKA BATCH] Recebidos {} eventos de estoque-reservado", eventos.size());

        // Processa todos em lote
        eventos.forEach(evento ->
            log.info("[KAFKA BATCH] Estoque reservado para pedido: {}", evento.pedidoId())
        );

        ack.acknowledge(); // commita após processar o lote inteiro
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void processarPagamentoConfirmado(PedidoEventos.PagamentoConfirmadoEvent evento) {
        // Simulação: atualiza status do pedido para CONFIRMADO
        log.info("[PEDIDO] Atualizando pedido {} para CONFIRMADO após pagamento {}",
                evento.pedidoId(), evento.transacaoId());
    }
}
