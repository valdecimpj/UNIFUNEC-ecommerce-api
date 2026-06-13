package com.ecommerce.webflux;

import com.ecommerce.webflux.application.usecase.ProdutoService;
import com.ecommerce.webflux.domain.model.Produto;
import com.ecommerce.webflux.domain.repository.ProdutoReactiveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes WebFlux com WebTestClient e StepVerifier.
 *
 * WebTestClient  → testa endpoints HTTP reativos
 * StepVerifier   → testa publishers (Mono/Flux) diretamente
 *
 * Diferenças do MockMvc:
 *   MockMvc é síncrono – usa .perform() e .andExpect()
 *   WebTestClient é reativo – usa .exchange() e .expectBody()
 *
 * WebEnvironment.MOCK: servidor em memória (sem porta real).
 *   Permite usar SecurityMockServerConfigurers.mockUser() para simular
 *   usuário autenticado sem precisar gerar tokens JWT reais.
 *
 * @MockBean ReactiveJwtDecoder: substitui o bean real no contexto de teste,
 *   evitando que o app tente validar tokens contra um servidor externo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@DisplayName("Testes WebFlux – WebTestClient e StepVerifier")
class WebFluxIntegrationTest {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    ProdutoReactiveRepository produtoRepository;

    @Autowired
    ProdutoService produtoService;

    // Substitui o ReactiveJwtDecoder real – o contexto de teste não precisa
    // de um servidor de autorização externo para iniciar.
    @MockBean
    ReactiveJwtDecoder jwtDecoder;

    private Produto produtoSalvo;

    @BeforeEach
    void setup() {
        produtoSalvo = produtoRepository.save(
                Produto.builder()
                        .nome("Produto Teste")
                        .preco(BigDecimal.valueOf(199.90))
                        .estoque(10)
                        .ativo(true)
                        .build()
        ).block(); // .block() é aceitável em testes @BeforeEach
    }

    // =========================================================================
    // WEBTEST CLIENT – testa endpoints HTTP
    // =========================================================================

    @Test
    @DisplayName("GET /produtos/{id} retorna produto existente")
    void buscarPorId_deveRetornarProduto() {
        webTestClient
                .get()
                .uri("/api/v1/produtos/{id}", produtoSalvo.getId())
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody(Produto.class)
                .value(produto -> {
                    assert produto.getNome().equals("Produto Teste");
                    assert produto.getPreco().compareTo(BigDecimal.valueOf(199.90)) == 0;
                });
    }

    @Test
    @DisplayName("GET /produtos/{id} retorna 404 para ID inexistente")
    void buscarPorId_naoEncontrado_deveRetornar404() {
        webTestClient
                .get()
                .uri("/api/v1/produtos/{id}", 999_999L)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GET /produtos retorna lista de produtos ativos")
    void listar_deveRetornarFluxDeProdutos() {
        webTestClient
                .get()
                .uri("/api/v1/produtos")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(Produto.class)
                .consumeWith(result ->
                    assertThat(result.getResponseBody()).hasSizeGreaterThanOrEqualTo(1));
    }

    @Test
    @DisplayName("POST /produtos cria produto e retorna 201")
    void criar_deveRetornarProdutoCriadoComStatus201() {
        Produto novoProduto = Produto.builder()
                .nome("Produto Novo")
                .preco(BigDecimal.valueOf(350.00))
                .estoque(5)
                .ativo(true)
                .build();

        // mutateWith(mockUser()) injeta um principal autenticado no contexto
        // sem validar JWT, permitindo testar endpoints protegidos.
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockUser())
                .post()
                .uri("/api/v1/produtos")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(novoProduto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Produto.class)
                .value(p -> {
                    assert p.getId() != null;
                    assert p.getNome().equals("Produto Novo");
                });
    }

    @Test
    @DisplayName("GET /produtos/stream retorna SSE (text/event-stream)")
    void stream_deveRetornarEventStream() {
        webTestClient
                .get()
                .uri("/api/v1/produtos/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
    }

    // =========================================================================
    // STEPVERIFIER – testa Mono/Flux diretamente (sem HTTP)
    // =========================================================================

    @Test
    @DisplayName("StepVerifier – buscarPorId emite produto correto")
    void stepVerifier_buscarPorId_emiteProduto() {
        StepVerifier
                .create(produtoService.buscarPorId(produtoSalvo.getId()))
                .expectNextMatches(p ->
                    p.getNome().equals("Produto Teste") &&
                    p.getPreco().compareTo(BigDecimal.valueOf(199.90)) == 0
                )
                .verifyComplete(); // espera onComplete sem mais elementos
    }

    @Test
    @DisplayName("StepVerifier – buscarPorId ID inexistente emite erro")
    void stepVerifier_buscarPorId_idInexistente_emiteErro() {
        StepVerifier
                .create(produtoService.buscarPorId(999_999L))
                .expectErrorMatches(ex ->
                    ex instanceof RuntimeException &&
                    ex.getMessage().contains("Produto não encontrado"))
                .verify();
    }

    @Test
    @DisplayName("StepVerifier – listarAtivos emite múltiplos produtos em ordem")
    void stepVerifier_listarAtivos_emiteFlux() {
        // Garante ao menos um produto ativo
        StepVerifier
                .create(produtoService.listarAtivos().take(3))
                .expectNextCount(1) // ao menos 1
                .thenCancel()       // cancela o Flux após verificar
                .verify();
    }

    @Test
    @DisplayName("StepVerifier – resumoEstoque combina dois Monos corretamente")
    void stepVerifier_resumoEstoque_combinaMonos() {
        StepVerifier
                .create(produtoService.resumoEstoque())
                .expectNextMatches(resumo ->
                    resumo.contains("Produtos ativos:") &&
                    resumo.contains("Com estoque:")
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("StepVerifier – buscarComFallback retorna fallback para ID inexistente")
    void stepVerifier_buscarComFallback_retornaFallback() {
        StepVerifier
                .create(produtoService.buscarComFallback(999_999L))
                // Verifica que o fallback é emitido com os dados corretos
                .expectNextMatches(p ->
                    !p.isAtivo() &&
                    p.getEstoque() == 0 &&
                    p.getNome().equals("Produto Indisponível")
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("StepVerifier com timeout – emissão deve ocorrer em até 5s")
    void stepVerifier_comTimeout() {
        // verify(Duration) existe apenas em LastStep; expectComplete() transita para LastStep
        StepVerifier
                .create(produtoService.buscarPorId(produtoSalvo.getId()))
                .expectNextCount(1)
                .expectComplete()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("StepVerifier – Flux.interval simula stream contínuo")
    void stepVerifier_fluxInterval_simula3Eventos() {
        // Testa um Flux periódico sem depender de dados reais
        Flux<Long> stream = Flux.interval(Duration.ofMillis(100)).take(3);

        StepVerifier
                .withVirtualTime(() -> stream) // virtualTime: acelera o tempo
                .expectSubscription()
                .thenAwait(Duration.ofMillis(300)) // avança 300ms virtuais
                .expectNext(0L, 1L, 2L)
                .verifyComplete();
    }
}
