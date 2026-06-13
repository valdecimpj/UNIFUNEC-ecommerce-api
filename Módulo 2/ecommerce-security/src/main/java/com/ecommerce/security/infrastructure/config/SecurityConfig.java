package com.ecommerce.security.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuração central do Spring Security.
 *
 * Pilares explicados no slide:
 *  - Filter Chain: cadeia de filtros que processa cada requisição HTTP
 *  - SecurityContext: armazena a autenticação corrente (por thread/request)
 *  - AuthenticationManager: ponto de entrada para autenticar credenciais
 *
 * @EnableMethodSecurity habilita @PreAuthorize, @PostAuthorize etc. nos métodos.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
    prePostEnabled = true,   // habilita @PreAuthorize / @PostAuthorize
    securedEnabled = true,   // habilita @Secured
    jsr250Enabled = true     // habilita @RolesAllowed (JSR-250)
)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtDecoder jwtDecoder;
    private final CorsConfigurationSource corsConfigurationSource;

    // =========================================================================
    // FILTER CHAIN PRINCIPAL – Resource Server stateless
    // =========================================================================

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── CSRF ──────────────────────────────────────────────────────────
            // DESABILITADO: API stateless com JWT não precisa de CSRF token.
            // ATENÇÃO: se a app tiver sessões HTTP, mantenha o CSRF ativo!
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS ──────────────────────────────────────────────────────────
            // Configuração explícita via CorsConfigurationSource (bean abaixo)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))

            // ── SESSÃO ────────────────────────────────────────────────────────
            // STATELESS: Spring não cria nem usa HttpSession.
            // Toda autenticação passa pelo JWT em cada request.
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── AUTORIZAÇÃO DE ROTAS ──────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Endpoints públicos – não precisam de autenticação
                .requestMatchers(
                    "/api/v1/auth/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api-docs/**",
                    "/h2-console/**",
                    "/actuator/health"
                ).permitAll()

                // Apenas leitura de produtos é pública
                .requestMatchers(HttpMethod.GET, "/api/v1/produtos/**").permitAll()

                // Criação/edição de produtos exige GERENTE ou ADMIN
                .requestMatchers(HttpMethod.POST,   "/api/v1/produtos/**").hasAnyRole("ADMIN", "GERENTE")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/produtos/**").hasAnyRole("ADMIN", "GERENTE")
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/produtos/**").hasAnyRole("ADMIN", "GERENTE")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/produtos/**").hasRole("ADMIN")

                // Rotas administrativas – apenas ADMIN
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                // Tudo mais exige autenticação
                .anyRequest().authenticated()
            )

            // ── RESOURCE SERVER – valida JWT em cada request ───────────────────
            // Desliga sessões e habilita validação automática do Bearer Token.
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder)
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            )

            // ── HEADERS DE SEGURANÇA ─────────────────────────────────────────
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin()) // necessário para H2 console
                .referrerPolicy(referrer ->
                    referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .contentTypeOptions(content -> {})  // X-Content-Type-Options: nosniff
            );

        return http.build();
    }

    // =========================================================================
    // JWT AUTHENTICATION CONVERTER
    // Extrai as authorities (roles) do claim correto dentro do JWT
    // =========================================================================

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // Por padrão, Spring lê o claim "scope" com prefixo "SCOPE_".
        // Aqui mapeamos para o claim "roles" sem prefixo adicional,
        // pois nossos roles já têm o prefixo ROLE_ (ex: ROLE_ADMIN).
        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix(""); // roles já têm ROLE_ no valor

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    // =========================================================================
    // AUTHENTICATION PROVIDER
    // Liga o UserDetailsService ao PasswordEncoder para autenticar usuários
    // =========================================================================

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager exposto como bean para ser injetado no AuthController.
     * Necessário para chamar authenticate() no fluxo de login manual.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // BCrypt com strength 12 – custo computacional adequado para produção
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
