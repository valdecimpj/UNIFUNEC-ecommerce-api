package com.ecommerce.presentation.controller;

import com.ecommerce.application.dto.CriarPedidoDTO;
import com.ecommerce.application.dto.PedidoResponseDTO;
import com.ecommerce.application.usecase.BuscarPedidosUseCase;
import com.ecommerce.application.usecase.CancelarPedidoUseCase;
import com.ecommerce.application.usecase.CriarPedidoUseCase;
import com.ecommerce.domain.model.enums.StatusPedido;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST de Pedidos.
 * Depende apenas dos Use Cases (application layer) – não acessa repositórios diretamente.
 * Responsabilidade única: receber requisição HTTP e delegar ao caso de uso correto.
 */
@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
@Tag(name = "Pedidos", description = "Gerenciamento de pedidos do e-commerce")
public class PedidoController {

    private final CriarPedidoUseCase criarPedidoUseCase;
    private final CancelarPedidoUseCase cancelarPedidoUseCase;
    private final BuscarPedidosUseCase buscarPedidosUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria um novo pedido")
    public PedidoResponseDTO criar(@RequestBody @Valid CriarPedidoDTO dto) {
        return criarPedidoUseCase.executar(dto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca pedido por ID")
    public PedidoResponseDTO buscarPorId(@PathVariable Long id) {
        return buscarPedidosUseCase.porId(id);
    }

    @GetMapping
    @Operation(summary = "Lista pedidos com paginação e filtro opcional por status")
    public Page<PedidoResponseDTO> listar(
            @RequestParam(required = false) StatusPedido status,
            @PageableDefault(size = 20, sort = "criadoEm",
                             direction = Sort.Direction.DESC) Pageable pageable) {
        return buscarPedidosUseCase.listar(status, pageable);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cancela um pedido")
    public void cancelar(
            @PathVariable Long id,
            @RequestParam @NotBlank String motivo) {
        cancelarPedidoUseCase.executar(id, motivo);
    }
}
