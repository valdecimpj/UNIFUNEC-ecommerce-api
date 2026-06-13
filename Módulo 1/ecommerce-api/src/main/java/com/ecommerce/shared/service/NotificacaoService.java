package com.ecommerce.shared.service;

import com.ecommerce.domain.model.Pedido;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Serviço de notificações.
 * Métodos @Async executam em thread separada (emailTaskExecutor).
 * Retornam CompletableFuture para composição e tratamento de erros.
 */
@Service
@Slf4j
public class NotificacaoService {

    @Async("emailTaskExecutor")
    public CompletableFuture<Void> notificarCriacao(Pedido pedido) {
        log.info("[EMAIL] Enviando confirmação de pedido id={} | thread={}",
                pedido.getId(), Thread.currentThread().getName());
        try {
            // Simulação de chamada a serviço externo de email
            Thread.sleep(200);
            log.info("[EMAIL] Confirmação enviada para pedido id={}", pedido.getId());
            return CompletableFuture.completedFuture(null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("[EMAIL] Falha ao enviar email para pedido id={}", pedido.getId(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Void> notificarCancelamento(Pedido pedido, String motivo) {
        log.info("[EMAIL] Enviando aviso de cancelamento pedido id={} motivo={} | thread={}",
                pedido.getId(), motivo, Thread.currentThread().getName());
        try {
            Thread.sleep(200);
            log.info("[EMAIL] Aviso de cancelamento enviado para pedido id={}", pedido.getId());
            return CompletableFuture.completedFuture(null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.failedFuture(e);
        }
    }
}
