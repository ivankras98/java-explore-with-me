package ru.practicum.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class StatsClient {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestClient restClient;

    public StatsClient(@Value("${stats-server.url}") String serverUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(serverUrl)
                .build();
    }

    public void hit(EndpointHitDto dto) {
        restClient.post()
                .uri("/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dto)
                .retrieve()
                .toBodilessEntity();
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                       List<String> uris, boolean unique) {
        return restClient.get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/stats")
                            .queryParam("start", start.format(FORMATTER))
                            .queryParam("end", end.format(FORMATTER))
                            .queryParam("unique", unique);
                    if (uris != null && !uris.isEmpty()) {
                        uriBuilder.queryParam("uris", uris);
                    }
                    return uriBuilder.build();
                })
                .retrieve()
                .body(new ParameterizedTypeReference<List<ViewStatsDto>>() {});
    }
}