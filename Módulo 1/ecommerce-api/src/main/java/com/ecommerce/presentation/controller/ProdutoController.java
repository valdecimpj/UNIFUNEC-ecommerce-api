package com.ecommerce.presentation.controller;

import com.ecommerce.application.dto.AtualizarProdutoDTO;
import com.ecommerce.application.dto.CriarProdutoDTO;
import com.ecommerce.application.dto.ProdutoResponseDTO;
import com.ecommerce.application.dto.projection.ProdutoCatalogoProjection;
import com.ecommerce.application.dto.projection.ProdutoResumoProjection;
import com.ecommerce.application.mapper.ProdutoMapper;
import com.ecommerce.domain.exception.ProdutoNaoEncontradoException;
import com.ecommerce.domain.model.Categoria;
import com.ecommerce.domain.model.Produto;
import com.ecommerce.domain.repository.CategoriaRepository;
import com.ecommerce.domain.repository.ProdutoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/produtos")
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "Catálogo de produtos")
public class ProdutoController {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProdutoMapper produtoMapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo produto")
    public ProdutoResponseDTO criar(@RequestBody @Valid CriarProdutoDTO dto) {
        @SuppressWarnings("null")
        Categoria categoria = categoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada: " + dto.getCategoriaId()));

        Produto produto = produtoMapper.toDomain(dto);
        produto.setCategoria(categoria);

        return produtoMapper.toResponseDTO(produtoRepository.save(produto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca produto por ID")
    public ProdutoResponseDTO buscarPorId(@PathVariable Long id) {
        return produtoRepository.findById(id)
                .map(produtoMapper::toResponseDTO)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    @GetMapping
    @Operation(summary = "Lista todos os produtos com paginação")
    public Page<ProdutoResponseDTO> listar(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return produtoRepository.findAll(pageable)
                .map(produtoMapper::toResponseDTO);
    }

    @GetMapping("/catalogo")
    @Operation(summary = "Catálogo com produtos disponíveis (Projection otimizada)")
    public List<ProdutoCatalogoProjection> catalogo() {
        return produtoRepository.findProdutosDisponiveis();
    }

    @GetMapping("/por-categoria/{categoriaNome}")
    @Operation(summary = "Lista resumo de produtos por categoria (Interface Projection)")
    public List<ProdutoResumoProjection> porCategoria(@PathVariable String categoriaNome) {
        return produtoRepository.findByCategoriaNome(categoriaNome);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualização parcial de produto (PATCH semântico via MapStruct)")
    public ProdutoResponseDTO atualizar(
            @PathVariable Long id,
            @RequestBody @Valid AtualizarProdutoDTO dto) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));

        // MapStruct ignora campos nulos – só atualiza o que veio no body
        produtoMapper.updateFromDTO(dto, produto);
        return produtoMapper.toResponseDTO(produtoRepository.save(produto));
    }
}
