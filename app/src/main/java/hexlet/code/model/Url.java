package hexlet.code.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Url {
    private Long id;
    private String name;
    private Timestamp createdAt;
    private Integer lastCheckStatusCode;
    private Timestamp lastCheckCreatedAt;

    public Url(String name) {
        this.name = name;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    public String getFormattedCreatedAt() {
        if (createdAt == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        return createdAt.toLocalDateTime().format(formatter);
    }

    public String getFormattedLastCheckCreatedAt() {
        if (lastCheckCreatedAt == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        return lastCheckCreatedAt.toLocalDateTime().format(formatter);
    }
}
