package gift.product;

import gift.category.Category;
import gift.category.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryService categoryService;

    public ProductService(ProductRepository productRepository, CategoryService categoryService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
    }

    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Product findById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    public Product getById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));
    }

    public Product create(ProductRequest request) {
        validateName(request.name());

        Category category = categoryService.findById(request.categoryId());
        if (category == null) {
            return null;
        }

        return productRepository.save(request.toEntity(category));
    }

    public Product update(Long id, ProductRequest request) {
        validateName(request.name());

        Category category = categoryService.findById(request.categoryId());
        if (category == null) {
            return null;
        }

        Product product = findById(id);
        if (product == null) {
            return null;
        }

        return updateAndSave(product, request.name(), request.price(), request.imageUrl(), category);
    }

    public void createForAdmin(String name, int price, String imageUrl, Long categoryId) {
        Category category = categoryService.getById(categoryId);
        productRepository.save(new Product(name, price, imageUrl, category));
    }

    public void updateForAdmin(Product product, String name, int price, String imageUrl, Long categoryId) {
        Category category = categoryService.getById(categoryId);
        updateAndSave(product, name, price, imageUrl, category);
    }

    public void delete(Long id) {
        productRepository.deleteById(id);
    }

    private Product updateAndSave(Product product, String name, int price, String imageUrl, Category category) {
        product.update(name, price, imageUrl, category);
        return productRepository.save(product);
    }

    private void validateName(String name) {
        List<String> errors = ProductNameValidator.validate(name);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(", ", errors));
        }
    }
}
