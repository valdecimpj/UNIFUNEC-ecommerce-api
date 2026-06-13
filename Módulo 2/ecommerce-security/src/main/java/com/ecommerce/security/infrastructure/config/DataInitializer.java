package com.ecommerce.security.infrastructure.config;

import com.ecommerce.security.domain.model.Usuario;
import com.ecommerce.security.domain.model.enums.Role;
import com.ecommerce.security.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Inicializa dados de teste no startup.
 * Remove em produção ou proteja com @Profile("dev").
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        criarUsuarioSeNaoExiste("Admin Geral",      "admin@ecommerce.com",   "Admin@123",   Set.of(Role.ROLE_ADMIN));
        criarUsuarioSeNaoExiste("Gerente Vendas",   "gerente@ecommerce.com", "Gerente@123", Set.of(Role.ROLE_GERENTE));
        criarUsuarioSeNaoExiste("Vendedor João",    "vendedor@ecommerce.com","Vendedor@123",Set.of(Role.ROLE_VENDEDOR));
        criarUsuarioSeNaoExiste("Cliente Maria",    "cliente@ecommerce.com", "Cliente@123", Set.of(Role.ROLE_CLIENTE));
        log.info("Usuários de teste criados. Acesse /swagger-ui.html para testar.");
        log.info("Login admin: admin@ecommerce.com / Admin@123");
    }

    private void criarUsuarioSeNaoExiste(String nome, String email, String senha, Set<Role> roles) {
        if (!usuarioRepository.existsByEmail(email)) {
            usuarioRepository.save(Usuario.builder()
                .nome(nome)
                .email(email)
                .senha(passwordEncoder.encode(senha))
                .roles(roles)
                .ativo(true)
                .build());
            log.info("Usuário criado: {} ({})", email, roles);
        }
    }
}
