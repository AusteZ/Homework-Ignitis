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
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;

    public UserService(UserRepository userRepository, MessageRepository messageRepository) {
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
    }

    public UUID createUser(UserCreateRequest userCreateRequest) {
        UUID id = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.now();
        AppUserRecord user = new AppUserRecord(
                id,
                userCreateRequest.username(),
                userCreateRequest.role().name(),
                createdAt);

        try {
            userRepository.create(user);
            return id;
        } catch (DuplicateKeyException e) {
            throw new ResourceAlreadyExistsException(userCreateRequest.username());
        }
    }

    @Transactional
    public void deleteUser(UUID userId) {
        messageRepository.anonymizeByUserId(userId);
        boolean wasDeleted = userRepository.delete(userId);
        if (!wasDeleted) {
            throw new ResourceNotFoundException("userId not found.");
        }
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
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
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
