package hexlet.code.repository;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Setter;

public class BaseRepository {
    @Setter
    protected static HikariDataSource dataSource;

}
