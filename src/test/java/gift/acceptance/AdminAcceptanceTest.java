package gift.acceptance;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gift.product.ProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@DirtiesContext
class AdminAcceptanceTest extends HttpAcceptanceTest {
    private static final String PRODUCTS = "/admin/products";
    private static final String MEMBERS = "/admin/members";
    private static final String PRODUCT_API = "/api/products";
    private static final String IMAGE = "https://example.com/admin.jpg";

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("관리자는 로그인 없이 상품 목록에서 맥북의 이름과 가격을 볼 수 있다.")
    void opensProductList() {
        // given & when
        String page = html(PRODUCTS);

        // then
        assertThat(page).contains("<h1>상품 관리</h1>", "맥북 프로 16인치", "3360000", PRODUCTS + "/new");
    }

    @Test
    @DisplayName("관리자는 로그인 없이 상품 등록 양식과 입력 필드를 볼 수 있다.")
    void opensNewProductForm() {
        // given & when
        String page = html(PRODUCTS + "/new");

        // then
        assertThat(page).contains("<h1>상품 추가</h1>", "action=\"/admin/products\"",
            "name=\"name\"", "name=\"price\"", "name=\"imageUrl\"", "name=\"categoryId\"");
    }

    @Test
    @DisplayName("관리자가 맥북 수정 화면을 열면 등록된 이름과 가격이 양식에 채워져 있다.")
    void opensProductEditForm() {
        // given
        String path = PRODUCTS + "/1/edit";

        // when
        String page = html(path);

        // then
        assertThat(page).contains("<h1>상품 수정</h1>", "value=\"맥북 프로 16인치\"", "value=\"3360000\"");
    }

    @Test
    @DisplayName("관리자가 상품 등록 양식을 보내면 목록으로 이동하고 등록한 상품을 조회할 수 있다.")
    void createsProductFromForm() {
        // given
        String name = "관리 상품 등록";
        var form = productForm(name, 1700);

        // when
        var response = submitForm(PRODUCTS, form);

        // then
        redirectsTo(response, PRODUCTS);
        long id = listedId(PRODUCTS, name);
        assertThat(product(id)).isEqualTo(new ProductResponse(id, name, 1700, IMAGE, 1L));
    }

    @Test
    @DisplayName("관리자가 상품 수정 양식을 보내면 목록으로 이동하고 상세에 변경된 값이 표시된다.")
    void updatesProductFromForm() {
        // given
        var created = createProduct("관리 수정 전", 1000);
        String name = "관리 수정 후";
        var form = productForm(name, 2500);

        // when
        var response = submitForm(PRODUCTS + "/" + created.id() + "/edit", form);

        // then
        redirectsTo(response, PRODUCTS);
        assertThat(product(created.id())).isEqualTo(new ProductResponse(created.id(), name, 2500, IMAGE, 1L));
    }

    @Test
    @DisplayName("관리자가 상품 삭제 양식을 보내면 목록으로 이동하고 해당 상품은 조회되지 않는다.")
    void deletesProductFromForm() {
        // given
        var created = createProduct("관리 삭제 상품", 1000);

        // when
        var response = submitForm(PRODUCTS + "/" + created.id() + "/delete", Map.of());

        // then
        redirectsTo(response, PRODUCTS);
        var detail = http.getForEntity(PRODUCT_API + "/" + created.id(), String.class);
        assertThat(detail.getStatusCode()).isEqualTo(NOT_FOUND);
        assertThat(detail.getBody()).isNullOrEmpty();
        assertThat(html(PRODUCTS)).doesNotContain("관리 삭제 상품");
    }

    @Test
    @DisplayName("관리자는 로그인 없이 회원 목록에서 초기 회원의 이메일과 포인트를 볼 수 있다.")
    void opensMemberList() {
        // given & when
        String page = html(MEMBERS);

        // then
        assertThat(page).contains("<h1>회원 관리</h1>", "admin@example.com", "10000000", MEMBERS + "/new");
    }

    @Test
    @DisplayName("관리자는 로그인 없이 회원 등록 양식의 이메일과 비밀번호 필드를 볼 수 있다.")
    void opensNewMemberForm() {
        // given & when
        String page = html(MEMBERS + "/new");

        // then
        assertThat(page).contains("<h1>회원 추가</h1>", "action=\"/admin/members\"",
            "name=\"email\"", "name=\"password\"");
    }

