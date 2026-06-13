package com.ecommerce.webflux.domain.repository;

import com.ecommerce.webflux.domain.model.ItemPedido;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ItemPedidoReactiveRepository extends R2dbcRepository<ItemPedido, Long> {
    Flux<ItemPedido> findByPedidoId(Long pedidoId);
}
