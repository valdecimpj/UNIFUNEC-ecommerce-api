package com.ecommerce.webflux.domain.repository;

import com.ecommerce.webflux.domain.model.Pedido;
import com.ecommerce.webflux.domain.model.enums.StatusPedido;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface PedidoReactiveRepository extends R2dbcRepository<Pedido, Long> {
    Flux<Pedido> findByClienteId(Long clienteId);
    Flux<Pedido> findByStatus(StatusPedido status);
}
