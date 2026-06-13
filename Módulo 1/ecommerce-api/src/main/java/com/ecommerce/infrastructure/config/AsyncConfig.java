package com.ecommerce.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configuração de pools de threads para processamento assíncrono.
 * Cada domínio tem seu próprio pool para evitar contenção de recursos.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Pool dedicado para tarefas de pedido (estoque, processamento).
     * Não bloqueia o pool HTTP do Tomcat.
     */
    @Bean(name = "pedidoTaskExecutor")
    public Executor pedidoTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("pedido-async-");
        // CallerRunsPolicy: se a fila estiver cheia, executa na thread chamadora
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Pool dedicado para envio de e-mails e notificações.
     * Menor pois notificações são tolerantes a atraso.
     */
    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("email-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.initialize();
        return executor;
    }

    /**
     * Handler global de exceções em métodos @Async.
     * Sem isso, exceções assíncronas são silenciadas.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
            log.error("[ASYNC ERROR] Método: {} | Parâmetros: {} | Erro: {}",
                    method.getName(), params, throwable.getMessage(), throwable);
    }
}
