package com.ecommerce.application.usecase;

import com.ecommerce.application.dto.CategoriaResponseDTO;
import com.ecommerce.application.dto.CriarCategoriaDTO;
import com.ecommerce.application.mapper.CategoriaMapper;
import com.ecommerce.domain.model.Categoria;
import com.ecommerce.domain.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso: Criar Categoria.
 * Responsabilidade única: orquestrar a criação, sem lógica de negócio própria.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CriarCategoriaUseCase {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    @Transactional
    public CategoriaResponseDTO executar(CriarCategoriaDTO dto) {
        log.info("Criando categoria: {}", dto.getNome());

        if (categoriaRepository.existsByNome(dto.getNome())) {
            throw new IllegalArgumentException("Já existe uma categoria com o nome: " + dto.getNome());
        }

        Categoria categoria = categoriaMapper.toDomain(dto);
        Categoria salva = categoriaRepository.save(categoria);

        log.info("Categoria criada com id={}", salva.getId());
        return categoriaMapper.toResponseDTO(salva);
    }
}
