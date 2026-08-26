package com.az.chatroom.rest;

import com.az.chatroom.dtos.StatisticsResponse;
import com.az.chatroom.services.StatisticsService;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/statistics")
@Validated
public class StatisticsController {
    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public StatisticsResponse getUserMessageStats(@PathVariable @NotNull UUID userId) {
        return statisticsService.getUserMessageStats(userId);
    }
}
