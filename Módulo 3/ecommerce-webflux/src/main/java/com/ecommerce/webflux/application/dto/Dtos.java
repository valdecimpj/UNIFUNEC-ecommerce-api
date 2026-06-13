package com.ecommerce.webflux.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// ─────────────────────────────────────────────────────────────
// Produto DTOs
// ─────────────────────────────────────────────────────────────

@Getter @Setter
class CriarProdutoDTO {
    @NotBlank @Size(min = 3, max = 100)
    public String nome;
    @Size(max = 500)
    public String descricao;
    @NotNull @DecimalMin("0.01")
    public BigDecimal preco;
    @NotNull @Min(0)
    public Integer estoque;
}

@Getter @Builder
class ProdutoResponseDTO {
    public Long id;
    public String nome;
    public String descricao;
    public BigDecimal preco;
    public Integer estoque;
    public boolean ativo;
}

// ─────────────────────────────────────────────────────────────
// Pedido DTOs
// ─────────────────────────────────────────────────────────────

@Getter @Setter
class CriarPedidoDTO {
    @NotNull
    public Long clienteId;
    @NotBlank
    public String cep;
    @NotEmpty @Valid
    public List<ItemPedidoDTO> itens;
}

@Getter @Setter
class ItemPedidoDTO {
    @NotNull public Long produtoId;
    @NotNull @Min(1) public Integer quantidade;
}

@Getter @Builder
class PedidoResponseDTO {
    public Long id;
    public Long clienteId;
    public String status;
    public BigDecimal frete;
    public BigDecimal total;
    public List<ItemPedidoResponseDTO> itens;
    public LocalDateTime criadoEm;
}

@Getter @Builder
class ItemPedidoResponseDTO {
    public Long produtoId;
    public String nomeProduto;
    public Integer quantidade;
    public BigDecimal precoUnitario;
    public BigDecimal subtotal;
}

// ─────────────────────────────────────────────────────────────
// Avaliação DTOs
// ─────────────────────────────────────────────────────────────

@Getter @Setter
class CriarAvaliacaoDTO {
    @NotNull public Long produtoId;
    @NotNull public Long clienteId;
    @NotBlank public String nomeCliente;
    @NotNull @Min(1) @Max(5) public Integer nota;
    @Size(max = 1000) public String comentario;
}

@Getter @Builder
class AvaliacaoResponseDTO {
    public String id;
    public Long produtoId;
    public String nomeCliente;
    public Integer nota;
    public String comentario;
    public LocalDateTime criadoEm;
}
