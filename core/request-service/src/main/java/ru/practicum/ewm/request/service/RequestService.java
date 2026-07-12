package ru.practicum.ewm.request.service;

import ru.practicum.ewm.request.dto.RequestDto;
import ru.practicum.ewm.events.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.events.dto.EventRequestStatusUpdateResult;

import java.util.List;

public interface RequestService {
    RequestDto addUserRequest(long userId, long eventId);

    RequestDto cancelRequest(long requesterId, long requestId);

    List<RequestDto> getUserRequests(long requesterId);

    List<RequestDto> getEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResult changeRequestStatus(
            Long userId, Long eventId, EventRequestStatusUpdateRequest request);
}


