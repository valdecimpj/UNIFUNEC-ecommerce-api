package com.ecommerce.application.mapper;

import com.ecommerce.application.dto.AtualizarProdutoDTO;
import com.ecommerce.application.dto.CriarProdutoDTO;
import com.ecommerce.application.dto.ProdutoResponseDTO;
import com.ecommerce.domain.model.Produto;
import org.mapstruct.*;

import java.util.List;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProdutoMapper {

    // Mapeamento simples – campos com o mesmo nome são mapeados automaticamente
    @Mapping(source = "categoria.nome", target = "categoriaNome")
    ProdutoResponseDTO toResponseDTO(Produto produto);

    List<ProdutoResponseDTO> toResponseDTOList(List<Produto> produtos);

    // CriarProdutoDTO → Produto (categoria precisa ser resolvida depois pelo use case)
    @Mapping(target = "id",       ignore = true)
    @Mapping(target = "categoria", ignore = true) // resolvido pelo use case via categoriaId
    @Mapping(target = "ativo",    constant = "true")
    Produto toDomain(CriarProdutoDTO dto);

    /**
     * Atualização parcial (PATCH) – campos nulos do DTO são ignorados.
     * NullValuePropertyMappingStrategy.IGNORE garante que apenas campos
     * não-nulos do DTO sobrescrevam o entity existente.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",       ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "ativo",    ignore = true)
    void updateFromDTO(AtualizarProdutoDTO dto, @MappingTarget Produto produto);
}
