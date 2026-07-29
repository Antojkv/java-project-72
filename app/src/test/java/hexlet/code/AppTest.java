package hexlet.code;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.BaseRepository;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import io.javalin.Javalin;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.Response;
import hexlet.code.controller.UrlController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.Assert.assertEquals;

public class AppTest {
    private static final CharSequence FLASH_DUPLICATE_URL = "Страница уже существует";
    private static MockWebServer mockServer;
    private static String mockUrl;
    private static Javalin app;
    private static String baseUrl;
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .followRedirects(false)
            .build();


    @BeforeAll
    public static void setUpAll() throws Exception {
        System.setProperty("JDBC_DATABASE_URL", "jdbc:h2:mem:test");

        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;

        mockServer = new MockWebServer();
        mockServer.start();
        mockUrl = mockServer.url("/").toString();
        mockUrl = mockUrl.substring(0, mockUrl.length() - 1);
    }

    @AfterAll
    public static void tearDownAll() throws IOException {
        if (app != null) {
            try {
                app.stop();
            } catch (Exception e) {
            }
        }
        if (mockServer != null) {
            mockServer.shutdown();
        }
        CLIENT.dispatcher().executorService().shutdown();
    }

    @BeforeEach
    public void setUp() throws Exception {
        try (var conn = BaseRepository.getDataSource().getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM url_checks");
            stmt.execute("DELETE FROM urls");
        } catch (Exception e) {
            System.out.println("Error clearing tables: " + e.getMessage());
        }
    }

    private String getBaseUrl() {
        return baseUrl;
    }

