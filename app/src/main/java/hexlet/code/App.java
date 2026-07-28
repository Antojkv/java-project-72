package hexlet.code;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import hexlet.code.controller.UrlController;
import hexlet.code.repository.BaseRepository;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinJte;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class App {
    public static final String PATH_URLS = "/urls";

    private static TemplateEngine createTemplateEngine() {
        ClassLoader classLoader = App.class.getClassLoader();
        ResourceCodeResolver codeResolver = new ResourceCodeResolver("templates", classLoader);
        System.out.println("Using ResourceCodeResolver for templates");
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

    private static HikariDataSource setupDataSource() throws Exception {
        var hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(getDatabaseUrl());
        hikariConfig.setMaximumPoolSize(5);
        hikariConfig.setDriverClassName("org.h2.Driver");
        var dataSource = new HikariDataSource(hikariConfig);

        String sql = readResourceFile("schema.sql");
        if (sql != null && !sql.isBlank()) {
            log.info(sql);
            try (var connection = dataSource.getConnection();
                 var statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }

        BaseRepository.setDataSource(dataSource);

        return dataSource;
    }

    private static void configureRoutes(Javalin app) {
        app.before(ctx -> ctx.contentType("text/html; charset=utf-8"));

        app.get("/", UrlController::index);
        app.get(PATH_URLS, UrlController::listUrls);
        app.post(PATH_URLS, UrlController::createUrl);
        app.get(PATH_URLS + "/{id}", UrlController::showUrlPage);
        app.post(PATH_URLS + "/{id}/checks", UrlController::checkUrl);
    }

    public static Javalin getApp() throws Exception {
        setupDataSource();
        TemplateEngine templateEngine = createTemplateEngine();

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableDevLogging();
            config.fileRenderer(new JavalinJte(templateEngine));
        });

        configureRoutes(app);
        return app;
    }

    public static int getPort(Map<String, String> env) {
        String portStr = env.getOrDefault("PORT", "7070");
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            System.err.println("Invalid PORT value '" + portStr + "', using default 7070");
            return 7070;
        }
    }

    public static void start(Map<String, String> env) {

        int port = getPort(env);
        try {
            Javalin app = getApp();
            app.start(port);
        } catch (Exception e) {
            log.error("Error starting application", e);
        }
    }

    public static void main(String[] args) {
        App.start(System.getenv());
    }
}
