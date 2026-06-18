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
import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class AppTest {
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
    public void testCreateInvalidUrl() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=not-a-valid-url";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
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
    public void testCreateEmptyUrl() throws Exception {
        JavalinTest.test(app, (server, client) -> {
            String requestBody = "url=";
            try (Response response = client.post("/urls", requestBody)) {
                assertThat(response.code()).isEqualTo(200);
            }
        });
    }
}
