package ru.practicum.main.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventViewsService {

    private static final String APP = "ewm-main-service";
    private static final String EVENTS_URI = "/events/";

    private final StatsClient statsClient;

    public void saveHit(String uri, String ip) {
        try {
            statsClient.hit(EndpointHitDto.builder()
                    .app(APP)
                    .uri(uri)
                    .ip(ip)
                    .timestamp(LocalDateTime.now())
                    .build());
        } catch (RuntimeException e) {
            log.warn("Could not save hit to the stats service: {}", e.getMessage());
        }
    }

    public Map<Long, Long> getViews(Collection<Event> events) {
        List<Event> published = events.stream()
                .filter(e -> e.getState() == EventState.PUBLISHED && e.getPublishedOn() != null)
                .toList();
        if (published.isEmpty()) {
            return Map.of();
        }
        LocalDateTime start = published.stream()
                .map(Event::getPublishedOn)
                .min(Comparator.naturalOrder())
                .orElseThrow();
        List<String> uris = published.stream()
                .map(e -> EVENTS_URI + e.getId())
                .toList();
        try {
            List<ViewStatsDto> stats = statsClient.getStats(start, LocalDateTime.now(), uris, true);
            Map<Long, Long> views = new HashMap<>();
            if (stats == null) {
                return views;
            }
            for (ViewStatsDto stat : stats) {
                if (!APP.equals(stat.getApp()) || !stat.getUri().startsWith(EVENTS_URI)) {
                    continue;
                }
                try {
                    views.put(Long.parseLong(stat.getUri().substring(EVENTS_URI.length())), stat.getHits());
                } catch (NumberFormatException e) {
                    log.warn("Unexpected uri in stats: {}", stat.getUri());
                }
            }
            return views;
        } catch (RuntimeException e) {
            log.warn("Could not get views from the stats service: {}", e.getMessage());
            return Map.of();
        }
    }
}