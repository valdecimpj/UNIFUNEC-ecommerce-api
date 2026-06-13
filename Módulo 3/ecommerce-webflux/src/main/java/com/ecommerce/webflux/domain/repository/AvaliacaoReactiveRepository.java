package com.ecommerce.webflux.domain.repository;

import com.ecommerce.webflux.domain.model.Avaliacao;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Tailable;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositório MongoDB Reativo.
 *
 * ReactiveMongoRepository fornece:
 *   Mono<T>    findById(ID)
 *   Flux<T>    findAll()
 *   Mono<T>    save(T)
 *   Mono<Void> deleteById(ID)
 *
 * @Tailable: mantém o cursor aberto para consumo contínuo
 * (requer coleção "capped" no MongoDB)
 */
@Repository
public interface AvaliacaoReactiveRepository
        extends ReactiveMongoRepository<Avaliacao, String> {

    // Busca todas as avaliações aprovadas de um produto
    Flux<Avaliacao> findByProdutoIdAndAprovadaTrue(Long produtoId);

    // Conta avaliações por produto
    Mono<Long> countByProdutoId(Long produtoId);

    // Média das notas (calculada no Java via Flux)
    Flux<Avaliacao> findByProdutoId(Long produtoId);

    // Avaliações de um cliente específico
    Flux<Avaliacao> findByClienteId(Long clienteId);

    /**
     * @Tailable – cursor que permanece aberto em coleção capped.
     * Emite novos documentos à medida que são inseridos (streaming contínuo).
     * Usado para feed de avaliações em tempo real.
     *
     * REQUISITO: a coleção deve ser criada como "capped" no MongoDB:
     *   db.createCollection("avaliacoes", { capped: true, size: 10485760 })
     */
    @Tailable
    Flux<Avaliacao> findWithTailableCursorByAprovadaTrue();
}
