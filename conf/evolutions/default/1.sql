-- !Ups

CREATE TABLE account (
    name VARCHAR(255) PRIMARY KEY,
    type VARCHAR(9) NOT NULL check (type in ('Essential', 'Elevated', 'Academic')),
    bearer_token TEXT NOT NULL
);

CREATE TABLE collect (
    name VARCHAR(255) PRIMARY KEY,
    created_at DATE DEFAULT now(),
    directory TEXT NOT NULL,
    account VARCHAR(255) NOT NULL REFERENCES account(name)
);

CREATE TABLE rule (
    id BIGINT PRIMARY KEY,
    tag TEXT NOT NULL,
    content TEXT NOT NULL,
    collect VARCHAR(255) NOT NULL REFERENCES collect(name),
    created_at DATE DEFAULT now(),
    is_active BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE temporary_rule (
    id BIGSERIAL NOT NULL PRIMARY KEY,
    tag TEXT NOT NULL,
    content TEXT NOT NULL,
    collect VARCHAR(255) NOT NULL REFERENCES collect(name),
    created_at DATE DEFAULT now()
);

CREATE TABLE postgres_configuration (
    collect VARCHAR(255) PRIMARY KEY REFERENCES collect(name),
    host VARCHAR(255) NOT NULL,
    port INTEGER NOT NULL,
    base VARCHAR(255) NOT NULL,
    schema_ VARCHAR(255) NOT NULL,
    user_ VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE module_file_processed (
    collect VARCHAR(255) NOT NULL REFERENCES collect(name),
    module_name VARCHAR(255) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    processed_at DATE DEFAULT now(),
    PRIMARY KEY(collect, module_name, file_name)
);

-- !Downs

DROP TABLE module_file_processed;
DROP TABLE postgres_configuration;
DROP TABLE temporary_rule;
DROP TABLE rule;
DROP TABLE collect;
DROP TABLE account;