package com.ecommerce.domain.model;

import com.ecommerce.domain.exception.EstoqueInsuficienteException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entidade de domínio Produto.
 * Contém regras de negócio puras – validação de estoque, ativação, preço.
 */
@Entity
@Table(name = "produtos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private Integer estoque;

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    // -------------------------------------------------------------------------
    // Relacionamento
    // -------------------------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    // -------------------------------------------------------------------------
    // Regras de Negócio (domínio)
    // -------------------------------------------------------------------------

    /**
     * Valida se há estoque suficiente para a quantidade solicitada.
     * Lança exceção de domínio caso contrário.
     */
    public void validarEstoque(int quantidade) {
        if (this.estoque < quantidade) {
            throw new EstoqueInsuficienteException(this.id, this.estoque, quantidade);
        }
    }

    /**
     * Decrementa o estoque após validação.
     */
    public void decrementarEstoque(int quantidade) {
        validarEstoque(quantidade);
        this.estoque -= quantidade;
    }

    /**
     * Incrementa o estoque (ex: cancelamento de pedido).
     */
    public void incrementarEstoque(int quantidade) {
        this.estoque += quantidade;
    }

    /**
     * Aplica desconto percentual e retorna o preço final.
     */
    public BigDecimal calcularPrecoComDesconto(BigDecimal percentualDesconto) {
        BigDecimal fator = BigDecimal.ONE.subtract(
                percentualDesconto.divide(BigDecimal.valueOf(100)));
        return this.preco.multiply(fator);
    }

    public boolean estaDisponivel() {
        return this.ativo && this.estoque > 0;
    }
}
