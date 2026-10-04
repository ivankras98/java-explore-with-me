package ru.practicum.main.event.dto;

import lombok.Getter;
import lombok.Setter;
import ru.practicum.main.event.StateActionUser;

@Getter
@Setter
public class UpdateEventUserRequest extends UpdateEventRequest {

    private StateActionUser stateAction;
}