package com.ecommerce.shared.service;

import com.ecommerce.domain.model.Pedido;
import com.ecommerce.domain.model.enums.TipoCliente;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Abstração para estratégias de desconto (padrão Strategy + Open/Closed Principle).
 * Para adicionar um novo tipo de desconto, basta criar uma nova implementação
 * com @Component – sem modificar DescontoService ou as implementações existentes.
 */
public interface DescontoStrategy {
    BigDecimal calcular(Pedido pedido);
    boolean aplicavelPara(TipoCliente tipoCliente);
}

@Component
class DescontoClienteVIP implements DescontoStrategy {
    private static final BigDecimal PERCENTUAL_VIP = BigDecimal.valueOf(0.15); // 15%

    @Override
    public BigDecimal calcular(Pedido pedido) {
        return pedido.calcularTotal().multiply(PERCENTUAL_VIP);
    }

    @Override
    public boolean aplicavelPara(TipoCliente tipo) {
        return tipo == TipoCliente.VIP;
    }
}

@Component
class DescontoClienteNovo implements DescontoStrategy {
    private static final BigDecimal DESCONTO_FIXO = BigDecimal.valueOf(20.00);

    @Override
    public BigDecimal calcular(Pedido pedido) {
        return DESCONTO_FIXO;
    }

    @Override
    public boolean aplicavelPara(TipoCliente tipo) {
        return tipo == TipoCliente.NOVO;
    }
}

@Component
class DescontoClienteCorporativo implements DescontoStrategy {
    private static final BigDecimal PERCENTUAL_CORP = BigDecimal.valueOf(0.20); // 20%

    @Override
    public BigDecimal calcular(Pedido pedido) {
        return pedido.calcularTotal().multiply(PERCENTUAL_CORP);
    }

    @Override
    public boolean aplicavelPara(TipoCliente tipo) {
        return tipo == TipoCliente.CORPORATIVO;
    }
}
