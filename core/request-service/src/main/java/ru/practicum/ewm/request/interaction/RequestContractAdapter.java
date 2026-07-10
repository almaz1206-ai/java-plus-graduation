package ru.practicum.ewm.request.interaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.request.model.StatusRequest;
import ru.practicum.ewm.request.repository.RequestRepository;
import ru.practicum.interaction.common.IdsRequest;
import ru.practicum.interaction.request.ConfirmedRequestCountResponse;
import ru.practicum.interaction.request.ConfirmedRequestCountsResponse;
import ru.practicum.interaction.request.ParticipationRequestExistsResponse;
import ru.practicum.interaction.request.RequestContract;
import ru.practicum.interaction.request.RequestStatus;
import ru.practicum.interaction.request.RequestStatusResponse;
import ru.practicum.interaction.request.RequestStatusesResponse;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestContractAdapter implements RequestContract {
    private final RequestRepository repository;

    @Override
    public ConfirmedRequestCountResponse getConfirmedCount(Long eventId) {
        return new ConfirmedRequestCountResponse(eventId,
                repository.countByEventIdAndStatus(eventId, StatusRequest.CONFIRMED));
    }

    @Override
    public ConfirmedRequestCountsResponse getConfirmedCounts(IdsRequest eventIds) {
        var counts = repository.countByEventIdsAndStatus(eventIds.ids().stream().toList(), StatusRequest.CONFIRMED)
                .stream().collect(java.util.stream.Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        return new ConfirmedRequestCountsResponse(eventIds.ids().stream()
                .map(id -> new ConfirmedRequestCountResponse(id, counts.getOrDefault(id, 0L))).toList());
    }

    @Override
    public ParticipationRequestExistsResponse exists(Long userId, Long eventId) {
        return new ParticipationRequestExistsResponse(userId, eventId,
                repository.existsByRequesterIdAndEventId(userId, eventId));
    }

    @Override
    public RequestStatusesResponse getStatuses(IdsRequest requestIds) {
        return new RequestStatusesResponse(repository.findAllById(requestIds.ids()).stream()
                .map(request -> new RequestStatusResponse(request.getId(),
                        RequestStatus.valueOf(request.getStatus().name())))
                .toList());
    }
}
