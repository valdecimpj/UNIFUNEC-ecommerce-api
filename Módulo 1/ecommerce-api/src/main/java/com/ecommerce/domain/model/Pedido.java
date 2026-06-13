package com.ecommerce.domain.model;

import com.ecommerce.domain.exception.EstadoInvalidoException;
import com.ecommerce.domain.model.enums.StatusPedido;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade de domínio Pedido.
 * Toda regra de negócio (cancelamento, cálculo de total, validações de estado)
 * reside aqui – não nos Services.
 */
@Entity
@Table(name = "pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemPedido> itens = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal frete = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusPedido status = StatusPedido.PENDENTE;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @PrePersist
    private void prePersist() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
        if (this.status == null) this.status = StatusPedido.PENDENTE;
    }

    @PreUpdate
    private void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    // -------------------------------------------------------------------------
    // Regras de Negócio
    // -------------------------------------------------------------------------

    /**
     * Calcula o total do pedido (subtotal de todos os itens + frete).
     */
    public BigDecimal calcularTotal() {
        BigDecimal subtotal = itens.stream()
                .map(ItemPedido::calcularSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return subtotal.add(frete);
    }

    /**
     * Cancela o pedido. Lança exceção se já foi enviado ou entregue.
     */
    public void cancelar() {
        if (this.status == StatusPedido.ENVIADO || this.status == StatusPedido.ENTREGUE) {
            throw new EstadoInvalidoException(
                    "Pedido com status " + this.status + " não pode ser cancelado.");
        }
        this.status = StatusPedido.CANCELADO;
    }

    /**
     * Confirma o pedido (ex: após pagamento aprovado).
     */
    public void confirmar() {
        if (this.status != StatusPedido.PENDENTE) {
            throw new EstadoInvalidoException(
                    "Apenas pedidos PENDENTES podem ser confirmados. Status atual: " + this.status);
        }
        this.status = StatusPedido.CONFIRMADO;
    }

    public boolean podeSerCancelado() {
        return this.status == StatusPedido.PENDENTE
                || this.status == StatusPedido.CONFIRMADO
                || this.status == StatusPedido.EM_SEPARACAO;
    }

    /**
     * Adiciona item ao pedido garantindo integridade bidirecional.
     */
    public void adicionarItem(ItemPedido item) {
        item.setPedido(this);
        this.itens.add(item);
    }
}
