package com.ecommerce.domain.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Categoria de produto.
 * Neste projeto simplificado a entidade JPA é usada direto no domínio.
 * Em projetos maiores, separa-se CategoriaEntity (infra) de Categoria (domínio).
 */
@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nome;

    @Column(length = 300)
    private String descricao;
}
