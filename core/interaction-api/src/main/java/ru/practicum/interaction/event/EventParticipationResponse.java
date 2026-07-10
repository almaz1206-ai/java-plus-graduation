package ru.practicum.interaction.event;

public record EventParticipationResponse(Long eventId, Long initiatorId, EventState state,
                                         Integer participantLimit, Boolean requestModeration) {
}
