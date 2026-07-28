package hexlet.code.dto;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class UrlWithLastCheckDto {
    private Long id;
    private String name;
    private Instant createdAt;
    private Integer lastCheckStatusCode;
    private Instant lastCheckCreatedAt;

    public UrlWithLastCheckDto(Url url, UrlCheck lastCheck) {
        this.id = url.getId();
        this.name = url.getName();
        this.createdAt = url.getCreatedAt();
        if (lastCheck != null) {
            this.lastCheckStatusCode = lastCheck.getStatusCode();
            this.lastCheckCreatedAt = lastCheck.getCreatedAt();
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Integer getLastCheckStatusCode() {
        return lastCheckStatusCode;
    }

    public Instant getLastCheckCreatedAt() {
        return lastCheckCreatedAt;
    }

    public String getFormattedCreatedAt() {
        if (createdAt == null) {
            return "";
        }
        return createdAt.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }

    public String getFormattedLastCheckCreatedAt() {
        if (lastCheckCreatedAt == null) {
            return "";
        }
        return lastCheckCreatedAt.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }
}
