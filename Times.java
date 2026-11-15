import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formats database timestamps for display. */
public final class Times {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.ENGLISH);

    private Times() { }

    public static String format(Timestamp timestamp) {
        return timestamp == null ? "" : timestamp.toLocalDateTime().format(FORMAT);
    }
}
