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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MessageService {
    private final String ANONYMIZED_USERNAME = "anonymous";

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
            return messageId;
        } catch (DataIntegrityViolationException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    public MessagePageResponse getMessageList(String encodedCursor, int size) {
        List<MessageRow> records = fetchMessages(encodedCursor, size);

        List<MessageResponse> messages = records.stream()
                .limit(size)
                .map(this::toResponse)
                .toList();

        return new MessagePageResponse(
                messages,
                messages.size(),
                getNextCursor(records, size)
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
