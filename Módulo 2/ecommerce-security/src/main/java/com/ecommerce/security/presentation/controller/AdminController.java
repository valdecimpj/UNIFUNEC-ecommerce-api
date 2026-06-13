package com.ecommerce.security.presentation.controller;

import com.ecommerce.security.application.dto.UsuarioResponseDTO;
import com.ecommerce.security.application.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller administrativo – demonstra:
 * 1. Restrição de acesso a ADMIN via @PreAuthorize
 * 2. Prevenção a SQL Injection com JPQL parametrizado e Criteria API
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Operações administrativas e exemplos de SQL Injection prevention")
@PreAuthorize("hasRole('ADMIN')") // aplica a TODOS os métodos desta classe
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final UsuarioService usuarioService;
    private final EntityManager em;

    @GetMapping("/usuarios/{id}")
    @Operation(summary = "Busca usuário por ID – apenas ADMIN")
    public UsuarioResponseDTO buscarUsuario(@PathVariable Long id) {
        return usuarioService.buscarPorId(id);
    }

    @PutMapping("/usuarios/{id}/promover-gerente")
    @Operation(summary = "Promove usuário a GERENTE – apenas ADMIN")
    public UsuarioResponseDTO promoverGerente(@PathVariable Long id) {
        return usuarioService.promoverAGerente(id);
    }

    // =========================================================================
    // PREVENÇÃO A SQL INJECTION
    // =========================================================================

    /**
     * ❌ VULNERÁVEL – NUNCA FAÇA ISSO
     * Concatenação direta do input do usuário na query.
     *
     * Ataque: nome = "' OR '1'='1" retorna todos os usuários
     * Ataque: nome = "'; DROP TABLE usuarios; --" destrói a tabela
     */
    @GetMapping("/usuarios/busca-vulneravel")
    @Operation(summary = "❌ EXEMPLO VULNERÁVEL – não use em produção!")
    public List<?> buscarVulneravel(@RequestParam String nome) {
        // ❌ VULNERÁVEL – concatenação direta
        String sql = "SELECT u FROM Usuario u WHERE u.nome = '" + nome + "'";
        log.warn("⚠️  QUERY VULNERÁVEL (apenas para demonstração): {}", sql);
        // Não execute: return em.createQuery(sql).getResultList();
        return List.of(Map.of("aviso", "Endpoint apenas demonstrativo – query não executada"));
    }

    /**
     * ✅ SEGURO – JPQL com parâmetro nomeado
     * O driver substitui :nome por um Prepared Statement internamente.
     * Mesmo que nome = "' OR '1'='1", é tratado como string literal.
     */
    @GetMapping("/usuarios/busca-jpql")
    @Operation(summary = "✅ SEGURO – JPQL parametrizado com :nome")
    public List<?> buscarComJpql(@RequestParam String nome) {
        // ✅ SEGURO – parâmetro nomeado previne injeção
        TypedQuery<com.ecommerce.security.domain.model.Usuario> query = em.createQuery(
            "SELECT u FROM Usuario u WHERE u.nome = :nome",
            com.ecommerce.security.domain.model.Usuario.class
        );
        query.setParameter("nome", nome); // binding seguro

        return query.getResultList().stream()
                .map(UsuarioResponseDTO::from)
                .toList();
    }

    /**
     * ✅ SEGURO – Spring Data JPA (Prepared Statement automático)
     * Qualquer método derivado (findBy...) ou @Query parametrizado é seguro.
     *
     * Spring Data gera internamente:
     *   SELECT u FROM Usuario u WHERE u.email = ?1
     * E usa Prepared Statement do JDBC.
     */
    @GetMapping("/usuarios/busca-spring-data")
    @Operation(summary = "✅ SEGURO – Spring Data usa Prepared Statements")
    public String demonstracaoSpringData() {
        return "Métodos findByEmail(), findByNome() do Spring Data são seguros por padrão. " +
               "O parâmetro nunca é concatenado na query.";
    }

    /**
     * ✅ SEGURO – Criteria API (type-safe, sem strings de query)
     * Ideal para queries dinâmicas com filtros opcionais.
     */
    @GetMapping("/usuarios/busca-criteria")
    @Operation(summary = "✅ SEGURO – Criteria API type-safe")
    public List<?> buscarComCriteria(@RequestParam(required = false) String nome,
                                      @RequestParam(required = false) Boolean ativo) {
        var cb = em.getCriteriaBuilder();
        var cq = cb.createQuery(com.ecommerce.security.domain.model.Usuario.class);
        var root = cq.from(com.ecommerce.security.domain.model.Usuario.class);

        var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

        // Filtros condicionais – nenhum usa concatenação de string
        if (nome != null && !nome.isBlank()) {
            // cb.like com parâmetro – seguro contra SQL Injection
            predicates.add(cb.like(cb.lower(root.get("nome")),
                    "%" + nome.toLowerCase() + "%"));
        }

        if (ativo != null) {
            predicates.add(cb.equal(root.get("ativo"), ativo));
        }

        cq.where(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));

        return em.createQuery(cq).getResultList().stream()
                .map(UsuarioResponseDTO::from)
                .toList();
    }
}
