package ru.practicum.ewm.events.interaction;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(name = "stats-server", contextId = "eventStatsClient")
public interface EventStatsFeignClient {
    @PostMapping("/hit")
    void saveHit(@RequestBody EndpointHitDto hit);

    @GetMapping("/stats")
    List<ViewStatsDto> getStats(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
            @RequestParam(required = false) List<String> uris,
            @RequestParam boolean unique);
}
