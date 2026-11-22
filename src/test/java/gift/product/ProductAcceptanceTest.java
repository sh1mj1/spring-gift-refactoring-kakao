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
    @DisplayName("방문자는 상품 6개를 보고 선택한 상품의 상세를 조회한다")
    void visitorBrowsesProductsAndOpensDetails() throws JsonProcessingException {
        JsonNode products = getProducts();
        JsonNode expectedProducts = objectMapper.readTree(EXPECTED_PRODUCTS);
        assertThat(products).containsExactlyInAnyOrderElementsOf(expectedProducts);

        JsonNode selectedProduct = expectedProducts.get(0);
        ResponseEntity<JsonNode> detail =
            http.getForEntity(PRODUCTS + "/{id}", JsonNode.class, selectedProduct.path("id").longValue());

        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody()).isEqualTo(selectedProduct);
    }

    @Test
    @DisplayName("없는 상품을 조회하면 404가 반환되고 상품 목록은 그대로다")
    void missingProductLeavesCatalogUnchanged() {
        JsonNode before = getProducts();

        ResponseEntity<String> missing = http.getForEntity(PRODUCTS + "/{id}", String.class, 999999L);

        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(missing.getBody()).isNullOrEmpty();
        assertThat(getProducts()).containsExactlyInAnyOrderElementsOf(before);
    }

    private JsonNode getProducts() {
        ResponseEntity<JsonNode> response = http.getForEntity(PRODUCTS, JsonNode.class);
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
