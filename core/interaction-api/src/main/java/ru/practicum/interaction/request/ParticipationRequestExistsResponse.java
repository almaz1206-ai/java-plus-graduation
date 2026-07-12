package ru.practicum.interaction.request;

public record ParticipationRequestExistsResponse(Long userId, Long eventId, boolean exists) {
}
