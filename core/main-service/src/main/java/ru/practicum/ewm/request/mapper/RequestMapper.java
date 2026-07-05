package ru.practicum.ewm.request.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.request.dto.RequestDto;
import ru.practicum.ewm.request.model.Request;

@UtilityClass
public class RequestMapper {

    public static RequestDto toRequestDto(Request request) {
        if (request == null) {
            return null;
        }
        return RequestDto.builder()
                .requester(request.getRequester().getId())
                .event(request.getEvent().getId())
                .id(request.getId())
                .status(request.getStatus())
                .created(request.getCreated())
                .build();
    }
}


