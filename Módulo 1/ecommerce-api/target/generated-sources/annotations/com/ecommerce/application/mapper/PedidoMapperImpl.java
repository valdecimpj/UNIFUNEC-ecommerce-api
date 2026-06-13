package com.ecommerce.application.mapper;

import com.ecommerce.application.dto.ItemPedidoResponseDTO;
import com.ecommerce.application.dto.PedidoResponseDTO;
import com.ecommerce.domain.model.ItemPedido;
import com.ecommerce.domain.model.Pedido;
import com.ecommerce.domain.model.Produto;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-12T16:51:42-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class PedidoMapperImpl implements PedidoMapper {

    @Override
    public PedidoResponseDTO toResponseDTO(Pedido pedido) {
        if ( pedido == null ) {
            return null;
        }

        PedidoResponseDTO.PedidoResponseDTOBuilder pedidoResponseDTO = PedidoResponseDTO.builder();

        pedidoResponseDTO.clienteId( pedido.getClienteId() );
        pedidoResponseDTO.id( pedido.getId() );
        pedidoResponseDTO.itens( itemPedidoListToItemPedidoResponseDTOList( pedido.getItens() ) );
        pedidoResponseDTO.frete( pedido.getFrete() );
        pedidoResponseDTO.status( pedido.getStatus() );

        pedidoResponseDTO.totalComFrete( pedido.calcularTotal() );
        pedidoResponseDTO.criadoEmFormatado( pedido.getCriadoEm() != null ? pedido.getCriadoEm().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : null );

        return pedidoResponseDTO.build();
    }

    @Override
    public List<PedidoResponseDTO> toResponseDTOList(List<Pedido> pedidos) {
        if ( pedidos == null ) {
            return null;
        }

        List<PedidoResponseDTO> list = new ArrayList<PedidoResponseDTO>( pedidos.size() );
        for ( Pedido pedido : pedidos ) {
            list.add( toResponseDTO( pedido ) );
        }

        return list;
    }

    @Override
    public ItemPedidoResponseDTO toItemDTO(ItemPedido item) {
        if ( item == null ) {
            return null;
        }

        ItemPedidoResponseDTO.ItemPedidoResponseDTOBuilder itemPedidoResponseDTO = ItemPedidoResponseDTO.builder();

        itemPedidoResponseDTO.produtoId( itemProdutoId( item ) );
        itemPedidoResponseDTO.nomeProduto( itemProdutoNome( item ) );
        itemPedidoResponseDTO.precoUnitario( item.getPrecoUnitario() );
        itemPedidoResponseDTO.quantidade( item.getQuantidade() );

        itemPedidoResponseDTO.subtotal( item.calcularSubtotal() );

        return itemPedidoResponseDTO.build();
    }

    protected List<ItemPedidoResponseDTO> itemPedidoListToItemPedidoResponseDTOList(List<ItemPedido> list) {
        if ( list == null ) {
            return null;
        }

        List<ItemPedidoResponseDTO> list1 = new ArrayList<ItemPedidoResponseDTO>( list.size() );
        for ( ItemPedido itemPedido : list ) {
            list1.add( toItemDTO( itemPedido ) );
        }

        return list1;
    }

    private Long itemProdutoId(ItemPedido itemPedido) {
        if ( itemPedido == null ) {
            return null;
        }
        Produto produto = itemPedido.getProduto();
        if ( produto == null ) {
            return null;
        }
        Long id = produto.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String itemProdutoNome(ItemPedido itemPedido) {
        if ( itemPedido == null ) {
            return null;
        }
        Produto produto = itemPedido.getProduto();
        if ( produto == null ) {
            return null;
        }
        String nome = produto.getNome();
        if ( nome == null ) {
            return null;
        }
        return nome;
    }
}
