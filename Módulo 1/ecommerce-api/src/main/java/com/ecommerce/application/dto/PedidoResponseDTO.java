package com.ecommerce.application.dto;

import com.ecommerce.domain.model.enums.StatusPedido;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class PedidoResponseDTO {
    private Long id;
    private Long clienteId;
    private List<ItemPedidoResponseDTO> itens;
    private BigDecimal frete;
    private BigDecimal totalComFrete;
    private StatusPedido status;
    private String criadoEmFormatado;
}
