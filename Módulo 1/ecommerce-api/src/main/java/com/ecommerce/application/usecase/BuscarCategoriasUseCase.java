package com.ecommerce.application.usecase;

import com.ecommerce.application.dto.CategoriaResponseDTO;
import com.ecommerce.application.mapper.CategoriaMapper;
import com.ecommerce.domain.exception.CategoriaNaoEncontradaException;
import com.ecommerce.domain.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Caso de uso: Buscar Categorias.
 * Responsabilidade única: consultas de leitura de categorias.
 */
@Service
@RequiredArgsConstructor
public class BuscarCategoriasUseCase {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    @Transactional(readOnly = true)
    public CategoriaResponseDTO buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .map(categoriaMapper::toResponseDTO)
                .orElseThrow(() -> new CategoriaNaoEncontradaException(id));
    }

    @Transactional(readOnly = true)
    public Page<CategoriaResponseDTO> listar(Pageable pageable) {
        return categoriaRepository.findAll(pageable)
                .map(categoriaMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponseDTO> buscarPorNome(String nome) {
        return categoriaRepository.findByNomeContainingIgnoreCase(nome)
                .stream()
                .map(categoriaMapper::toResponseDTO)
                .toList();
    }
}