    @Test
    @DisplayName("관리자가 회원 수정 화면을 열면 현재 이메일이 양식에 채워져 있다.")
    void opensMemberEditForm() {
        // given
        String email = "admin-member-edit-form@example.com";
        register(email);
        long id = listedId(MEMBERS, email);

        // when
        String page = html(MEMBERS + "/" + id + "/edit");

        // then
        assertThat(page).contains("<h1>회원 수정</h1>", "value=\"" + email + "\"");
    }

    @Test
    @DisplayName("관리자가 회원 등록 양식을 보내면 목록으로 이동하고 새 이메일이 표시된다.")
    void createsMemberFromForm() {
        // given
        String email = "admin-created-member@example.com";
        var form = memberForm(email);

        // when
        var response = submitForm(MEMBERS, form);

        // then
        redirectsTo(response, MEMBERS);
        assertThat(memberPoints(listedId(MEMBERS, email), email)).isZero();
    }

    @Test
    @DisplayName("관리자가 회원 이메일을 수정하면 목록에 같은 회원의 새 이메일이 표시된다.")
    void updatesMemberFromForm() {
        // given
        String before = "admin-old-email@example.com";
        String after = "admin-new-email@example.com";
        register(before);
        long id = listedId(MEMBERS, before);

        // when
        var response = submitForm(MEMBERS + "/" + id + "/edit", memberForm(after));

        // then
        redirectsTo(response, MEMBERS);
        assertThat(listedId(MEMBERS, after)).isEqualTo(id);
        assertThat(html(MEMBERS)).doesNotContain(before);
    }

    @Test
    @DisplayName("관리자가 회원 삭제 양식을 보내면 목록으로 이동하고 해당 이메일은 사라진다.")
    void deletesMemberFromForm() {
        // given
        String email = "admin-deleted-member@example.com";
        register(email);
        long id = listedId(MEMBERS, email);

        // when
        var response = submitForm(MEMBERS + "/" + id + "/delete", Map.of());

        // then
        redirectsTo(response, MEMBERS);
        assertThat(html(MEMBERS)).doesNotContain(email);
    }

    @Test
    @DisplayName("관리자가 포인트를 30,000 충전하면 회원 목록에 잔액 30,000이 표시된다.")
    void chargesMemberPoints() {
        // given
        String email = "admin-charge@example.com";
        register(email);
        long id = listedId(MEMBERS, email);
        assertThat(memberPoints(id, email)).isZero();

        // when
        var response = submitForm(MEMBERS + "/" + id + "/charge-point", Map.of("amount", "30000"));

        // then
        redirectsTo(response, MEMBERS);
        assertThat(memberPoints(id, email)).isEqualTo(30000);
    }

    @Test
    @DisplayName("관리자가 0포인트를 충전하면 500 응답을 받고 회원 잔액은 그대로다.")
    void rejectsZeroPointCharge() throws JsonProcessingException {
        // given
        String email = "admin-zero-charge@example.com";
        register(email);
        long id = listedId(MEMBERS, email);
        String path = MEMBERS + "/" + id + "/charge-point";
        redirectsTo(submitForm(path, Map.of("amount", "30000")), MEMBERS);

        // when
        var response = submitForm(path, Map.of("amount", "0"));

        // then
        var error = objectMapper.readTree(body(response, INTERNAL_SERVER_ERROR));
        assertThat(error.path("status").intValue()).isEqualTo(500);
        assertThat(error.path("path").textValue()).isEqualTo(path);
        assertThat(memberPoints(id, email)).isEqualTo(30000);
    }

    private ProductResponse product(long id) {
        return body(http.getForEntity(PRODUCT_API + "/" + id, ProductResponse.class), OK);
    }

    private Map<String, String> productForm(String name, int price) {
        return Map.of("name", name, "price", Integer.toString(price), "imageUrl", IMAGE, "categoryId", "1");
    }

    private Map<String, String> memberForm(String email) {
        return Map.of("email", email, "password", "test-password");
    }
}
