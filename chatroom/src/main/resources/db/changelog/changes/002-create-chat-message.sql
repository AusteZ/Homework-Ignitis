--liquibase formatted sql

--changeset az:002-create-chat-message
CREATE TABLE chat_message
(
    id            UUID                     NOT NULL,
    user_id       UUID,
    content       VARCHAR(1000)            NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_chat_message
        PRIMARY KEY (id),

    CONSTRAINT fk_chat_message_user
        FOREIGN KEY (user_id)
            REFERENCES app_user (id),

    CONSTRAINT chk_chat_message_content_not_blank
        CHECK (LENGTH(TRIM(content)) > 0)
);

CREATE INDEX idx_chat_message_created_at_id
    ON chat_message (created_at DESC, id DESC);

--rollback DROP TABLE chat_message;
