package com.ecommerce.application.mapper;

import com.ecommerce.application.dto.AtualizarCategoriaDTO;
import com.ecommerce.application.dto.CategoriaResponseDTO;
import com.ecommerce.application.dto.CriarCategoriaDTO;
import com.ecommerce.domain.model.Categoria;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-13T13:42:41-0300",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.0.v20260407-0427, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class CategoriaMapperImpl implements CategoriaMapper {

    @Override
    public Categoria toDomain(CriarCategoriaDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Categoria.CategoriaBuilder categoria = Categoria.builder();

        categoria.descricao( dto.getDescricao() );
        categoria.nome( dto.getNome() );

        return categoria.build();
    }

    @Override
    public CategoriaResponseDTO toResponseDTO(Categoria categoria) {
        if ( categoria == null ) {
            return null;
        }

        CategoriaResponseDTO.CategoriaResponseDTOBuilder categoriaResponseDTO = CategoriaResponseDTO.builder();

        categoriaResponseDTO.descricao( categoria.getDescricao() );
        categoriaResponseDTO.id( categoria.getId() );
        categoriaResponseDTO.nome( categoria.getNome() );

        return categoriaResponseDTO.build();
    }

    @Override
    public void updateFromDTO(AtualizarCategoriaDTO dto, Categoria categoria) {
        if ( dto == null ) {
            return;
        }

        if ( dto.getDescricao() != null ) {
            categoria.setDescricao( dto.getDescricao() );
        }
        if ( dto.getNome() != null ) {
            categoria.setNome( dto.getNome() );
        }
    }
}
