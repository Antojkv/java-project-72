package hexlet.code;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import hexlet.code.repository.BaseRepository;
import hexlet.code.repository.UrlRepository;
import hexlet.code.model.Url;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinJte;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

public class App {

    private static HikariDataSource dataSource;

    private static TemplateEngine createTemplateEngine() {
        ClassLoader classLoader = App.class.getClassLoader();
        ResourceCodeResolver codeResolver = new ResourceCodeResolver("templates", classLoader);
        return TemplateEngine.create(codeResolver, ContentType.Html);
    }

    public static Javalin getApp() throws Exception {
        setupDatabase();

        TemplateEngine templateEngine = createTemplateEngine();

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableDevLogging();
            config.fileRenderer(new JavalinJte(templateEngine));
        });

        app.get("/", ctx -> {
            ctx.render("index.jte");
        });

        app.post("/urls", ctx -> {
            String name = ctx.formParam("url");
            if (name == null || name.isBlank()) {
                ctx.sessionAttribute("flash", "URL не может быть пустым");
                ctx.redirect("/");
                return;
            }

            Url url = new Url(name);
            url.setCreatedAt(Timestamp.from(Instant.now()));
            UrlRepository.save(url);

            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.redirect("/urls/" + url.getId());
        });

        app.get("/urls", ctx -> {
            var urls = UrlRepository.all();
            ctx.render("urls/index.jte", Map.of("urls", urls));
        });

        app.get("/urls/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            var url = UrlRepository.find(id);
            if (url.isPresent()) {
                ctx.render("urls/show.jte", Map.of("url", url.get()));
            } else {
                ctx.status(404).result("URL not found");
            }
        });

        return app;
    }

    private static void setupDatabase() throws Exception {
        String databaseUrl = System.getenv().getOrDefault("JDBC_DATABASE_URL",
                "jdbc:h2:mem:project;DB_CLOSE_DELAY=-1");

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(databaseUrl);

        if (databaseUrl.startsWith("jdbc:h2:")) {
            config.setDriverClassName("org.h2.Driver");
        }

        dataSource = new HikariDataSource(config);
        BaseRepository.setDataSource(dataSource);

        initSchema();
    }

    private static void initSchema() throws Exception {
        String sql = readResourceFile("schema.sql");
        if (sql == null || sql.isBlank()) {
            return;
        }

        try (var conn = dataSource.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
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

    public static void main(String[] args) throws Exception {
        String portStr = System.getenv().getOrDefault("PORT", "7070");
        int port = Integer.parseInt(portStr);

        Javalin app = getApp();
        app.start(port);

    }
}
