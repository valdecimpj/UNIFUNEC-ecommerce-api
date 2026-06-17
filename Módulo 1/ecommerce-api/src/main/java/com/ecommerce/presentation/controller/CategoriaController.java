package com.ecommerce.presentation.controller;

import com.ecommerce.application.dto.AtualizarCategoriaDTO;
import com.ecommerce.application.dto.CategoriaResponseDTO;
import com.ecommerce.application.dto.CriarCategoriaDTO;
import com.ecommerce.application.usecase.AtualizarCategoriaUseCase;
import com.ecommerce.application.usecase.BuscarCategoriasUseCase;
import com.ecommerce.application.usecase.CriarCategoriaUseCase;
import com.ecommerce.application.usecase.DeletarCategoriaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
@Tag(name = "Categorias", description = "Gerenciamento de categorias dos Produtos do e-commerce")
public class CategoriaController {

    private final CriarCategoriaUseCase criarCategoriaUseCase;
    private final BuscarCategoriasUseCase buscarCategoriasUseCase;
    private final AtualizarCategoriaUseCase atualizarCategoriaUseCase;
    private final DeletarCategoriaUseCase deletarCategoriaUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma nova categoria")
    public CategoriaResponseDTO criar(@RequestBody @Valid CriarCategoriaDTO dto) {
        return criarCategoriaUseCase.executar(dto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca categoria por ID")
    public CategoriaResponseDTO buscarPorId(@PathVariable Long id) {
        return buscarCategoriasUseCase.buscarPorId(id);
    }

    @GetMapping
    @Operation(summary = "Lista todas as categorias com paginação")
    public Page<CategoriaResponseDTO> listar(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return buscarCategoriasUseCase.listar(pageable);
    }

    @GetMapping("/buscar")
    @Operation(summary = "Busca categorias pelo nome (parcial, sem distinção de maiúsculas)")
    public List<CategoriaResponseDTO> buscarPorNome(@RequestParam String nome) {
        return buscarCategoriasUseCase.buscarPorNome(nome);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Atualização parcial de categoria (PATCH semântico via MapStruct)")
    public CategoriaResponseDTO atualizar(
            @PathVariable Long id,
            @RequestBody @Valid AtualizarCategoriaDTO dto) {
        return atualizarCategoriaUseCase.executar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove uma categoria")
    public void deletar(@PathVariable Long id) {
        deletarCategoriaUseCase.executar(id);
    }
}
