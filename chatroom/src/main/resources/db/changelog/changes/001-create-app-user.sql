--liquibase formatted sql

--changeset az:001-create-app-user
CREATE TABLE app_user
(
    id            UUID                     NOT NULL,
    username      VARCHAR(64)              NOT NULL,
    role          VARCHAR(16)              NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_app_user
        PRIMARY KEY (id),

    CONSTRAINT uq_app_user_username
        UNIQUE (username),

    CONSTRAINT chk_app_user_role
        CHECK (role IN ('USER', 'ADMIN'))
);

CREATE INDEX idx_app_user_created_at_id
    ON app_user (created_at DESC, id DESC);

--rollback DROP TABLE app_user;