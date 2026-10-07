package ru.practicum.main.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findAllByRequesterIdOrderByIdAsc(Long requesterId);

    List<Request> findAllByEventIdOrderByIdAsc(Long eventId);

    List<Request> findAllByEventIdAndStatusOrderByIdAsc(Long eventId, RequestStatus status);

    List<Request> findAllByEventIdAndIdInOrderByIdAsc(Long eventId, Collection<Long> ids);

    Optional<Request> findByIdAndRequesterId(Long id, Long requesterId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("select r.eventId as eventId, count(r) as cnt "
            + "from Request r "
            + "where r.eventId in :ids and r.status = :status "
            + "group by r.eventId")
    List<EventRequestCount> countGroupedByEvent(@Param("ids") Collection<Long> ids,
                                                @Param("status") RequestStatus status);
}