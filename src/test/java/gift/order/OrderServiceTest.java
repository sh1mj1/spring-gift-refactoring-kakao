package gift.order;

import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.Option;
import gift.option.OptionRepository;
import gift.product.Product;
import gift.wish.WishRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {
    @Test
    void chargesForAllUnitsAndSavesTheOrder() {
        var product = new Product("상품", 12500, null, null);
        var option = new Option(product, "옵션", 10);
        var member = new Member("buyer@example.com");
        member.chargePoint(30000);
        member.updateKakaoAccessToken("test-token");

        var orders = mock(OrderRepository.class);
        var options = mock(OptionRepository.class);
        var members = mock(MemberRepository.class);
        var wishes = mock(WishRepository.class);
        var notifications = mock(KakaoMessageClient.class);
        when(options.findById(1L)).thenReturn(Optional.of(option));
        when(orders.save(any(Order.class))).thenAnswer(call -> call.getArgument(0));
        var service = new OrderService(orders, options, members, wishes, notifications);

        Order order = service.create(member, new OrderRequest(1L, 2, "축하해!"));

        assertThat(option.getQuantity()).isEqualTo(8);
        assertThat(member.getPoint()).isEqualTo(5000);
        assertThat(order.getQuantity()).isEqualTo(2);
        verify(options).save(option);
        verify(members).save(member);
        verify(orders).save(order);
        verify(notifications).sendToMe("test-token", order, product);
    }
}
