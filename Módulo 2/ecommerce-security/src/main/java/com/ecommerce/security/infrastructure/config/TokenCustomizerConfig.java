package com.ecommerce.security.infrastructure.config;

import com.ecommerce.security.domain.model.Usuario;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/**
 * Personaliza o payload (claims) dos tokens JWT emitidos pelo Authorization Server.
 *
 * Claims padrão já incluídos automaticamente:
 *   sub   – subject (userId ou email)
 *   iss   – issuer
 *   iat   – issued at
 *   exp   – expiration
 *   aud   – audience
 *
 * Claims customizados adicionados aqui:
 *   roles     – papéis do usuário (ROLE_ADMIN, ROLE_CLIENTE etc.)
 *   nome      – nome completo do usuário
 *   email     – email do usuário
 */
@Configuration
public class TokenCustomizerConfig {

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> tokenCustomizer() {
        return context -> {

            // Aplica apenas ao access_token (não ao id_token do OIDC)
            if (context.getTokenType().getValue().equals("access_token")) {

                Authentication principal = context.getPrincipal();

                // Adiciona roles ao token se o principal for um Usuario
                if (principal.getPrincipal() instanceof Usuario usuario) {

                    // Claim "roles" – lido pelo JwtAuthenticationConverter no Resource Server
                    context.getClaims().claim("roles",
                        usuario.getAuthorities().stream()
                            .map(auth -> auth.getAuthority())
                            .toList()
                    );

                    // Claims extras de perfil
                    context.getClaims().claim("nome",  usuario.getNome());
                    context.getClaims().claim("email", usuario.getEmail());
                    context.getClaims().claim("userId", usuario.getId());
                }
            }

            // Para o id_token (OIDC), adiciona claims de perfil
            if (context.getTokenType().getValue().equals("id_token")) {
                Authentication principal = context.getPrincipal();
                if (principal.getPrincipal() instanceof Usuario usuario) {
                    context.getClaims().claim("nome",  usuario.getNome());
                    context.getClaims().claim("email", usuario.getEmail());
                }
            }
        };
    }
}
