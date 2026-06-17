package com.ecommerce.application.usecase;

import com.ecommerce.application.dto.AtualizarCategoriaDTO;
import com.ecommerce.application.dto.CategoriaResponseDTO;
import com.ecommerce.application.mapper.CategoriaMapper;
import com.ecommerce.domain.exception.CategoriaNaoEncontradaException;
import com.ecommerce.domain.model.Categoria;
import com.ecommerce.domain.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Atualizar Categoria (PATCH semântico via MapStruct).
 * Responsabilidade única: orquestrar atualização parcial de categoria.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AtualizarCategoriaUseCase {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    @Transactional
    public CategoriaResponseDTO executar(Long id, AtualizarCategoriaDTO dto) {
        log.info("Atualizando categoria id={}", id);

        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new CategoriaNaoEncontradaException(id));

        // MapStruct ignora campos nulos – só atualiza o que veio no body
        categoriaMapper.updateFromDTO(dto, categoria);
        Categoria atualizada = categoriaRepository.save(categoria);

        return categoriaMapper.toResponseDTO(atualizada);
    }
}
