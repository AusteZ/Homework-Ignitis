package com.az.chatroom.repositories;

import com.az.chatroom.repositories.projections.MessageRow;
import com.az.chatroom.repositories.projections.StatisticsRow;
import com.az.generated.jooq.tables.records.ChatMessageRecord;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.az.generated.jooq.Tables.APP_USER;
import static com.az.generated.jooq.Tables.CHAT_MESSAGE;
import static org.jooq.impl.DSL.avg;
import static org.jooq.impl.DSL.length;
import static org.jooq.impl.DSL.max;
import static org.jooq.impl.DSL.min;
import static org.jooq.impl.DSL.rowNumber;
import static org.jooq.impl.DSL.trueCondition;
import static org.jooq.impl.DSL.when;

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

    public Optional<StatisticsRow> fetchUserMessageStats(UUID userId) {
        Field<String> content = CHAT_MESSAGE.CONTENT.as("content");
        Field<OffsetDateTime> createdAt = CHAT_MESSAGE.CREATED_AT.as("created_at");
        Field<Integer> rowNumber = rowNumber()
                .over()
                .orderBy(CHAT_MESSAGE.CREATED_AT.desc(), CHAT_MESSAGE.ID.desc())
                .as("row_number");

        Table<?> userMessages = dsl.select(content, createdAt, rowNumber)
                .from(CHAT_MESSAGE)
                .where(CHAT_MESSAGE.USER_ID.eq(userId))
                .asTable("user_messages");

        Field<String> userMessageContent = userMessages.field(content);
        Field<OffsetDateTime> userMessageCreatedAt = userMessages.field(createdAt);
        Field<Integer> userMessageRowNumber = userMessages.field(rowNumber);
        Field<Integer> messageCount = DSL.count().as("message_count");
        Field<OffsetDateTime> firstMessageAt = min(userMessageCreatedAt).as("first_message_at");
        Field<OffsetDateTime> lastMessageAt = max(userMessageCreatedAt).as("last_message_at");
        Field<BigDecimal> averageMessageLength = avg(length(userMessageContent)).as("average_message_length");
        Field<String> lastMessageText = max(when(userMessageRowNumber.eq(1), userMessageContent)).as("last_message_text");

        return dsl.select(
                        APP_USER.USERNAME,
                        messageCount,
                        firstMessageAt,
                        lastMessageAt,
                        averageMessageLength,
                        lastMessageText
                )
                .from(userMessages)
                .innerJoin(APP_USER)
                .on(APP_USER.ID.eq(userId))
                .groupBy(APP_USER.USERNAME)
                .fetchOptional(record -> new StatisticsRow(
                        record.get(APP_USER.USERNAME),
                        record.get(messageCount),
                        record.get(firstMessageAt),
                        record.get(lastMessageAt),
                        record.get(averageMessageLength).doubleValue(),
                        record.get(lastMessageText)
                ));
    }
}
