-- Rulat o singura data in pgAdmin (Query Tool), conectat la serverul PostgreSQL local.
-- 1. Creeaza baza de date (nu se poate face din aceeasi tranzactie/conexiune cu restul):
--    CREATE DATABASE socialnetwork;
-- 2. Deschide o conexiune noua la baza socialnetwork si ruleaza restul scriptului.

CREATE TABLE users (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username   VARCHAR NOT NULL UNIQUE,
    first_name VARCHAR NOT NULL,
    last_name  VARCHAR NOT NULL,
    email      VARCHAR NOT NULL
);

CREATE TABLE friendships (
    id1          BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    id2          BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    friends_from TIMESTAMP NOT NULL,
    PRIMARY KEY (id1, id2),
    CHECK (id1 < id2)
);
