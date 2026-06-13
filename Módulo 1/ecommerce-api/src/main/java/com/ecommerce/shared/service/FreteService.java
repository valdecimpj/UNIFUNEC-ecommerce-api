package com.ecommerce.shared.service;

import com.ecommerce.domain.model.ItemPedido;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Serviço de domínio para cálculo de frete.
 * Responsabilidade única: calcular o frete com base no CEP e peso total.
 */
@Service
@Slf4j
public class FreteService {

    private static final BigDecimal TAXA_POR_KG = BigDecimal.valueOf(2.50);
    private static final BigDecimal FRETE_MINIMO  = BigDecimal.valueOf(8.00);
    private static final BigDecimal FRETE_GRATIS_ACIMA = BigDecimal.valueOf(300.00);

    public BigDecimal calcular(String cep, List<ItemPedido> itens) {
        // Simulação de cálculo de frete por peso
        double pesoTotalKg = itens.stream()
                .mapToDouble(i -> 0.5 * i.getQuantidade()) // 500g por item (simulado)
                .sum();

        BigDecimal subtotal = itens.stream()
                .map(ItemPedido::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Frete grátis acima de R$300
        if (subtotal.compareTo(FRETE_GRATIS_ACIMA) >= 0) {
            log.info("Frete grátis aplicado para subtotal={}", subtotal);
            return BigDecimal.ZERO;
        }

        BigDecimal frete = TAXA_POR_KG.multiply(BigDecimal.valueOf(pesoTotalKg));
        BigDecimal freteCalculado = frete.max(FRETE_MINIMO); // mínimo de R$8,00

        log.info("Frete calculado: R${} para CEP={} peso={}kg", freteCalculado, cep, pesoTotalKg);
        return freteCalculado;
    }
}
