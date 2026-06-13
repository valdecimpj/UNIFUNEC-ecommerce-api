package com.ecommerce.security.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Controller de produtos demonstrando CONTROLE DE ACESSO FINO.
 *
 * Hierarquia de anotações:
 *   @PreAuthorize  → verifica ANTES de executar o método
 *   @PostAuthorize → verifica DEPOIS, com acesso ao returnObject
 *   @PreFilter     → filtra coleção de ENTRADA antes do método
 *   @PostFilter    → filtra coleção de RETORNO após o método
 *
 * SpEL disponível:
 *   #variavel       → parâmetro do método
 *   returnObject    → valor retornado (@PostAuthorize/@PostFilter)
 *   authentication  → objeto Authentication atual
 *   principal       → principal do Authentication
 *   hasRole()       → verifica role com prefixo ROLE_
 *   hasAuthority()  → verifica authority exata
 *   hasAnyRole()    → qualquer um dos roles listados
 */
@RestController
@RequestMapping("/api/v1/produtos")
@Slf4j
@Tag(name = "Produtos (Seguro)", description = "Exemplos de controle de acesso fino")
public class ProdutoController {

    // =========================================================================
    // LEITURA – público
    // =========================================================================

    @GetMapping("/{id}")
    @Operation(summary = "Leitura pública – sem autenticação")
    public Map<String, Object> buscar(@PathVariable Long id) {
        return Map.of("id", id, "nome", "Produto " + id, "preco", 99.90);
    }

    // =========================================================================
    // @PreAuthorize – verifica ANTES de executar
    // =========================================================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    // hasRole("ADMIN") equivale a hasAuthority("ROLE_ADMIN")
    @PreAuthorize("hasRole('ADMIN') or hasRole('GERENTE')")
    @Operation(summary = "@PreAuthorize – só ADMIN ou GERENTE podem criar")
    @SecurityRequirement(name = "bearerAuth")
    public Map<String, Object> criar(@RequestBody Map<String, Object> dto, @AuthenticationPrincipal Jwt jwt) {
        log.info("Produto criado por: {}", jwt.getClaimAsString("email"));
        return Map.of("id", 1, "status", "criado", "criadoPor", jwt.getSubject());
    }

    @DeleteMapping("/{id}")
    // Apenas ADMIN pode deletar – regra simples e direta
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "@PreAuthorize – deleção restrita a ADMIN")
    @SecurityRequirement(name = "bearerAuth")
    public void deletar(@PathVariable Long id) {
        log.info("Produto {} deletado", id);
    }

    @PatchMapping("/{id}/preco")
    // SpEL com lógica composta: ADMIN pode qualquer preço, GERENTE só até 10% de desconto
    @PreAuthorize("""
        hasRole('ADMIN') or
        (hasRole('GERENTE') and #novoPreco >= 0)
    """)
    @Operation(summary = "@PreAuthorize com SpEL composto – GERENTE tem restrições")
    @SecurityRequirement(name = "bearerAuth")
    public Map<String, Object> atualizarPreco(
            @PathVariable Long id,
            @RequestParam double novoPreco) {
        return Map.of("id", id, "novoPreco", novoPreco);
    }

    // =========================================================================
    // @PostAuthorize – verifica DEPOIS de executar (acessa returnObject)
    // Útil quando a verificação depende dos dados retornados
    // =========================================================================

    @GetMapping("/{id}/privado")
    // Só o dono ou ADMIN pode ver o produto privado
    // returnObject.get("vendedorEmail") acessa o retorno do método via SpEL
    @PostAuthorize("""
        hasRole('ADMIN') or
        returnObject.get('vendedorEmail') == authentication.name
    """)
    @Operation(summary = "@PostAuthorize – vendedor só vê seus próprios produtos")
    @SecurityRequirement(name = "bearerAuth")
    public Map<String, Object> buscarPrivado(@PathVariable Long id,
                                              Authentication auth) {
        // Simula produto retornado do banco
        return Map.of(
            "id", id,
            "nome", "Produto Privado " + id,
            "vendedorEmail", auth.getName() // dono = usuário logado (simulado)
        );
    }

    // =========================================================================
    // @PreFilter – filtra coleção de ENTRADA antes do método
    // =========================================================================

    @PostMapping("/bulk")
    // Filtra os IDs da lista: só processa produtos que o usuário tem permissão
    // filterObject representa cada elemento da coleção
    @PreAuthorize("hasRole('ADMIN') or hasRole('GERENTE')")
    @PreFilter("filterObject > 0") // remove IDs inválidos (≤ 0) da lista de entrada
    @Operation(summary = "@PreFilter – filtra IDs inválidos da entrada")
    @SecurityRequirement(name = "bearerAuth")
    public List<Long> processarBulk(@RequestBody List<Long> ids) {
        log.info("Processando {} produtos após filtro", ids.size());
        return ids;
    }

    // =========================================================================
    // @PostFilter – filtra coleção de RETORNO após o método
    // =========================================================================

    @GetMapping("/meus")
    @PreAuthorize("isAuthenticated()")
    // Filtra a lista retornada: cliente só vê seus próprios produtos
    // ADMIN vê todos (sem filtro)
    @PostFilter("""
        hasRole('ADMIN') or
        filterObject.get('vendedorEmail') == authentication.name
    """)
    @Operation(summary = "@PostFilter – cada usuário vê apenas seus produtos")
    @SecurityRequirement(name = "bearerAuth")
    public List<Map<String, Object>> listarMeus(Authentication auth) {
        // Simulação de lista com produtos de vários vendedores
        List<Map<String, Object>> todos = new ArrayList<>();
        todos.add(Map.of("id", 1, "nome", "Produto A", "vendedorEmail", auth.getName()));
        todos.add(Map.of("id", 2, "nome", "Produto B", "vendedorEmail", "outro@email.com"));
        todos.add(Map.of("id", 3, "nome", "Produto C", "vendedorEmail", auth.getName()));
        // @PostFilter remove os que não passam na expressão SpEL
        return todos;
    }

    // =========================================================================
    // Extraindo claims do JWT diretamente no Controller
    // =========================================================================

    @GetMapping("/claims-demo")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Demonstra extração de claims customizados do JWT")
    @SecurityRequirement(name = "bearerAuth")
    public Map<String, Object> claimsDemo(@AuthenticationPrincipal Jwt jwt) {
        // @AuthenticationPrincipal Jwt injeta o token decodificado diretamente
        return Map.of(
            "sub",    jwt.getSubject(),
            "email",  jwt.getClaimAsString("email"),
            "nome",   jwt.getClaimAsString("nome"),
            "roles",  jwt.getClaimAsStringList("roles"),
            "userId", jwt.getClaim("userId"),
            "iat",    jwt.getIssuedAt(),
            "exp",    jwt.getExpiresAt()
        );
    }
}
