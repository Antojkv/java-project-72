package hexlet.code;

import io.javalin.Javalin;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

public class AppExceptionTest {

    @Test
    void testSQLExceptionHandlerOnlyBody() {
        Javalin app = Javalin.create().start(0);

        try {

            app.get("/test-sql", ctx -> {
                throw new SQLException("Simulated DB error");
            });
            var client = java.net.http.HttpClient.newHttpClient();

        } finally {
            app.stop();
        }
    }
}
