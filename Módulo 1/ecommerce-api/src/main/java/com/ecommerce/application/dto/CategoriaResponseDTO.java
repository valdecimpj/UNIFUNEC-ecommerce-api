package com.ecommerce.application.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoriaResponseDTO {
    private Long id;
    private String nome;
    private String descricao;
}
