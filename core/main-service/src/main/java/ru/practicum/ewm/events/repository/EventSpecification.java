package ru.practicum.ewm.events.repository;

import org.springframework.data.jpa.domain.Specification;
import ru.practicum.ewm.events.model.Event;
import ru.practicum.ewm.events.model.EventState;

import java.time.LocalDateTime;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> hasState(EventState state) {
        return (root, query, cb) -> state == null ? null : cb.equal(root.get("state"), state);
    }

    public static Specification<Event> hasStates(List<EventState> states) {
        return (root, query, cb) -> (states == null || states.isEmpty()) ? null : root.get("state").in(states);
    }

    public static Specification<Event> hasUsers(List<Long> users) {
        return (root, query, cb) -> (users == null || users.isEmpty()) ? null : root.get("initiator").get("id").in(users);
    }

    public static Specification<Event> hasCategories(List<Long> categories) {
        return (root, query, cb) -> (categories == null || categories.isEmpty()) ? null : root.get("category").get("id").in(categories);
    }

    public static Specification<Event> hasPaid(Boolean paid) {
        return (root, query, cb) -> paid == null ? null : cb.equal(root.get("paid"), paid);
    }

    public static Specification<Event> eventDateAfter(LocalDateTime start) {
        return (root, query, cb) -> start == null ? null : cb.greaterThanOrEqualTo(root.get("eventDate"), start);
    }

    public static Specification<Event> eventDateBefore(LocalDateTime end) {
        return (root, query, cb) -> end == null ? null : cb.lessThanOrEqualTo(root.get("eventDate"), end);
    }

    public static Specification<Event> hasText(String text) {
        return (root, query, cb) -> {
            if (text == null || text.isBlank()) return null;
            String pattern = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("annotation")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    public static Specification<Event> isAvailable(Boolean onlyAvailable) {
        return (root, query, cb) -> {
            if (onlyAvailable == null || !onlyAvailable) return null;
            return cb.or(
                    cb.equal(root.get("participantLimit"), 0),
                    cb.lessThan(root.get("confirmedRequests"), root.get("participantLimit"))
            );
        };
    }
}
