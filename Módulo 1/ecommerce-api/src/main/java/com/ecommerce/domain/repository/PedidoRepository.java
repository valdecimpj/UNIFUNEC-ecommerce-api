package com.ecommerce.domain.repository;

import com.ecommerce.domain.model.Pedido;
import com.ecommerce.domain.model.enums.StatusPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Contrato de repositório de Pedido definido no domínio.
 * A implementação real (JPA) fica na camada de infraestrutura.
 */
public interface PedidoRepository {
    Optional<Pedido> findById(Long id);
    Pedido save(Pedido pedido);
    Page<Pedido> findByClienteId(Long clienteId, Pageable pageable);
    Page<Pedido> findByStatus(StatusPedido status, Pageable pageable);
    Page<Pedido> findAll(Pageable pageable);
}
