package gift.order;

import gift.option.Option;
import gift.product.Product;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoMessageClientTest {
    @Test
    void sendsTheFormattedTotalForAllUnits() {
        var product = new Product("상품", 12500, null, null);
        var order = new Order(new Option(product, "옵션", 10), 1L, 2, "축하해!");
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new KakaoMessageClient(builder);
        var formattedAmount = String.format(Locale.getDefault(Locale.Category.FORMAT), "%,d", 25000);
        server.expect(requestTo("https://kapi.kakao.com/v2/api/talk/memo/default/send"))
            .andExpect(request -> {
                var body = ((MockClientHttpRequest) request).getBodyAsString();
                assertThat(URLDecoder.decode(body, StandardCharsets.UTF_8))
                    .contains("수량: 2개", "금액: " + formattedAmount + "원", "축하해!");
            })
            .andRespond(withSuccess());

        client.sendToMe("test-token", order, product);

        server.verify();
    }
}
