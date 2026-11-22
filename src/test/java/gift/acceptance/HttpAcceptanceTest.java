package gift.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import gift.auth.TokenResponse;
import gift.member.MemberRequest;
import gift.option.OptionRequest;
import gift.option.OptionResponse;
import gift.product.ProductRequest;
import gift.product.ProductResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;

import java.net.http.HttpClient;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
abstract class HttpAcceptanceTest {
    private final TestRestTemplate formHttp = new TestRestTemplate();

    HttpAcceptanceTest() {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        formHttp.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory(client));
    }

    @Autowired
    protected TestRestTemplate http;

    protected <T> T body(ResponseEntity<T> response, HttpStatus status) {
        assertThat(response.getStatusCode()).isEqualTo(status);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    protected void noContent(ResponseEntity<Void> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    protected <T> ResponseEntity<T> request(String path, HttpMethod method, Object input, Class<T> type) {
        return http.exchange(path, method, new HttpEntity<>(input), type);
    }

    protected <T> ResponseEntity<T> authenticated(
        String path, HttpMethod method, String authorization, Object input, Class<T> type
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorization);
        return http.exchange(path, method, new HttpEntity<>(input, headers), type);
    }

    protected String register(String email) {
        TokenResponse credentials = body(http.postForEntity(
            "/api/members/register", new MemberRequest(email, "test-password"), TokenResponse.class
        ), HttpStatus.CREATED);
        assertThat(credentials.token()).isNotBlank();
        return "Bearer " + credentials.token();
    }

    protected ProductResponse createProduct(String name, int price) {
        return body(http.postForEntity("/api/products", new ProductRequest(
            name, price, "https://example.com/test.jpg", 1L
        ), ProductResponse.class), HttpStatus.CREATED);
    }

    protected OptionResponse createOption(String path, String name, int quantity) {
        return body(http.postForEntity(path, new OptionRequest(name, quantity), OptionResponse.class),
            HttpStatus.CREATED);
    }

    protected ResponseEntity<String> submitForm(String path, Map<String, String> fields) {
        var form = new LinkedMultiValueMap<String, String>();
        fields.forEach(form::add);
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return formHttp.postForEntity(http.getRootUri() + path, new HttpEntity<>(form, headers), String.class);
    }

    protected void redirectsTo(ResponseEntity<String> response, String path) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).isNotNull();
        assertThat(response.getHeaders().getLocation().getPath()).isEqualTo(path);
    }

    protected String html(String path) {
        ResponseEntity<String> response = http.getForEntity(path, String.class);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_HTML)).isTrue();
        return body(response, HttpStatus.OK);
    }

    protected long listedId(String path, String label) {
        var row = Pattern.compile("<tr>\\s*<td>(\\d+)</td>\\s*<td>" + Pattern.quote(label) + "</td>")
            .matcher(html(path));
        assertThat(row.find()).as("목록에서 %s의 ID 찾기", label).isTrue();
        return Long.parseLong(row.group(1));
    }

    protected int memberPoints(long id, String email) {
        var row = Pattern.compile("<tr>\\s*<td>" + id + "</td>\\s*<td>"
            + Pattern.quote(email) + "</td>\\s*<td>(\\d+)</td>").matcher(html("/admin/members"));
        assertThat(row.find()).as("회원 %s의 포인트 찾기", email).isTrue();
        return Integer.parseInt(row.group(1));
    }

    protected void assertError(ResponseEntity<JsonNode> response, HttpStatus status, String path) {
        JsonNode error = body(response, status);
        assertThat(error.path("status").intValue()).isEqualTo(status.value());
        assertThat(error.path("path").textValue()).isEqualTo(path);
    }
}
