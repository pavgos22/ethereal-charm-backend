CREATE TABLE "users"
(
    id          SERIAL PRIMARY KEY,
    uuid        VARCHAR NOT NULL,
    login       VARCHAR NOT NULL,
    email       VARCHAR NOT NULL,
    password    VARCHAR NOT NULL,
    role        VARCHAR NOT NULL,
    isLock      BOOLEAN DEFAULT true,
    isEnabled   BOOLEAN DEFAULT false,
    firstname   VARCHAR,
    lastname    VARCHAR,
    phone       VARCHAR,
    city        VARCHAR,
    street      VARCHAR,
    apartment   VARCHAR,
    postalcode  VARCHAR,
    iscompany   BOOLEAN DEFAULT FALSE,
    companyname VARCHAR,
    nip         VARCHAR
);

ALTER TABLE "users"
    ADD CONSTRAINT unique_uuid UNIQUE (uuid);