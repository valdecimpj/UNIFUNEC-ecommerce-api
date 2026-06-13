package com.ecommerce.security.presentation.controller;

import com.ecommerce.security.application.dto.*;
import com.ecommerce.security.application.service.TokenService;
import com.ecommerce.security.application.service.UsuarioService;
import com.ecommerce.security.domain.model.Usuario;
import com.ecommerce.security.domain.repository.UsuarioRepository;
import com.ecommerce.security.infrastructure.config.SecurityProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller de autenticação.
 * Endpoints públicos (sem autenticação exigida – configurado no SecurityConfig).
 *
 * Fluxo de login:
 *   1. Cliente envia email + senha
 *   2. AuthenticationManager autentica via CustomUserDetailsService + BCrypt
 *   3. TokenService gera access_token (30min) + refresh_token (7 dias)
 *   4. Cliente usa access_token no header Authorization: Bearer <token>
 *   5. Quando access_token expira, usa refresh_token para obter novo par
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Autenticação", description = "Login, cadastro e renovação de token")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final JwtDecoder jwtDecoder;
    private final SecurityProperties props;

    // =========================================================================
    // LOGIN
    // =========================================================================

    @PostMapping("/login")
    @Operation(summary = "Autentica usuário e retorna access + refresh token")
    public TokenResponseDTO login(@RequestBody @Valid LoginRequestDTO dto) {
        log.info("Tentativa de login: {}", dto.getEmail());

        // AuthenticationManager valida as credenciais via CustomUserDetailsService
        // Lança AuthenticationException se inválido (tratado no GlobalExceptionHandler)
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getSenha())
        );

        Usuario usuario = (Usuario) auth.getPrincipal();

        String accessToken  = tokenService.gerarAccessToken(usuario);
        String refreshToken = tokenService.gerarRefreshToken(usuario);

        log.info("Login bem-sucedido: {}", usuario.getEmail());

        return TokenResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(props.getJwt().getExpirationMinutes() * 60)
                .build();
    }

    // =========================================================================
    // REFRESH TOKEN
    // =========================================================================

    @PostMapping("/refresh")
    @Operation(summary = "Renova o access token usando o refresh token")
    public TokenResponseDTO refresh(@RequestParam String refreshToken) {
        // Valida e decodifica o refresh token
        Jwt jwt = jwtDecoder.decode(refreshToken); // lança JwtException se inválido

        if (!tokenService.isRefreshToken(jwt)) {
            throw new IllegalArgumentException("Token fornecido não é um refresh token");
        }

        String email = tokenService.extrairEmail(jwt);
        Usuario usuario = (Usuario) usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        // Emite novo par de tokens (rotation: refresh token antigo é descartado)
        String novoAccessToken  = tokenService.gerarAccessToken(usuario);
        String novoRefreshToken = tokenService.gerarRefreshToken(usuario);

        log.info("Tokens renovados para: {}", email);

        return TokenResponseDTO.builder()
                .accessToken(novoAccessToken)
                .refreshToken(novoRefreshToken)
                .tokenType("Bearer")
                .expiresIn(props.getJwt().getExpirationMinutes() * 60)
                .build();
    }

    // =========================================================================
    // CADASTRO
    // =========================================================================

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra novo usuário com role CLIENTE")
    public UsuarioResponseDTO cadastrar(@RequestBody @Valid CadastroRequestDTO dto) {
        return usuarioService.cadastrar(dto);
    }

    // =========================================================================
    // PERFIL DO USUÁRIO LOGADO
    // =========================================================================

    @GetMapping("/me")
    @Operation(summary = "Retorna dados do usuário autenticado via JWT")
    public UsuarioResponseDTO meuPerfil(Authentication authentication) {
        // Spring Security popula o Authentication a partir do JWT validado
        String email = authentication.getName(); // == jwt.getSubject()
        return usuarioRepository.findByEmail(email)
                .map(UsuarioResponseDTO::from)
                .orElseThrow();
    }
}
