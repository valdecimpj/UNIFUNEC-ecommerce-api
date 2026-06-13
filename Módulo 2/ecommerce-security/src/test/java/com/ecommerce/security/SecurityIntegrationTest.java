package com.ecommerce.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de Segurança com Spring Security Test.
 *
 * Ferramentas disponíveis:
 *
 * @WithMockUser         – simula usuário autenticado (útil para MVC clássico)
 * @WithSecurityContext  – cria SecurityContext customizado
 * jwt()                 – simula JWT para Resource Server (Spring Boot 3)
 * mockOAuth2Login()     – simula login OAuth2
 *
 * Boas práticas:
 *   ✅ Teste de autorização: verifique 403 em rotas restritas
 *   ✅ Teste de autenticação: verifique 401 em rotas protegidas sem token
 *   ✅ Teste de roles: verifique que role errada retorna 403
 *   ✅ Teste de regra de negócio: verifique que role certa retorna 2xx
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Testes de Segurança – Autorização e Autenticação")
class SecurityIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    // =========================================================================
    // TESTES DE AUTENTICAÇÃO (401)
    // =========================================================================

    @Test
    @DisplayName("401 – Endpoint protegido sem token deve retornar Unauthorized")
    void semToken_deveRetornar401() throws Exception {
        mockMvc.perform(post("/api/v1/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Produto Teste\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("200 – Endpoint público GET /produtos não precisa de token")
    void endpointPublico_deveRetornar200SemToken() throws Exception {
        mockMvc.perform(get("/api/v1/produtos/1"))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // TESTES COM @WithMockUser – simula usuário autenticado
    // Útil para controllers que usam Authentication.getName() mas não lêem JWT
    // =========================================================================

    @Test
    @WithMockUser(username = "admin@ecommerce.com", roles = {"ADMIN"})
    @DisplayName("201 – ADMIN pode criar produto (@WithMockUser)")
    void admin_podeCriarProduto() throws Exception {
        mockMvc.perform(post("/api/v1/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Produto Admin\",\"preco\":99.90}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "cliente@ecommerce.com", roles = {"CLIENTE"})
    @DisplayName("403 – CLIENTE não pode criar produto (@WithMockUser)")
    void cliente_naoPoderCriarProduto() throws Exception {
        mockMvc.perform(post("/api/v1/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Tentativa\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"CLIENTE"})
    @DisplayName("403 – CLIENTE não pode acessar /admin")
    void cliente_naoPoderAcessarAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/usuarios/1"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // TESTES COM jwt() – simula JWT para Resource Server stateless
    // Preferível ao @WithMockUser quando o controller lê claims do JWT
    // =========================================================================

    @Test
    @DisplayName("200 – JWT com role ADMIN pode criar produto")
    void jwtAdmin_podeCriarProduto() throws Exception {
        mockMvc.perform(post("/api/v1/produtos")
                .with(jwt()
                    .jwt(token -> token
                        .subject("admin@ecommerce.com")
                        .claim("roles", java.util.List.of("ROLE_ADMIN"))
                        .claim("nome", "Admin Teste")
                        .claim("email", "admin@ecommerce.com")
                    )
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Produto JWT\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("403 – JWT com role VENDEDOR não pode deletar produto")
    void jwtVendedor_naoPoderDeletar() throws Exception {
        mockMvc.perform(delete("/api/v1/produtos/1")
                .with(jwt()
                    .jwt(token -> token
                        .subject("vendedor@ecommerce.com")
                        .claim("roles", java.util.List.of("ROLE_VENDEDOR"))
                    )
                ))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("200 – Claims customizados acessíveis via @AuthenticationPrincipal Jwt")
    void jwtComClaimsCustomizados_deveRetornarClaims() throws Exception {
        mockMvc.perform(get("/api/v1/produtos/claims-demo")
                .with(jwt()
                    .jwt(token -> token
                        .subject("usuario@ecommerce.com")
                        .claim("roles", java.util.List.of("ROLE_CLIENTE"))
                        .claim("nome",  "Usuário Teste")
                        .claim("email", "usuario@ecommerce.com")
                        .claim("userId", 42L)
                    )
                ))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // TESTES DE ADMIN
    // =========================================================================

    @Test
    @WithMockUser(roles = {"ADMIN"})
    @DisplayName("200 – ADMIN pode promover usuário a GERENTE")
    void admin_podePormoverGerente() throws Exception {
        mockMvc.perform(put("/api/v1/admin/usuarios/1/promover-gerente"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"GERENTE"})
    @DisplayName("403 – GERENTE não pode acessar /admin (apenas ADMIN)")
    void gerente_naoPoderAcessarAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/usuarios/1"))
                .andExpect(status().isForbidden());
    }
}
