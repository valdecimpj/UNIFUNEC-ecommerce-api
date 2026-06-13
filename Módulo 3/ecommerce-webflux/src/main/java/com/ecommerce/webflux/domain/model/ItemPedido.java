package com.ecommerce.webflux.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table("itens_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedido {

    @Id
    private Long id;

    @Column("pedido_id")
    private Long pedidoId;

    @Column("produto_id")
    private Long produtoId;

    @Column("quantidade")
    private Integer quantidade;

    @Column("preco_unitario")
    private BigDecimal precoUnitario;

    public BigDecimal calcularSubtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
