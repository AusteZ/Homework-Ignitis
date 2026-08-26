package com.az.chatroom.rest;

import com.az.chatroom.services.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/messages")
public class AdminMessageController {
    private final MessageService messageService;

    public AdminMessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @DeleteMapping("/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(@PathVariable UUID messageId) {
        messageService.deleteMessage(messageId);
    }
}
