package com.az.chatroom.services;

import com.az.chatroom.dtos.StatisticsResponse;
import com.az.chatroom.exceptions.ResourceNotFoundException;
import com.az.chatroom.repositories.MessageRepository;
import com.az.chatroom.repositories.projections.StatisticsRow;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StatisticsService {
    private final MessageRepository messageRepository;

    public StatisticsService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public StatisticsResponse getUserMessageStats(UUID userId) {
        return messageRepository.fetchUserMessageStats(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("userId not found."));
    }

    private StatisticsResponse toResponse(StatisticsRow stats) {
        return new StatisticsResponse(
                stats.username(),
                stats.messageCount(),
                stats.firstMessageAt(),
                stats.lastMessageAt(),
                stats.averageMessageLength(),
                stats.lastMessageText()
        );
    }
}
