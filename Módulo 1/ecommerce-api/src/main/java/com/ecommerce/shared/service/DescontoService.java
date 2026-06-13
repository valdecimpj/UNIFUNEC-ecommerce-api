package com.ecommerce.shared.service;

import com.ecommerce.domain.model.Pedido;
import com.ecommerce.domain.model.enums.TipoCliente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Orquestrador de descontos.
 * Fechado para modificação: nunca precisa de if/else para novos tipos.
 * Aberto para extensão: basta criar nova DescontoStrategy com @Component.
 * Spring injeta TODAS as implementações automaticamente via List<DescontoStrategy>.
 */
@Service
@RequiredArgsConstructor
public class DescontoService {

    private final List<DescontoStrategy> strategies;

    public BigDecimal calcularDesconto(Pedido pedido, TipoCliente tipoCliente) {
        return strategies.stream()
                .filter(s -> s.aplicavelPara(tipoCliente))
                .findFirst()
                .map(s -> s.calcular(pedido))
                .orElse(BigDecimal.ZERO); // sem desconto para clientes regulares
    }
}
