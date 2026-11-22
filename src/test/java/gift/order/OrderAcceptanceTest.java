package gift.order;

import com.fasterxml.jackson.databind.JsonNode;
import gift.auth.TokenResponse;
import gift.member.MemberRequest;
import gift.option.OptionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class OrderAcceptanceTest {
    private static final String TANGERINE_OPTIONS = "/api/products/5/options";

    @Autowired
    private TestRestTemplate http;

    @Test
    @DisplayName("포인트가 부족해 주문이 실패하면 주문 내역은 비어 있고 재고는 그대로다")
    void orderWithInsufficientPointsLeavesStockUnchanged() {
        // 준비: 포인트가 없는 새 회원과 재고가 있는 옵션
        MemberRequest member = new MemberRequest("no-points-order@example.com", "test-password");
        ResponseEntity<TokenResponse> registration =
            http.postForEntity("/api/members/register", member, TokenResponse.class);
        assertThat(registration.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TokenResponse token = registration.getBody();
        assertThat(token).isNotNull();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.token());

        OptionResponse optionBeforeOrder = getTangerineOption();
        assertThat(optionBeforeOrder.quantity()).isPositive();

        // 요청: 옵션 1개를 주문한다.
        OrderRequest order = new OrderRequest(optionBeforeOrder.id(), 1, null);
        ResponseEntity<String> orderResponse =
            http.postForEntity("/api/orders", new HttpEntity<>(order, headers), String.class);

        // 검증: 주문이 실패하고 주문 내역과 재고가 바뀌지 않는다.
        assertThat(orderResponse.getStatusCode().isError()).isTrue();

        ResponseEntity<JsonNode> orderHistory =
            http.exchange("/api/orders", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(orderHistory.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode history = orderHistory.getBody();
        assertThat(history).isNotNull();
        JsonNode orders = history.path("content");
        assertThat(orders.isArray()).isTrue();
        assertThat(orders.size()).isZero();

        assertThat(getTangerineOption().quantity())
            .as("실패한 주문 이후 재고")
            .isEqualTo(optionBeforeOrder.quantity());
    }

    private OptionResponse getTangerineOption() {
        ResponseEntity<OptionResponse[]> response =
            http.getForEntity(TANGERINE_OPTIONS, OptionResponse[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        OptionResponse[] options = response.getBody();
        assertThat(options).hasSize(1);
        return options[0];
    }
}
