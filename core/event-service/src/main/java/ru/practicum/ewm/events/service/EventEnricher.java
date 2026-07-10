package ru.practicum.ewm.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.user.UserContract;
import ru.practicum.interaction.user.UserResponse;
import ru.practicum.interaction.event.CategoryContract;
import ru.practicum.interaction.event.CategoryResponse;
import ru.practicum.interaction.request.RequestContract;
import ru.practicum.interaction.request.ConfirmedRequestCountResponse;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventEnricher {
    private final UserContract userContract;
    private final CategoryContract categoryContract;
    private final RequestContract requestContract;

    public void enrich(Event event) {
        UserResponse user = userContract.getById(event.getInitiatorId());
        event.setInitiatorName(user.name());
        CategoryResponse category = categoryContract.getById(event.getCategoryId());
        event.setCategoryName(category.name());
        event.setConfirmedRequests((int) requestContract.getConfirmedCount(event.getId()).confirmedCount());
    }

    public void enrich(Collection<Event> events) {
        if (events.isEmpty()) return;
        Map<Long, UserResponse> users = userContract.getByIds(new IdsRequest(events.stream()
                        .map(Event::getInitiatorId).collect(Collectors.toSet())))
                .users().stream().collect(Collectors.toMap(UserResponse::id, Function.identity()));
        Map<Long, CategoryResponse> categories = categoryContract.getByIds(new IdsRequest(events.stream()
                        .map(Event::getCategoryId).collect(Collectors.toSet())))
                .categories().stream().collect(Collectors.toMap(CategoryResponse::id, Function.identity()));
        Map<Long, Long> confirmed = requestContract.getConfirmedCounts(new IdsRequest(events.stream()
                        .map(Event::getId).collect(Collectors.toSet())))
                .counts().stream().collect(Collectors.toMap(ConfirmedRequestCountResponse::eventId,
                        ConfirmedRequestCountResponse::confirmedCount));
        events.forEach(event -> {
            UserResponse user = users.get(event.getInitiatorId());
            if (user != null) event.setInitiatorName(user.name());
            CategoryResponse category = categories.get(event.getCategoryId());
            if (category != null) event.setCategoryName(category.name());
            event.setConfirmedRequests(confirmed.getOrDefault(event.getId(), 0L).intValue());
        });
    }
}
