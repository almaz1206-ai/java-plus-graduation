package ru.practicum.ewm.events.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.categories.dto.CategoryDto;
import ru.practicum.ewm.error.BadRequestException;
import ru.practicum.ewm.events.dto.Location;
import ru.practicum.ewm.events.model.EventState;
import ru.practicum.ewm.events.dto.EventFullDto;
import ru.practicum.ewm.events.dto.EventShortDto;
import ru.practicum.ewm.events.dto.UpdateEventUserRequest;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.ewm.user.dto.UserShortDto;

@UtilityClass
public class EventMapper {

    public static EventShortDto toEventShortDto(Event event) {
        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setAnnotation(event.getAnnotation());
        if (event.getCategory() != null) {
            CategoryDto cat = new CategoryDto();
            cat.setId(event.getCategory().getId());
            cat.setName(event.getCategory().getName());
            dto.setCategory(cat);
        }
        dto.setPaid(event.getPaid());
        dto.setEventDate(event.getEventDate());
        dto.setConfirmedRequests(event.getConfirmedRequests());
        dto.setViews(event.getViews());
        if (event.getInitiator() != null) {
            UserShortDto initiator = new UserShortDto();
            initiator.setId(event.getInitiator().getId());
            initiator.setName(event.getInitiator().getName());
            dto.setInitiator(initiator);
        }
        return dto;
    }

    public static EventFullDto toEventFullDto(Event event) {
        EventFullDto dto = new EventFullDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setAnnotation(event.getAnnotation());
        dto.setDescription(event.getDescription());
        if (event.getCategory() != null) {
            CategoryDto cat = new CategoryDto();
            cat.setId(event.getCategory().getId());
            cat.setName(event.getCategory().getName());
            dto.setCategory(cat);
        }
        dto.setPaid(event.getPaid());
        dto.setParticipantLimit(event.getParticipantLimit());
        dto.setRequestModeration(event.getRequestModeration());
        dto.setCreatedOn(event.getCreatedOn());
        dto.setEventDate(event.getEventDate());
        dto.setPublishedOn(event.getPublishedOn());
        dto.setState(event.getState());
        dto.setConfirmedRequests(event.getConfirmedRequests());
        dto.setViews(event.getViews());
        if (event.getInitiator() != null) {
            UserShortDto initiator = new UserShortDto();
            initiator.setId(event.getInitiator().getId());
            initiator.setName(event.getInitiator().getName());
            dto.setInitiator(initiator);
        }
        if (event.getLocation() != null) {
            Location loc = new Location();
            loc.setLat(event.getLocation().getLat());
            loc.setLon(event.getLocation().getLon());
            dto.setLocation(loc);
        }
        return dto;
    }

    public static void updateEventFromDto(UpdateEventUserRequest request, Event event) {
        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getAnnotation() != null) event.setAnnotation(request.getAnnotation());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getPaid() != null) event.setPaid(request.getPaid());
        if (request.getEventDate() != null) event.setEventDate(request.getEventDate());
        if (request.getParticipantLimit() != null) event.setParticipantLimit(request.getParticipantLimit());
        if (request.getRequestModeration() != null) event.setRequestModeration(request.getRequestModeration());
        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
                case PUBLISH_EVENT, REJECT_EVENT -> throw new BadRequestException(
                        "State action '" + request.getStateAction() + "' is not allowed for user requests");
            }
        }
    }
}
