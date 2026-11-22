package gift.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import gift.member.MemberRepository;
import gift.option.OptionResponse;
import gift.order.KakaoMessageClient;
import gift.order.OrderRequest;
import gift.order.OrderResponse;
import gift.product.ProductResponse;
import gift.wish.Wish;
import gift.wish.WishRepository;
import gift.wish.WishRequest;
import gift.wish.WishResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.client.RestClientException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@DirtiesContext
class OrderingAcceptanceTest extends HttpAcceptanceTest {
    private static final String ORDERS = "/api/orders";
    private static final String MEMBERS = "/admin/members";
    private static final String WISHES = "/api/wishes";

    @Autowired
    private MemberRepository memberRepository;

    @MockitoBean
    private KakaoMessageClient kakaoMessageClient;

    @MockitoSpyBean
    private WishRepository wishRepository;

    @Test
    @DisplayName("회원이 포인트로 주문하면 주문이 기록되고 재고와 포인트가 줄며 해당 위시만 삭제된다.")
    void buysProductWithPoints() {
        // given
        String email = "successful-order@example.com";
        String buyer = fundedBuyer(email);
        long memberId = listedId(MEMBERS, email);
        var product = createProduct("주문 성공 상품", 25000);
        String options = optionsPath(product);
        var option = createOption(options, "주문 옵션", 50);
        body(authenticated(WISHES, POST, buyer, new WishRequest(product.id()), WishResponse.class), CREATED);
        var otherProduct = createProduct("구매하지 않은 상품", 25000);
        var otherWish = body(authenticated(WISHES, POST, buyer, new WishRequest(otherProduct.id()),
            WishResponse.class), CREATED);
        String otherMember = register("other-wish-owner@example.com");
        var otherMemberWish = body(authenticated(WISHES, POST, otherMember, new WishRequest(product.id()),
            WishResponse.class), CREATED);
        var input = new OrderRequest(option.id(), 1, "생일 축하해!");
        assertThat(memberPoints(memberId, email)).isEqualTo(30000);
        assertThat(orders(buyer)).isEmpty();

        // when
        var response = authenticated(ORDERS, POST, buyer, input, OrderResponse.class);

        // then
        var order = body(response, CREATED);
        assertOrder(order, input);
        verify(kakaoMessageClient).sendToMe(eq(email), any(), any());
        assertThat(response.getHeaders().getLocation()).hasToString(ORDERS + "/" + order.id());
        assertThat(stock(options, option.id())).isEqualTo(49);
        assertThat(memberPoints(memberId, email)).isEqualTo(5000);
        var content = orders(buyer);
        assertThat(content).hasSize(1);
        assertThat(content.get(0).path("id").longValue()).isEqualTo(order.id());
        assertOnlyWish(buyer, otherWish);
        assertOnlyWish(otherMember, otherMemberWish);
    }

    @Test
    @DisplayName("포인트가 부족한 주문은 실패하고 재고를 유지한다.")
    void insufficientPointsPreserveStock() {
        // given
        String email = "insufficient-order@example.com";
        String buyer = registerWithKakao(email);
        long memberId = listedId(MEMBERS, email);
        String options = optionsPath(createProduct("주문 실패 상품", 25000));
        var option = createOption(options, "주문 실패 옵션", 50);
        assertThat(stock(options, option.id())).isEqualTo(50);
        assertThat(memberPoints(memberId, email)).isZero();
        assertThat(orders(buyer)).isEmpty();

        // when
        var response = authenticated(ORDERS, POST, buyer, new OrderRequest(option.id(), 1, null), JsonNode.class);

        // then
        assertError(response, INTERNAL_SERVER_ERROR, ORDERS);
        assertThat(stock(options, option.id())).isEqualTo(50);
        assertThat(memberPoints(memberId, email)).isZero();
        verifyNoInteractions(kakaoMessageClient);
        assertThat(orders(buyer)).isEmpty();
    }

    @Test
    @DisplayName("아직 주문하지 않은 회원의 주문 목록은 비어 있다.")
    void listsEmptyOrders() {
        // given
        String buyer = register("empty-orders@example.com");

        // when
        var response = authenticated(ORDERS, GET, buyer, null, JsonNode.class);

        // then
        var page = body(response, OK);
        assertThat(page.path("totalElements").intValue()).isZero();
        assertThat(page.path("content").isArray()).isTrue();
        assertThat(page.path("content")).isEmpty();
    }

