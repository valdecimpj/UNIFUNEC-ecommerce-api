package com.ecommerce.application.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO para atualização parcial (PATCH) de produto.
 * Todos os campos são opcionais – apenas os não-nulos serão aplicados via MapStruct.
 */
@Getter
@Setter
public class AtualizarProdutoDTO {

    @Size(min = 3, max = 100)
    private String nome;

    @Size(max = 500)
    private String descricao;

    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal preco;

    @Min(0)
    private Integer estoque;
}
