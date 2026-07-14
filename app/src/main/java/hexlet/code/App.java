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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import kong.unirest.Unirest;
import kong.unirest.HttpResponse;
import io.javalin.http.Context;



public class App {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    private static final String FLASH_ERROR_URL = "Некорректный URL";
    public static final String FLASH_DUPLICATE_URL = "Страница уже существует";
    private static final String FLASH_SUCCESS_ADD = "Страница успешно добавлена";
    private static final String FLASH_SUCCESS_CHECK = "Страница успешно проверена";
    private static final String FLASH_ERROR_CHECK = "Произошла ошибка при проверке";
    private static final String FLASH_URL_NOT_FOUND = "URL not found";
    private static final String PATH_URLS = "/urls";
    private static final String PATH_INDEX = "index.jte";
    private static final String PATH_URLS_INDEX = "urls/index.jte";
    private static final String PATH_URLS_SHOW = "urls/show.jte";
    private static final String PARAM_PAGE = "page";
    private static final String PARAM_URLS = "urls";
    private static final String PARAM_URL = "url";
    private static final String PARAM_CHECKS = "checks";
    private static final String PARAM_FLASH = "flash";
    private static final int STATUS_UNPROCESSABLE_ENTITY = 422;
    private static final int STATUS_NOT_FOUND = 404;
    private static final int STATUS_INTERNAL_ERROR = 500;
    private static final int STATUS_BAD_REQUEST = 400;
    private static final int CONNECT_TIMEOUT = 5000;
    private static final int SOCKET_TIMEOUT = 5000;

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
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        URI uri = new URI(url);
        String scheme = uri.getScheme() != null ? uri.getScheme() : "https";
        String host = uri.getHost();
        if (host == null) {
            String[] parts = uri.getSchemeSpecificPart().split("/");
            host = parts.length > 0 ? parts[0] : "";
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
                String[] parts = uri.getSchemeSpecificPart().split("/");
                host = parts.length > 0 ? parts[0] : "";
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
        configureMainPage(app);
        configureUrlCreation(app);
        configureUrlsList(app);
        configureUrlPage(app);
        configureUrlCheck(app);
    }

    private static void configureMainPage(Javalin app) {
        app.get("/", ctx -> {
            try {
                String flash = ctx.sessionAttribute(PARAM_FLASH);
                ctx.sessionAttribute(PARAM_FLASH, null);

                MainPage page = new MainPage();
                page.setFlash(flash);
                ctx.render(PATH_INDEX, Map.of(PARAM_PAGE, page));
            } catch (Exception e) {
                LOG.error("Error rendering index page", e);
                ctx.result("Error: " + e.getMessage());
            }
        });
    }

    private static void configureUrlCreation(Javalin app) {
        app.post(PATH_URLS, ctx -> {
            String rawUrl = ctx.formParam(PARAM_URL);

            if (!isValidInputUrl(rawUrl)) {
                handleInvalidUrl(ctx, FLASH_ERROR_URL);
                return;
            }

            String normalizedUrl = getNormalizedUrl(ctx, rawUrl);
            if (normalizedUrl == null) {
                return;
            }

            Url existingUrl = findExistingUrl(ctx, normalizedUrl);
            if (existingUrl != null) {
                return;
            }
            saveNewUrl(ctx, normalizedUrl);
        });
    }

    private static boolean isValidInputUrl(String url) {
        return url != null && !url.isBlank() && isValidUrl(url);
    }

    private static String getNormalizedUrl(Context ctx, String rawUrl) {
        try {
            String normalized = normalizeUrl(rawUrl);
            return normalized;
        } catch (URISyntaxException e) {
            handleInvalidUrl(ctx, "Некорректный URL");
            return null;
        }
    }

    private static Url findExistingUrl(Context ctx, String normalizedUrl) {
        try {
            var existingUrlOpt = UrlRepository.findByName(normalizedUrl);
            if (existingUrlOpt.isPresent()) {
                Url existingUrl = existingUrlOpt.get();
                ctx.sessionAttribute(PARAM_FLASH, FLASH_DUPLICATE_URL);
                ctx.redirect(PATH_URLS + "/" + existingUrl.getId());
                return existingUrl;
            }
            return null;
        } catch (Exception e) {
            handleInvalidUrl(ctx, FLASH_ERROR_URL);
            return null;
        }
    }

    private static void saveNewUrl(Context ctx, String normalizedUrl) {
        try {
            Url url = new Url(normalizedUrl);
            url.setCreatedAt(Timestamp.from(Instant.now()));
            UrlRepository.save(url);
            ctx.sessionAttribute(PARAM_FLASH, FLASH_SUCCESS_ADD);
            ctx.redirect(PATH_URLS + "/" + url.getId());
        } catch (Exception e) {
            handleInvalidUrl(ctx, FLASH_ERROR_URL);
        }
    }

