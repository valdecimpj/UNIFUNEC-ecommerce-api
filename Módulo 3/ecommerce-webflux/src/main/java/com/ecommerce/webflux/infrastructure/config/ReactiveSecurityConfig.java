package com.ecommerce.webflux.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Configuração de segurança para WebFlux (ReactiveSecurityConfig).
 *
 * DIFERENÇAS em relação ao SecurityConfig do MVC:
 *   - Usa ServerHttpSecurity em vez de HttpSecurity
 *   - Usa SecurityWebFilterChain em vez de SecurityFilterChain
 *   - @EnableReactiveMethodSecurity para @PreAuthorize reativo
 *   - Não usa WebSecurityConfigurerAdapter (depreciado mesmo no MVC)
 *   - Sessões são STATELESS por padrão no WebFlux Resource Server
 *
 * NOTA: Spring Boot não auto-configura ReactiveJwtDecoder para chaves simétricas
 * (HMAC). É necessário declarar o bean manualmente com NimbusReactiveJwtDecoder.
 */
@Configuration
@EnableReactiveMethodSecurity // habilita @PreAuthorize em métodos reativos
public class ReactiveSecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.secret}")
    private String jwtSecret;

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        SecretKeySpec secretKey = new SecretKeySpec(
                jwtSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build();
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable) // API stateless com JWT
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.GET, "/api/v1/produtos/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/avaliacoes/**").permitAll()
                        .pathMatchers("/swagger-ui/**", "/api-docs/**",
                                      "/webjars/**", "/v3/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtDecoder(jwtDecoder())) // usa o bean declarado acima
                )
                .build();
    }
}
