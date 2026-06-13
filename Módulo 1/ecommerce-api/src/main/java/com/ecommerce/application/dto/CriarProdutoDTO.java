package com.ecommerce.application.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CriarProdutoDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100)
    private String nome;

    @Size(max = 500)
    private String descricao;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 8, fraction = 2)
    private BigDecimal preco;

    @NotNull
    @Min(0)
    private Integer estoque;

    @NotNull(message = "categoriaId é obrigatório")
    private Long categoriaId;
}