    private static void handleInvalidUrl(Context ctx, String message) {
        ctx.sessionAttribute(PARAM_FLASH, message);
        ctx.status(STATUS_UNPROCESSABLE_ENTITY);
        ctx.redirect("/");
    }

    private static void configureUrlsList(Javalin app) {
        app.get(PATH_URLS, ctx -> {
            try {
                var urls = UrlRepository.all();
                String flash = ctx.sessionAttribute(PARAM_FLASH);
                ctx.sessionAttribute(PARAM_FLASH, null);
                MainPage page = new MainPage();
                page.setFlash(flash);
                ctx.render(PATH_URLS_INDEX, Map.of(PARAM_URLS, urls, PARAM_PAGE, page));
            } catch (Exception e) {
                LOG.error("Error rendering urls list", e);
                ctx.status(STATUS_INTERNAL_ERROR).result("Internal server error");
            }
        });
    }

    private static void configureUrlPage(Javalin app) {
        app.get(PATH_URLS + "/{id}", ctx -> {
            try {
                Long id = parseId(ctx);
                var url = UrlRepository.find(id);
                if (url.isPresent()) {
                    showUrlPage(ctx, id, url.get());
                } else {
                    ctx.status(STATUS_NOT_FOUND).result(FLASH_URL_NOT_FOUND);
                }
            } catch (NumberFormatException e) {
                ctx.status(STATUS_BAD_REQUEST).result("Invalid ID format");
            } catch (Exception e) {
                LOG.error("Error rendering url page", e);
                ctx.status(STATUS_INTERNAL_ERROR).result("Internal server error");
            }
        });
    }

    private static void showUrlPage(Context ctx, Long id, Url url) throws Exception {
        String flash = ctx.sessionAttribute(PARAM_FLASH);
        ctx.sessionAttribute(PARAM_FLASH, null);
        MainPage page = new MainPage();
        page.setFlash(flash);
        var checks = UrlCheckRepository.findByUrlId(id);
        ctx.render(PATH_URLS_SHOW, Map.of(PARAM_URL, url, PARAM_PAGE, page, PARAM_CHECKS, checks));
    }

    private static void configureUrlCheck(Javalin app) {
        app.post(PATH_URLS + "/{id}/checks", ctx -> {
            try {
                Long id = parseId(ctx);
                var url = UrlRepository.find(id);

                if (url.isEmpty()) {
                    ctx.status(STATUS_NOT_FOUND).result(FLASH_URL_NOT_FOUND);
                    return;
                }

                performUrlCheck(ctx, id, url.get());
            } catch (NumberFormatException e) {
                ctx.status(STATUS_BAD_REQUEST).result("Invalid ID format");
            } catch (Exception e) {
                LOG.error("Error performing URL check", e);
                ctx.sessionAttribute(PARAM_FLASH, FLASH_ERROR_CHECK);
                ctx.redirect(PATH_URLS + "/" + ctx.pathParam("id"));
            }
        });
    }

    private static void performUrlCheck(Context ctx, Long id, Url url) {
        try {
            HttpResponse<String> response = Unirest.get(url.getName())
                    .connectTimeout(CONNECT_TIMEOUT)
                    .socketTimeout(SOCKET_TIMEOUT)
                    .asString();

            int statusCode = response.getStatus();
            if (isErrorStatusCode(statusCode)) {
                ctx.sessionAttribute(PARAM_FLASH, FLASH_ERROR_CHECK);
                ctx.redirect(PATH_URLS + "/" + id);
                return;
            }

            saveCheckResult(ctx, id, response, statusCode);
        } catch (Exception e) {
            LOG.error("Error during URL check", e);
            ctx.sessionAttribute(PARAM_FLASH, FLASH_ERROR_CHECK);
            ctx.redirect(PATH_URLS + "/" + id);
        }
    }

    static boolean isErrorStatusCode(int statusCode) {
        return statusCode >= 400 && statusCode <= 599;
    }

    private static void saveCheckResult(Context ctx, Long id, HttpResponse<String> response, int statusCode) {
        try {
            var url = UrlRepository.find(id);
            if (url.isEmpty()) {
                return;
            }
            Document doc = Jsoup.parse(response.getBody(), url.get().getName());

            String title = doc.title();
            String h1 = doc.selectFirst("h1") != null ? doc.selectFirst("h1").text() : null;
            String description = doc.selectFirst("meta[name=description]") != null
                    ? doc.selectFirst("meta[name=description]").attr("content")
                    : null;

            UrlCheck check = new UrlCheck(id, statusCode, h1, title, description);
            UrlCheckRepository.save(check);

            ctx.sessionAttribute(PARAM_FLASH, FLASH_SUCCESS_CHECK);
        } catch (Exception e) {
            LOG.error("Error saving check result", e);
            ctx.sessionAttribute(PARAM_FLASH, FLASH_ERROR_CHECK);
        }
        ctx.redirect(PATH_URLS + "/" + id);
    }

    private static Long parseId(Context ctx) throws NumberFormatException {
        return Long.parseLong(ctx.pathParam("id"));
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
