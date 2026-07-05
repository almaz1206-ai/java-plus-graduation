package ru.practicum.ewm.request.service;

import ru.practicum.ewm.request.dto.RequestDto;

import java.util.List;

public interface RequestService {
    RequestDto addUserRequest(long userId, long eventId);

    RequestDto cancelRequest(long requesterId, long requestId);

    List<RequestDto> getUserRequests(long requesterId);
}


