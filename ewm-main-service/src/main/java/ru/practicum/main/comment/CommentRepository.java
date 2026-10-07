package ru.practicum.main.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {

    @EntityGraph(attributePaths = {"author"})
    Optional<Comment> findByIdAndAuthorId(Long id, Long authorId);

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findAllByAuthorId(Long authorId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    List<Comment> findAllByEventId(Long eventId, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    Page<Comment> findAll(Specification<Comment> spec, Pageable pageable);
}