package com.ecommerce.webflux.presentation.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Tratamento global de erros no WebFlux.
 *
 * No WebFlux, @RestControllerAdvice com @ExceptionHandler também funciona,
 * mas ErrorWebExceptionHandler é mais baixo nível e captura erros do
 * pipeline reativo que não chegam ao Controller.
 *
 * @Order(-2) – prioridade alta para executar antes do DefaultErrorWebExceptionHandler
 */
@Component
@Order(-2)
@Slf4j
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        var response = exchange.getResponse();

        // Determina status HTTP baseado no tipo de erro
        HttpStatus status = determinarStatus(ex);
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = buildBody(status, ex.getMessage(), exchange.getRequest().getPath().value());
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        log.error("[ERROR] {} {} – {}: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getPath(),
                ex.getClass().getSimpleName(),
                ex.getMessage());

        return response.writeWith(Mono.just(buffer));
    }

    private HttpStatus determinarStatus(Throwable ex) {
        String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (msg.contains("não encontrado") || msg.contains("not found")) {
            return HttpStatus.NOT_FOUND;
        }
        if (msg.contains("estoque insuficiente") || msg.contains("inválido")) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }
        if (ex instanceof IllegalArgumentException) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String buildBody(HttpStatus status, String message, String path) {
        return """
            {
              "status": %d,
              "error": "%s",
              "message": "%s",
              "path": "%s",
              "timestamp": "%s"
            }
            """.formatted(
                status.value(),
                status.getReasonPhrase(),
                message != null ? message.replace("\"", "'") : "Erro interno",
                path,
                Instant.now()
        );
    }
}
