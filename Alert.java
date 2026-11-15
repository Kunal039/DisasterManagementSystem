/** A message on the alert feed (one row of the "alerts" table). */
public class Alert {

    private final AlertLevel level;
    private final String message;
    private final String createdAt;

    public Alert(AlertLevel level, String message, String createdAt) {
        this.level = level;
        this.message = message;
        this.createdAt = createdAt;
    }

    public AlertLevel getLevel() { return level; }
    public String getMessage() { return message; }
    public String getCreatedAt() { return createdAt; }
}
