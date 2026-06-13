package com.ecommerce.webflux.domain.model;

import com.ecommerce.webflux.domain.model.enums.StatusPedido;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Table("pedidos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    private Long id;

    @Column("cliente_id")
    private Long clienteId;

    @Column("status")
    @Builder.Default
    private StatusPedido status = StatusPedido.PENDENTE;

    @Column("frete")
    @Builder.Default
    private BigDecimal frete = BigDecimal.ZERO;

    @Column("criado_em")
    private LocalDateTime criadoEm;

    /**
     * R2DBC não suporta @OneToMany.
     * Itens são carregados separadamente no Use Case (sem lazy loading).
     * @Transient indica que este campo NÃO é persistido pela entidade.
     */
    @Transient
    @Builder.Default
    private List<ItemPedido> itens = new ArrayList<>();
}
