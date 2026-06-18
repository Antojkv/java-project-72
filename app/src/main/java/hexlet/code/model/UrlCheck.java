package hexlet.code.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UrlCheck {
    private Long id;
    private Long urlId;
    private Integer statusCode;
    private String h1;
    private String title;
    private String description;
    private Timestamp createdAt;

    public UrlCheck(Long urlId, Integer statusCode, String h1, String title, String description) {
        this.urlId = urlId;
        this.statusCode = statusCode;
        this.h1 = truncate(h1);
        this.title = truncate(title);
        this.description = truncate(description);
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    private String truncate(String str) {
        if (str == null) {
            return null;
        }
        if (str.length() <= 200) {
            return str;
        }
        return str.substring(0, 197) + "...";
    }
}
