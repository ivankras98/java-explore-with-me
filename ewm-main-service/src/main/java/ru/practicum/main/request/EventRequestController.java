package ru.practicum.main.request;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.main.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.main.request.dto.ParticipationRequestDto;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
public class EventRequestController {

    private final RequestService requestService;

    @GetMapping
    public List<ParticipationRequestDto> getEventRequests(@PathVariable long userId,
                                                          @PathVariable long eventId) {
        return requestService.getEventRequests(userId, eventId);
    }

    @PatchMapping
    public EventRequestStatusUpdateResult changeStatus(@PathVariable long userId,
                                                       @PathVariable long eventId,
                                                       @Valid @RequestBody EventRequestStatusUpdateRequest update) {
        return requestService.changeStatus(userId, eventId, update);
    }
}