    @Test
    @DisplayName("회원은 유효한 토큰만 인증 헤더에 넣어도 자신이 주문한 내역을 볼 수 있다.")
    void listsOwnOrderWithBareToken() {
        // given
        String buyer = fundedBuyer("order-list@example.com");
        var request = new OrderRequest(orderOption("주문 목록 상품"), 1, "주문 목록 메시지");
        var order = body(authenticated(ORDERS, POST, buyer, request, OrderResponse.class), CREATED);
        String token = buyer.substring("Bearer ".length());

        // when
        var response = authenticated(ORDERS, GET, token, null, JsonNode.class);

        // then
        var page = body(response, OK);
        assertThat(page.path("totalElements").intValue()).isEqualTo(1);
        var content = page.path("content");
        assertThat(content).hasSize(1);
        var entry = content.get(0);
        assertThat(entry.path("id").longValue()).isEqualTo(order.id());
        assertThat(entry.path("optionId").longValue()).isEqualTo(request.optionId());
        assertThat(entry.path("quantity").intValue()).isEqualTo(1);
        assertThat(entry.path("message").textValue()).isEqualTo("주문 목록 메시지");
        assertThat(entry.path("orderDateTime").textValue()).isNotBlank();
    }

    @Test
    @DisplayName("생성 응답에 담긴 주문 주소로 조회하면 404를 받지만 주문은 목록에 남는다.")
    void rejectsOrderDetailAtCreationLocation() {
        // given
        String buyer = fundedBuyer("order-location@example.com");
        var request = new OrderRequest(orderOption("주문 주소 상품"), 1, null);
        var creation = authenticated(ORDERS, POST, buyer, request, OrderResponse.class);
        var order = body(creation, CREATED);
        String path = ORDERS + "/" + order.id();
        assertThat(creation.getHeaders().getLocation()).hasToString(path);

        // when
        var response = authenticated(path, GET, buyer, null, JsonNode.class);

        // then
        assertError(response, NOT_FOUND, path);
        var content = orders(buyer);
        assertThat(content).hasSize(1);
        assertThat(content.get(0).path("id").longValue()).isEqualTo(order.id());
    }

    @Test
    void rollsBackWhenAllKakaoAttemptsFail() {
        String email = "notification-failure@example.com";
        String buyer = fundedBuyer(email);
        long memberId = listedId(MEMBERS, email);
        var product = createProduct("알림 실패 상품", 25000);
        String options = optionsPath(product);
        var wish = body(authenticated(WISHES, POST, buyer, new WishRequest(product.id()), WishResponse.class), CREATED);
        var option = createOption(options, "알림 실패 옵션", 50);
        doThrow(new RestClientException("test failure")).when(kakaoMessageClient).sendToMe(eq(email), any(), any());

        var response = authenticated(ORDERS, POST, buyer, new OrderRequest(option.id(), 1, null), String.class);

        assertThat(body(response, FORBIDDEN)).isEqualTo("카카오 메시지를 보내지 못해 주문이 취소되었습니다.");
        verify(kakaoMessageClient, times(6)).sendToMe(eq(email), any(), any());
        assertThat(stock(options, option.id())).isEqualTo(50);
        assertThat(memberPoints(memberId, email)).isEqualTo(30000);
        assertThat(orders(buyer)).isEmpty();
        assertOnlyWish(buyer, wish);
        verify(wishRepository).delete(any(Wish.class));
    }

    @Test
    void succeedsAfterRetry() {
        String email = "notification-retry@example.com";
        String buyer = fundedBuyer(email);
        long memberId = listedId(MEMBERS, email);
        String options = optionsPath(createProduct("알림 재시도 상품", 25000));
        var option = createOption(options, "알림 재시도 옵션", 50);
        doThrow(new RestClientException("test failure")).doNothing()
            .when(kakaoMessageClient).sendToMe(eq(email), any(), any());

        var order = body(authenticated(ORDERS, POST, buyer,
            new OrderRequest(option.id(), 1, null), OrderResponse.class), CREATED);

        verify(kakaoMessageClient, times(2)).sendToMe(eq(email), any(), any());
        assertThat(stock(options, option.id())).isEqualTo(49);
        assertThat(memberPoints(memberId, email)).isEqualTo(5000);
        assertThat(orders(buyer).get(0).path("id").longValue()).isEqualTo(order.id());
    }

