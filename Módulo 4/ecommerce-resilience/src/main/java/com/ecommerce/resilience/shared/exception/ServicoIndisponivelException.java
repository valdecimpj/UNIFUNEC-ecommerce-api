package com.ecommerce.resilience.shared.exception;

public class ServicoIndisponivelException extends RuntimeException {
    public ServicoIndisponivelException(String servico) {
        super("Serviço indisponível: " + servico + ". Tente novamente mais tarde.");
    }
}
