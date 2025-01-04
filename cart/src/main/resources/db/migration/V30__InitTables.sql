CREATE TABLE "cart"
(
    id   serial primary key,
    uuid varchar not null
);

CREATE TABLE "cart_items"
(
    id       serial primary key,
    uuid     varchar not null,
    product  varchar not null,
    cart     integer REFERENCES cart (id),
    quantity int     not null DEFAULT 1
);

ALTER TABLE cart_items
    ADD COLUMN price_unit DOUBLE PRECISION NOT NULL DEFAULT 0.0;
