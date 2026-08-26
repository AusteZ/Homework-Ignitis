package com.az.chatroom.repositories;

import com.az.chatroom.repositories.projections.MessageRow;
import com.az.generated.jooq.tables.records.ChatMessageRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.az.generated.jooq.Tables.APP_USER;
import static com.az.generated.jooq.Tables.CHAT_MESSAGE;
import static org.jooq.impl.DSL.trueCondition;

@Repository
public class MessageRepository {
    private final DSLContext dsl;

    public MessageRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public void create(ChatMessageRecord message) {
        dsl.insertInto(CHAT_MESSAGE)
                .set(message)
                .execute();
    }

    public void anonymizeByUserId(UUID userId) {
        dsl.update(CHAT_MESSAGE)
                .setNull(CHAT_MESSAGE.USER_ID)
                .where(CHAT_MESSAGE.USER_ID.eq(userId))
                .execute();
    }

    public List<MessageRow> fetchMessages(OffsetDateTime cursorCreatedAt, UUID cursorMessageId, int size) {
        Condition cursorCondition = cursorCreatedAt == null
                ? trueCondition()
                : CHAT_MESSAGE.CREATED_AT.lt(cursorCreatedAt)
                .or(CHAT_MESSAGE.CREATED_AT.eq(cursorCreatedAt)
                        .and(CHAT_MESSAGE.ID.lt(cursorMessageId)));

        return dsl.select(
                        CHAT_MESSAGE.ID,
                        APP_USER.USERNAME,
                        CHAT_MESSAGE.CONTENT,
                        CHAT_MESSAGE.CREATED_AT
                )
                .from(CHAT_MESSAGE)
                .leftJoin(APP_USER)
                .on(CHAT_MESSAGE.USER_ID.eq(APP_USER.ID))
                .where(cursorCondition)
                .orderBy(
                        CHAT_MESSAGE.CREATED_AT.desc(),
                        CHAT_MESSAGE.ID.desc()
                )
                .limit(size + 1)
                .fetch(record -> new MessageRow(
                        record.get(CHAT_MESSAGE.ID),
                        record.get(APP_USER.USERNAME),
                        record.get(CHAT_MESSAGE.CONTENT),
                        record.get(CHAT_MESSAGE.CREATED_AT)
                ));
    }
}
