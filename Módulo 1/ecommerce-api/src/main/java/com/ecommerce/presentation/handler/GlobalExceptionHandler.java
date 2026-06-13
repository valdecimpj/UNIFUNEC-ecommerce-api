package com.ecommerce.presentation.handler;

import com.ecommerce.domain.exception.*;
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

/**
 * Tratamento centralizado de exceções.
 * ProblemDetail (RFC 7807) – padrão Spring Boot 3 para respostas de erro.
 * Cada tipo de exceção retorna um status HTTP e corpo padronizados.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @SuppressWarnings("null")
@ExceptionHandler(PedidoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handlePedidoNaoEncontrado(PedidoNaoEncontradoException ex) {
        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Pedido não encontrado");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ProdutoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleProdutoNaoEncontrado(ProdutoNaoEncontradoException ex) {
        @SuppressWarnings("null")
        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Produto não encontrado");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ProblemDetail handleEstoqueInsuficiente(EstoqueInsuficienteException ex) {
        @SuppressWarnings("null")
        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Estoque insuficiente");
        problem.setProperty("produtoId", ex.getProdutoId());
        problem.setProperty("estoqueDisponivel", ex.getEstoqueDisponivel());
        problem.setProperty("quantidadeSolicitada", ex.getQuantidadeSolicitada());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(EstadoInvalidoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleEstadoInvalido(EstadoInvalidoException ex) {
        @SuppressWarnings("null")
        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Operação inválida para o estado atual");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    // Erros de validação do Bean Validation (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = ex.getBindingResult()
                .getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "valor inválido",
                        (e1, e2) -> e1 // mantém o primeiro se houver duplicata
                ));

        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados de entrada inválidos");
        problem.setTitle("Falha de validação");
        problem.setProperty("erros", erros);
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    // Fallback para erros não tratados
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGenerico(Exception ex) {
        log.error("Erro não tratado", ex);
        ProblemDetail problem = ProblemDetail
                .forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Erro interno. Contate o suporte.");
        problem.setTitle("Erro interno");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
