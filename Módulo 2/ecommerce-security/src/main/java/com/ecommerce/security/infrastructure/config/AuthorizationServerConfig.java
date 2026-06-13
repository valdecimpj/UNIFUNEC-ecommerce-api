package com.ecommerce.security.infrastructure.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

/**
 * Configuração do Spring Authorization Server (projeto oficial).
 * Substitui o legado spring-security-oauth2.
 *
 * Expõe os endpoints padrão OAuth2:
 *   POST /oauth2/token          – emite access/refresh tokens
 *   GET  /oauth2/authorize      – inicia Authorization Code flow
 *   GET  /.well-known/openid-configuration – discovery OIDC
 *   GET  /oauth2/jwks            – chaves públicas RS256 para validação
 *
 * ATENÇÃO: esta classe é separada do SecurityConfig principal.
 * @Order(1) garante que o Authorization Server tenha prioridade na filter chain.
 */
@Configuration
public class AuthorizationServerConfig {

    // =========================================================================
    // FILTER CHAIN DO AUTHORIZATION SERVER (prioridade máxima)
    // =========================================================================

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerFilterChain(HttpSecurity http) throws Exception {

        // Aplica configurações padrão do Authorization Server
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            // Habilita OpenID Connect (OIDC) – expõe /userinfo endpoint
            .oidc(Customizer.withDefaults());

        http
            // Redireciona para /login se tentar acessar endpoint OAuth2 sem autenticação
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(
                    new LoginUrlAuthenticationEntryPoint("/login")))
            // Resource Server para validar tokens dentro do próprio Auth Server
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    // =========================================================================
    // CLIENTES REGISTRADOS
    // Em produção: use JdbcRegisteredClientRepository (persiste no banco)
    // =========================================================================

    @Bean
    public RegisteredClientRepository registeredClientRepository() {

        // ------------------------------------------------------------------
        // Cliente 1: SPA / Mobile – Authorization Code Flow com PKCE
        // Usado por frontends públicos que não podem guardar client_secret
        // ------------------------------------------------------------------
        RegisteredClient spaClient = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId("ecommerce-spa")
                // Sem client_secret – PKCE substitui o secret em apps públicos
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:3000/callback")
                .redirectUri("https://meu-frontend.com.br/callback")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("produtos:read")
                .scope("pedidos:write")
                .clientSettings(ClientSettings.builder()
                    .requireAuthorizationConsent(false) // sem tela de consentimento
                    .requireProofKey(true)              // exige PKCE (code_challenge)
                    .build())
                .tokenSettings(TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofMinutes(30))
                    .refreshTokenTimeToLive(Duration.ofDays(7))
                    .reuseRefreshTokens(false) // gera novo refresh token a cada uso
                    .build())
                .build();

        // ------------------------------------------------------------------
        // Cliente 2: Serviço interno (M2M) – Client Credentials Flow
        // Usado por microsserviços que se comunicam entre si sem usuário
        // ------------------------------------------------------------------
        RegisteredClient serviceClient = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId("estoque-service")
                .clientSecret("{bcrypt}$2a$12$HG2xK1Z.X1pSmOiRXzANIuP9TW3glwsHaKQHdL7pqHHvT4J5KQZ4m") // "service-secret"
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .scope("estoque:read")
                .scope("estoque:write")
                .tokenSettings(TokenSettings.builder()
                    .accessTokenTimeToLive(Duration.ofHours(1))
                    .build())
                .build();

        // ------------------------------------------------------------------
        // Cliente 3: BFF (Backend for Frontend) – Authorization Code com secret
        // Servidor Node/Next.js que guarda o secret com segurança
        // ------------------------------------------------------------------
        RegisteredClient bffClient = RegisteredClient
                .withId(UUID.randomUUID().toString())
                .clientId("ecommerce-bff")
                .clientSecret("{bcrypt}$2a$12$bff-secret-hash-aqui")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:4000/api/auth/callback")
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("produtos:read")
                .scope("pedidos:write")
                .clientSettings(ClientSettings.builder()
                    .requireProofKey(false) // BFF pode guardar secret, PKCE opcional
                    .build())
                .build();

        return new InMemoryRegisteredClientRepository(spaClient, serviceClient, bffClient);
    }

    // =========================================================================
    // CONFIGURAÇÕES DO SERVIDOR
    // =========================================================================

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8080") // URL base do Authorization Server
                .authorizationEndpoint("/oauth2/authorize")
                .tokenEndpoint("/oauth2/token")
                .jwkSetEndpoint("/oauth2/jwks")
                .tokenRevocationEndpoint("/oauth2/revoke")
                .tokenIntrospectionEndpoint("/oauth2/introspect")
                .oidcUserInfoEndpoint("/userinfo")
                .build();
    }

    // =========================================================================
    // PAR DE CHAVES RSA – RS256
    // Em produção: carregue de um KeyStore (.jks/.p12) ou Vault
    // =========================================================================

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = generateRsaKey();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * Gera par RSA de 2048 bits em memória.
     * ATENÇÃO: em produção, use chaves persistidas em KeyStore ou Vault.
     * Regenerar chaves invalida todos os tokens em circulação!
     */
    private static RSAKey generateRsaKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            RSAPublicKey  publicKey  = (RSAPublicKey)  keyPair.getPublic();
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao gerar par de chaves RSA", e);
        }
    }
}
