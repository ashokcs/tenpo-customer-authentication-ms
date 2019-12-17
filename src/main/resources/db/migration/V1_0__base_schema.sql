CREATE TABLE customer_login(
    id              UUID NOT NULL,
    pan_hash        VARCHAR(100) NOT NULL,
    user_name       VARCHAR(100) NOT NULL,
    user_attempt    INTEGER NOT NULL,
    created         TIMESTAMP NOT NULL,
    updated         TIMESTAMP NOT NULL,
    CONSTRAINT customer_login_pk PRIMARY KEY(id),
);

CREATE TABLE customer_transaction_context(
    id              UUID NOT NULL,
    external_id     UUID NOT NULL ,
    tx_type         VARCHAR(20) NOT NULL,
    tx_amount       NUMERIC(15,2) NOT NULL,
    tx_currency     INTEGER NOT NULL,
    tx_merchant     VARCHAR(100) NOT NULL,
    tx_country_code INTEGER NOT NULL,
    tx_place_name   VARCHAR(100) NOT NULL,
    tx_other        VARCHAR(200) NOT NULL,
    created         TIMESTAMP NOT NULL,
    updated         TIMESTAMP NOT NULL,
    CONSTRAINT customer_transaction_context_pk PRIMARY KEY(id),
    UNIQUE (external_id)
);

CREATE TABLE customer_challenge(
    id                      UUID NOT NULL,
    transaction_context_id  UUID NOT NULL,
    challenge_type          VARCHAR(20) NOT NULL,
    callback_uri            VARCHAR(200) NOT NULL,
    created                 TIMESTAMP NOT NULL,
    updated                 TIMESTAMP NOT NULL,
    CONSTRAINT customer_challenge_pk PRIMARY KEY(id),
    FOREIGN KEY (transaction_context_id) REFERENCES customer_transaction_context(id),
);
