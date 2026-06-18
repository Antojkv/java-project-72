package hexlet.code;

import hexlet.code.model.UrlCheck;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlCheckTest {

    @Test
    public void testUrlCheckConstructor() {
        Long urlId = 1L;
        Integer statusCode = 200;
        String h1 = "Test Header";
        String title = "Test Title";
        String description = "Test Description";

        UrlCheck check = new UrlCheck(urlId, statusCode, h1, title, description);

        assertThat(check.getUrlId()).isEqualTo(urlId);
        assertThat(check.getStatusCode()).isEqualTo(statusCode);
        assertThat(check.getH1()).isEqualTo(h1);
        assertThat(check.getTitle()).isEqualTo(title);
        assertThat(check.getDescription()).isEqualTo(description);
        assertThat(check.getCreatedAt()).isNotNull();
    }

    @Test
    public void testUrlCheckAllArgsConstructor() {
        Long id = 1L;
        Long urlId = 1L;
        Integer statusCode = 200;
        String h1 = "Test Header";
        String title = "Test Title";
        String description = "Test Description";
        Timestamp createdAt = Timestamp.from(Instant.now());

        UrlCheck check = new UrlCheck(id, urlId, statusCode, h1, title, description, createdAt);

        assertThat(check.getId()).isEqualTo(id);
        assertThat(check.getUrlId()).isEqualTo(urlId);
        assertThat(check.getStatusCode()).isEqualTo(statusCode);
        assertThat(check.getH1()).isEqualTo(h1);
        assertThat(check.getTitle()).isEqualTo(title);
        assertThat(check.getDescription()).isEqualTo(description);
        assertThat(check.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    public void testUrlCheckNoArgsConstructor() {
        UrlCheck check = new UrlCheck();
        check.setId(1L);
        check.setUrlId(1L);
        check.setStatusCode(200);
        check.setH1("Test Header");
        check.setTitle("Test Title");
        check.setDescription("Test Description");
        check.setCreatedAt(Timestamp.from(Instant.now()));

        assertThat(check.getId()).isEqualTo(1L);
        assertThat(check.getUrlId()).isEqualTo(1L);
        assertThat(check.getStatusCode()).isEqualTo(200);
        assertThat(check.getH1()).isEqualTo("Test Header");
        assertThat(check.getTitle()).isEqualTo("Test Title");
        assertThat(check.getDescription()).isEqualTo("Test Description");
        assertThat(check.getCreatedAt()).isNotNull();
    }

    @Test
    public void testUrlCheckTruncateLongText() {
        // Создаём очень длинный текст
        String longText = "a".repeat(250);
        String expected = "a".repeat(197) + "...";

        UrlCheck check = new UrlCheck(1L, 200, longText, longText, longText);

        assertThat(check.getH1()).isEqualTo(expected);
        assertThat(check.getTitle()).isEqualTo(expected);
        assertThat(check.getDescription()).isEqualTo(expected);
        assertThat(check.getH1().length()).isEqualTo(200);
    }

    @Test
    public void testUrlCheckTruncateNull() {
        UrlCheck check = new UrlCheck(1L, 200, null, null, null);

        assertThat(check.getH1()).isNull();
        assertThat(check.getTitle()).isNull();
        assertThat(check.getDescription()).isNull();
    }

    @Test
    public void testUrlCheckTruncateShortText() {
        String shortText = "Short text";
        UrlCheck check = new UrlCheck(1L, 200, shortText, shortText, shortText);

        assertThat(check.getH1()).isEqualTo(shortText);
        assertThat(check.getTitle()).isEqualTo(shortText);
        assertThat(check.getDescription()).isEqualTo(shortText);
    }

    @Test
    public void testUrlCheckGettersAndSetters() {
        UrlCheck check = new UrlCheck();
        Long id = 1L;
        Long urlId = 2L;
        Integer statusCode = 404;
        String h1 = "Not Found";
        String title = "404 Page";
        String description = "Page not found";
        Timestamp createdAt = Timestamp.from(Instant.now());

        check.setId(id);
        check.setUrlId(urlId);
        check.setStatusCode(statusCode);
        check.setH1(h1);
        check.setTitle(title);
        check.setDescription(description);
        check.setCreatedAt(createdAt);

        assertThat(check.getId()).isEqualTo(id);
        assertThat(check.getUrlId()).isEqualTo(urlId);
        assertThat(check.getStatusCode()).isEqualTo(statusCode);
        assertThat(check.getH1()).isEqualTo(h1);
        assertThat(check.getTitle()).isEqualTo(title);
        assertThat(check.getDescription()).isEqualTo(description);
        assertThat(check.getCreatedAt()).isEqualTo(createdAt);
    }
}