    @Test
    void rejectsOrderWithoutKakaoToken() {
        String email = "without-kakao@example.com";
        String buyer = register(email);
        long memberId = listedId(MEMBERS, email);
        redirectsTo(submitForm(MEMBERS + "/" + memberId + "/charge-point", Map.of("amount", "30000")), MEMBERS);
        String options = optionsPath(createProduct("연결 확인 상품", 25000));
        var option = createOption(options, "카카오 미연결 옵션", 50);

        var response = authenticated(ORDERS, POST, buyer, new OrderRequest(option.id(), 1, null), String.class);

        assertThat(body(response, FORBIDDEN)).isEqualTo("주문하려면 카카오 로그인이 필요합니다.");
        verifyNoInteractions(kakaoMessageClient);
        assertThat(stock(options, option.id())).isEqualTo(50);
        assertThat(memberPoints(memberId, email)).isEqualTo(30000);
        assertThat(orders(buyer)).isEmpty();
    }

    @Test
    void preservesStateWhenStockOrOptionIsMissing() {
        String email = "insufficient-stock@example.com";
        String buyer = fundedBuyer(email);
        long memberId = listedId(MEMBERS, email);
        String options = optionsPath(createProduct("재고 부족 상품", 25000));
        var option = createOption(options, "재고 부족 옵션", 1);

        assertError(authenticated(ORDERS, POST, buyer,
            new OrderRequest(option.id(), 2, null), JsonNode.class), INTERNAL_SERVER_ERROR, ORDERS);
        assertThat(authenticated(ORDERS, POST, buyer,
            new OrderRequest(Long.MAX_VALUE, 1, null), String.class).getStatusCode()).isEqualTo(NOT_FOUND);

        verifyNoInteractions(kakaoMessageClient);
        assertThat(stock(options, option.id())).isEqualTo(1);
        assertThat(memberPoints(memberId, email)).isEqualTo(30000);
        assertThat(orders(buyer)).isEmpty();
    }

    @Test
    void rollsBackWhenWishDeletionFails() {
        String email = "wish-cleanup-failure@example.com";
        String buyer = fundedBuyer(email);
        long memberId = listedId(MEMBERS, email);
        var product = createProduct("위시 삭제 실패 상품", 25000);
        String options = optionsPath(product);
        var option = createOption(options, "위시 삭제 실패 옵션", 50);
        var wish = body(authenticated(WISHES, POST, buyer, new WishRequest(product.id()), WishResponse.class), CREATED);
        doThrow(new IllegalStateException("test deletion failure")).when(wishRepository).delete(any(Wish.class));

        var response = authenticated(ORDERS, POST, buyer, new OrderRequest(option.id(), 1, null), JsonNode.class);

        assertError(response, INTERNAL_SERVER_ERROR, ORDERS);
        verifyNoInteractions(kakaoMessageClient);
        assertThat(stock(options, option.id())).isEqualTo(50);
        assertThat(memberPoints(memberId, email)).isEqualTo(30000);
        assertThat(orders(buyer)).isEmpty();
        assertOnlyWish(buyer, wish);
    }

    private void assertOnlyWish(String buyer, WishResponse wish) {
        var wishes = body(authenticated(WISHES, GET, buyer, null, JsonNode.class), OK).path("content");
        assertThat(wishes).hasSize(1);
        assertThat(wishes.get(0).path("id").longValue()).isEqualTo(wish.id());
    }

    private String registerWithKakao(String email) {
        String buyer = register(email);
        var member = memberRepository.findByEmail(email).orElseThrow();
        member.updateKakaoAccessToken(email);
        memberRepository.save(member);
        return buyer;
    }

    private String fundedBuyer(String email) {
        String buyer = registerWithKakao(email);
        long id = listedId(MEMBERS, email);
        redirectsTo(submitForm(MEMBERS + "/" + id + "/charge-point", Map.of("amount", "30000")), MEMBERS);
        return buyer;
    }

    private long orderOption(String productName) {
        String path = optionsPath(createProduct(productName, 25000));
        return createOption(path, "주문 옵션", 50).id();
    }

    private String optionsPath(ProductResponse product) {
        return "/api/products/" + product.id() + "/options";
    }

    private int stock(String path, long optionId) {
        var options = body(http.getForEntity(path, OptionResponse[].class), OK);
        assertThat(options).hasSize(1);
        assertThat(options[0].id()).isEqualTo(optionId);
        return options[0].quantity();
    }

    private JsonNode orders(String buyer) {
        var content = body(authenticated(ORDERS, GET, buyer, null, JsonNode.class), OK).path("content");
        assertThat(content.isArray()).isTrue();
        return content;
    }

    private void assertOrder(OrderResponse order, OrderRequest request) {
        assertThat(order.id()).isPositive();
        assertThat(order.optionId()).isEqualTo(request.optionId());
        assertThat(order.quantity()).isEqualTo(request.quantity());
        assertThat(order.message()).isEqualTo(request.message());
        assertThat(order.orderDateTime()).isNotNull();
    }
}
