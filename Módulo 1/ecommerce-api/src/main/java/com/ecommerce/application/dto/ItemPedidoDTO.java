package com.ecommerce.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemPedidoDTO {

    @NotNull(message = "produtoId é obrigatório")
    private Long produtoId;

    @NotNull
    @Min(value = 1, message = "Quantidade mínima é 1")
    private Integer quantidade;
}