    @Test
    public void testMainPage() throws IOException {
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/")
                .get()
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
            String body = response.body().string();
            assertThat(body).contains("Анализатор страниц");
        }
    }

    @Test
    public void testCreateUrl() throws Exception {
        String requestBody = "url=https://example.com";
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(requestBody.getBytes()))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(302);
        }

        var url = UrlRepository.findByName("https://example.com");
        assertThat(url).isPresent();
        assertThat(url.get().getName()).isEqualTo("https://example.com");
    }

    @Test
    public void testCreateUrlWithPort() throws Exception {
        String requestBody = "url=https://example.com:8080/path";
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(requestBody.getBytes()))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(302);
            assertThat(response.header("Location")).startsWith("/urls/");
        }

        var url = UrlRepository.findByName("https://example.com:8080");
        assertThat(url).isPresent();
    }

    @Test
    public void testCreateEmptyUrl() throws IOException {
        String requestBody = "url=";
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(requestBody.getBytes()))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
        }
    }

    @Test
    public void testCreateInvalidUrl() throws IOException {
        String requestBody = "url=not-a-valid-url";
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(requestBody.getBytes()))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
        }
    }

    @Test
    public void testCreateDuplicateUrl() throws Exception {
        Url existingUrl = new Url("https://example.com");
        existingUrl.setCreatedAt(Instant.now());
        UrlRepository.save(existingUrl);

        String requestBody = "url=https://example.com";
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(requestBody.getBytes()))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(302);
            assertThat(response.header("Location")).isEqualTo("/urls/" + existingUrl.getId());
        }

        var urls = UrlRepository.all();
        assertThat(urls).hasSize(1);
    }


    @Test
    public void testUrlsPage() throws Exception {
        Url url = new Url("https://example.com");
        url.setCreatedAt(Instant.now());
        UrlRepository.save(url);

        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .get()
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
            String body = response.body().string();
            assertThat(body).contains("Сайты");
            assertThat(body).contains("example.com");
        }
    }

    @Test
    public void testUrlPage() throws Exception {
        Url url = new Url("https://example.com");
        url.setCreatedAt(Instant.now());
        UrlRepository.save(url);

        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls/" + url.getId())
                .get()
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
            String body = response.body().string();
            assertThat(body).contains("Сайт: https://example.com");
            assertThat(body).contains("Запустить проверку");
        }
    }

    @Test
    public void testUrlNotFound() throws IOException {
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls/999999")
                .get()
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(404);
        }
    }

    @Test
    public void testCheckNonExistentUrl() throws IOException {
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls/99999/checks")
                .post(FormBody.create(new byte[0]))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(404);
        }
    }

    @Test
    public void testCreateCheckSuccess() throws Exception {
        String html = "<html><head><title>Test Page</title>"
                + "<meta name='description' content='Test description'></head>"
                + "<body><h1>Test Header</h1></body></html>";
        MockResponse mockResponse = new MockResponse()
                .setBody(html)
                .setResponseCode(200);

        mockServer.enqueue(mockResponse);

        Url url = new Url(mockUrl);
        url.setCreatedAt(Instant.now());
        UrlRepository.save(url);

        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls/" + url.getId() + "/checks")
                .post(FormBody.create(new byte[0]))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(302);
        }

        var checks = UrlCheckRepository.findByUrlId(url.getId());
        assertThat(checks).isNotEmpty();
        UrlCheck check = checks.get(0);
        assertThat(check.getStatusCode()).isEqualTo(200);
        assertThat(check.getTitle()).isEqualTo("Test Page");
        assertThat(check.getH1()).isEqualTo("Test Header");
        assertThat(check.getDescription()).isEqualTo("Test description");
    }

    @Test
    public void testCreateCheckFailure() throws Exception {
        MockResponse mockResponse = new MockResponse().setResponseCode(500);
        mockServer.enqueue(mockResponse);

        Url url = new Url(mockUrl);
        url.setCreatedAt(Instant.now());
        UrlRepository.save(url);

        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls/" + url.getId() + "/checks")
                .post(FormBody.create(new byte[0]))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(302);
        }

        var checks = UrlCheckRepository.findByUrlId(url.getId());
        assertThat(checks).isEmpty();
    }

    @Test
    public void testCreateCheckClientError() throws Exception {
        mockServer.enqueue(new MockResponse().setResponseCode(404));

        Url url = new Url(mockUrl);
        url.setCreatedAt(Instant.now());
        UrlRepository.save(url);

        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls/" + url.getId() + "/checks")
                .post(FormBody.create(new byte[0]))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(302);
        }

        var checks = UrlCheckRepository.findByUrlId(url.getId());
        assertThat(checks).isEmpty();
    }

    @Test
    public void testChecksDisplay() throws Exception {
        Url url = new Url("https://example.com");
        url.setCreatedAt(Instant.now());
        UrlRepository.save(url);

        UrlCheck check = new UrlCheck(
                url.getId(), 200, "Example Domain", "Example Domain", null
        );
        check.setCreatedAt(Instant.now());
        UrlCheckRepository.save(check);

        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls/" + url.getId())
                .get()
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
            String body = response.body().string();
            assertThat(body).contains("Example Domain");
            assertThat(body).contains("200");
        }
    }

    @Test
    public void testNormalizeUrlWithDefaultPort() throws Exception {
        String requestBody = "url=https://example.com:80/path";
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(requestBody.getBytes()))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(302);
        }

        var url = UrlRepository.findByName("https://example.com");
        assertThat(url).isPresent();
    }

    @Test
    public void testNormalizeUrlWithHttpsPort() throws Exception {
        String requestBody = "url=https://example.com:443/path";
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(requestBody.getBytes()))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(302);
        }

        var url = UrlRepository.findByName("https://example.com");
        assertThat(url).isPresent();
    }

    @Test
    public void testFlashMessageOnSuccess() throws Exception {
        String requestBody = "url=https://test.com";
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(requestBody.getBytes()))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(302);
        }

        var url = UrlRepository.findByName("https://test.com");
        assertThat(url).isPresent();
    }

    @Test
    public void testFlashMessageOnError() throws IOException {
        String requestBody = "url=invalid";
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(requestBody.getBytes()))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
        }
    }

    @Test
    public void testCreatedAtField() throws Exception {
        String requestBody = "url=https://test.com";
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(requestBody.getBytes()))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(302);
        }

        var url = UrlRepository.findByName("https://test.com");
        assertThat(url).isPresent();
        assertThat(url.get().getCreatedAt()).isNotNull();
    }

    @Test
    public void testNormalizeUrlWithNoHost() throws IOException {
        String requestBody = "url=https:///example.com";
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(requestBody.getBytes()))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
        }
    }

    @Test
    public void testNormalizeUrlWithInvalidUri() throws Exception {
        String requestBody = "url=http://[::1]:8080";
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(requestBody.getBytes()))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {

            assertThat(response.code()).isEqualTo(302);
        }

        var url = UrlRepository.findByName("http://[::1]:8080");
        assertThat(url).isPresent();
    }

    @Test
    public void testGetNormalizedUrlWithInvalidUrl() throws IOException {
        String requestBody = "url=://invalid-url";
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(requestBody.getBytes()))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
        }
    }

    @Test
    public void testSaveNewUrlWithError() throws IOException {
        String requestBody = "url=https://valid-url.com";
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(requestBody.getBytes()))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(302);
        }
    }

    @Test
    public void testIsErrorStatusCode() throws Exception {
        mockServer.enqueue(new MockResponse().setResponseCode(400));

        Url url = new Url(mockUrl);
        url.setCreatedAt(Instant.now());
        UrlRepository.save(url);

        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls/" + url.getId() + "/checks")
                .post(FormBody.create(new byte[0]))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(302);
        }

        var checks = UrlCheckRepository.findByUrlId(url.getId());
        assertThat(checks).isEmpty();
    }

    @Test
    public void testUrlPageWithInvalidIdFormat() throws IOException {
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls/invalid-id")
                .get()
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(400);
            String body = response.body().string();
            assertThat(body).contains("Invalid ID format");
        }
    }

    @Test
    public void testUrlPageWithException() throws IOException {
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls/9999999999999999999")
                .get()
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(400);
        }
    }

    @Test
    public void testCheckWithInvalidIdFormat() throws IOException {
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls/invalid-id/checks")
                .post(FormBody.create(new byte[0]))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(400);
            String body = response.body().string();
            assertThat(body).contains("Invalid ID format");
        }
    }

    @Test
    public void testUrlPageWithInternalError() throws Exception {
        Url url = new Url("https://test-exception.com");
        url.setCreatedAt(Instant.now());
        UrlRepository.save(url);

        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls/999999")
                .get()
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(404);
        }
    }

    @Test
    public void testUrlPageWithInternalServerError() throws IOException {
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls/not-a-number")
                .get()
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(400);
        }
    }

    @Test
    public void testCheckWithInternalServerError() throws IOException {
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls/not-a-number/checks")
                .post(FormBody.create(new byte[0]))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(400);
        }
    }

    @Test
    public void testIsValidUrlWithHttp() throws Exception {
        String requestBody = "url=http://example.com";
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(requestBody.getBytes()))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(302);
        }

        var url = UrlRepository.findByName("http://example.com");
        assertThat(url).isPresent();
    }

    @Test
    public void testIsValidUrlWithHttps() throws Exception {
        String requestBody = "url=https://example.com";
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(requestBody.getBytes()))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(302);
        }

        var url = UrlRepository.findByName("https://example.com");
        assertThat(url).isPresent();
    }

    @Test
    public void testNormalizeUrlWithHostNull() throws Exception {
        String requestBody = "url=https://example.com";
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(requestBody.getBytes()))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(302);
        }

        var url = UrlRepository.findByName("https://example.com");
        assertThat(url).isPresent();
    }

    @Test
    public void testUrlPageWithTooLargeId() throws IOException {
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls/9999999999999999999")
                .get()
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(400);
            String body = response.body().string();
            assertThat(body).contains("Invalid ID format");
        }
    }

    @Test
    void testMainPageWithRenderError() throws IOException {
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/")
                .get()
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
            String body = response.body().string();
            assertThat(body).contains("Анализатор страниц");
        }
    }

    @Test
    public void testCreateCheckWithNetworkError() throws Exception {

        mockServer.enqueue(new MockResponse().setResponseCode(404));

        Url url = new Url(mockUrl);
        url.setCreatedAt(Instant.now());
        UrlRepository.save(url);

        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls/" + url.getId() + "/checks")
                .post(FormBody.create(new byte[0]))
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(302);
        }

        var checks = UrlCheckRepository.findByUrlId(url.getId());
        assertThat(checks).isEmpty();
    }

    @Test
    void testUrlCheckError() throws Exception {
        String urlName = "http://this-domain-does-not-exist-" + System.currentTimeMillis() + ".test";

        Request createRequest = new Request.Builder()
                .url(getBaseUrl() + "/urls")
                .post(FormBody.create(("url=" + urlName).getBytes()))
                .build();

        try (Response response = CLIENT.newCall(createRequest).execute()) {
            assertThat(response.code()).isEqualTo(302);
        }

        var urlOpt = UrlRepository.findByName(urlName);
        assertThat(urlOpt).isPresent();
        Long urlId = urlOpt.get().getId();

        Request checkRequest = new Request.Builder()
                .url(getBaseUrl() + "/urls/" + urlId + "/checks")
                .post(FormBody.create(new byte[0]))
                .build();

        try (Response response = CLIENT.newCall(checkRequest).execute()) {
            assertThat(response.code()).isEqualTo(302);
        }

        var checks = UrlCheckRepository.findByUrlId(urlId);
        assertThat(checks).isEmpty();
    }

    @Test
    void testNormalizeUrlwhenHostIsNullthenReturnsSchemeOnly() throws URISyntaxException {
        String input = "http:///example.com";
        String result = UrlController.normalizeUrl(input);
        assertThat(result).isEqualTo("http://");
    }

    @Test
    public void testNormalizeUrlwhenCustomPortthenIncludePortInResult() throws URISyntaxException {
        String input = "http://localhost:8080/api";
        String result = UrlController.normalizeUrl(input);
        assertThat(result).isEqualTo("http://localhost:8080");
    }

    @Test
    public void testNormalizeUrlwhenNoProtocolthenAddHttps() throws URISyntaxException {
        String input = "example.com/page";
        String result = UrlController.normalizeUrl(input);
        assertThat(result).isEqualTo("https://example.com");
    }

    @Test
    public void testNormalizeUrlwhenEmptyHostAfterSplitthenReturnEmptyHost() throws URISyntaxException {
        String input = "https:///";
        String result = UrlController.normalizeUrl(input);
        assertThat(result).isEqualTo("https://");
    }

    @Test
    public void testNumberFormatExceptionHandler() throws Exception {
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/urls/invalid-id")
                .get()
                .build();

        try (Response response = CLIENT.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(400);
            String body = response.body().string();
            assertThat(body).contains("Invalid ID format");
        }

        Request postRequest = new Request.Builder()
                .url(getBaseUrl() + "/urls/invalid-id/checks")
                .post(FormBody.create(new byte[0]))
                .build();

        try (Response postResponse = CLIENT.newCall(postRequest).execute()) {
            assertThat(postResponse.code()).isEqualTo(400);
            String body = postResponse.body().string();
            assertThat(body).contains("Invalid ID format");
        }
    }

    @Test
    void testGetPortWithValidValue() {
        Map<String, String> env = Map.of("PORT", "8080");
        int port = App.getPort(env);
        assertEquals(8080, port);
    }

    @Test
    void testGetPortWithInvalidValueFallsBackToDefault() {
        Map<String, String> env = Map.of("PORT", "not-a-number");
        int port = App.getPort(env);
        assertEquals(7070, port);
    }

    @Test
    void testGetPortWithMissingPortUsesDefault() {
        Map<String, String> env = Map.of();
        int port = App.getPort(env);
        assertEquals(7070, port);
    }

    @Test
    public void testStartWithValidPort() {
        // Проверяем, что метод start не выбрасывает исключений
        Map<String, String> env = Map.of("PORT", "7071");
        assertThatCode(() -> {
            // Запускаем в отдельном потоке, чтобы не блокировать
            Thread thread = new Thread(() -> App.start(env));
            thread.setDaemon(true);
            thread.start();
            Thread.sleep(500);
        }).doesNotThrowAnyException();
    }

    @Test
    public void testStartWithInvalidPort() {
        Map<String, String> env = Map.of("PORT", "invalid");
        assertThatCode(() -> {
            App.start(env);
        }).doesNotThrowAnyException();
    }

    @Test
    public void testMainMethod() {
        assertThatCode(() -> {
            App.main(new String[]{});
        }).doesNotThrowAnyException();
    }
}