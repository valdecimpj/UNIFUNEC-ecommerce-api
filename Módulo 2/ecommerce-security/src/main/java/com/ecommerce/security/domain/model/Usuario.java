package com.ecommerce.security.domain.model;

import com.ecommerce.security.domain.model.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

/**
 * Entidade Usuario implementa UserDetails do Spring Security.
 * Isso permite que o Spring Security gerencie autenticação diretamente
 * a partir do banco de dados, sem precisar de um adapter separado.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    // username no contexto do Spring Security = email aqui
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String senha; // sempre armazenar com BCrypt

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_roles",
                     joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = Set.of(Role.ROLE_CLIENTE);

    @Column(nullable = false)
    @Builder.Default
    private boolean ativo = true;

    // -------------------------------------------------------------------------
    // UserDetails – contrato com Spring Security
    // -------------------------------------------------------------------------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Converte cada Role em SimpleGrantedAuthority
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.name()))
                .toList();
    }

    @Override
    public String getPassword() {
        return this.senha;
    }

    @Override
    public String getUsername() {
        // Spring Security usa getUsername() – aqui mapeamos para o email
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return this.ativo; }
}
