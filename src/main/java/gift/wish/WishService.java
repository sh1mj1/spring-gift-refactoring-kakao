package gift.wish;

import gift.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class WishService {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    public WishService(WishRepository wishRepository, ProductRepository productRepository) {
        this.wishRepository = wishRepository;
        this.productRepository = productRepository;
    }

    public Page<Wish> findByMemberId(Long memberId, Pageable pageable) {
        return wishRepository.findByMemberId(memberId, pageable);
    }

    public AddResult add(Long memberId, Long productId) {
        var product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return null;
        }

        var existing = wishRepository.findByMemberIdAndProductId(memberId, product.getId()).orElse(null);
        if (existing != null) {
            return new AddResult(existing, false);
        }

        var saved = wishRepository.save(new Wish(memberId, product));
        return new AddResult(saved, true);
    }

    public RemoveResult remove(Long memberId, Long wishId) {
        var wish = wishRepository.findById(wishId).orElse(null);
        if (wish == null) {
            return RemoveResult.NOT_FOUND;
        }

        if (!wish.getMemberId().equals(memberId)) {
            return RemoveResult.NOT_OWNED;
        }

        wishRepository.delete(wish);
        return RemoveResult.REMOVED;
    }

    public record AddResult(Wish wish, boolean created) {
    }

    public enum RemoveResult {
        NOT_FOUND,
        NOT_OWNED,
        REMOVED
    }
}
