package ru.practicum.main.event.dto;

import lombok.Getter;
import lombok.Setter;
import ru.practicum.main.event.StateActionAdmin;

@Getter
@Setter
public class UpdateEventAdminRequest extends UpdateEventRequest {

    private StateActionAdmin stateAction;
}