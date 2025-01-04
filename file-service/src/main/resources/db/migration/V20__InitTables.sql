CREATE TABLE "image_data"
(
    id       serial primary key,
    uuid     varchar not null,
    pathfile varchar not null,
    isUsed   boolean not null DEFAULT FALSE,
    createAt DATE    not null
);