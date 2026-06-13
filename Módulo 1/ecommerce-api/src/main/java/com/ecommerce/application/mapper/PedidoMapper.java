package com.ecommerce.application.mapper;

import com.ecommerce.application.dto.*;
import com.ecommerce.domain.model.ItemPedido;
import com.ecommerce.domain.model.Pedido;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper gerado em compile-time pelo MapStruct.
 * componentModel = "spring" → torna o mapper um @Component injetável.
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PedidoMapper {

    @Mapping(source = "clienteId", target = "clienteId")
    @Mapping(target = "totalComFrete",  expression = "java(pedido.calcularTotal())")
    @Mapping(target = "criadoEmFormatado",
             expression = "java(pedido.getCriadoEm() != null ? pedido.getCriadoEm().format(java.time.format.DateTimeFormatter.ofPattern(\"dd/MM/yyyy HH:mm\")) : null)")
    PedidoResponseDTO toResponseDTO(Pedido pedido);

    List<PedidoResponseDTO> toResponseDTOList(List<Pedido> pedidos);

    // Mapeamento de item
    @Mapping(source = "produto.id",    target = "produtoId")
    @Mapping(source = "produto.nome",  target = "nomeProduto")
    @Mapping(source = "precoUnitario", target = "precoUnitario")
    @Mapping(target = "subtotal",      expression = "java(item.calcularSubtotal())")
    ItemPedidoResponseDTO toItemDTO(ItemPedido item);
}
