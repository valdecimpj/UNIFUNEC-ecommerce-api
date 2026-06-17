package com.ecommerce.application.mapper;

import com.ecommerce.application.dto.AtualizarCategoriaDTO;
import com.ecommerce.application.dto.CategoriaResponseDTO;
import com.ecommerce.application.dto.CriarCategoriaDTO;
import com.ecommerce.domain.model.Categoria;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CategoriaMapper {

    @Mapping(target = "id", ignore = true)
    Categoria toDomain(CriarCategoriaDTO dto);

    CategoriaResponseDTO toResponseDTO(Categoria categoria);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateFromDTO(AtualizarCategoriaDTO dto, @MappingTarget Categoria categoria);
}
