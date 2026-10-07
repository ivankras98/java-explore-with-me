package ru.practicum.main.comment;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.comment.dto.CommentDto;
import ru.practicum.main.comment.dto.NewCommentDto;
import ru.practicum.main.event.EventRepository;
import ru.practicum.main.event.EventState;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.user.User;
import ru.practicum.main.user.UserRepository;
import ru.practicum.main.util.OffsetPageRequest;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private static final Sort NEWEST_FIRST = Sort.by("created").descending().and(Sort.by("id").descending());

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Transactional
    public CommentDto add(long userId, long eventId, NewCommentDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User with id=" + userId + " was not found"));
        if (!eventRepository.existsById(eventId)) {
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
        if (!eventRepository.existsByIdAndState(eventId, EventState.PUBLISHED)) {
            throw new ConflictException("Cannot comment on an unpublished event");
        }
        Comment comment = Comment.builder()
                .text(dto.getText())
                .eventId(eventId)
                .author(user)
                .created(LocalDateTime.now())
                .build();
        return CommentMapper.toDto(commentRepository.save(comment));
    }

    @Transactional
    public CommentDto update(long userId, long commentId, NewCommentDto dto) {
        checkUserExists(userId);
        Comment comment = findAuthorComment(userId, commentId);
        comment.setText(dto.getText());
        comment.setEdited(LocalDateTime.now());
        return CommentMapper.toDto(comment);
    }

    @Transactional
    public void delete(long userId, long commentId) {
        checkUserExists(userId);
        commentRepository.delete(findAuthorComment(userId, commentId));
    }

    public List<CommentDto> getUserComments(long userId, int from, int size) {
        checkUserExists(userId);
        Pageable page = new OffsetPageRequest(from, size, NEWEST_FIRST);
        return commentRepository.findAllByAuthorId(userId, page).stream()
                .map(CommentMapper::toDto)
                .toList();
    }

    private Comment findAuthorComment(long userId, long commentId) {
        return commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Comment with id=" + commentId + " was not found"));
    }

    private void checkUserExists(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }
}