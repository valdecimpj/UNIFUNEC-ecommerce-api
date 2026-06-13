package com.ecommerce.security.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuração explícita de CORS.
 *
 * REGRAS DE PRODUÇÃO:
 *   ❌ NUNCA use allowedOrigins("*") em produção com credenciais
 *   ✅ Sempre liste as origens permitidas explicitamente
 *   ✅ Configure via application.yml para flexibilidade por ambiente
 *   ✅ Exponha apenas os headers necessários
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final SecurityProperties props;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Origens permitidas vindas do application.yml (por ambiente: dev/prod)
        config.setAllowedOrigins(props.getCors().getAllowedOrigins());

        // Métodos HTTP permitidos
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Headers permitidos na requisição
        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "X-Correlation-ID" // para rastreamento distribuído
        ));

        // Headers expostos na resposta (cliente pode ler via JS)
        config.setExposedHeaders(List.of(
                "X-Total-Count",   // paginação
                "X-Correlation-ID"
        ));

        // Permite cookies e Authorization header em cross-origin
        config.setAllowCredentials(true);

        // Tempo de cache do preflight (OPTIONS) em segundos
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // aplica a todas as rotas
        return source;
    }
}
