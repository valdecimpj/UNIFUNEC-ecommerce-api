package com.ecommerce.webflux.infrastructure.config;

import com.ecommerce.webflux.application.usecase.PedidoService;
import com.ecommerce.webflux.domain.model.Pedido;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Roteamento funcional do WebFlux.
 *
 * Alternativa ao @RestController – mais explícita e testável.
 *
 * Vantagens do estilo funcional:
 *   ✅ Rotas declaradas em um único lugar (RouterFunction)
 *   ✅ Handler separado da definição da rota
 *   ✅ Mais fácil de testar HandlerFunction isolada
 *   ✅ Melhor performance (sem reflexão de anotações)
 *
 * Quando usar:
 *   - APIs simples com poucas rotas
 *   - Funções lambda (serverless / lightweight)
 *   - Quando quiser composição explícita de rotas
 */
@Configuration
@RequiredArgsConstructor
public class RouterConfig {

    private final PedidoService pedidoService;

    @Bean
    public RouterFunction<ServerResponse> pedidoRouter() {
        return RouterFunctions.route()

                // GET /api/v1/pedidos/{id}
                .GET("/api/v1/pedidos/{id}",
                        request -> {
                            Long id = Long.parseLong(request.pathVariable("id"));
                            return pedidoService.buscarPorIdComItens(id)
                                    .flatMap(pedido -> ServerResponse.ok()
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .bodyValue(pedido))
                                    .onErrorResume(RuntimeException.class, ex ->
                                            ServerResponse.notFound().build());
                        })

                // GET /api/v1/pedidos/cliente/{clienteId}
                .GET("/api/v1/pedidos/cliente/{clienteId}",
                        request -> {
                            Long clienteId = Long.parseLong(request.pathVariable("clienteId"));
                            return ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(pedidoService.listarPorClienteComItens(clienteId),
                                          Pedido.class);
                        })

                // POST /api/v1/pedidos
                .POST("/api/v1/pedidos",
                        request -> request.bodyToMono(CriarPedidoRequest.class)
                                .flatMap(dto -> {
                                    Map<Long, Integer> itens = new HashMap<>();
                                    dto.itens().forEach(i -> itens.put(i.produtoId(), i.quantidade()));
                                    return pedidoService.criarPedido(dto.clienteId(), itens);
                                })
                                .flatMap(pedido -> ServerResponse
                                        .status(org.springframework.http.HttpStatus.CREATED)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(pedido))
                                .onErrorResume(RuntimeException.class, ex ->
                                        ServerResponse.badRequest()
                                                .bodyValue(Map.of("erro", ex.getMessage()))))

                .build();
    }

    // Records para o body do POST (Java 21 – sem Lombok necessário)
    record CriarPedidoRequest(Long clienteId, java.util.List<ItemRequest> itens) {}
    record ItemRequest(Long produtoId, Integer quantidade) {}
}
