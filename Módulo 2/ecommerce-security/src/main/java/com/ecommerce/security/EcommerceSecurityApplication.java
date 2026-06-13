package com.ecommerce.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // habilita leitura de @ConfigurationProperties
public class EcommerceSecurityApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcommerceSecurityApplication.class, args);
    }
}
