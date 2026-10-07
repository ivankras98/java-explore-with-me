package ru.practicum.main.request;

import ru.practicum.main.request.dto.ParticipationRequestDto;

public final class RequestMapper {

    private RequestMapper() {
    }

    public static ParticipationRequestDto toDto(Request request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .event(request.getEventId())
                .requester(request.getRequesterId())
                .status(request.getStatus())
                .build();
    }
}