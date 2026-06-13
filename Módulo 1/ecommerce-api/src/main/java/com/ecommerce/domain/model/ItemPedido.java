package com.ecommerce.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Item de pedido – value object com lógica de subtotal.
 */
@Entity
@Table(name = "itens_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private Integer quantidade;

    /**
     * Preço unitário no momento da compra (imutável após criação).
     */
    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;

    // -------------------------------------------------------------------------
    // Regra de negócio
    // -------------------------------------------------------------------------

    /**
     * Calcula o subtotal do item (preço × quantidade).
     */
    public BigDecimal calcularSubtotal() {
        return this.precoUnitario.multiply(BigDecimal.valueOf(this.quantidade));
    }

    /**
     * Factory – garante que o preço unitário é capturado no momento da criação.
     */
    public static ItemPedido of(Produto produto, int quantidade) {
        return ItemPedido.builder()
                .produto(produto)
                .quantidade(quantidade)
                .precoUnitario(produto.getPreco()) // snapshot do preço atual
                .build();
    }
}
