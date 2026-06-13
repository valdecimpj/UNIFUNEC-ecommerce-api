package com.ecommerce.security.application.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Resposta do endpoint de login.
 * Contém access token (curto prazo) e refresh token (longo prazo).
 */
@Getter
@Builder
public class TokenResponseDTO {
    private String accessToken;
    private String refreshToken;
    private String tokenType;       // sempre "Bearer"
    private long   expiresIn;       // segundos até o access token expirar
}
