package com.ecommerce.observability.presentation.handler;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private final Counter erros4xxCounter;
    private final Counter erros5xxCounter;

    public GlobalExceptionHandler(MeterRegistry registry) {
        this.erros4xxCounter = Counter.builder("http.erros")
                .tag("tipo", "4xx")
                .register(registry);
        this.erros5xxCounter = Counter.builder("http.erros")
                .tag("tipo", "5xx")
                .register(registry);
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNotFound(RuntimeException ex) {
        erros4xxCounter.increment();
        log.warn("[ERROR 404] {}", ex.getMessage());
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        p.setTitle("Recurso não encontrado");
        p.setProperty("timestamp", Instant.now());
        return p;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidacao(MethodArgumentNotValidException ex) {
        erros4xxCounter.increment();
        Map<String, String> erros = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "inválido",
                        (e1, e2) -> e1));
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos");
        p.setTitle("Falha de validação");
        p.setProperty("erros", erros);
        p.setProperty("timestamp", Instant.now());
        return p;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGenerico(Exception ex) {
        erros5xxCounter.increment();
        log.error("[ERROR 500] Erro não tratado", ex);
        ProblemDetail p = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno");
        p.setTitle("Erro interno");
        p.setProperty("timestamp", Instant.now());
        return p;
    }
}
