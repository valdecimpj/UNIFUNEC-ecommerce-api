package com.ecommerce.webflux.domain.repository;

import com.ecommerce.webflux.domain.model.Produto;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Repositório R2DBC – todos os métodos retornam Mono ou Flux.
 *
 * ReactiveCrudRepository fornece:
 *   Mono<T>    findById(ID)
 *   Flux<T>    findAll()
 *   Mono<T>    save(T)
 *   Mono<Void> deleteById(ID)
 *   Mono<Long> count()
 *
 * R2dbcRepository adiciona paginação e ordenação reativas.
 */
@Repository
public interface ProdutoReactiveRepository extends R2dbcRepository<Produto, Long> {

    // Spring Data gera a query automaticamente a partir do nome do método
    Flux<Produto> findByAtivoTrue();

    Flux<Produto> findByNomeContainingIgnoreCase(String nome);

    Flux<Produto> findByEstoqueGreaterThan(int estoque);

    // Query R2DBC nativa (SQL, não JPQL)
    @Query("SELECT * FROM produtos WHERE preco BETWEEN :min AND :max AND ativo = true ORDER BY preco ASC")
    Flux<Produto> findByPrecoBetween(double min, double max);

    // Atualização reativa – retorna Mono<Integer> com número de linhas afetadas
    @Query("UPDATE produtos SET estoque = estoque - :quantidade WHERE id = :id AND estoque >= :quantidade")
    Mono<Integer> decrementarEstoque(Long id, int quantidade);

    Mono<Long> countByAtivoTrue();
}
