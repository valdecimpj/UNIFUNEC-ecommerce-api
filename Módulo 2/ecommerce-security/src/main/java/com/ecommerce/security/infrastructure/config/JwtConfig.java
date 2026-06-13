package com.ecommerce.security.infrastructure.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Configuração de JWT – criação e validação de tokens.
 *
 * ESTRATÉGIA DE CHAVES:
 *
 * HS256 (HMAC SHA-256) – chave simétrica:
 *   ✅ Simples de configurar (apenas uma chave secreta)
 *   ❌ Todos os serviços precisam compartilhar a mesma chave
 *   ✅ Bom para: monólito ou microsserviços com segredo compartilhado
 *
 * RS256 (RSA SHA-256) – par de chaves assimétricas:
 *   ✅ Chave privada só no Authorization Server (emite tokens)
 *   ✅ Chave pública distribuída para Resource Servers (validam)
 *   ✅ Recomendado para produção com múltiplos serviços
 *   → Ver AuthorizationServerConfig.java para exemplo com RS256
 *
 * Este arquivo usa HS256 para simplicidade de desenvolvimento local.
 */
@Configuration
@RequiredArgsConstructor
public class JwtConfig {

    private final SecurityProperties props;

    /**
     * JwtEncoder – usado pelo TokenService para GERAR tokens.
     * Usa a chave secreta do application.yml com algoritmo HS256.
     */
    @Bean
    public JwtEncoder jwtEncoder() {
        byte[] keyBytes = props.getJwt().getSecret()
                .getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    /**
     * JwtDecoder – usado pelo Resource Server para VALIDAR tokens recebidos.
     * Verifica assinatura, expiração (exp) e emissor (iss) automaticamente.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] keyBytes = props.getJwt().getSecret()
                .getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        // Valida também o claim "iss" (issuer)
        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(props.getJwt().getIssuer());
        decoder.setJwtValidator(issuerValidator);

        return decoder;
    }
}
