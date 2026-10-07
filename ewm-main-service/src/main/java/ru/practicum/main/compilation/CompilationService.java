package ru.practicum.main.compilation;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.compilation.dto.CompilationDto;
import ru.practicum.main.compilation.dto.NewCompilationDto;
import ru.practicum.main.compilation.dto.UpdateCompilationRequest;
import ru.practicum.main.event.Event;
import ru.practicum.main.event.EventMapper;
import ru.practicum.main.event.EventRepository;
import ru.practicum.main.event.EventViewsService;
import ru.practicum.main.event.dto.EventShortDto;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.request.EventRequestCount;
import ru.practicum.main.request.RequestRepository;
import ru.practicum.main.request.RequestStatus;
import ru.practicum.main.util.OffsetPageRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final RequestRepository requestRepository;
    private final EventViewsService viewsService;

    @Transactional
    public CompilationDto add(NewCompilationDto dto) {
        Set<Long> eventIds = dto.getEvents() == null ? new HashSet<>() : new HashSet<>(dto.getEvents());
        checkEventsExist(eventIds);
        Compilation compilation = Compilation.builder()
                .pinned(dto.isPinned())
                .title(dto.getTitle())
                .eventIds(eventIds)
                .build();
        Compilation saved = compilationRepository.save(compilation);
        return toDtos(List.of(saved)).get(0);
    }

    @Transactional
    public CompilationDto update(long compId, UpdateCompilationRequest request) {
        Compilation compilation = getEntity(compId);
        if (request.getEvents() != null) {
            Set<Long> eventIds = new HashSet<>(request.getEvents());
            checkEventsExist(eventIds);
            compilation.getEventIds().clear();
            compilation.getEventIds().addAll(eventIds);
        }
        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }
        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }
        compilationRepository.flush();
        return toDtos(List.of(compilation)).get(0);
    }

    @Transactional
    public void delete(long compId) {
        Compilation compilation = getEntity(compId);
        compilationRepository.delete(compilation);
        compilationRepository.flush();
    }

    public List<CompilationDto> getAll(Boolean pinned, int from, int size) {
        Pageable page = new OffsetPageRequest(from, size, Sort.by("id").ascending());
        List<Compilation> compilations = pinned == null
                ? compilationRepository.findAll(page).getContent()
                : compilationRepository.findAllByPinned(pinned, page);
        return toDtos(compilations);
    }

    public CompilationDto get(long compId) {
        return toDtos(List.of(getEntity(compId))).get(0);
    }

    private Compilation getEntity(long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
    }

    private void checkEventsExist(Set<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return;
        }
        Set<Long> found = eventRepository.findAllById(eventIds).stream()
                .map(Event::getId)
                .collect(Collectors.toSet());
        eventIds.stream()
                .sorted()
                .filter(id -> !found.contains(id))
                .findFirst()
                .ifPresent(id -> {
                    throw new NotFoundException("Event with id=" + id + " was not found");
                });
    }

    private List<CompilationDto> toDtos(List<Compilation> compilations) {
        Set<Long> allEventIds = compilations.stream()
                .flatMap(c -> c.getEventIds().stream())
                .collect(Collectors.toSet());
        Map<Long, EventShortDto> events = loadShortEvents(allEventIds);
        return compilations.stream()
                .map(c -> CompilationMapper.toDto(c, c.getEventIds().stream()
                        .sorted()
                        .map(events::get)
                        .filter(Objects::nonNull)
                        .toList()))
                .toList();
    }

    private Map<Long, EventShortDto> loadShortEvents(Set<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        List<Event> events = eventRepository.findAllByIdIn(eventIds);
        Map<Long, Long> confirmed = requestRepository.countGroupedByEvent(eventIds, RequestStatus.CONFIRMED)
                .stream()
                .collect(Collectors.toMap(EventRequestCount::getEventId, EventRequestCount::getCnt));
        Map<Long, Long> views = viewsService.getViews(events);
        return events.stream()
                .collect(Collectors.toMap(Event::getId, e -> EventMapper.toShortDto(e,
                        confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)), (a, b) -> a));
    }
}