package ru.practicum.main.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.event.Event;
import ru.practicum.main.event.EventRepository;
import ru.practicum.main.event.EventState;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.main.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.main.request.dto.ParticipationRequestDto;
import ru.practicum.main.user.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestService {

    private final RequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Transactional
    public ParticipationRequestDto add(long userId, long eventId) {
        checkUserExists(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Request already exists");
        }
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot add a request to participate in own event");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot participate in an unpublished event");
        }
        int limit = event.getParticipantLimit();
        if (limit > 0 && requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED) >= limit) {
            throw new ConflictException("The participant limit has been reached");
        }

        boolean autoConfirm = limit == 0 || !event.isRequestModeration();
        Request request = Request.builder()
                .created(LocalDateTime.now())
                .eventId(eventId)
                .requesterId(userId)
                .status(autoConfirm ? RequestStatus.CONFIRMED : RequestStatus.PENDING)
                .build();
        return RequestMapper.toDto(requestRepository.save(request));
    }

    public List<ParticipationRequestDto> getUserRequests(long userId) {
        checkUserExists(userId);
        return requestRepository.findAllByRequesterIdOrderByIdAsc(userId).stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Transactional
    public ParticipationRequestDto cancel(long userId, long requestId) {
        Request request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));
        request.setStatus(RequestStatus.CANCELED);
        return RequestMapper.toDto(request);
    }

    public List<ParticipationRequestDto> getEventRequests(long userId, long eventId) {
        findInitiatorEvent(userId, eventId);
        return requestRepository.findAllByEventIdOrderByIdAsc(eventId).stream()
                .map(RequestMapper::toDto)
                .toList();
    }

    @Transactional
    public EventRequestStatusUpdateResult changeStatus(long userId, long eventId,
                                                       EventRequestStatusUpdateRequest update) {
        Event event = findInitiatorEvent(userId, eventId);
        EventRequestStatusUpdateResult result =
                new EventRequestStatusUpdateResult(new ArrayList<>(), new ArrayList<>());

        int limit = event.getParticipantLimit();
        if (limit == 0 || !event.isRequestModeration()) {
            return result;
        }

        List<Request> requests = requestRepository.findAllByEventIdAndIdInOrderByIdAsc(
                eventId, update.getRequestIds());
        if (requests.size() != new HashSet<>(update.getRequestIds()).size()) {
            throw new NotFoundException("Some requests were not found for event with id=" + eventId);
        }
        if (requests.stream().anyMatch(r -> r.getStatus() != RequestStatus.PENDING)) {
            throw new ConflictException("Request must have status PENDING");
        }

        if (update.getStatus() == RequestUpdateStatus.REJECTED) {
            for (Request r : requests) {
                r.setStatus(RequestStatus.REJECTED);
                result.getRejectedRequests().add(RequestMapper.toDto(r));
            }
            return result;
        }

        long confirmed = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        if (confirmed >= limit) {
            throw new ConflictException("The participant limit has been reached");
        }
        for (Request r : requests) {
            if (confirmed < limit) {
                r.setStatus(RequestStatus.CONFIRMED);
                confirmed++;
                result.getConfirmedRequests().add(RequestMapper.toDto(r));
            } else {
                r.setStatus(RequestStatus.REJECTED);
                result.getRejectedRequests().add(RequestMapper.toDto(r));
            }
        }
        if (confirmed >= limit) {
            List<Request> rest = requestRepository.findAllByEventIdAndStatusOrderByIdAsc(
                    eventId, RequestStatus.PENDING);
            for (Request r : rest) {
                if (r.getStatus() == RequestStatus.PENDING) {
                    r.setStatus(RequestStatus.REJECTED);
                    result.getRejectedRequests().add(RequestMapper.toDto(r));
                }
            }
        }
        return result;
    }

    private Event findInitiatorEvent(long userId, long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private void checkUserExists(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }
}