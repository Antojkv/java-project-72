package hexlet.code.repository;

import hexlet.code.model.UrlCheck;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UrlCheckRepository {
    private static HikariDataSource dataSource;

    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_URL_ID = "url_id";
    private static final String COLUMN_STATUS_CODE = "status_code";

    public static void setDataSource(HikariDataSource ds) {
        dataSource = ds;
    }

    public static void save(UrlCheck check) throws SQLException {
        String sql = "INSERT INTO url_checks (url_id, status_code, h1, title, description, "
                + COLUMN_CREATED_AT + ") VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, check.getUrlId());
            stmt.setInt(2, check.getStatusCode());
            stmt.setString(3, check.getH1());
            stmt.setString(4, check.getTitle());
            stmt.setString(5, check.getDescription());
            stmt.setTimestamp(6, check.getCreatedAt());
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
                UrlCheck check = new UrlCheck(
                        rs.getLong("id"),
                        rs.getLong(COLUMN_URL_ID),
                        rs.getInt(COLUMN_STATUS_CODE),
                        rs.getString("h1"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getTimestamp(COLUMN_CREATED_AT)
                );
                checks.add(check);
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
                return new UrlCheck(
                        rs.getLong("id"),
                        rs.getLong(COLUMN_URL_ID),
                        rs.getInt(COLUMN_STATUS_CODE),
                        rs.getString("h1"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getTimestamp(COLUMN_CREATED_AT)
                );
            }
        }
        return null;
    }
}
