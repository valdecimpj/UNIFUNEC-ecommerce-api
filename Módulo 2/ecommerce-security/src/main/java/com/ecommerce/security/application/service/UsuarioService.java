package com.ecommerce.security.application.service;

import com.ecommerce.security.application.dto.CadastroRequestDTO;
import com.ecommerce.security.application.dto.UsuarioResponseDTO;
import com.ecommerce.security.domain.exception.EmailJaCadastradoException;
import com.ecommerce.security.domain.exception.UsuarioNaoEncontradoException;
import com.ecommerce.security.domain.model.Usuario;
import com.ecommerce.security.domain.model.enums.Role;
import com.ecommerce.security.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cadastra novo usuário.
     * A senha é sempre armazenada com BCrypt (nunca em texto claro).
     */
    @Transactional
    public UsuarioResponseDTO cadastrar(CadastroRequestDTO dto) {

        if (usuarioRepository.existsByEmail(dto.getEmail())) {
            throw new EmailJaCadastradoException(dto.getEmail());
        }

        Usuario usuario = Usuario.builder()
                .nome(dto.getNome())
                .email(dto.getEmail())
                // BCrypt com custo 12 (configurado no SecurityConfig)
                .senha(passwordEncoder.encode(dto.getSenha()))
                .roles(Set.of(Role.ROLE_CLIENTE)) // role padrão para auto-cadastro
                .ativo(true)
                .build();

        Usuario salvo = usuarioRepository.save(usuario);
        log.info("Usuário cadastrado: id={} email={}", salvo.getId(), salvo.getEmail());
        return UsuarioResponseDTO.from(salvo);
    }

    @Transactional(readOnly = true)
    public UsuarioResponseDTO buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .map(UsuarioResponseDTO::from)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(id));
    }

    /**
     * Promove usuário a GERENTE – apenas ADMIN pode chamar.
     * O controle de quem pode chamar este método fica no Controller com @PreAuthorize.
     */
    @Transactional
    public UsuarioResponseDTO promoverAGerente(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException(usuarioId));

        usuario.getRoles().add(Role.ROLE_GERENTE);
        return UsuarioResponseDTO.from(usuarioRepository.save(usuario));
    }
}
