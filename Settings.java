import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Settings that differ between your own computer and a hosting service.
 * They are read from environment variables, so nothing secret is written in the source code.
 *
 *   PORT            port to listen on (hosting services set this); default 8080
 *   DMS_BIND        network address to listen on; default 127.0.0.1 (this computer only).
 *                   On a hosting service it is 0.0.0.0 (reachable from the internet).
 *   DMS_SETUP_CODE  secret code that must be typed in when the first (administrator) account
 *                   is created. Required when the application is reachable from the internet,
 *                   otherwise the first stranger to open the site could make themselves administrator.
 *   DMS_DB_URL      JDBC URL of the database (optional, see Database.java)
 *   DMS_DB_USER     database user name (optional)
 *   DMS_DB_PASSWORD database password (optional)
 */
public final class Settings {

    private Settings() { }

    public static String bindAddress() {
        return value("DMS_BIND", "127.0.0.1");
    }

    /** True when the application can be reached from other computers. */
    public static boolean isPublic() {
        try {
            return !InetAddress.getByName(bindAddress()).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return true;   // unknown address: be careful and treat it as public
        }
    }

    /** The port given on the command line, else the PORT environment variable, else 8080. */
    public static int port(String[] args) {
        String text = args.length > 0 ? args[0] : value("PORT", "8080");
        try {
            int port = Integer.parseInt(text.trim());
            if (port >= 1 && port <= 65535) return port;
        } catch (NumberFormatException e) {
            // handled below
        }
        throw new IllegalArgumentException("The port must be a number from 1 to 65535, but was: " + text);
    }

    public static boolean setupCodeRequired() {
        return isPublic() || !value("DMS_SETUP_CODE", "").isEmpty();
    }

    /** True if a setup code is configured. A public installation without one cannot create its first account. */
    public static boolean setupCodeConfigured() {
        return !value("DMS_SETUP_CODE", "").isEmpty();
    }

    /** Compares the typed code with the configured one in constant time. */
    public static boolean setupCodeMatches(String typed) {
        String expected = value("DMS_SETUP_CODE", "");
        if (expected.isEmpty() || typed == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                typed.getBytes(StandardCharsets.UTF_8));
    }

    /** An environment variable, or the default when it is not set or empty. */
    public static String value(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
