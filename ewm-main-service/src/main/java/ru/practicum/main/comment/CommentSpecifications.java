package ru.practicum.main.comment;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class CommentSpecifications {

    private CommentSpecifications() {
    }

    public static Specification<Comment> adminFilter(List<Long> users, List<Long> events) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (users != null && !users.isEmpty()) {
                predicates.add(root.get("author").get("id").in(users));
            }
            if (events != null && !events.isEmpty()) {
                predicates.add(root.get("eventId").in(events));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}