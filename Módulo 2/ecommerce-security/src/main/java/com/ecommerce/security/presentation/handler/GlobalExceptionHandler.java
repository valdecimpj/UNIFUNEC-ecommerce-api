package com.ecommerce.security.presentation.handler;

import com.ecommerce.security.domain.exception.EmailJaCadastradoException;
import com.ecommerce.security.domain.exception.UsuarioNaoEncontradoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tratamento centralizado de erros de segurança e domínio.
 *
 * IMPORTANTE: AccessDeniedException (403) e AuthenticationException (401)
 * são capturadas pelo Spring Security ANTES de chegar aqui.
 * Para personalizá-las, configure:
 *   .exceptionHandling(e -> e
 *     .authenticationEntryPoint(...)   // 401
 *     .accessDeniedHandler(...)        // 403
 *   )
 * no SecurityConfig.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 401 – Credenciais inválidas (email/senha errados)
    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        // ⚠️ Mensagem genérica proposital – não revele qual campo está errado
        ProblemDetail p = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
        p.setTitle("Autenticação falhou");
        p.setProperty("timestamp", Instant.now());
        return p;
    }

    // 401 – Usuário desativado
    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleDisabled(DisabledException ex) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Conta desativada. Contate o suporte.");
        p.setTitle("Conta desativada");
        p.setProperty("timestamp", Instant.now());
        return p;
    }

    // 401 – JWT inválido, expirado ou malformado
    @ExceptionHandler(JwtException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleJwt(JwtException ex) {
        log.warn("JWT inválido: {}", ex.getMessage());
        ProblemDetail p = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Token inválido ou expirado");
        p.setTitle("Token inválido");
        p.setProperty("timestamp", Instant.now());
        return p;
    }

    // 403 – Usuário autenticado mas sem permissão
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "Você não tem permissão para esta operação");
        p.setTitle("Acesso negado");
        p.setProperty("timestamp", Instant.now());
        return p;
    }

    // 404 – Usuário não encontrado
    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleUsuarioNaoEncontrado(UsuarioNaoEncontradoException ex) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        p.setTitle("Usuário não encontrado");
        p.setProperty("timestamp", Instant.now());
        return p;
    }

    // 409 – Email duplicado
    @ExceptionHandler(EmailJaCadastradoException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleEmailDuplicado(EmailJaCadastradoException ex) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        p.setTitle("E-mail já cadastrado");
        p.setProperty("timestamp", Instant.now());
        return p;
    }

    // 400 – Erros de validação (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> erros = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "inválido",
                        (e1, e2) -> e1
                ));
        ProblemDetail p = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Dados inválidos");
        p.setTitle("Falha de validação");
        p.setProperty("erros", erros);
        p.setProperty("timestamp", Instant.now());
        return p;
    }

    // 500 – Fallback
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGenerico(Exception ex) {
        log.error("Erro não tratado", ex);
        ProblemDetail p = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno");
        p.setTitle("Erro interno");
        p.setProperty("timestamp", Instant.now());
        return p;
    }
}
