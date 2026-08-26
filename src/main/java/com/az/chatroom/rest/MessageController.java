package com.az.chatroom.rest;

import com.az.chatroom.dtos.MessageCreateRequest;
import com.az.chatroom.dtos.MessagePageResponse;
import com.az.chatroom.services.MessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UUID createMessage(@Valid @RequestBody MessageCreateRequest createRequest) {
        return messageService.createMessage(createRequest);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public MessagePageResponse getMessageList(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "100")
            @Min(1) @Max(500)
            int size
    ) {
        return messageService.getMessageList(cursor, size);
    }
}
