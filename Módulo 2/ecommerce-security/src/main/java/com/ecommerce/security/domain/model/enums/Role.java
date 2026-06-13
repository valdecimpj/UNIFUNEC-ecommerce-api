package com.ecommerce.security.domain.model.enums;

/**
 * Papéis de usuário.
 * Prefixo ROLE_ é exigido pelo Spring Security para hasRole() funcionar.
 * hasAuthority("ROLE_ADMIN") é equivalente a hasRole("ADMIN").
 */
public enum Role {
    ROLE_ADMIN,
    ROLE_GERENTE,
    ROLE_VENDEDOR,
    ROLE_CLIENTE
}
