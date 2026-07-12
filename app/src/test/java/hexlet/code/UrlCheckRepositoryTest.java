package hexlet.code;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class UrlCheckRepositoryTest {
    private static HikariDataSource dataSource;
    private Url url;

    @BeforeEach
    public void setUp() throws Exception {
        System.setProperty("JDBC_DATABASE_URL", "jdbc:h2:mem:test");

        var hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:h2:mem:test");
        dataSource = new HikariDataSource(hikariConfig);
        UrlRepository.setDataSource(dataSource);
        UrlCheckRepository.setDataSource(dataSource);

        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS urls ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "name VARCHAR(255) NOT NULL, "
                    + "created_at TIMESTAMP NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS url_checks ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "url_id BIGINT NOT NULL, "
                    + "status_code INTEGER, "
                    + "h1 VARCHAR(255), "
                    + "title VARCHAR(255), "
                    + "description TEXT, "
                    + "created_at TIMESTAMP NOT NULL, "
                    + "FOREIGN KEY (url_id) REFERENCES urls(id) ON DELETE CASCADE)");
        }

        url = new Url("https://example.com");
        url.setCreatedAt(Timestamp.from(Instant.now()));
        UrlRepository.save(url);
    }

    @AfterEach
    public void tearDown() throws Exception {
        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS url_checks");
            stmt.execute("DROP TABLE IF EXISTS urls");
        }
        dataSource.close();
    }

    @Test
    public void testSaveCheck() throws Exception {
        UrlCheck check = new UrlCheck(url.getId(), 200, "Test H1", "Test Title", "Test Description");
        UrlCheckRepository.save(check);

        var checks = UrlCheckRepository.findByUrlId(url.getId());
        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getStatusCode()).isEqualTo(200);
        assertThat(checks.get(0).getH1()).isEqualTo("Test H1");
        assertThat(checks.get(0).getTitle()).isEqualTo("Test Title");
        assertThat(checks.get(0).getDescription()).isEqualTo("Test Description");
        assertThat(checks.get(0).getUrlId()).isEqualTo(url.getId());
        assertThat(checks.get(0).getCreatedAt()).isNotNull();
    }

    @Test
    public void testFindByUrlId() throws Exception {
        UrlCheck check1 = new UrlCheck(url.getId(), 200, "H1 1", "Title 1", "Desc 1");
        UrlCheck check2 = new UrlCheck(url.getId(), 404, "H1 2", "Title 2", "Desc 2");
        UrlCheckRepository.save(check1);
        UrlCheckRepository.save(check2);

        var checks = UrlCheckRepository.findByUrlId(url.getId());
        assertThat(checks).hasSize(2);
        assertThat(checks.get(0).getId()).isGreaterThan(checks.get(1).getId());
    }

    @Test
    public void testFindLastByUrlId() throws Exception {
        UrlCheck check1 = new UrlCheck(url.getId(), 200, "H1 1", "Title 1", "Desc 1");
        UrlCheck check2 = new UrlCheck(url.getId(), 404, "H1 2", "Title 2", "Desc 2");
        UrlCheckRepository.save(check1);
        UrlCheckRepository.save(check2);

        var lastCheck = UrlCheckRepository.findLastByUrlId(url.getId());
        assertThat(lastCheck).isNotNull();
        assertThat(lastCheck.getStatusCode()).isEqualTo(404);
        assertThat(lastCheck.getH1()).isEqualTo("H1 2");
    }

    @Test
    public void testFindLastByUrlIdWhenNoChecks() throws Exception {
        var lastCheck = UrlCheckRepository.findLastByUrlId(url.getId());
        assertThat(lastCheck).isNull();
    }

    @Test
    public void testFindByUrlIdWhenNoChecks() throws Exception {
        var checks = UrlCheckRepository.findByUrlId(url.getId());
        assertThat(checks).isEmpty();
    }

    @Test
    public void testSaveCheckWithNullFields() throws Exception {
        UrlCheck check = new UrlCheck(url.getId(), 200, null, null, null);
        UrlCheckRepository.save(check);

        var checks = UrlCheckRepository.findByUrlId(url.getId());
        assertThat(checks).hasSize(1);
        assertThat(checks.get(0).getH1()).isNull();
        assertThat(checks.get(0).getTitle()).isNull();
        assertThat(checks.get(0).getDescription()).isNull();
    }
}
