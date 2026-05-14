package hexlet.code;

import gg.jte.resolve.DirectoryCodeResolver;
import hexlet.code.dto.MainPage;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
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

    private static String normalizeUrl(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String scheme = uri.getScheme() != null ? uri.getScheme() : "https";
        String host = uri.getHost();
        if (host == null) {
            host = uri.getSchemeSpecificPart().split("/")[0];
        }
        int port = uri.getPort();
        String normalized = scheme + "://" + host;
        if (port != -1 && port != 80 && port != 443) {
            normalized += ":" + port;
        }
        return normalized.toLowerCase();
    }

    private static boolean isValidUrl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) {
                host = uri.getSchemeSpecificPart().split("/")[0];
            }
            return (uri.getScheme() != null && (uri.getScheme().equals("http") || uri.getScheme().equals("https")))
                    && host != null && !host.isEmpty();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static HikariDataSource setupDataSource() throws Exception {
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

        UrlRepository.setDataSource(dataSource);
        UrlCheckRepository.setDataSource(dataSource);

        return dataSource;
    }

    private static void configureRoutes(Javalin app) {
        app.before(ctx -> ctx.contentType("text/html; charset=utf-8"));

        app.get("/", ctx -> {
            try {
                String flash = ctx.sessionAttribute("flash");
                ctx.sessionAttribute("flash", null);
                MainPage page = new MainPage();
                page.setFlash(flash);
                ctx.render("index.jte", Map.of("page", page));
            } catch (Exception e) {
                LOG.error("Error rendering index page", e);
                ctx.result("Error: " + e.getMessage());
            }
        });

        app.post("/urls", ctx -> {
            String rawUrl = ctx.formParam("url");
            if (rawUrl == null || rawUrl.isBlank()) {
                ctx.sessionAttribute("flash", "Некорректный URL");
                ctx.status(422);
                ctx.redirect("/");
                return;
            }

            if (!isValidUrl(rawUrl)) {
                ctx.sessionAttribute("flash", "Некорректный URL");
                ctx.status(422);
                ctx.redirect("/");
                return;
            }

            String normalizedUrl;
            try {
                normalizedUrl = normalizeUrl(rawUrl);
            } catch (URISyntaxException e) {
                ctx.sessionAttribute("flash", "Некорректный URL");
                ctx.status(422);
                ctx.redirect("/");
                return;
            }

            var existingUrl = UrlRepository.findByName(normalizedUrl);
            if (existingUrl.isPresent()) {
                ctx.sessionAttribute("flash", "Страница уже существует");
                ctx.redirect("/urls/" + existingUrl.get().getId());
                return;
            }

            Url url = new Url(normalizedUrl);
            url.setCreatedAt(Timestamp.from(Instant.now()));
            UrlRepository.save(url);

            ctx.sessionAttribute("flash", "Страница успешно добавлена");
            ctx.redirect("/urls/" + url.getId());
        });

        app.get("/urls", ctx -> {
            var urls = UrlRepository.all();
            String flash = ctx.sessionAttribute("flash");
            ctx.sessionAttribute("flash", null);
            MainPage page = new MainPage();
            page.setFlash(flash);
            ctx.render("urls/index.jte", Map.of("urls", urls, "page", page));
        });

        app.get("/urls/{id}", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            var url = UrlRepository.find(id);
            if (url.isPresent()) {
                String flash = ctx.sessionAttribute("flash");
                ctx.sessionAttribute("flash", null);
                MainPage page = new MainPage();
                page.setFlash(flash);
                var checks = UrlCheckRepository.findByUrlId(id);
                ctx.render("urls/show.jte", Map.of("url", url.get(), "page", page, "checks", checks));
            } else {
                ctx.status(404).result("URL not found");
            }
        });

        app.post("/urls/{id}/checks", ctx -> {
            Long id = Long.parseLong(ctx.pathParam("id"));
            var url = UrlRepository.find(id);

            if (url.isEmpty()) {
                ctx.status(404).result("URL not found");
                return;
            }

            try {
                HttpURLConnection connection = (HttpURLConnection)
                        new java.net.URL(url.get().getName()).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();

                String h1 = null;
                String title = null;
                String description = null;

                if (responseCode == 200) {
                    Document doc = Jsoup.parse(connection.getInputStream(), "UTF-8", url.get().getName());
                    h1 = doc.select("h1").stream().findFirst().map(e -> e.text()).orElse(null);
                    title = doc.select("title").stream().findFirst().map(e -> e.text()).orElse(null);
                    description = doc.select("meta[name=description]").stream()
                            .findFirst()
                            .map(e -> e.attr("content"))
                            .orElse(null);
                }

                UrlCheck check = new UrlCheck(id, responseCode, h1, title, description);
                UrlCheckRepository.save(check);

                ctx.sessionAttribute("flash", "Проверка выполнена. Код ответа: " + responseCode);

            } catch (Exception e) {
                ctx.sessionAttribute("flash", "Ошибка при проверке: " + e.getMessage());
            }

            ctx.redirect("/urls/" + id);
        });
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

    public static void main(String[] args) throws Exception {
        String portStr = System.getenv().getOrDefault("PORT", "7070");
        int port = Integer.parseInt(portStr);

        Javalin app = getApp();
        app.start(port);
    }
}
