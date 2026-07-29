package hexlet.code.dto;

public record ErrorResponse(String error, String message) {
    @Override
    public String toString() {
        return error + ": " + message;
    }
}
