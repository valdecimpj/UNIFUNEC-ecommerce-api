package com.ecommerce.resilience.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/**
 * Configuração do Kafka para produção.
 *
 * Tópicos criados:
 *   pedido-criado       – emitido quando pedido é criado (Event Sourcing)
 *   pedido-cancelado    – emitido quando pedido é cancelado
 *   estoque-reservado   – emitido pelo estoque-service após reserva
 *   pagamento-confirmado – emitido pelo pagamento-service
 *   pedido-criado.DLT   – Dead Letter Topic (falhas após N tentativas)
 *
 * Consumer Groups:
 *   order-service-group    – processa eventos de pagamento e estoque
 *   notificacao-group      – envia emails/SMS
 *   auditoria-group        – registra auditoria (replay possível)
 */
@Configuration
@Slf4j
public class KafkaConfig {

    // ── Tópicos ──────────────────────────────────────────────────────────────

    @Bean
    public NewTopic topicPedidoCriado() {
        return TopicBuilder.name("pedido-criado")
                .partitions(3)    // 3 partições = 3 consumers em paralelo
                .replicas(1)      // 1 réplica (dev); produção: 3
                .build();
    }

    @Bean
    public NewTopic topicPedidoCancelado() {
        return TopicBuilder.name("pedido-cancelado")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic topicEstoqueReservado() {
        return TopicBuilder.name("estoque-reservado")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic topicPagamentoConfirmado() {
        return TopicBuilder.name("pagamento-confirmado")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Dead Letter Topic (DLT) – mensagens que falharam após todos os retries.
     * Convenção: nome-do-topico.DLT
     * Permite investigação e reprocessamento manual das mensagens com falha.
     */
    @Bean
    public NewTopic topicPedidoCriadoDLT() {
        return TopicBuilder.name("pedido-criado.DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }

    // ── Error Handler com Dead Letter Topic ──────────────────────────────────

    /**
     * Configura o error handler do Kafka Listener.
     *
     * Fluxo de erro:
     *   1ª falha → retry após 1 segundo
     *   2ª falha → retry após 2 segundos
     *   3ª falha → retry após 4 segundos
     *   4ª falha (MaxAttempts) → publica no DLT e commita offset
     *
     * DeadLetterPublishingRecoverer:
     *   - Publica a mensagem original no DLT com headers de diagnóstico
     *   - Headers incluem: causa do erro, stack trace, tópico/partição/offset originais
     *   - Permite reprocessamento via consumer do DLT ou ferramentas externas
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(
            KafkaTemplate<String, Object> kafkaTemplate) {

        // Publica no DLT quando todos os retries esgotarem
        DeadLetterPublishingRecoverer dltRecoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate,
                        (record, ex) -> {
                            log.error("[KAFKA DLT] Publicando no DLT. topic={} key={} erro={}",
                                    record.topic(), record.key(), ex.getMessage());
                            // Convenção: tópico original + ".DLT"
                            return new org.apache.kafka.common.TopicPartition(
                                    record.topic() + ".DLT", 0);
                        });

        // Backoff exponencial: 1s → 2s → 4s (máximo 4 tentativas)
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(3); // 3 retries + 1 tentativa original = 4 total

        DefaultErrorHandler handler = new DefaultErrorHandler(dltRecoverer, backOff);

        // Exceções que NÃO devem fazer retry (vai direto para DLT)
        handler.addNotRetryableExceptions(
                org.springframework.kafka.support.serializer.DeserializationException.class,
                IllegalArgumentException.class
        );

        return handler;
    }

    /**
     * Factory do Listener Container com o error handler customizado.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object>
    kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            DefaultErrorHandler errorHandler) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        // Ack manual: o consumer controla quando o offset é commitado (at-least-once)
        // Sem esta linha, o factory customizado ignora o spring.kafka.listener.ack-mode do YAML
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        // Concorrência: quantas threads processam mensagens em paralelo
        factory.setConcurrency(3); // 1 thread por partição do tópico
        return factory;
    }
}
