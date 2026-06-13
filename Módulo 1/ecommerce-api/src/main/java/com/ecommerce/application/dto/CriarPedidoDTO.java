package com.ecommerce.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CriarPedidoDTO {

    @NotNull(message = "clienteId é obrigatório")
    private Long clienteId;

    @NotBlank(message = "CEP é obrigatório para cálculo de frete")
    @Pattern(regexp = "\\d{5}-\\d{3}", message = "CEP deve estar no formato 00000-000")
    private String cep;

    @NotEmpty(message = "O pedido deve ter ao menos um item")
    @Valid
    private List<ItemPedidoDTO> itens;
}
