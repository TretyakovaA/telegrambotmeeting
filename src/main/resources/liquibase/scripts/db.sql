-- liquibase formatted sql

-- changeset nastya:1

create table events
(
    id                    bigint not null
        primary key,
        name                  varchar(255) not null,
        event_date            timestamp not null,
        link                  text,
        creator               bigint not null
);