package com.az.chatroom.services;

import com.az.chatroom.dtos.MessageCreateRequest;
import com.az.chatroom.dtos.MessageCursor;
import com.az.chatroom.dtos.MessagePageResponse;
import com.az.chatroom.dtos.MessageResponse;
import com.az.chatroom.exceptions.ResourceNotFoundException;
import com.az.chatroom.repositories.MessageRepository;
import com.az.chatroom.repositories.projections.MessageRow;
import com.az.chatroom.utils.CursorCoderUtil;
import com.az.generated.jooq.tables.records.ChatMessageRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MessageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageService.class);
    private static final String ANONYMIZED_USERNAME = "anonymous";

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public UUID createMessage(UUID userId, MessageCreateRequest request) {
        UUID messageId = UUID.randomUUID();
        ChatMessageRecord message = new ChatMessageRecord(
                messageId,
                userId,
                request.content(),
                OffsetDateTime.now()
        );

        try {
            messageRepository.create(message);
            LOGGER.info("Message created. [messageId={}, userId={}, contentLength={}]",
                    messageId,
                    userId,
                    request.content().length());
            return messageId;
        } catch (DataIntegrityViolationException e) {
            LOGGER.warn("Message creation failed because author was not found. [userId={}]", userId);
            throw new ResourceNotFoundException("Message author not found.");
        }
    }

    public void deleteMessage(UUID messageId) {
        boolean wasDeleted = messageRepository.delete(messageId);
        if (!wasDeleted) {
            LOGGER.warn("Message deletion failed because message was not found. [messageId={}]", messageId);
            throw new ResourceNotFoundException("Message not found.");
        }

        LOGGER.info("Message deleted. [messageId={}]", messageId);
    }

    public MessagePageResponse getMessageList(String encodedCursor, int size) {
        List<MessageRow> records = fetchMessages(encodedCursor, size);

        List<MessageResponse> messages = records.stream()
                .limit(size)
                .map(this::toResponse)
                .toList();

        String nextCursor = getNextCursor(records, size);
        LOGGER.debug("Message page fetched. [requestedSize={}, returnedSize={}, hasCursor={}, hasNextPage={}]",
                size,
                messages.size(),
                encodedCursor != null && !encodedCursor.isBlank(),
                nextCursor != null);

        return new MessagePageResponse(
                messages,
                messages.size(),
                nextCursor
        );
    }

    private List<MessageRow> fetchMessages(String encodedCursor, int size) {
        MessageCursor cursor = CursorCoderUtil.decodeCursor(encodedCursor);
        if (cursor == null) {
            return messageRepository.fetchMessages(null, null, size);
        }

        return messageRepository.fetchMessages(cursor.createdAt(), cursor.messageId(), size);
    }

    private String getNextCursor(List<MessageRow> records, int size) {
        if (records.size() <= size) {
            return null;
        }

        MessageRow lastRecord = records.get(size - 1);
        MessageCursor cursorResponse = new MessageCursor(lastRecord.createdAt(), lastRecord.id());
        return CursorCoderUtil.encodeCursor(cursorResponse);
    }

    private MessageResponse toResponse(MessageRow record) {
        return new MessageResponse(
                usernameOrAnonymous(record),
                record.content(),
                record.createdAt()
        );
    }

    private String usernameOrAnonymous(MessageRow message) {
        return message.username() == null ? ANONYMIZED_USERNAME : message.username();
    }
}
