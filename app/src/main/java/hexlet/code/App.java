package hexlet.code;

import gg.jte.resolve.DirectoryCodeResolver;
import hexlet.code.dto.MainPage;
import hexlet.code.repository.UrlRepository;
import hexlet.code.model.Url;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinJte;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

public class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    private static TemplateEngine createTemplateEngine() {
        Path path = Paths.get("src/main/jte").toAbsolutePath();
        System.out.println("Looking for templates in: " + path);
        DirectoryCodeResolver codeResolver = new DirectoryCodeResolver(path);
        return TemplateEngine.create(codeResolver, ContentType.Html);
    }

    private static String getDatabaseUrl() {
        return System.getenv().getOrDefault("JDBC_DATABASE_URL", "jdbc:h2:mem:project");
    }

    private static String readResourceFile(String fileName) throws Exception {
        var inputStream = App.class.getClassLoader().getResourceAsStream(fileName);
        if (inputStream == null) {
            return null;
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    public static Javalin getApp() throws Exception {
        // Настройка базы данных как у ментора
        var hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(getDatabaseUrl());
        hikariConfig.setMaximumPoolSize(5);
        var dataSource = new HikariDataSource(hikariConfig);

        String sql = readResourceFile("schema.sql");
        if (sql != null && !sql.isBlank()) {
            LOG.info(sql);
            try (var connection = dataSource.getConnection();
                 var statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }

        // Устанавливаем DataSource в репозиторий
        UrlRepository.setDataSource(dataSource);

        // Настройка JTE
        TemplateEngine templateEngine = createTemplateEngine();

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableDevLogging();
            config.fileRenderer(new JavalinJte(templateEngine));
        });

        app.before(ctx -> {
            ctx.contentType("text/html; charset=utf-8");
        });

        // Главная страница
        app.get("/", ctx -> {
            try {
                String flash = ctx.sessionAttribute("flash");
                ctx.sessionAttribute("flash", null);
                MainPage page = new MainPage();
                page.setFlash(flash);
                ctx.render("index.jte", Map.of("page", page));
            } catch (Exception e) {
                LOG.error("Error rendering index page", e);
                ctx.result("Error: " + e.getMessage() + "\n" + java.util.Arrays.toString(e.getStackTrace()));
            }
        });

        // Добавление URL
        app.post("/urls", ctx -> {
            String name = ctx.formParam("url");
            if (name == null || name.isBlank()) {
                ctx.sessionAttribute("flash", "URL не может быть пустым");
                ctx.redirect("/");
                return;
            }

            if (!name.startsWith("http")) {
                name = "https://" + name;
            }

            Url url = new Url(name);
            url.setCreatedAt(Timestamp.from(Instant.now()));
            UrlRepository.save(url);

            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.redirect("/");
        });

        return app;
    }

    public static void main(String[] args) throws Exception {
        String portStr = System.getenv().getOrDefault("PORT", "7070");
        int port = Integer.parseInt(portStr);

        Javalin app = getApp();
        app.start(port);
        System.out.println("Application started on port " + port);
    }
}
