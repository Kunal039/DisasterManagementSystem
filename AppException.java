/**
 * Thrown when user input is wrong or a business rule is broken
 * (for example: not enough stock). The message is shown to the user.
 */
public class AppException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AppException(String message) {
        super(message);
    }
}
