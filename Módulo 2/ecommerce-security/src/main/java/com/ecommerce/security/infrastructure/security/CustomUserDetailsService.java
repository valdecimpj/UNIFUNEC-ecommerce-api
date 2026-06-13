package com.ecommerce.security.infrastructure.security;

import com.ecommerce.security.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação do UserDetailsService.
 * Spring Security chama loadUserByUsername() durante o processo de autenticação
 * para carregar o usuário do banco e verificar a senha.
 *
 * O "username" aqui é o e-mail do usuário (definido em Usuario.getUsername()).
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        // Spring Security captura a exceção e converte em AuthenticationException
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() ->
                    new UsernameNotFoundException("Usuário não encontrado: " + email));
    }
}
