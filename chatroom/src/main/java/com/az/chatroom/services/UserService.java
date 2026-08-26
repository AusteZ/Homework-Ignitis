package com.az.chatroom.services;

import com.az.chatroom.dtos.UserCreateRequest;
import com.az.chatroom.dtos.UserPageResponse;
import com.az.chatroom.dtos.UserResponse;
import com.az.chatroom.enums.UserRole;
import com.az.chatroom.exceptions.ResourceAlreadyExistsException;
import com.az.chatroom.exceptions.ResourceNotFoundException;
import com.az.chatroom.repositories.MessageRepository;
import com.az.chatroom.repositories.UserRepository;
import com.az.generated.jooq.tables.records.AppUserRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, MessageRepository messageRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UUID createUser(UserCreateRequest userCreateRequest) {
        UUID id = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();
        AppUserRecord user = new AppUserRecord();
        user.setId(id);
        user.setUsername(userCreateRequest.username());
        user.setRole(userCreateRequest.role().name());
        user.setCreatedAt(createdAt);
        user.setPasswordHash(passwordEncoder.encode(userCreateRequest.password()));

        try {
            userRepository.create(user);
            LOGGER.info("User created. [userId={}, username={}, role={}]",
                    id,
                    userCreateRequest.username(),
                    userCreateRequest.role());
            return id;
        } catch (DuplicateKeyException e) {
            LOGGER.warn("User creation failed because username already exists. [username={}]",
                    userCreateRequest.username());
            throw new ResourceAlreadyExistsException("User already exists.");
        }
    }

    @Transactional
    public void deleteUser(UUID userId) {
        int anonymizedMessageCount = messageRepository.anonymizeByUserId(userId);
        boolean wasDeleted = userRepository.delete(userId);
        if (!wasDeleted) {
            LOGGER.warn("User deletion failed because user was not found. [userId={}, anonymizedMessageCount={}]",
                    userId,
                    anonymizedMessageCount);
            throw new ResourceNotFoundException("User not found.");
        }

        LOGGER.info("User deleted and messages anonymized. [userId={}, anonymizedMessageCount={}]",
                userId,
                anonymizedMessageCount);
    }

    public UserPageResponse getUserList(int page, int size) {
        List<AppUserRecord> records = userRepository.fetchUsers(page, size);

        List<UserResponse> users = records.stream()
                .limit(size)
                .map(this::toResponse)
                .toList();

        boolean hasNext = records.size() > size;

        return new UserPageResponse(
                users,
                page,
                users.size(),
                hasNext
        );
    }

    public UserResponse getUser(UUID userId) {
        AppUserRecord appUserRecord = userRepository.findById(userId)
                .orElseThrow(() -> {
                    LOGGER.warn("User lookup failed because user was not found. [userId={}]", userId);
                    return new ResourceNotFoundException("User not found.");
                });
        return toResponse(appUserRecord);
    }

    private UserResponse toResponse(AppUserRecord record) {
        return new UserResponse(
                record.getId(),
                record.getUsername(),
                UserRole.valueOf(record.getRole()),
                record.getCreatedAt()
        );
    }
}
