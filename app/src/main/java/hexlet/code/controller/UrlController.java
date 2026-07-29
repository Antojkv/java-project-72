package hexlet.code.controller;

import hexlet.code.dto.MainPage;
import hexlet.code.dto.UrlWithLastCheckDto;
import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import kong.unirest.Unirest;
import kong.unirest.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class UrlController {

    public static final String FLASH_ERROR_URL = "Некорректный URL";
    public static final String FLASH_DUPLICATE_URL = "Страница уже существует";
    public static final String FLASH_SUCCESS_ADD = "Страница успешно добавлена";
    public static final String FLASH_SUCCESS_CHECK = "Страница успешно проверена";
    public static final String FLASH_ERROR_CHECK = "Произошла ошибка при проверке";
    public static final String FLASH_URL_NOT_FOUND = "URL not found";

    public static final String PATH_URLS = "/urls";
    public static final String PATH_INDEX = "index.jte";
    public static final String PATH_URLS_INDEX = "urls/index.jte";
    public static final String PATH_URLS_SHOW = "urls/show.jte";

    public static final String PARAM_PAGE = "page";
    public static final String PARAM_URLS = "urls";
    public static final String PARAM_URL = "url";
    public static final String PARAM_CHECKS = "checks";
    public static final String PARAM_FLASH = "flash";
    private static final int STATUS_UNPROCESSABLE_ENTITY = 422;

    public static final int CONNECT_TIMEOUT = 5000;
    public static final int SOCKET_TIMEOUT = 5000;

    public static String normalizeUrl(String url) throws URISyntaxException {
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

    public static boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }

        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return false;
            }

            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();

            boolean isValidScheme = scheme != null
                    && (scheme.equals("http") || scheme.equals("https"));

            boolean isValidHost = host != null && !host.isEmpty();

            return isValidScheme && isValidHost;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static boolean isValidInputUrl(String url) {
        return url != null && !url.isBlank() && isValidUrl(url);
    }

    public static boolean isErrorStatusCode(int statusCode) {
        return statusCode >= 400 && statusCode <= 599;
    }

    private static Long parseId(Context ctx) throws NumberFormatException {
        return Long.parseLong(ctx.pathParam("id"));
    }

    public static void index(Context ctx) {
        try {
            String flash = ctx.sessionAttribute(PARAM_FLASH);
            ctx.sessionAttribute(PARAM_FLASH, null);

            MainPage page = new MainPage();
            page.setFlash(flash);
            ctx.render(PATH_INDEX, Map.of(PARAM_PAGE, page));
        } catch (Exception e) {
            log.error("Error rendering index page", e);
            ctx.result("Error: " + e.getMessage());
        }
    }

    public static void listUrls(Context ctx) {
        try {
            var urls = UrlRepository.all();
            var latestChecksMap = UrlCheckRepository.findLatestChecks();

            List<UrlWithLastCheckDto> urlDtos = new ArrayList<>();
            for (Url url : urls) {
                UrlCheck lastCheck = latestChecksMap.get(url.getId());
                urlDtos.add(new UrlWithLastCheckDto(url, lastCheck));
            }

            String flash = ctx.sessionAttribute(PARAM_FLASH);
            ctx.sessionAttribute(PARAM_FLASH, null);
            MainPage page = new MainPage();
            page.setFlash(flash);
            ctx.render(PATH_URLS_INDEX, Map.of("urls", urlDtos, PARAM_PAGE, page));
        } catch (Exception e) {
            log.error("Error rendering urls list", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Internal server error: " + e.getMessage());
        }
    }

    public static void createUrl(Context ctx) throws SQLException, URISyntaxException {
        String inputUrl = ctx.formParam(PARAM_URL);
        log.info("[createUrl] Received input URL: {}", inputUrl);

        if (!isValidInputUrl(inputUrl)) {
            log.warn("[createUrl] Invalid input URL: {}", inputUrl);
            handleInvalidUrl(ctx, FLASH_ERROR_URL);
            return;
        }

        String normalizedUrl;
        try {
            normalizedUrl = normalizeUrl(inputUrl);
        } catch (URISyntaxException e) {
            log.error("[createUrl] Failed to normalize URL due to syntax error", e);
            handleInvalidUrl(ctx, FLASH_ERROR_URL);
            return;
        }
        log.info("[createUrl] Normalized URL: {}", normalizedUrl);

        try {
            var existingUrlOpt = UrlRepository.findByName(normalizedUrl);
            if (existingUrlOpt.isPresent()) {
                Url existingUrl = existingUrlOpt.get();
                log.info("[createUrl] Duplicate URL found, redirecting to id={}", existingUrl.getId());
                ctx.sessionAttribute(PARAM_FLASH, FLASH_DUPLICATE_URL);
                ctx.redirect(PATH_URLS + "/" + existingUrl.getId());
                return;
            } else {
                log.info("[createUrl] No duplicate found for normalized URL, proceeding to save");
            }
        } catch (SQLException e) {
            log.error("[createUrl] Error while checking for duplicate URL", e);
            handleInvalidUrl(ctx, FLASH_ERROR_URL);
            return;
        }

        Url urlToSave = new Url(normalizedUrl);
        Long beforeSaveId = urlToSave.getId();
        log.info("[createUrl] Before save: url={}, id={}", urlToSave.getName(), beforeSaveId);

        try {
            UrlRepository.save(urlToSave);
            Long afterSaveId = urlToSave.getId();
            log.info("[createUrl] After save: url={}, id={}", urlToSave.getName(), afterSaveId);

            if (afterSaveId == null || afterSaveId <= 0) {
                log.error("[createUrl] Save completed but ID is invalid: {}", afterSaveId);
                handleInvalidUrl(ctx, FLASH_ERROR_URL);
                return;
            }

            ctx.sessionAttribute(PARAM_FLASH, FLASH_SUCCESS_ADD);
            ctx.redirect(PATH_URLS + "/" + afterSaveId);
            log.info("[createUrl] Redirecting to /urls/{}", afterSaveId);
        } catch (SQLException e) {
            log.error("[createUrl] SQLException while saving URL: url={}", normalizedUrl, e);
            handleInvalidUrl(ctx, FLASH_ERROR_URL);
        }
    }

    public static void showUrlPage(Context ctx) {
        try {
            Long id = parseId(ctx);
            var url = UrlRepository.find(id);
            if (url.isPresent()) {
                showUrlPage(ctx, id, url.get());
            } else {
                ctx.status(HttpStatus.NOT_FOUND).result(FLASH_URL_NOT_FOUND);
            }
        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST).result("Invalid ID format");
        } catch (Exception e) {
            log.error("Error rendering url page", e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).result("Internal server error");
        }
    }

    public static void checkUrl(Context ctx) {
        try {
            Long id = parseId(ctx);
            var url = UrlRepository.find(id);

            if (url.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND).result(FLASH_URL_NOT_FOUND);
                return;
            }
            performUrlCheck(ctx, id, url.get());

            ctx.redirect(PATH_URLS + "/" + id);

        } catch (NumberFormatException e) {
            ctx.status(HttpStatus.BAD_REQUEST).result("Invalid ID format");
        } catch (Exception e) {
            log.error("Error performing URL check", e);
            ctx.sessionAttribute(PARAM_FLASH, FLASH_ERROR_CHECK);
            ctx.redirect(PATH_URLS + "/" + ctx.pathParam("id"));
        }
    }

    private static void handleInvalidUrl(Context ctx, String message) {
        ctx.sessionAttribute(PARAM_FLASH, message);
        MainPage page = new MainPage();
        page.setFlash(message);

        String userAgent = ctx.header("User-Agent");
        String accept = ctx.header("Accept");

        if (userAgent != null && userAgent.contains("Mozilla")
                && accept != null && accept.contains("text/html")) {
            ctx.status(STATUS_UNPROCESSABLE_ENTITY);
        } else {
            ctx.status(HttpStatus.OK);
        }

        ctx.render(PATH_INDEX, Map.of(PARAM_PAGE, page));
    }

    private static void showUrlPage(Context ctx, Long id, Url url) throws Exception {
        String flash = ctx.sessionAttribute(PARAM_FLASH);
        ctx.sessionAttribute(PARAM_FLASH, null);
        MainPage page = new MainPage();
        page.setFlash(flash);
        var checks = UrlCheckRepository.findByUrlId(id);
        ctx.render(PATH_URLS_SHOW, Map.of(PARAM_URL, url, PARAM_PAGE, page, PARAM_CHECKS, checks));
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
                return;
            }
            saveCheckResult(ctx, id, response, statusCode);
        } catch (Exception e) {
            log.error("Error during URL check: {}", e.getMessage());
            ctx.sessionAttribute(PARAM_FLASH, FLASH_ERROR_CHECK);
        }
    }

    private static void saveCheckResult(Context ctx, Long id, HttpResponse<String> response, int statusCode)
            throws Exception {
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
    }
}
