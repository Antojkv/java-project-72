package hexlet.code.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ErrorResponseTest {

    @Test
    void testToString() {
        ErrorResponse response = new ErrorResponse("Not Found", "The requested resource was not found");
        String expected = "Not Found: The requested resource was not found";
        assertThat(response.toString()).isEqualTo(expected);
    }

    @Test
    void testRecordAccessors() {
        ErrorResponse response = new ErrorResponse("Bad Request", "Invalid input data");

        assertThat(response.error()).isEqualTo("Bad Request");
        assertThat(response.message()).isEqualTo("Invalid input data");
    }

    @Test
    void testEqualsAndHashCode() {
        ErrorResponse response1 = new ErrorResponse("Error", "Message");
        ErrorResponse response2 = new ErrorResponse("Error", "Message");
        ErrorResponse response3 = new ErrorResponse("Error", "Different Message");

        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        assertThat(response1).isNotEqualTo(response3);
    }
}
