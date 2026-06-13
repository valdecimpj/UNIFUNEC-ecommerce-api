package com.ecommerce.webflux.domain.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Documento MongoDB – avaliações de produtos.
 *
 * MongoDB é ideal para reviews porque:
 *   - Schema flexível (campos opcionais sem migration)
 *   - Leitura rápida de documento único com todos os dados
 *   - Change Streams para notificações em tempo real
 *   - Tailable Cursors para consumo contínuo
 */
@Document(collection = "avaliacoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Avaliacao {

    @Id
    private String id; // MongoDB usa String para _id (ObjectId)

    @Indexed // índice para buscas frequentes por produto
    @Field("produto_id")
    private Long produtoId;

    @Field("cliente_id")
    private Long clienteId;

    @Field("nome_cliente")
    private String nomeCliente;

    @Field("nota")
    private Integer nota; // 1 a 5

    @Field("comentario")
    private String comentario;

    @Field("criado_em")
    @Builder.Default
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Field("aprovada")
    @Builder.Default
    private boolean aprovada = false; // moderação antes de exibir
}
