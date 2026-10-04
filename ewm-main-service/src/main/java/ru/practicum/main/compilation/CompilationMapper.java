package ru.practicum.main.compilation;

import ru.practicum.main.compilation.dto.CompilationDto;
import ru.practicum.main.event.dto.EventShortDto;

import java.util.List;

public final class CompilationMapper {

    private CompilationMapper() {
    }

    public static CompilationDto toDto(Compilation compilation, List<EventShortDto> events) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .pinned(compilation.isPinned())
                .title(compilation.getTitle())
                .events(events)
                .build();
    }
}