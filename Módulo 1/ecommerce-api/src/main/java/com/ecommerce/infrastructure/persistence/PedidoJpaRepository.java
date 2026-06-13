package com.ecommerce.infrastructure.persistence;

import com.ecommerce.domain.model.Pedido;
import com.ecommerce.domain.model.enums.StatusPedido;
import com.ecommerce.domain.repository.PedidoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Implementação JPA do repositório de Pedido.
 * Fetch join para evitar N+1 ao carregar itens.
 */
//@Repository
public interface PedidoJpaRepository
        extends JpaRepository<Pedido, Long>, PedidoRepository {

    Page<Pedido> findByClienteId(Long clienteId, Pageable pageable);
    Page<Pedido> findByStatus(StatusPedido status, Pageable pageable);

    // Fetch join: carrega itens e produtos em uma única query (evita N+1)
    @Query("""
        SELECT DISTINCT p FROM Pedido p
        LEFT JOIN FETCH p.itens i
        LEFT JOIN FETCH i.produto
        WHERE p.id = :id
        """)
    java.util.Optional<Pedido> findByIdWithItens(Long id);
}
