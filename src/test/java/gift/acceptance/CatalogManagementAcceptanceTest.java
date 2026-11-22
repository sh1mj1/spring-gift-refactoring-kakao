package gift.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import gift.category.CategoryRequest;
import gift.category.CategoryResponse;
import gift.option.OptionRequest;
import gift.option.OptionResponse;
import gift.product.ProductRequest;
import gift.product.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@DirtiesContext
class CatalogManagementAcceptanceTest extends HttpAcceptanceTest {
    private static final String PRODUCTS = "/api/products";
    private static final String CATEGORIES = "/api/categories";
    private static final String IMAGE = "https://example.com/test.jpg";

    @Test
    @DisplayName("상품 관리 담당자는 토큰 없이 상품을 등록하고 상세에서 등록 정보를 확인할 수 있다.")
    void createsProductWithoutAuthentication() {
        // given
        var input = new ProductRequest("등록 상품", 1000, IMAGE, 1L);

        // when
        var response = http.postForEntity(PRODUCTS, input, ProductResponse.class);

        // then
        var product = body(response, CREATED);
        String path = PRODUCTS + "/" + product.id();
        assertThat(response.getHeaders().getLocation()).hasToString(path);
        assertThat(product).isEqualTo(new ProductResponse(product.id(), "등록 상품", 1000, IMAGE, 1L));
        assertThat(body(http.getForEntity(path, ProductResponse.class), OK)).isEqualTo(product);
    }

    @Test
    @DisplayName("상품 관리 담당자가 토큰 없이 상품을 수정하면 상세에 변경된 정보가 표시된다.")
    void updatesProductWithoutAuthentication() {
        // given
        var product = createProduct("수정 전 상품", 1000);
        String path = PRODUCTS + "/" + product.id();
        var input = new ProductRequest("수정 상품", 2000, IMAGE, 2L);

        // when
        var response = request(path, PUT, input, ProductResponse.class);

        // then
        var updated = new ProductResponse(product.id(), "수정 상품", 2000, IMAGE, 2L);
        assertThat(body(response, OK)).isEqualTo(updated);
        assertThat(body(http.getForEntity(path, ProductResponse.class), OK)).isEqualTo(updated);
    }

    @Test
    @DisplayName("상품 관리 담당자가 토큰 없이 상품을 삭제하면 상세를 더 이상 조회할 수 없다.")
    void deletesProductWithoutAuthentication() {
        // given
        String path = PRODUCTS + "/" + createProduct("삭제 상품", 1000).id();

        // when
        var response = request(path, DELETE, null, Void.class);

        // then
        noContent(response);
        assertProductMissing(path);
    }

    @Test
    @DisplayName("이미 삭제한 상품을 다시 삭제해도 204 응답을 받고 상품은 여전히 조회되지 않는다.")
    void deletesAlreadyRemovedProduct() {
        // given
        String path = PRODUCTS + "/" + createProduct("반복 삭제 상품", 1000).id();
        noContent(request(path, DELETE, null, Void.class));

        // when
        var response = request(path, DELETE, null, Void.class);

        // then
        noContent(response);
        assertProductMissing(path);
    }

    @Test
    @DisplayName("주문이 참조하는 상품을 삭제하면 500 응답을 받고 상품은 그대로 남는다.")
    void cannotDeleteProductReferencedByOrder() {
        // given
        String path = PRODUCTS + "/2";
        var before = body(http.getForEntity(path, ProductResponse.class), OK);

        // when
        var response = request(path, DELETE, null, JsonNode.class);

        // then
        assertError(response, INTERNAL_SERVER_ERROR, path);
        assertThat(body(http.getForEntity(path, ProductResponse.class), OK)).isEqualTo(before);
    }

    @Test
    @DisplayName("방문자는 카테고리 목록에서 초기 카테고리 세 개의 정보를 볼 수 있다.")
    void listsCategories() {
        // given
        var electronics = new CategoryResponse(1L, "전자기기", "#1E90FF",
            "https://example.com/images/electronics.jpg", "스마트폰, 노트북 등 전자기기");
        var fashion = new CategoryResponse(2L, "패션", "#FF6347",
            "https://example.com/images/fashion.jpg", "의류, 신발, 액세서리");
        var food = new CategoryResponse(3L, "식품", "#32CD32",
            "https://example.com/images/food.jpg", "신선식품, 가공식품, 음료");

        // when
        var response = http.getForEntity(CATEGORIES, CategoryResponse[].class);

        // then
        assertThat(body(response, OK)).containsExactlyInAnyOrder(electronics, fashion, food);
    }

    @Test
    @DisplayName("상품 관리 담당자가 토큰 없이 카테고리를 등록하면 목록에 등록 정보가 표시된다.")
    void createsCategoryWithoutAuthentication() {
        // given
        var input = new CategoryRequest("등록 분류", "#112233", IMAGE, "등록 설명");

        // when
        var response = http.postForEntity(CATEGORIES, input, CategoryResponse.class);

        // then
        var created = body(response, CREATED);
        assertThat(response.getHeaders().getLocation()).hasToString(CATEGORIES + "/" + created.id());
        assertThat(created).isEqualTo(new CategoryResponse(created.id(), "등록 분류", "#112233", IMAGE, "등록 설명"));
        assertThat(categories()).contains(created);
        noContent(request(CATEGORIES + "/" + created.id(), DELETE, null, Void.class));
    }

