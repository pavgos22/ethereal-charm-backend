CREATE TABLE category_parameters
(
    id            serial primary key,
    short_id      varchar not null,
    category_name varchar not null
);

CREATE TABLE products
(
    id                  serial primary key,
    uid                 varchar        not null,
    activate            boolean        not null DEFAULT FALSE,
    product_name        varchar        not null,
    main_desc           TEXT           not null,
    desc_html           TEXT           not null,
    price               decimal(12, 2) not null,
    discounted_price    decimal(12, 2),
    discount            boolean        not null DEFAULT FALSE,
    image_urls          varchar[]      not null,
    parameters          TEXT,
    create_at           DATE,
    category_parameters integer REFERENCES "category_parameters" (id),
    priority            INT                     DEFAULT 0 NOT NULL
);

ALTER TABLE products
    ADD CONSTRAINT unique_uid UNIQUE (uid);

CREATE TABLE user_favourites
(
    user_uuid    VARCHAR NOT NULL,
    product_uuid VARCHAR NOT NULL,
    PRIMARY KEY (user_uuid, product_uuid),
    FOREIGN KEY (user_uuid) REFERENCES "users" (uuid),
    FOREIGN KEY (product_uuid) REFERENCES products (uid)
);
