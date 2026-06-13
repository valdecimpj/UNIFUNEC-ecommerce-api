package com.ecommerce.resilience.shared.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração completa do RabbitMQ com AMQP.
 *
 * Arquitetura:
 *
 *   Producer → [pedidos.exchange] → binding por routingKey → [fila]
 *
 *   pedidos.exchange (Topic Exchange):
 *     ├── pedido.criado    → pedidos.criados.queue
 *     ├── pedido.cancelado → pedidos.cancelados.queue
 *     └── pedido.*         → pedidos.todos.queue (wildcard)
 *
 *   Filas com DLQ:
 *     pedidos.criados.queue → falha → pedidos.criados.dlq
 *     pedidos.cancelados.queue → falha → pedidos.cancelados.dlq
 *
 * Tipos de Exchange:
 *   Direct:  routingKey exato (ex: "pedido.criado")
 *   Topic:   routingKey com wildcards (* = 1 palavra, # = N palavras)
 *   Fanout:  envia para TODAS as filas vinculadas (broadcast)
 *   Headers: roteamento por headers (mais flexível, menos usado)
 */
@Configuration
public class RabbitMQConfig {

    // ── Nomes das filas e exchanges ──────────────────────────────────────────

    public static final String EXCHANGE_PEDIDOS      = "pedidos.exchange";
    public static final String EXCHANGE_DLQ          = "pedidos.dlq.exchange";
    public static final String QUEUE_PEDIDOS_CRIADOS = "pedidos.criados.queue";
    public static final String QUEUE_PEDIDOS_CANC    = "pedidos.cancelados.queue";
    public static final String QUEUE_PEDIDOS_TODOS   = "pedidos.todos.queue";
    public static final String DLQ_PEDIDOS_CRIADOS   = "pedidos.criados.dlq";
    public static final String DLQ_PEDIDOS_CANC      = "pedidos.cancelados.dlq";

    public static final String RK_PEDIDO_CRIADO    = "pedido.criado";
    public static final String RK_PEDIDO_CANCELADO = "pedido.cancelado";
    public static final String RK_PEDIDO_WILDCARD  = "pedido.*";

    // ── Exchanges ────────────────────────────────────────────────────────────

    /**
     * Topic Exchange – roteamento por padrão com wildcards.
     * durable=true: sobrevive a restart do RabbitMQ.
     */
    @Bean
    public TopicExchange pedidosExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE_PEDIDOS)
                .durable(true)
                .build();
    }

    /**
     * Direct Exchange para Dead Letter Queue.
     * Mensagens com falha são roteadas aqui.
     */
    @Bean
    public DirectExchange dlqExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_DLQ)
                .durable(true)
                .build();
    }

    // ── Filas principais com configuração de DLQ ─────────────────────────────

    /**
     * Fila de pedidos criados.
     * x-dead-letter-exchange: quando uma mensagem é rejeitada (NACK sem requeue),
     *   ela é automaticamente enviada para o DLQ exchange com a routingKey definida.
     * x-message-ttl: mensagens expiram após 24h (evita acúmulo)
     */
    @Bean
    public Queue pedidosCriadosQueue() {
        return QueueBuilder.durable(QUEUE_PEDIDOS_CRIADOS)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLQ)
                .withArgument("x-dead-letter-routing-key", DLQ_PEDIDOS_CRIADOS)
                .withArgument("x-message-ttl", 86_400_000) // 24h em ms
                .build();
    }

    @Bean
    public Queue pedidosCanceladosQueue() {
        return QueueBuilder.durable(QUEUE_PEDIDOS_CANC)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DLQ)
                .withArgument("x-dead-letter-routing-key", DLQ_PEDIDOS_CANC)
                .build();
    }

    /**
     * Fila que recebe TODOS os eventos de pedido (via wildcard "pedido.*").
     * Útil para auditoria e logging centralizado.
     */
    @Bean
    public Queue pedidosTodosQueue() {
        return QueueBuilder.durable(QUEUE_PEDIDOS_TODOS).build();
    }

    // ── Dead Letter Queues ────────────────────────────────────────────────────

    @Bean
    public Queue dlqPedidosCriados() {
        return QueueBuilder.durable(DLQ_PEDIDOS_CRIADOS).build();
    }

    @Bean
    public Queue dlqPedidosCancelados() {
        return QueueBuilder.durable(DLQ_PEDIDOS_CANC).build();
    }

    // ── Bindings – associa exchange + routingKey → fila ──────────────────────

    @Bean
    public Binding bindingPedidosCriados(Queue pedidosCriadosQueue,
                                          TopicExchange pedidosExchange) {
        return BindingBuilder.bind(pedidosCriadosQueue)
                .to(pedidosExchange)
                .with(RK_PEDIDO_CRIADO);
    }

    @Bean
    public Binding bindingPedidosCancelados(Queue pedidosCanceladosQueue,
                                             TopicExchange pedidosExchange) {
        return BindingBuilder.bind(pedidosCanceladosQueue)
                .to(pedidosExchange)
                .with(RK_PEDIDO_CANCELADO);
    }

    @Bean
    public Binding bindingPedidosTodos(Queue pedidosTodosQueue,
                                        TopicExchange pedidosExchange) {
        // Wildcard: recebe pedido.criado, pedido.cancelado, pedido.qualquercoisa
        return BindingBuilder.bind(pedidosTodosQueue)
                .to(pedidosExchange)
                .with(RK_PEDIDO_WILDCARD);
    }

    @Bean
    public Binding bindingDlqCriados(Queue dlqPedidosCriados,
                                      DirectExchange dlqExchange) {
        return BindingBuilder.bind(dlqPedidosCriados)
                .to(dlqExchange)
                .with(DLQ_PEDIDOS_CRIADOS);
    }

    @Bean
    public Binding bindingDlqCancelados(Queue dlqPedidosCancelados,
                                         DirectExchange dlqExchange) {
        return BindingBuilder.bind(dlqPedidosCancelados)
                .to(dlqExchange)
                .with(DLQ_PEDIDOS_CANC);
    }

    // ── Serialização JSON ─────────────────────────────────────────────────────

    /**
     * Converte mensagens para/de JSON automaticamente.
     * Sem isso, RabbitMQ serializa com Java serialization (inseguro e inflexível).
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        // Habilita publisher confirms (sabe quando a mensagem chegou ao broker)
        template.setConfirmCallback((correlation, ack, reason) -> {
            if (!ack) {
                // NACK do broker – mensagem não foi persistida
                System.err.println("[RABBIT] NACK do broker: " + reason);
            }
        });
        return template;
    }
}
