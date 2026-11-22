package gift.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import gift.auth.TokenResponse;
import gift.member.MemberRequest;
import gift.wish.WishRequest;
import gift.wish.WishResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

class MemberAndWishAcceptanceTest extends HttpAcceptanceTest {
    private static final String REGISTER = "/api/members/register";
    private static final String LOGIN = "/api/members/login";
    private static final String WISHES = "/api/wishes";

    @Test
    @DisplayName("비회원이 새 이메일로 가입하면 발급된 토큰으로 자신의 빈 위시를 볼 수 있다.")
    void registersWithEmail() {
        // given
        var input = new MemberRequest("email-registration@example.com", "test-password");

        // when
        var response = http.postForEntity(REGISTER, input, TokenResponse.class);

        // then
        var token = body(response, CREATED).token();
        assertThat(token).isNotBlank();
        assertThat(wishes("Bearer " + token)).isEmpty();
    }

    @Test
    @DisplayName("이미 가입한 이메일로 다시 가입하면 기존 비밀번호로만 로그인할 수 있다.")
    void rejectsDuplicateRegistration() {
        // given
        String email = "duplicate-registration@example.com";
        register(email);
        var duplicate = new MemberRequest(email, "other-password");

        // when
        var response = http.postForEntity(REGISTER, duplicate, String.class);

        // then
        assertThat(body(response, BAD_REQUEST)).isEqualTo("Email is already registered.");
        assertThat(http.postForEntity(LOGIN, duplicate, String.class).getStatusCode()).isEqualTo(BAD_REQUEST);
        assertCanUseLoginToken(email);
    }

    @Test
    @DisplayName("회원이 올바른 이메일과 비밀번호로 로그인하면 발급된 토큰으로 자신의 위시를 볼 수 있다.")
    void logsInWithValidCredentials() {
        // given
        String email = "valid-login@example.com";
        register(email);
        var input = new MemberRequest(email, "test-password");

        // when
        var response = http.postForEntity(LOGIN, input, TokenResponse.class);

        // then
        var token = body(response, OK).token();
        assertThat(token).isNotBlank();
        assertThat(wishes("Bearer " + token)).isEmpty();
    }

    @Test
    @DisplayName("회원이 틀린 비밀번호를 입력하면 오류를 받고 기존 비밀번호로는 로그인할 수 있다.")
    void rejectsWrongPassword() {
        // given
        String email = "invalid-login@example.com";
        register(email);
        var input = new MemberRequest(email, "wrong-password");

        // when
        var response = http.postForEntity(LOGIN, input, String.class);

        // then
        assertThat(body(response, BAD_REQUEST)).isEqualTo("Invalid email or password.");
        assertCanUseLoginToken(email);
    }

    @Test
    @DisplayName("회원은 자신이 담은 상품만 위시 목록에서 볼 수 있다.")
    void listsOnlyMembersOwnWishes() {
        // given
        String owner = register("wish-list@example.com");
        String other = register("other-wish-list@example.com");
        var wish = addWish(owner, 1L);
        addWish(other, 2L);

        // when
        var response = authenticated(WISHES, GET, owner, null, JsonNode.class);

        // then
        var page = body(response, OK);
        assertThat(page.path("totalElements").intValue()).isEqualTo(1);
        assertThat(page.path("content")).hasSize(1);
        assertThat(page.path("content").get(0).path("id").longValue()).isEqualTo(wish.id());
        assertThat(page.path("content").get(0).path("productId").longValue()).isEqualTo(1L);
    }

    @Test
    @DisplayName("회원이 상품을 처음 위시에 담으면 생성된 위시와 상품 정보가 목록에 표시된다.")
    void addsWish() {
        // given
        String owner = register("add-wish@example.com");

        // when
        var response = authenticated(WISHES, POST, owner, new WishRequest(1L), WishResponse.class);

        // then
        var wish = body(response, CREATED);
        assertThat(response.getHeaders().getLocation()).hasToString(WISHES + "/" + wish.id());
        assertThat(wish).isEqualTo(new WishResponse(wish.id(), 1L, "맥북 프로 16인치",
            3360000, "https://example.com/images/macbook.jpg"));
        var content = wishes(owner);
        assertThat(content).hasSize(1);
        assertThat(content.get(0).path("id").longValue()).isEqualTo(wish.id());
    }

