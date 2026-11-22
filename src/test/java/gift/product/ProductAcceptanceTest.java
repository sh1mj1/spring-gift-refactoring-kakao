package gift.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class ProductAcceptanceTest {
    private static final String PRODUCTS = "/api/products";
    private static final String EXPECTED_PRODUCTS = """
        [
          {"id": 1, "name": "맥북 프로 16인치", "price": 3360000,
           "imageUrl": "https://example.com/images/macbook.jpg", "categoryId": 1},
          {"id": 2, "name": "아이폰 16", "price": 1350000,
           "imageUrl": "https://example.com/images/iphone.jpg", "categoryId": 1},
          {"id": 3, "name": "나이키 에어맥스", "price": 179000,
           "imageUrl": "https://example.com/images/airmax.jpg", "categoryId": 2},
          {"id": 4, "name": "레비스 청바지", "price": 89000,
           "imageUrl": "https://example.com/images/jeans.jpg", "categoryId": 2},
          {"id": 5, "name": "제주 감귤 5kg", "price": 25000,
           "imageUrl": "https://example.com/images/tangerine.jpg", "categoryId": 3},
          {"id": 6, "name": "한우 등심 1kg", "price": 65000,
           "imageUrl": "https://example.com/images/beef.jpg", "categoryId": 3}
        ]
        """;

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("상품이 6개 등록되어 있으면 방문자는 목록에서 6개 상품의 정보를 볼 수 있다.")
    void visitorBrowsesProducts() throws JsonProcessingException {
        // given
        JsonNode expectedProducts = objectMapper.readTree(EXPECTED_PRODUCTS);

        // when
        ResponseEntity<JsonNode> response = http.getForEntity(PRODUCTS, JsonNode.class);

        // then
        JsonNode products = assertProductList(response);
        assertThat(products).containsExactlyInAnyOrderElementsOf(expectedProducts);
    }

    @Test
    @DisplayName("방문자가 등록된 맥북 프로 16인치의 상세를 열면 해당 상품의 정보를 볼 수 있다.")
    void visitorOpensProductDetails() throws JsonProcessingException {
        // given
        JsonNode selectedProduct = objectMapper.readTree(EXPECTED_PRODUCTS).get(0);

        // when
        ResponseEntity<JsonNode> detail =
            http.getForEntity(PRODUCTS + "/{id}", JsonNode.class, selectedProduct.path("id").longValue());

        // then
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody()).isEqualTo(selectedProduct);
    }

    @Test
    @DisplayName("방문자가 등록되지 않은 상품의 상세를 조회하면 404 응답을 받고 상품 목록은 그대로다.")
    void missingProductLeavesCatalogUnchanged() {
        // given
        JsonNode before = assertProductList(http.getForEntity(PRODUCTS, JsonNode.class));

        // when
        ResponseEntity<String> missing = http.getForEntity(PRODUCTS + "/{id}", String.class, 999999L);

        // then
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missing.getBody()).isNullOrEmpty();
        JsonNode after = assertProductList(http.getForEntity(PRODUCTS, JsonNode.class));
        assertThat(after).containsExactlyInAnyOrderElementsOf(before);
    }

    private JsonNode assertProductList(ResponseEntity<JsonNode> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode page = response.getBody();
        assertThat(page).isNotNull();
        assertThat(page.path("totalElements")).isEqualTo(objectMapper.getNodeFactory().numberNode(6));
        JsonNode content = page.path("content");
        assertThat(content.isArray()).isTrue();
        assertThat(content).hasSize(6);
        return content;
    }
}
