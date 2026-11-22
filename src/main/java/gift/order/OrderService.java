package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.wish.WishRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Service
public class OrderService {
    private static final int MAX_NOTIFICATION_ATTEMPTS = 6;
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final WishRepository wishRepository;
    private final KakaoMessageClient kakaoMessageClient;

    public OrderService(
        OrderRepository orderRepository,
        OptionRepository optionRepository,
        MemberRepository memberRepository,
        WishRepository wishRepository,
        KakaoMessageClient kakaoMessageClient
    ) {
        this.orderRepository = orderRepository;
        this.optionRepository = optionRepository;
        this.memberRepository = memberRepository;
        this.wishRepository = wishRepository;
        this.kakaoMessageClient = kakaoMessageClient;
    }

    public Page<Order> findByMemberId(Long memberId, Pageable pageable) {
        return orderRepository.findByMemberId(memberId, pageable);
    }

    @Transactional
    public Order create(Member member, OrderRequest request) {
        // validate option
        var option = optionRepository.findById(request.optionId()).orElse(null);
        if (option == null) {
            return null;
        }

        String kakaoAccessToken = member.getKakaoAccessToken();
        if (kakaoAccessToken == null || kakaoAccessToken.isBlank()) {
            throw new IllegalStatusException("주문하려면 카카오 로그인이 필요합니다.");
        }

        var orderQuantity = request.quantity();

        // subtract stock
        option.subtractQuantity(orderQuantity);
        optionRepository.save(option);

        // deduct points
        var order = new Order(option, member.getId(), orderQuantity, request.message());
        member.deductPoint(order.totalPrice());
        memberRepository.save(member);

        // save order
        var saved = orderRepository.save(order);

        wishRepository.findByMemberIdAndProductId(member.getId(), option.getProduct().getId())
            .ifPresent(wishRepository::delete);

        orderRepository.flush();
        sendKakaoMessage(kakaoAccessToken, saved, option);
        return saved;
    }

    private void sendKakaoMessage(String kakaoAccessToken, Order order, Option option) {
        var product = option.getProduct();
        for (int attempt = 1; attempt <= MAX_NOTIFICATION_ATTEMPTS; attempt++) {
            try {
                kakaoMessageClient.sendToMe(kakaoAccessToken, order, product);
                return;
            } catch (RestClientException failure) {
                if (attempt == MAX_NOTIFICATION_ATTEMPTS) {
                    throw new IllegalStatusException("카카오 메시지를 보내지 못해 주문이 취소되었습니다.", failure);
                }
            }
        }
    }
}
