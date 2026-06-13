package com.ecommerce.security.application.dto;

import com.ecommerce.security.domain.model.Usuario;
import com.ecommerce.security.domain.model.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
public class UsuarioResponseDTO {
    private Long id;
    private String nome;
    private String email;
    private Set<Role> roles;
    private boolean ativo;

    // Factory – nunca expõe a senha
    public static UsuarioResponseDTO from(Usuario usuario) {
        return UsuarioResponseDTO.builder()
                .id(usuario.getId())
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .roles(usuario.getRoles())
                .ativo(usuario.isAtivo())
                .build();
    }
}
