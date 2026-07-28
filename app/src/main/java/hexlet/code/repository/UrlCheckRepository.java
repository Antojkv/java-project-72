package hexlet.code.repository;

import hexlet.code.model.UrlCheck;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static hexlet.code.repository.BaseRepository.dataSource;

public class UrlCheckRepository {
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_URL_ID = "url_id";
    private static final String COLUMN_STATUS_CODE = "status_code";

    public static void save(UrlCheck check) throws SQLException {

        if (check.getCreatedAt() == null) {
            check.setCreatedAt(Instant.now());
        }

        String sql = "INSERT INTO url_checks (url_id, status_code, h1, title, description, "
                + COLUMN_CREATED_AT + ") VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, check.getUrlId());
            stmt.setInt(2, check.getStatusCode());
            stmt.setString(3, check.getH1());
            stmt.setString(4, check.getTitle());
            stmt.setString(5, check.getDescription());
            stmt.setTimestamp(6, check.getCreatedAt() != null ? Timestamp.from(check.getCreatedAt()) : null);
            stmt.executeUpdate();

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                check.setId(generatedKeys.getLong(1));
            }
        }
    }

    public static List<UrlCheck> findByUrlId(Long urlId) throws SQLException {
        List<UrlCheck> checks = new ArrayList<>();
        String sql = "SELECT * FROM url_checks WHERE " + COLUMN_URL_ID + " = ? ORDER BY id DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, urlId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                checks.add(mapRowToUrlCheck(rs));
            }
        }
        return checks;
    }

    public static UrlCheck findLastByUrlId(Long urlId) throws SQLException {
        String sql = "SELECT * FROM url_checks WHERE " + COLUMN_URL_ID + " = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, urlId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowToUrlCheck(rs);
            }
        }
        return null;
    }

    public static Map<Long, UrlCheck> findLatestChecksForAllUrls() throws SQLException {
        var sql = """
        SELECT DISTINCT ON (url_id) *
        FROM url_checks
        ORDER BY url_id DESC, id DESC
            """;

        var result = new HashMap<Long, UrlCheck>();

        try (var conn = dataSource.getConnection();
             var stmt = conn.prepareStatement(sql)) {
            var resultSet = stmt.executeQuery();

            while (resultSet.next()) {
                var check = mapRowToUrlCheck(resultSet);
                result.put(check.getUrlId(), check);
            }
        }
        return result;
    }

    private static UrlCheck mapRowToUrlCheck(ResultSet rs) throws SQLException {
        UrlCheck check = new UrlCheck();
        check.setId(rs.getLong("id"));
        check.setUrlId(rs.getLong("url_id"));
        check.setStatusCode(rs.getInt("status_code"));
        check.setH1(rs.getString("h1"));
        check.setTitle(rs.getString("title"));
        check.setDescription(rs.getString("description"));
        Timestamp timestamp = rs.getTimestamp("created_at");
        check.setCreatedAt(timestamp != null ? timestamp.toInstant() : null);
        return check;
    }
}
