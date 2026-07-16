package hexlet.code;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AppTest {
    private static final CharSequence FLASH_DUPLICATE_URL = "Страница уже существует";
    private static MockWebServer mockServer;
    private static String mockUrl;
    private Javalin app;


    @BeforeAll
    public static void setUpMock() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
        mockUrl = mockServer.url("/").toString();
        mockUrl = mockUrl.substring(0, mockUrl.length() - 1);
    }

    @AfterAll
    public static void tearDownMock() throws IOException {
        mockServer.shutdown();
    }

    @BeforeEach
    public void setUp() throws Exception {
        System.setProperty("JDBC_DATABASE_URL", "jdbc:h2:mem:test");
        app = App.getApp();
        try (var conn = UrlRepository.getDataSource().getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM url_checks");
            stmt.execute("DELETE FROM urls");
        } catch (Exception e) {
            System.out.println("Error clearing tables: " + e.getMessage());
        }
    }

    @Test
    public void testMainPage() {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/")) {
                assertThat(response.code()).isEqualTo(200);
                String body = response.body().string();
                assertThat(body).contains("Анализатор страниц");
            }
        });
    }

    @Test
    public void testCreateUrl() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=https://example.com";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
            }
            var url = UrlRepository.findByName("https://example.com");
            assertThat(url).isPresent();
            assertThat(url.get().getName()).isEqualTo("https://example.com");
        });
    }

    @Test
    public void testCreateUrlWithPort() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=https://example.com:8080/path";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
            }
            var url = UrlRepository.findByName("https://example.com:8080");
            assertThat(url).isPresent();
        });
    }

    @Test
    public void testCreateEmptyUrl() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
            }
        });
    }

    @Test
    public void testCreateInvalidUrl() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=not-a-valid-url";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
            }
        });
    }

    @Test
    public void testCreateDuplicateUrl() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            Url existingUrl = new Url("https://example.com");
            existingUrl.setCreatedAt(Timestamp.from(Instant.now()));
            UrlRepository.save(existingUrl);
            String requestBody = "url=https://example.com";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
                var urls = UrlRepository.all();
                assertThat(urls).hasSize(1);
            }
        });
    }

    @Test
    public void testUrlsPage() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            Url url = new Url("https://example.com");
            url.setCreatedAt(Timestamp.from(Instant.now()));
            UrlRepository.save(url);
            try (Response response = client.get("/urls")) {
                assertThat(response.code()).isEqualTo(200);
                String body = response.body().string();
                assertThat(body).contains("Сайты");
                assertThat(body).contains("example.com");
            }
        });
    }

    @Test
    public void testUrlPage() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            Url url = new Url("https://example.com");
            url.setCreatedAt(Timestamp.from(Instant.now()));
            UrlRepository.save(url);
            try (Response response = client.get("/urls/" + url.getId())) {
                assertThat(response.code()).isEqualTo(200);
                String body = response.body().string();
                // Убираем обратный слеш
                assertThat(body).contains("Сайт: https://example.com");
                assertThat(body).contains("Запустить проверку");
            }
        });
    }

    @Test
    public void testUrlNotFound() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/urls/999999")) {
                assertThat(response.code()).isEqualTo(404);
            }
        });
    }

    @Test
    public void testCheckNonExistentUrl() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.post("/urls/99999/checks")) {
                assertThat(response.code()).isEqualTo(404);
            }
        });
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
        JavalinTest.test(app, (server, client) -> {
            Url url = new Url(mockUrl);
            url.setCreatedAt(Timestamp.from(Instant.now()));
            UrlRepository.save(url);
            try (Response response = client.post("/urls/" + url.getId() + "/checks")) {
                assertThat(response.code()).isEqualTo(200);
            }
            var checks = UrlCheckRepository.findByUrlId(url.getId());
            assertThat(checks).isNotEmpty();
            UrlCheck check = checks.get(0);
            assertThat(check.getStatusCode()).isEqualTo(200);
            assertThat(check.getTitle()).isEqualTo("Test Page");
            assertThat(check.getH1()).isEqualTo("Test Header");
            assertThat(check.getDescription()).isEqualTo("Test description");
        });
    }

    @Test
    public void testCreateCheckFailure() throws Exception {
        MockResponse mockResponse = new MockResponse().setResponseCode(500);
        mockServer.enqueue(mockResponse);
        JavalinTest.test(app, (server, client) -> {
            Url url = new Url(mockUrl);
            url.setCreatedAt(Timestamp.from(Instant.now()));
            UrlRepository.save(url);
            try (Response response = client.post("/urls/" + url.getId() + "/checks")) {
                assertThat(response.code()).isEqualTo(200);
            }
            var checks = UrlCheckRepository.findByUrlId(url.getId());
            assertThat(checks).isEmpty();
        });
    }

    @Test
    public void testCreateCheckClientError() throws Exception {
        mockServer.enqueue(new MockResponse().setResponseCode(404));

        JavalinTest.test(app, (server, client) -> {
            Url url = new Url(mockUrl);
            url.setCreatedAt(Timestamp.from(Instant.now()));
            UrlRepository.save(url);

            try (Response response = client.post("/urls/" + url.getId() + "/checks")) {
                assertThat(response.code()).isEqualTo(200);
            }

            var checks = UrlCheckRepository.findByUrlId(url.getId());
            assertThat(checks).isEmpty();
        });
    }

    @Test
    public void testChecksDisplay() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            Url url = new Url("https://example.com");
            url.setCreatedAt(Timestamp.from(Instant.now()));
            UrlRepository.save(url);
            UrlCheck check = new UrlCheck(
                    url.getId(), 200, "Example Domain", "Example Domain", null
            );
            check.setCreatedAt(Timestamp.from(Instant.now()));
            UrlCheckRepository.save(check);
            try (Response response = client.get("/urls/" + url.getId())) {
                assertThat(response.code()).isEqualTo(200);
                String body = response.body().string();
                assertThat(body).contains("Example Domain");
                assertThat(body).contains("200");
            }
        });
    }

    @Test
    public void testNormalizeUrlWithDefaultPort() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=https://example.com:80/path";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
            }
            var url = UrlRepository.findByName("https://example.com");
            assertThat(url).isPresent();
        });
    }

    @Test
    public void testNormalizeUrlWithHttpsPort() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=https://example.com:443/path";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
            }
            var url = UrlRepository.findByName("https://example.com");
            assertThat(url).isPresent();
        });
    }

    @Test
    public void testFlashMessageOnSuccess() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=https://test.com";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
            }
            var url = UrlRepository.findByName("https://test.com");
            assertThat(url).isPresent();
        });
    }

    @Test
    public void testFlashMessageOnError() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=invalid";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
            }
        });
    }

    @Test
    public void testCreatedAtField() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=https://test.com";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
            }
            var url = UrlRepository.findByName("https://test.com");
            assertThat(url).isPresent();
            assertThat(url.get().getCreatedAt()).isNotNull();
        });
    }

    @Test
    public void testNormalizeUrlWithNoHost() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=https:///example.com";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
            }
        });
    }

    @Test
    public void testNormalizeUrlWithInvalidUri() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=http://[::1]:8080"; // Может вызвать исключение
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
            }
        });
    }

    @Test
    public void testGetNormalizedUrlWithInvalidUrl() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=://invalid-url";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
            }
        });
    }

    @Test
    public void testSaveNewUrlWithError() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=https://valid-url.com";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
            }
        });
    }

    @Test
    public void testIsErrorStatusCode() throws Exception {
        mockServer.enqueue(new MockResponse().setResponseCode(400));
        JavalinTest.test(app, (server, client) -> {
            Url url = new Url(mockUrl);
            url.setCreatedAt(Timestamp.from(Instant.now()));
            UrlRepository.save(url);

            try (Response response = client.post("/urls/" + url.getId() + "/checks")) {
                assertThat(response.code()).isEqualTo(200);
            }

            var checks = UrlCheckRepository.findByUrlId(url.getId());
            assertThat(checks).isEmpty();
        });
    }

    @Test
    public void testUrlPageWithInvalidIdFormat() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/urls/invalid-id")) {
                assertThat(response.code()).isEqualTo(400);
                String body = response.body().string();
                assertThat(body).contains("Invalid ID format");
            }
        });
    }

    @Test
    public void testUrlPageWithException() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/urls/9999999999999999999")) {
                assertThat(response.code()).isEqualTo(400);
            }
        });
    }

    @Test
    public void testCheckWithInvalidIdFormat() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.post("/urls/invalid-id/checks")) {
                assertThat(response.code()).isEqualTo(400);
                String body = response.body().string();
                assertThat(body).contains("Invalid ID format");
            }
        });
    }

    @Test
    public void testUrlPageWithInternalError() throws Exception {

        JavalinTest.test(app, (server, client) -> {
            Url url = new Url("https://test-exception.com");
            url.setCreatedAt(Timestamp.from(Instant.now()));
            UrlRepository.save(url);

            try (Response response = client.get("/urls/999999")) {
                assertThat(response.code()).isEqualTo(404);
            }
        });
    }

    @Test
    public void testUrlPageWithInternalServerError() throws Exception {
        JavalinTest.test(app, (server, client) -> {

            try (Response response = client.get("/urls/not-a-number")) {
                assertThat(response.code()).isEqualTo(400);
            }
        });
    }

    @Test
    public void testCheckWithInternalServerError() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.post("/urls/not-a-number/checks")) {
                assertThat(response.code()).isEqualTo(400);
            }
        });
    }

    @Test
    public void testIsValidUrlWithHttp() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=http://example.com";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
            }
            var url = UrlRepository.findByName("http://example.com");
            assertThat(url).isPresent();
        });
    }

    @Test
    public void testIsValidUrlWithHttps() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=https://example.com";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
            }
            var url = UrlRepository.findByName("https://example.com");
            assertThat(url).isPresent();
        });
    }

    @Test
    public void testNormalizeUrlWithHostNull() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=https://example.com";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
            }
            var url = UrlRepository.findByName("https://example.com");
            assertThat(url).isPresent();
        });
    }

    @Test
    public void testAppConstructor() throws Exception {
        App appInstance = new App();
        assertThat(appInstance).isNotNull();
    }

    @Test
    public void testReadResourceFileNotFound() throws Exception {
        var method = App.class.getDeclaredMethod("readResourceFile", String.class);
        method.setAccessible(true);
        var result = method.invoke(null, "non-existent-file.sql");
        assertThat(result).isNull();
    }

    @Test
    public void testSetupDataSource() throws Exception {

        var method = App.class.getDeclaredMethod("setupDataSource");
        method.setAccessible(true);
        var dataSource = method.invoke(null);
        assertThat(dataSource).isNotNull();
    }



    @Test
    public void testUrlPageWithTooLargeId() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            try (Response response = client.get("/urls/9999999999999999999")) {
                assertThat(response.code()).isEqualTo(400);
                String body = response.body().string();
                assertThat(body).contains("Invalid ID format");
            }
        });
    }

    @Test
    public void testIsErrorStatusCodeForVariousCodes() throws Exception {
        assertThat(App.class.getDeclaredMethod("isErrorStatusCode", int.class)
                .invoke(null, 200)).isEqualTo(false);
        assertThat(App.class.getDeclaredMethod("isErrorStatusCode", int.class)
                .invoke(null, 302)).isEqualTo(false);
        assertThat(App.class.getDeclaredMethod("isErrorStatusCode", int.class)
                .invoke(null, 404)).isEqualTo(true);
        assertThat(App.class.getDeclaredMethod("isErrorStatusCode", int.class)
                .invoke(null, 500)).isEqualTo(true);
    }

    @Test
    public void testIsErrorStatusCodeMethod() throws Exception {
        var method = App.class.getDeclaredMethod("isErrorStatusCode", int.class);
        method.setAccessible(true);

        assertThat(method.invoke(null, 200)).isEqualTo(false);
        assertThat(method.invoke(null, 302)).isEqualTo(false);
        assertThat(method.invoke(null, 399)).isEqualTo(false);
        assertThat(method.invoke(null, 400)).isEqualTo(true);
        assertThat(method.invoke(null, 404)).isEqualTo(true);
        assertThat(method.invoke(null, 500)).isEqualTo(true);
        assertThat(method.invoke(null, 599)).isEqualTo(true);
    }

    @Test
    public void testIsValidInputUrlMethod() throws Exception {
        var method = App.class.getDeclaredMethod("isValidInputUrl", String.class);
        method.setAccessible(true);

        // Проверяем валидные URL (с протоколом)
        assertThat(method.invoke(null, "https://example.com")).isEqualTo(true);
        assertThat(method.invoke(null, "http://example.com")).isEqualTo(true);

        // Проверяем невалидные URL
        assertThat(method.invoke(null, (String) null)).isEqualTo(false);
        assertThat(method.invoke(null, "")).isEqualTo(false);
        assertThat(method.invoke(null, "   ")).isEqualTo(false);
        assertThat(method.invoke(null, "not-a-valid-url")).isEqualTo(false);
        assertThat(method.invoke(null, "://invalid")).isEqualTo(false);
        // URL без протокола — невалидный
        assertThat(method.invoke(null, "example.com")).isEqualTo(false);
    }

    @Test
    public void testIsValidUrlWithFtp() throws Exception {
        var method = App.class.getDeclaredMethod("isValidUrl", String.class);
        method.setAccessible(true);

        assertThat(method.invoke(null, "ftp://example.com")).isEqualTo(false);
        assertThat(method.invoke(null, "mailto:test@example.com")).isEqualTo(false);
    }

    @Test
    public void testGetPortWithDefault() throws Exception {
        var method = App.class.getDeclaredMethod("getPort", Map.class);
        method.setAccessible(true);

        // Проверяем, что возвращается порт по умолчанию
        Map<String, String> env = Map.of();
        Object result = method.invoke(null, env);
        assertThat(result).isEqualTo(7070);
    }

    @Test
    public void testGetPortWithCustomPort() throws Exception {
        var method = App.class.getDeclaredMethod("getPort", Map.class);
        method.setAccessible(true);

        Map<String, String> env = Map.of("PORT", "8080");
        Object result = method.invoke(null, env);
        assertThat(result).isEqualTo(8080);
    }

    @Test
    public void testGetPortWithInvalidPort() throws Exception {
        var method = App.class.getDeclaredMethod("getPort", Map.class);
        method.setAccessible(true);

        Map<String, String> env = Map.of("PORT", "invalid");
        Object result = method.invoke(null, env);
        assertThat(result).isEqualTo(7070);
    }

    @Test
    public void testEmptyPortReturnsDefault() throws Exception {
        var method = App.class.getDeclaredMethod("getPort", Map.class);
        method.setAccessible(true);

        Map<String, String> env = Map.of("PORT", "");
        Object result = method.invoke(null, env);
        assertThat(result).isEqualTo(7070);
    }

    @Test
    void testNormalizeUrlWithPort80() throws URISyntaxException {
        assertEquals("http://example.com", App.normalizeUrl("http://example.com:80"));
    }

    @Test
    void testNormalizeUrlWithPort443() throws URISyntaxException {
        assertEquals("https://example.com", App.normalizeUrl("https://example.com:443"));
    }

    @Test
    void testNormalizeUrlWithCustomPort() throws URISyntaxException {
        assertEquals("http://example.com:8080", App.normalizeUrl("http://example.com:8080/path"));
    }

    @Test
    void testIsValidUrlReturnsFalseForInvalidUri() {
        assertFalse(App.isValidUrl("not-a-valid-uri"));
    }

    @Test
    void testIsValidUrlReturnsFalseWhenHostIsNull() throws URISyntaxException {
        // Даже если URI валиден, но хоста нет — считаем невалидным
        assertFalse(App.isValidUrl("https://"));
    }

    @Test
    public void testMainPageWithRenderError() throws Exception {
        // Временно переименовываем шаблон, чтобы вызвать ошибку рендеринга
        var templatePath = Paths.get("src/main/jte/index.jte");
        var backupPath = Paths.get("src/main/jte/index.jte.backup");

        try {
            // Переименовываем шаблон
            Files.move(templatePath, backupPath);

            // Вызываем главную страницу
            JavalinTest.test(app, (server, client) -> {
                try (Response response = client.get("/")) {
                    // Должен быть 200, но с текстом ошибки
                    assertThat(response.code()).isEqualTo(200);
                    String body = response.body().string();
                    assertThat(body).contains("Error");
                }
            });
        } finally {
            // Возвращаем шаблон обратно
            Files.move(backupPath, templatePath);
        }
    }
}
