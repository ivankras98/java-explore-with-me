package ru.practicum.server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.server.model.EndpointHit;
import ru.practicum.server.repository.StatsRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final StatsRepository repository;

    @Transactional
    public void saveHit(EndpointHitDto dto) {
        EndpointHit hit = EndpointHit.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
        repository.save(hit);
    }

    @Transactional(readOnly = true)
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, boolean unique) {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException("Start must not be after end");
        }
        boolean hasUris = uris != null && !uris.isEmpty();

        if (unique) {
            return hasUris
                    ? repository.getUniqueStatsByUris(start, end, uris)
                    : repository.getUniqueStats(start, end);
        }
        return hasUris
                ? repository.getStatsByUris(start, end, uris)
                : repository.getStats(start, end);
    }
}