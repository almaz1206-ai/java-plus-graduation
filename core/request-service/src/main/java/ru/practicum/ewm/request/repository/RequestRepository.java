package ru.practicum.ewm.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.request.model.Request;
import ru.practicum.ewm.request.model.StatusRequest;

import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findAllByRequesterId(long requesterId);

    List<Request> findAllByEventId(long eventId);

    List<Request> findAllByIdInAndEventId(List<Long> ids, Long eventId);

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    boolean existsByRequesterIdAndEventIdAndStatus(Long requesterId, Long eventId, StatusRequest status);

    long countByEventIdAndStatus(long eventId, StatusRequest status);

    @Query("select r.eventId, count(r) from Request r " +
            "where r.eventId in :eventIds and r.status = :status group by r.eventId")
    List<Object[]> countByEventIdsAndStatus(List<Long> eventIds, StatusRequest status);
}

