package hexlet.code.dto;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

class UrlWithLastCheckDtoTest {

    @Test
    void testConstructorWithLastCheck() {

        Url url = new Url("https://example.com");
        url.setId(1L);
        url.setCreatedAt(Instant.now());

        UrlCheck lastCheck = new UrlCheck();
        lastCheck.setStatusCode(200);
        lastCheck.setCreatedAt(Instant.now());

        UrlWithLastCheckDto dto = new UrlWithLastCheckDto(url, lastCheck);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("https://example.com");
        assertThat(dto.getCreatedAt()).isEqualTo(url.getCreatedAt());
        assertThat(dto.getLastCheckStatusCode()).isEqualTo(200);
        assertThat(dto.getLastCheckCreatedAt()).isEqualTo(lastCheck.getCreatedAt());
    }

    @Test
    void testConstructorWithoutLastCheck() {
        Url url = new Url("https://example.com");
        url.setId(1L);
        url.setCreatedAt(Instant.now());

        UrlWithLastCheckDto dto = new UrlWithLastCheckDto(url, null);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("https://example.com");
        assertThat(dto.getCreatedAt()).isEqualTo(url.getCreatedAt());
        assertThat(dto.getLastCheckStatusCode()).isNull();
        assertThat(dto.getLastCheckCreatedAt()).isNull();
    }

    @Test
    void testGetFormattedLastCheckCreatedAt() {

        Url url = new Url("https://example.com");
        url.setId(1L);
        url.setCreatedAt(Instant.now());

        Instant checkTime = Instant.parse("2026-07-28T19:20:11Z");
        UrlCheck lastCheck = new UrlCheck();
        lastCheck.setStatusCode(200);
        lastCheck.setCreatedAt(checkTime);

        UrlWithLastCheckDto dto = new UrlWithLastCheckDto(url, lastCheck);

        String expected = checkTime.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        assertThat(dto.getFormattedLastCheckCreatedAt()).isEqualTo(expected);
    }

    @Test
    void testGetFormattedLastCheckCreatedAtWhenNull() {

        Url url = new Url("https://example.com");
        url.setId(1L);
        url.setCreatedAt(Instant.now());

        UrlWithLastCheckDto dto = new UrlWithLastCheckDto(url, null);

        assertThat(dto.getFormattedLastCheckCreatedAt()).isEmpty();
    }

    @Test
    void testGetFormattedCreatedAt() {
        Url url = new Url("https://example.com");
        url.setId(1L);
        Instant createdAt = Instant.parse("2026-07-28T19:20:11Z");
        url.setCreatedAt(createdAt);

        UrlWithLastCheckDto dto = new UrlWithLastCheckDto(url, null);

        String expected = createdAt.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
        assertThat(dto.getFormattedCreatedAt()).isEqualTo(expected);
    }

    @Test
    void testGetFormattedCreatedAtWhenNull() {
        Url url = new Url("https://example.com");
        url.setId(1L);
        url.setCreatedAt(null);

        UrlWithLastCheckDto dto = new UrlWithLastCheckDto(url, null);

        assertThat(dto.getFormattedCreatedAt()).isEmpty();
    }

    @Test
    void testAllGetters() {

        Url url = new Url("https://example.com");
        url.setId(1L);
        Instant createdAt = Instant.now();
        url.setCreatedAt(createdAt);

        UrlCheck lastCheck = new UrlCheck();
        lastCheck.setStatusCode(404);
        Instant checkTime = Instant.now();
        lastCheck.setCreatedAt(checkTime);

        UrlWithLastCheckDto dto = new UrlWithLastCheckDto(url, lastCheck);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("https://example.com");
        assertThat(dto.getCreatedAt()).isEqualTo(createdAt);
        assertThat(dto.getLastCheckStatusCode()).isEqualTo(404);
        assertThat(dto.getLastCheckCreatedAt()).isEqualTo(checkTime);
    }
}
