--liquibase formatted sql

--changeset az:003-add-app-user-password-hash
ALTER TABLE app_user
    ADD password_hash VARCHAR(255) NOT NULL;

--rollback ALTER TABLE app_user DROP COLUMN password_hash;
