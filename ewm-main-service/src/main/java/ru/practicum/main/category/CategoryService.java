package ru.practicum.main.category;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.category.dto.CategoryDto;
import ru.practicum.main.category.dto.NewCategoryDto;
import ru.practicum.main.event.EventRepository;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.util.OffsetPageRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    @Transactional
    public CategoryDto add(NewCategoryDto dto) {
        Category saved = categoryRepository.save(CategoryMapper.toEntity(dto));
        return CategoryMapper.toDto(saved);
    }

    @Transactional
    public CategoryDto update(long catId, CategoryDto dto) {
        Category category = getEntity(catId);
        category.setName(dto.getName());
        return CategoryMapper.toDto(categoryRepository.saveAndFlush(category));
    }

    @Transactional
    public void delete(long catId) {
        Category category = getEntity(catId);
        if (eventRepository.existsByCategoryId(catId)) {
            throw new ConflictException("The category is not empty");
        }
        categoryRepository.delete(category);
        categoryRepository.flush();
    }

    public List<CategoryDto> getAll(int from, int size) {
        Pageable page = new OffsetPageRequest(from, size, Sort.by("id").ascending());
        return categoryRepository.findAll(page).getContent().stream()
                .map(CategoryMapper::toDto)
                .toList();
    }

    public CategoryDto get(long catId) {
        return CategoryMapper.toDto(getEntity(catId));
    }

    public Category getEntity(long catId) {
        return categoryRepository.findById(catId)
                .orElseThrow(() -> new NotFoundException("Category with id=" + catId + " was not found"));
    }
}