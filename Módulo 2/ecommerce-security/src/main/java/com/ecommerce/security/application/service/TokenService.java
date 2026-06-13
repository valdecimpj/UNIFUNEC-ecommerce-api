package com.ecommerce.security.application.service;

import com.ecommerce.security.domain.model.Usuario;
import com.ecommerce.security.infrastructure.config.SecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Serviço responsável por GERAR tokens JWT.
 *
 * Estrutura do JWT:
 *   Header:    { "alg": "HS256", "typ": "JWT" }
 *   Payload:   { "sub", "iss", "iat", "exp", "roles", "nome", "email", "jti" }
 *   Signature: HMAC-SHA256(base64(header) + "." + base64(payload), secret)
 *
 * Claims gerados:
 *   sub   – email do usuário (subject)
 *   iss   – emissor (ecommerce-api)
 *   iat   – issued at (agora)
 *   exp   – expiration (agora + expirationMinutes)
 *   jti   – JWT ID único (UUID) – permite blacklist por jti
 *   roles – papéis do usuário
 *   nome  – nome completo
 *   email – email
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties props;

    // =========================================================================
    // ACCESS TOKEN
    // =========================================================================

    public String gerarAccessToken(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiracao = agora.plus(
                props.getJwt().getExpirationMinutes(), ChronoUnit.MINUTES);

        // Roles do usuário como lista de strings
        var roles = usuario.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.getJwt().getIssuer())   // iss
                .subject(usuario.getEmail())           // sub
                .issuedAt(agora)                       // iat
                .expiresAt(expiracao)                  // exp
                .id(UUID.randomUUID().toString())      // jti – único por token
                .claim("roles", roles)                 // custom: papéis
                .claim("nome",  usuario.getNome())     // custom: nome
                .claim("email", usuario.getEmail())    // custom: email
                .claim("userId", usuario.getId())      // custom: id do usuário
                .claim("token_type", "access")
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        log.debug("Access token gerado para usuário: {} | exp: {}", usuario.getEmail(), expiracao);
        return token;
    }

    // =========================================================================
    // REFRESH TOKEN
    // =========================================================================

    /**
     * Refresh token com vida útil mais longa e claims mínimos.
     * Contém apenas o necessário para identificar o usuário e renovar o access token.
     * NÃO contém roles nem dados sensíveis desnecessários.
     */
    public String gerarRefreshToken(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expiracao = agora.plus(
                props.getJwt().getRefreshExpirationDays(), ChronoUnit.DAYS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.getJwt().getIssuer())
                .subject(usuario.getEmail())
                .issuedAt(agora)
                .expiresAt(expiracao)
                .id(UUID.randomUUID().toString())
                .claim("token_type", "refresh")
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    // =========================================================================
    // EXTRAÇÃO DE DADOS DO TOKEN
    // =========================================================================

    /**
     * Extrai o email (subject) de um token já validado.
     */
    public String extrairEmail(Jwt jwt) {
        return jwt.getSubject();
    }

    /**
     * Verifica se o token é do tipo "refresh".
     */
    public boolean isRefreshToken(Jwt jwt) {
        return "refresh".equals(jwt.getClaimAsString("token_type"));
    }
}
