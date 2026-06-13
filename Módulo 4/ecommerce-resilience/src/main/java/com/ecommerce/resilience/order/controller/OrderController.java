package com.ecommerce.resilience.order.controller;

import com.ecommerce.resilience.order.client.EstoqueClient;
import com.ecommerce.resilience.order.dto.PedidoEventos;
import com.ecommerce.resilience.order.service.PedidoKafkaProducer;
import com.ecommerce.resilience.order.service.PedidoRabbitService;
import com.ecommerce.resilience.shared.config.CircuitBreakerMonitor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controller de Pedidos – demonstra todos os padrões de resiliência.
 */
@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Pedidos (Resiliência)", description = "Demonstração de Circuit Breaker, Retry, Rate Limiter")
public class OrderController {

    private final EstoqueClient estoqueClient;
    private final PedidoKafkaProducer kafkaProducer;
    private final PedidoRabbitService rabbitService;
    private final CircuitBreakerMonitor cbMonitor;

    // ── Criar Pedido ──────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Cria pedido – chama estoque com Circuit Breaker + Retry + Bulkhead")
    public Mono<ResponseEntity<Map<String, Object>>> criarPedido(
            @RequestParam Long clienteId,
            @RequestParam Long produtoId,
            @RequestParam int quantidade) {

        return estoqueClient.verificarDisponibilidade(produtoId, quantidade)
                .flatMap(disponivel -> {
                    if (!disponivel) {
                        return Mono.just(ResponseEntity.unprocessableEntity()
                                .<Map<String, Object>>body(Map.of(
                                    "erro", "Estoque insuficiente")));
                    }

                    // Evento para Kafka (Event Sourcing + auditoria)
                    var evento = new PedidoEventos.PedidoCriadoEvent(
                            System.currentTimeMillis(),
                            clienteId,
                            List.of(new PedidoEventos.ItemEvento(produtoId, quantidade, BigDecimal.valueOf(99.90))),
                            BigDecimal.valueOf(99.90),
                            "01310-100",
                            LocalDateTime.now()
                    );
                    kafkaProducer.publicarPedidoCriado(evento);

                    // Evento para RabbitMQ (notificações e processamento de pedido)
                    rabbitService.publicarPedidoCriado(evento);

                    return Mono.just(ResponseEntity.ok(Map.of(
                            "pedidoId", evento.pedidoId(),
                            "status", "CRIADO",
                            "mensagem", "Pedido criado e eventos publicados no Kafka e RabbitMQ"
                    )));
                });
    }

    // ── Status do Circuit Breaker ─────────────────────────────────────────────

    @GetMapping("/circuit-breaker/status")
    @Operation(summary = "Consulta o estado atual dos Circuit Breakers")
    public Map<String, String> circuitBreakerStatus() {
        return Map.of(
            "estoque-service", String.valueOf(cbMonitor.consultarEstado("estoque-service")),
            "pagamento-service", String.valueOf(cbMonitor.consultarEstado("pagamento-service")),
            "estoque-metricas", cbMonitor.resumoMetricas("estoque-service")
        );
    }

    // ── Simulação de falha para testar Circuit Breaker ───────────────────────

    @GetMapping("/simular-falha")
    @Operation(summary = "Simula chamada ao estoque para testar o Circuit Breaker")
    public Mono<Map<String, Object>> simularFalha(
            @RequestParam(defaultValue = "1") Long produtoId) {

        log.info("[TEST] Testando Circuit Breaker – será que o estoque responde?");

        return estoqueClient.verificarDisponibilidade(produtoId, 1)
                .map(ok -> Map.<String, Object>of(
                    "estoque", ok,
                    "circuitBreaker", String.valueOf(cbMonitor.consultarEstado("estoque-service")),
                    "mensagem", "Chamada bem-sucedida"
                ))
                .onErrorReturn(Map.of(
                    "erro", "Serviço indisponível – fallback acionado",
                    "circuitBreaker", String.valueOf(cbMonitor.consultarEstado("estoque-service"))
                ));
    }
}
