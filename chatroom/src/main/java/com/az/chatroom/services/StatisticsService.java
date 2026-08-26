package com.az.chatroom.services;

import com.az.chatroom.dtos.StatisticsResponse;
import com.az.chatroom.exceptions.ResourceNotFoundException;
import com.az.chatroom.repositories.MessageRepository;
import com.az.chatroom.repositories.projections.StatisticsRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StatisticsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsService.class);

    private final MessageRepository messageRepository;

    public StatisticsService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    public StatisticsResponse getUserMessageStats(UUID userId) {
        return messageRepository.fetchUserMessageStats(userId)
                .map(stats -> {
                    LOGGER.debug("User message statistics fetched. [userId={}, messageCount={}]",
                            userId,
                            stats.messageCount());
                    return toResponse(stats);
                })
                .orElseThrow(() -> {
                    LOGGER.warn("User message statistics not found. [userId={}]", userId);
                    return new ResourceNotFoundException("User statistics not found.");
                });
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