    @Test
    @DisplayName("상품 관리 담당자가 토큰 없이 카테고리를 수정하면 목록에 변경된 정보가 표시된다.")
    void updatesCategoryWithoutAuthentication() {
        // given
        var before = createCategory("수정 전 분류");
        String path = CATEGORIES + "/" + before.id();
        var input = new CategoryRequest("수정 분류", "#445566", IMAGE, "수정 설명");

        // when
        var response = request(path, PUT, input, CategoryResponse.class);

        // then
        var updated = new CategoryResponse(before.id(), "수정 분류", "#445566", IMAGE, "수정 설명");
        assertThat(body(response, OK)).isEqualTo(updated);
        assertThat(categories()).contains(updated).doesNotContain(before);
        noContent(request(path, DELETE, null, Void.class));
    }

    @Test
    @DisplayName("상품 관리 담당자가 토큰 없이 카테고리를 삭제하면 해당 카테고리가 목록에서 사라진다.")
    void deletesCategoryWithoutAuthentication() {
        // given
        var category = createCategory("삭제 분류");

        // when
        var response = request(CATEGORIES + "/" + category.id(), DELETE, null, Void.class);

        // then
        noContent(response);
        assertThat(categories()).doesNotContain(category);
    }

    @Test
    @DisplayName("방문자는 맥북에 등록된 두 옵션의 이름과 재고를 볼 수 있다.")
    void listsOptionsWithStock() {
        // given
        String path = PRODUCTS + "/1/options";

        // when
        var response = http.getForEntity(path, OptionResponse[].class);

        // then
        assertThat(body(response, OK)).containsExactlyInAnyOrder(
            new OptionResponse(1L, "스페이스 블랙 / M1 Pro", 10),
            new OptionResponse(2L, "실버 / M1 Max", 5)
        );
    }

    @Test
    @DisplayName("등록 직후 옵션을 추가하지 않은 상품의 옵션 목록은 비어 있다.")
    void listsNoOptionsForNewProduct() {
        // given
        String path = optionsFor("옵션 없는 상품");

        // when
        var response = http.getForEntity(path, OptionResponse[].class);

        // then
        assertThat(body(response, OK)).isEmpty();
    }

    @Test
    @DisplayName("상품 관리 담당자가 토큰 없이 옵션을 추가하면 목록에서 새 옵션과 재고를 확인할 수 있다.")
    void addsOptionWithoutAuthentication() {
        // given
        String path = optionsFor("옵션 추가 상품");
        var input = new OptionRequest("새 옵션", 20);

        // when
        var response = http.postForEntity(path, input, OptionResponse.class);

        // then
        var option = body(response, CREATED);
        assertThat(response.getHeaders().getLocation()).hasToString(path + "/" + option.id());
        assertThat(option).isEqualTo(new OptionResponse(option.id(), "새 옵션", 20));
        assertThat(options(path)).containsExactly(option);
    }

    @Test
    @DisplayName("옵션 두 개 중 하나를 토큰 없이 삭제하면 다른 옵션만 남는다.")
    void deletesOneOfTwoOptionsWithoutAuthentication() {
        // given
        String path = optionsFor("옵션 삭제 상품");
        var removed = createOption(path, "삭제 옵션", 10);
        var remaining = createOption(path, "남은 옵션", 20);

        // when
        var response = request(path + "/" + removed.id(), DELETE, null, Void.class);

        // then
        noContent(response);
        assertThat(options(path)).containsExactly(remaining);
    }

    @Test
    @DisplayName("옵션이 하나뿐인 상품에서 마지막 옵션을 삭제하면 400 응답을 받고 옵션은 그대로 남는다.")
    void rejectsDeletionOfLastOption() {
        // given
        String path = optionsFor("마지막 옵션 상품");
        var remaining = createOption(path, "마지막 옵션", 20);

        // when
        var response = request(path + "/" + remaining.id(), DELETE, null, String.class);

        // then
        assertThat(body(response, BAD_REQUEST)).isEqualTo("옵션이 1개인 상품은 옵션을 삭제할 수 없습니다.");
        assertThat(options(path)).containsExactly(remaining);
    }

    private void assertProductMissing(String path) {
        var response = http.getForEntity(path, String.class);
        assertThat(response.getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(response.getBody()).isNullOrEmpty();
    }

    private CategoryResponse createCategory(String name) {
        return body(http.postForEntity(CATEGORIES, new CategoryRequest(name, "#112233", IMAGE, "설명"),
            CategoryResponse.class), CREATED);
    }

    private CategoryResponse[] categories() {
        return body(http.getForEntity(CATEGORIES, CategoryResponse[].class), OK);
    }

    private String optionsFor(String productName) {
        return PRODUCTS + "/" + createProduct(productName, 1000).id() + "/options";
    }

    private OptionResponse[] options(String path) {
        return body(http.getForEntity(path, OptionResponse[].class), OK);
    }
}
