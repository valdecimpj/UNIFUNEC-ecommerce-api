package com.ecommerce.application.usecase;

import com.ecommerce.domain.exception.CategoriaNaoEncontradaException;
import com.ecommerce.domain.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Deletar Categoria.
 * Responsabilidade única: remover uma categoria existente.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeletarCategoriaUseCase {

    private final CategoriaRepository categoriaRepository;

    @Transactional
    public void executar(Long id) {
        log.info("Deletando categoria id={}", id);

        if (!categoriaRepository.existsById(id)) {
            throw new CategoriaNaoEncontradaException(id);
        }

        categoriaRepository.deleteById(id);
        log.info("Categoria id={} deletada com sucesso", id);
    }
}
