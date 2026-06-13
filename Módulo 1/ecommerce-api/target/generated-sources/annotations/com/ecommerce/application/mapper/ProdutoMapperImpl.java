package com.ecommerce.application.mapper;

import com.ecommerce.application.dto.AtualizarProdutoDTO;
import com.ecommerce.application.dto.CriarProdutoDTO;
import com.ecommerce.application.dto.ProdutoResponseDTO;
import com.ecommerce.domain.model.Categoria;
import com.ecommerce.domain.model.Produto;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-13T13:42:41-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class ProdutoMapperImpl implements ProdutoMapper {

    @Override
    public ProdutoResponseDTO toResponseDTO(Produto produto) {
        if ( produto == null ) {
            return null;
        }

        ProdutoResponseDTO.ProdutoResponseDTOBuilder produtoResponseDTO = ProdutoResponseDTO.builder();

        produtoResponseDTO.categoriaNome( produtoCategoriaNome( produto ) );
        produtoResponseDTO.ativo( produto.isAtivo() );
        produtoResponseDTO.descricao( produto.getDescricao() );
        produtoResponseDTO.estoque( produto.getEstoque() );
        produtoResponseDTO.id( produto.getId() );
        produtoResponseDTO.nome( produto.getNome() );
        produtoResponseDTO.preco( produto.getPreco() );

        return produtoResponseDTO.build();
    }

    @Override
    public List<ProdutoResponseDTO> toResponseDTOList(List<Produto> produtos) {
        if ( produtos == null ) {
            return null;
        }

        List<ProdutoResponseDTO> list = new ArrayList<ProdutoResponseDTO>( produtos.size() );
        for ( Produto produto : produtos ) {
            list.add( toResponseDTO( produto ) );
        }

        return list;
    }

    @Override
    public Produto toDomain(CriarProdutoDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Produto.ProdutoBuilder produto = Produto.builder();

        produto.descricao( dto.getDescricao() );
        produto.estoque( dto.getEstoque() );
        produto.nome( dto.getNome() );
        produto.preco( dto.getPreco() );

        produto.ativo( true );

        return produto.build();
    }

    @Override
    public void updateFromDTO(AtualizarProdutoDTO dto, Produto produto) {
        if ( dto == null ) {
            return;
        }

        if ( dto.getDescricao() != null ) {
            produto.setDescricao( dto.getDescricao() );
        }
        if ( dto.getEstoque() != null ) {
            produto.setEstoque( dto.getEstoque() );
        }
        if ( dto.getNome() != null ) {
            produto.setNome( dto.getNome() );
        }
        if ( dto.getPreco() != null ) {
            produto.setPreco( dto.getPreco() );
        }
    }

    private String produtoCategoriaNome(Produto produto) {
        if ( produto == null ) {
            return null;
        }
        Categoria categoria = produto.getCategoria();
        if ( categoria == null ) {
            return null;
        }
        String nome = categoria.getNome();
        if ( nome == null ) {
            return null;
        }
        return nome;
    }
}
