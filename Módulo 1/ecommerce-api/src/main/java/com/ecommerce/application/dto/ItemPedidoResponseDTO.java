package com.ecommerce.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ItemPedidoResponseDTO {
    private Long produtoId;
    private String nomeProduto;
    private BigDecimal precoUnitario;
    private Integer quantidade;
    private BigDecimal subtotal;
}
