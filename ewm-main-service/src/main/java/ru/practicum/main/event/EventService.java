package ru.practicum.main.event;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.category.Category;
import ru.practicum.main.category.CategoryService;
import ru.practicum.main.event.dto.EventFullDto;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.event.dto.NewEventDto;
import ru.practicum.main.event.dto.UpdateEventAdminRequest;
import ru.practicum.main.event.dto.UpdateEventRequest;
import ru.practicum.main.event.dto.UpdateEventUserRequest;
import ru.practicum.main.exception.BadRequestException;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.EventRequestCount;
import ru.practicum.main.request.RequestRepository;
import ru.practicum.main.request.RequestStatus;
import ru.practicum.main.user.User;
import ru.practicum.main.user.UserRepository;
import ru.practicum.main.util.OffsetPageRequest;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private static final int USER_MIN_HOURS = 2;
    private static final int ADMIN_MIN_HOURS = 1;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;
    private final CategoryService categoryService;
    private final EventViewsService viewsService;

    @Transactional
    public EventFullDto add(long userId, NewEventDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        Category category = categoryService.getEntity(dto.getCategory());
        checkEventDate(dto.getEventDate(), USER_MIN_HOURS);
        Event saved = eventRepository.save(EventMapper.toEntity(dto, category, user, LocalDateTime.now()));
        return toFullDto(saved);
    }

    public List<EventShortDto> getUserEvents(long userId, int from, int size) {
        checkUserExists(userId);
        Pageable page = new OffsetPageRequest(from, size, Sort.by("id").ascending());
        List<Event> events = eventRepository.findAllByInitiatorId(userId, page);
        return toShortDtos(events);
    }

    public EventFullDto getUserEvent(long userId, long eventId) {
        return toFullDto(findUserEvent(userId, eventId));
    }

    @Transactional
    public EventFullDto updateByUser(long userId, long eventId, UpdateEventUserRequest request) {
        Event event = findUserEvent(userId, eventId);
        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }
        if (request.getEventDate() != null) {
            checkEventDate(request.getEventDate(), USER_MIN_HOURS);
        }
        applyUpdate(event, request);
        if (request.getStateAction() == StateActionUser.SEND_TO_REVIEW) {
            event.setState(EventState.PENDING);
        } else if (request.getStateAction() == StateActionUser.CANCEL_REVIEW) {
            event.setState(EventState.CANCELED);
        }
        return toFullDto(event);
    }

    public List<EventFullDto> searchAdmin(List<Long> users, List<EventState> states, List<Long> categories,
                                          LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                          int from, int size) {
        if (rangeStart != null && rangeEnd != null && rangeStart.isAfter(rangeEnd)) {
            throw new BadRequestException("rangeStart must not be after rangeEnd");
        }
        Pageable page = new OffsetPageRequest(from, size, Sort.by("id").ascending());
        Specification<Event> spec = EventSpecifications.adminFilter(users, states, categories, rangeStart, rangeEnd);
        List<Event> events = eventRepository.findAll(spec, page).getContent();
        return toFullDtos(events);
    }

    @Transactional
    public EventFullDto updateByAdmin(long eventId, UpdateEventAdminRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        applyUpdate(event, request);
        StateActionAdmin action = request.getStateAction();
        if (action == StateActionAdmin.PUBLISH_EVENT) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException(
                        "Cannot publish the event because it's not in the right state: " + event.getState());
            }
            checkEventDate(event.getEventDate(), ADMIN_MIN_HOURS);
            event.setState(EventState.PUBLISHED);
            event.setPublishedOn(LocalDateTime.now());
        } else if (action == StateActionAdmin.REJECT_EVENT) {
            if (event.getState() == EventState.PUBLISHED) {
                throw new ConflictException("Cannot reject the event because it's already published");
            }
            event.setState(EventState.CANCELED);
        }
        return toFullDto(event);
    }

    public List<EventShortDto> searchPublic(String text, List<Long> categories, Boolean paid,
                                            LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                            boolean onlyAvailable, EventSort sort, int from, int size) {
        LocalDateTime start = rangeStart != null ? rangeStart : LocalDateTime.now();
        if (rangeEnd != null && start.isAfter(rangeEnd)) {
            throw new BadRequestException("rangeStart must not be after rangeEnd");
        }
        Specification<Event> spec = EventSpecifications.publicFilter(
                text, categories, paid, start, rangeEnd, onlyAvailable);

        if (sort == EventSort.VIEWS) {
            return searchSortedByViews(spec, from, size);
        }
        Sort order = sort == EventSort.EVENT_DATE
                ? Sort.by("eventDate").ascending().and(Sort.by("id").ascending())
                : Sort.by("id").ascending();
        Pageable page = new OffsetPageRequest(from, size, order);
        return toShortDtos(eventRepository.findAll(spec, page).getContent());
    }

    public EventFullDto getPublicEvent(long eventId) {
        Event event = eventRepository.findById(eventId)
                .filter(e -> e.getState() == EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        return toFullDto(event);
    }

    private List<EventShortDto> searchSortedByViews(Specification<Event> spec, int from, int size) {
        List<Event> events = eventRepository.findAll(spec, Sort.by("id").ascending());
        Map<Long, Long> views = viewsService.getViews(events);
        List<Event> pageEvents = events.stream()
                .sorted(Comparator.comparingLong((Event e) -> views.getOrDefault(e.getId(), 0L)).reversed())
                .skip(from)
                .limit(size)
                .toList();
        Map<Long, Long> confirmed = getConfirmedRequests(pageEvents);
        return pageEvents.stream()
                .map(e -> EventMapper.toShortDto(e,
                        confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    private void applyUpdate(Event event, UpdateEventRequest request) {
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getCategory() != null) {
            event.setCategory(categoryService.getEntity(request.getCategory()));
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getEventDate() != null) {
            event.setEventDate(request.getEventDate());
        }
        if (request.getLocation() != null) {
            event.setLat(request.getLocation().getLat());
            event.setLon(request.getLocation().getLon());
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
    }

    private Event findUserEvent(long userId, long eventId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private void checkUserExists(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private void checkEventDate(LocalDateTime eventDate, int minHours) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(minHours))) {
            throw new ConflictException("Field: eventDate. Error: must be at least " + minHours
                    + " hour(s) from now. Value: " + eventDate);
        }
    }

    private EventFullDto toFullDto(Event event) {
        return toFullDtos(List.of(event)).get(0);
    }

    private List<EventFullDto> toFullDtos(List<Event> events) {
        Map<Long, Long> confirmed = getConfirmedRequests(events);
        Map<Long, Long> views = viewsService.getViews(events);
        return events.stream()
                .map(e -> EventMapper.toFullDto(e,
                        confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    private List<EventShortDto> toShortDtos(List<Event> events) {
        Map<Long, Long> confirmed = getConfirmedRequests(events);
        Map<Long, Long> views = viewsService.getViews(events);
        return events.stream()
                .map(e -> EventMapper.toShortDto(e,
                        confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    private Map<Long, Long> getConfirmedRequests(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = events.stream().map(Event::getId).toList();
        return requestRepository.countGroupedByEvent(ids, RequestStatus.CONFIRMED).stream()
                .collect(Collectors.toMap(EventRequestCount::getEventId, EventRequestCount::getCnt));
    }
}