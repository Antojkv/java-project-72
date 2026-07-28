package hexlet.code.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Url {
    private Long id;
    private String name;
    private Instant createdAt;

    public Url(String name) {
        this.name = name;
        this.createdAt = Instant.now();
    }
}
