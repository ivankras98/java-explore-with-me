package ru.practicum.main.user;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.user.dto.NewUserRequest;
import ru.practicum.main.user.dto.UserDto;
import ru.practicum.main.util.OffsetPageRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserDto register(NewUserRequest request) {
        User saved = userRepository.save(UserMapper.toEntity(request));
        return UserMapper.toDto(saved);
    }

    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        Pageable page = new OffsetPageRequest(from, size, Sort.by("id").ascending());
        List<User> users;
        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(page).getContent();
        } else {
            users = userRepository.findAllByIdIn(ids, page);
        }
        return users.stream()
                .map(UserMapper::toDto)
                .toList();
    }

    @Transactional
    public void delete(long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        userRepository.deleteById(userId);
        userRepository.flush();
    }
}