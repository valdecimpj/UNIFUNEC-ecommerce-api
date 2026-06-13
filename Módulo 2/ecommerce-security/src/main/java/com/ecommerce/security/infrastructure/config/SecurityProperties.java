package com.ecommerce.security.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Leitura type-safe das propriedades de segurança do application.yml.
 * Muito mais seguro e legível que @Value direto nos beans.
 *
 * Exemplo de uso:
 *   @Autowired SecurityProperties props;
 *   props.getJwt().getExpirationMinutes();
 */
@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();

    @Getter
    @Setter
    public static class Jwt {
        private long expirationMinutes = 30;
        private long refreshExpirationDays = 7;
        private String issuer = "ecommerce-api";
        private String secret;
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:3000");
    }
}
