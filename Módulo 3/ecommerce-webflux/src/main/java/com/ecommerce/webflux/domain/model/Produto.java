package com.ecommerce.webflux.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade mapeada via R2DBC.
 *
 * DIFERENÇAS para JPA:
 *   @Table/@Column vêm de spring.data.relational (não jakarta.persistence)
 *   Sem @Entity, sem @GeneratedValue – R2DBC usa convenções simples
 *   Sem relacionamentos lazy/eager – R2DBC é explícito (sem ORM complexo)
 *   Sem Hibernate SessionFactory – cada operação é uma query independente
 */
@Table("produtos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    private Long id;

    @Column("nome")
    private String nome;

    @Column("descricao")
    private String descricao;

    @Column("preco")
    private BigDecimal preco;

    @Column("estoque")
    private Integer estoque;

    @Column("ativo")
    @Builder.Default
    private boolean ativo = true;

    @Column("criado_em")
    private LocalDateTime criadoEm;

    // Regra de domínio
    public boolean estaDisponivel() {
        return this.ativo && this.estoque > 0;
    }

    public void decrementarEstoque(int quantidade) {
        if (this.estoque < quantidade) {
            throw new IllegalStateException(
                "Estoque insuficiente: disponível=" + estoque + ", solicitado=" + quantidade);
        }
        this.estoque -= quantidade;
    }
}
