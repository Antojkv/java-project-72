package hexlet.code.repository;

import hexlet.code.model.Url;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static hexlet.code.repository.BaseRepository.dataSource;

public class UrlRepository {
    private static final String COLUMN_CREATED_AT = "created_at";

    public static void save(Url url) throws SQLException {
        if (url.getCreatedAt() == null) {
            url.setCreatedAt(Instant.now());
        }
        String sql = "INSERT INTO urls (name, created_at) VALUES (?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, url.getName());
            stmt.setTimestamp(2, url.getCreatedAt() != null ? Timestamp.from(url.getCreatedAt()) : null);
            stmt.executeUpdate();

            ResultSet generatedKeys = stmt.getGeneratedKeys();
            if (generatedKeys.next()) {
                url.setId(generatedKeys.getLong(1));
            }
        }
    }

    public static Optional<Url> find(Long id) throws SQLException {
        String sql = "SELECT * FROM urls WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToUrl(rs));
            }
            return Optional.empty();
        }
    }

    public static Optional<Url> findByName(String name) throws SQLException {
        String sql = "SELECT * FROM urls WHERE name = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRowToUrl(rs));
            }
            return Optional.empty();
        }
    }

    public static List<Url> all() throws SQLException {
        List<Url> urls = new ArrayList<>();
        String sql = "SELECT * FROM urls ORDER BY id DESC";
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                try {
                    urls.add(mapRowToUrl(rs));
                } catch (Exception e) {
                    System.err.println("Error mapping row: " + e.getMessage());
                    throw e;
                }
            }
        }
        return urls;
    }

    private static Url mapRowToUrl(ResultSet rs) throws SQLException {
        Url url = new Url();
        url.setId(rs.getLong("id"));
        url.setName(rs.getString("name"));
        Timestamp timestamp = rs.getTimestamp("created_at");
        url.setCreatedAt(timestamp != null ? timestamp.toInstant() : null);
        return url;
    }
}
