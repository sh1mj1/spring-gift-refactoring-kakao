package gift.category;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    public Category getById(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("카테고리가 존재하지 않습니다. id=" + id));
    }

    public Category create(CategoryRequest request) {
        return categoryRepository.save(request.toEntity());
    }

    public Category update(Long id, CategoryRequest request) {
        Category category = findById(id);
        if (category == null) {
            return null;
        }

        category.update(request.name(), request.color(), request.imageUrl(), request.description());
        categoryRepository.save(category);
        return category;
    }

    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }
}