    @Test
    @DisplayName("회원이 같은 상품을 다시 위시에 담으면 기존 위시가 반환되고 목록에는 한 건만 남는다.")
    void returnsExistingWishWhenProductIsAddedAgain() {
        // given
        String owner = register("duplicate-wish@example.com");
        var original = addWish(owner, 1L);

        // when
        var response = authenticated(WISHES, POST, owner, new WishRequest(1L), WishResponse.class);

        // then
        assertThat(body(response, OK)).isEqualTo(original);
        var page = body(authenticated(WISHES, GET, owner, null, JsonNode.class), OK);
        assertThat(page.path("totalElements").longValue()).isEqualTo(1L);
        var content = page.path("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).path("id").longValue()).isEqualTo(original.id());
        assertThat(content.get(0).path("productId").longValue()).isEqualTo(1L);
    }

    @Test
    @DisplayName("회원이 위시를 삭제하면 해당 상품이 자신의 위시 목록에서 사라진다.")
    void removesOwnWish() {
        // given
        String owner = register("remove-wish@example.com");
        var wish = addWish(owner, 1L);

        // when
        var response = authenticated(WISHES + "/" + wish.id(), DELETE, owner, null, Void.class);

        // then
        noContent(response);
        assertThat(wishes(owner)).isEmpty();
    }

    @Test
    @DisplayName("회원이 다른 회원의 위시를 삭제하면 403 응답을 받고 소유자의 위시는 그대로 남는다.")
    void rejectsRemovalByAnotherMember() {
        // given
        String owner = register("wish-owner@example.com");
        String other = register("wish-nonowner@example.com");
        var wish = addWish(owner, 1L);
        JsonNode before = wishes(owner);

        // when
        var response = authenticated(WISHES + "/" + wish.id(), DELETE, other, null, String.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(FORBIDDEN);
        assertThat(response.getBody()).isNullOrEmpty();
        assertThat(wishes(owner)).isEqualTo(before);
    }

    @Test
    @DisplayName("인증 헤더 없이 위시 목록을 요청하면 400 응답을 받는다.")
    void rejectsMissingAuthorization() {
        // given & when
        var response = http.getForEntity(WISHES, JsonNode.class);

        // then
        assertError(response, BAD_REQUEST, WISHES);
    }

    @Test
    @DisplayName("잘못된 토큰으로 위시 목록을 요청하면 401 응답을 받는다.")
    void rejectsInvalidAuthorization() {
        // given
        String invalid = "Bearer invalid-token";

        // when
        var response = authenticated(WISHES, GET, invalid, null, String.class);

        // then
        assertThat(response.getStatusCode()).isEqualTo(UNAUTHORIZED);
        assertThat(response.getBody()).isNullOrEmpty();
    }

    @Test
    @DisplayName("회원이 유효한 토큰만 인증 헤더에 넣어도 자신의 위시 목록을 볼 수 있다.")
    void listsWishesWithBareToken() {
        // given
        String owner = register("wish-bare-token@example.com");
        var wish = addWish(owner, 1L);
        String token = owner.substring("Bearer ".length());

        // when
        var response = authenticated(WISHES, GET, token, null, JsonNode.class);

        // then
        var content = body(response, OK).path("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).path("id").longValue()).isEqualTo(wish.id());
    }

    private void assertCanUseLoginToken(String email) {
        var input = new MemberRequest(email, "test-password");
        var token = body(http.postForEntity(LOGIN, input, TokenResponse.class), OK).token();
        assertThat(token).isNotBlank();
        assertThat(wishes("Bearer " + token)).isEmpty();
    }

    private WishResponse addWish(String owner, long productId) {
        return body(authenticated(WISHES, POST, owner, new WishRequest(productId), WishResponse.class), CREATED);
    }

    private JsonNode wishes(String owner) {
        JsonNode content = body(authenticated(WISHES, GET, owner, null, JsonNode.class), OK).path("content");
        assertThat(content.isArray()).isTrue();
        return content;
    }
}
