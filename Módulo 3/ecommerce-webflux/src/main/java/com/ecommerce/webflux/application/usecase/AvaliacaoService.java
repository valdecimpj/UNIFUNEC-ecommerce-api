package com.ecommerce.webflux.application.usecase;

import com.ecommerce.webflux.domain.model.Avaliacao;
import com.ecommerce.webflux.domain.repository.AvaliacaoReactiveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Serviço de avaliações usando MongoDB Reativo.
 *
 * ReactiveMongoRepository – operações CRUD simples
 * ReactiveMongoTemplate   – queries complexas, aggregations, Change Streams
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AvaliacaoService {

    private final AvaliacaoReactiveRepository avaliacaoRepository;
    private final ReactiveMongoTemplate mongoTemplate; // para queries complexas

    // =========================================================================
    // CRUD REATIVO
    // =========================================================================

    public Mono<Avaliacao> criar(Avaliacao avaliacao) {
        return avaliacaoRepository.save(avaliacao)
                .doOnSuccess(a -> log.info("Avaliação criada: id={} produto={}",
                        a.getId(), a.getProdutoId()));
    }

    public Flux<Avaliacao> listarAprovadas(Long produtoId) {
        return avaliacaoRepository.findByProdutoIdAndAprovadaTrue(produtoId)
                .doOnNext(a -> log.debug("Emitindo avaliação: id={}", a.getId()));
    }

    // =========================================================================
    // AGGREGATION – média das notas por produto
    // =========================================================================

    /**
     * Calcula a média das notas de um produto usando aggregation pipeline.
     * ReactiveMongoTemplate permite queries complexas além do Repository.
     */
    public Mono<Double> calcularMediaNotas(Long produtoId) {
        return avaliacaoRepository.findByProdutoId(produtoId)
                .filter(Avaliacao::isAprovada)
                .map(Avaliacao::getNota)
                .reduce(0, Integer::sum)
                .zipWith(avaliacaoRepository.countByProdutoId(produtoId))
                .map(tuple -> {
                    long total = tuple.getT2();
                    if (total == 0) return 0.0;
                    return tuple.getT1().doubleValue() / total;
                });
    }

    /**
     * Aggregation com ReactiveMongoTemplate.
     * Mais expressiva para pipelines complexas.
     */
    public Mono<Double> calcularMediaNotasAggregate(Long produtoId) {
        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("produto_id").is(produtoId)
                        .and("aprovada").is(true)),
                Aggregation.group().avg("nota").as("media")
        );

        return mongoTemplate
                .aggregate(agg, "avaliacoes", org.bson.Document.class)
                .next()
                .map(doc -> doc.getDouble("media"))
                .defaultIfEmpty(0.0);
    }

    // =========================================================================
    // CHANGE STREAMS – notificações em tempo real de novas avaliações
    // =========================================================================

    /**
     * Change Stream: emite um evento para cada INSERT/UPDATE na coleção.
     * Mantém o Flux aberto indefinidamente – ideal para SSE ou WebSocket.
     *
     * REQUISITO: MongoDB deve estar em modo Replica Set (ou sharded cluster).
     * Para dev local: mongod --replSet rs0
     */
    public Flux<Avaliacao> streamNovasAvaliacoes() {
        return mongoTemplate.changeStream(
                        "avaliacoes",
                        org.springframework.data.mongodb.core.ChangeStreamOptions.builder()
                                .filter(Aggregation.newAggregation(
                                    Aggregation.match(Criteria.where("operationType").is("insert"))
                                ))
                                .build(),
                        Avaliacao.class
                )
                .map(org.springframework.data.mongodb.core.ChangeStreamEvent::getBody)
                .doOnNext(a -> log.info("[STREAM] Nova avaliação: produto={} nota={}",
                        a.getProdutoId(), a.getNota()));
    }

    /**
     * Tailable Cursor – fluxo contínuo de documentos de uma coleção capped.
     * Mais simples que Change Streams, mas requer coleção capped.
     */
    public Flux<Avaliacao> streamAvaliacoesAprovadas() {
        return avaliacaoRepository.findWithTailableCursorByAprovadaTrue()
                .doOnNext(a -> log.debug("[TAILABLE] Avaliação aprovada: {}", a.getId()));
    }

    // =========================================================================
    // MODERAÇÃO – aprovação de avaliações
    // =========================================================================

    public Mono<Avaliacao> aprovar(String avaliacaoId) {
        return avaliacaoRepository.findById(avaliacaoId)
                .switchIfEmpty(Mono.error(new RuntimeException("Avaliação não encontrada: " + avaliacaoId)))
                .flatMap(avaliacao -> {
                    avaliacao.setAprovada(true);
                    return avaliacaoRepository.save(avaliacao);
                })
                .doOnSuccess(a -> log.info("Avaliação aprovada: id={}", a.getId()));
    }

    /**
     * ReactiveMongoTemplate para query dinâmica com filtros opcionais.
     */
    public Flux<Avaliacao> buscarComFiltros(Long produtoId, Integer notaMinima, boolean apenasAprovadas) {
        var criteria = Criteria.where("produto_id").is(produtoId);

        if (notaMinima != null) {
            criteria = criteria.and("nota").gte(notaMinima);
        }
        if (apenasAprovadas) {
            criteria = criteria.and("aprovada").is(true);
        }

        return mongoTemplate.find(
                Query.query(criteria),
                Avaliacao.class,
                "avaliacoes"
        );
    }
}
