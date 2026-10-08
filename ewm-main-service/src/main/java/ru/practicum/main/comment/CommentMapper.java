package ru.practicum.main.comment;

import ru.practicum.main.comment.dto.CommentDto;
import ru.practicum.main.user.UserMapper;

public final class CommentMapper {

    private CommentMapper() {
    }

    public static CommentDto toDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .eventId(comment.getEventId())
                .author(UserMapper.toShortDto(comment.getAuthor()))
                .created(comment.getCreated())
                .edited(comment.getEdited())
                .build();
    }
}