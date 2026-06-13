-- R2DBC não usa Hibernate DDL – schema criado manualmente
CREATE TABLE IF NOT EXISTS produtos (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome        VARCHAR(100)   NOT NULL,
    descricao   VARCHAR(500),
    preco       DECIMAL(10, 2) NOT NULL,
    estoque     INT            NOT NULL DEFAULT 0,
    ativo       BOOLEAN        NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS pedidos (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id  BIGINT         NOT NULL,
    status      VARCHAR(20)    NOT NULL DEFAULT 'PENDENTE',
    frete       DECIMAL(10, 2) NOT NULL DEFAULT 0,
    criado_em   TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS itens_pedido (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id       BIGINT         NOT NULL,
    produto_id      BIGINT         NOT NULL,
    quantidade      INT            NOT NULL,
    preco_unitario  DECIMAL(10, 2) NOT NULL
);
