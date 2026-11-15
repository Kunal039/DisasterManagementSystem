import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The login page.
 * On the very first start no account exists yet, so the page asks you to create the
 * administrator account. After that it shows the normal login form.
 */
public class LoginServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private static final int MAX_FAILURES = 5;
    private static final long LOCK_MILLIS = 60_000;

    /** Failed login attempts per user name: {number of failures, time until which the name is locked}. */
    private static final Map<String, long[]> FAILURES = new ConcurrentHashMap<>();

    private final transient UserDao userDao = new UserDao();

    @Override protected String path() { return "/login"; }
    @Override protected String title() { return "Log in"; }
    @Override protected boolean requiresLogin() { return false; }

    /** After a successful login go to the dashboard; after a failed one show the login page again. */
    @Override
    protected String redirectAfterPost(HttpServletRequest request) {
        return currentUser(request) != null ? "/dashboard" : "/login";
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (currentUser(request) != null) {
            response.sendRedirect("/dashboard");   // already logged in
            return;
        }
        super.doGet(request, response);
    }

    @Override
    protected String handlePost(HttpServletRequest request) throws SQLException {
        String username = text(request, "username", "User name", 30).toLowerCase(Locale.ROOT);
        String password = password(request, "password");

        switch (action(request)) {
            case "setup":
                UserDao.checkUsername(username);
                UserDao.checkNewPassword(password, password(request, "confirm"));
                userDao.createFirstUser(username, password);
                logIn(request, username);
                return "Account created. Welcome, " + username + ".";

            case "login":
                long[] record = FAILURES.get(username);
                if (record != null && System.currentTimeMillis() < record[1]) {
                    throw new AppException("Too many failed attempts. Please wait a minute and try again.");
                }
                if (!userDao.passwordMatches(username, password)) {
                    rememberFailure(username);
                    // The same message for a wrong name and a wrong password, so nothing is revealed
                    throw new AppException("Wrong user name or password.");
                }
                FAILURES.remove(username);
                logIn(request, username);
                return "Welcome, " + username + ".";

            default:
                throw new AppException("Unknown action.");
        }
    }

    private static void logIn(HttpServletRequest request, String username) {
        request.changeSessionId();   // new session id after login (protects against session fixation)
        request.getSession().setAttribute(USER, username);
    }

    /** After 5 wrong passwords the user name is locked for one minute, which slows down guessing. */
    private static void rememberFailure(String username) {
        if (FAILURES.size() > 10_000) FAILURES.clear();   // keeps the map small
        FAILURES.merge(username, new long[] {1, 0}, (old, one) -> {
            long failures = old[0] + 1;
            return failures >= MAX_FAILURES
                    ? new long[] {0, System.currentTimeMillis() + LOCK_MILLIS}
                    : new long[] {failures, 0};
        });
    }

    /** Reads a password field. Unlike other fields it is not trimmed. */
    static String password(HttpServletRequest request, String field) {
        String value = request.getParameter(field);
        if (value == null || value.isEmpty()) throw new AppException("The password is required.");
        if (value.length() > 100) throw new AppException("The password is too long (maximum 100 characters).");
        return value;
    }

    @Override
    protected String body(HttpServletRequest request) throws SQLException {
        boolean firstStart = userDao.noUsersExist();
        StringBuilder h = new StringBuilder();
        h.append("<div class=\"panel narrow\">");
        if (firstStart) {
            h.append("<h2>Create the administrator account</h2>")
             .append("<p class=\"muted\">No account exists yet. Choose the user name and password ")
             .append("you will use to log in.</p>")
             .append(formStart(request, "setup", "form-stack"))
             .append("<label>User name <input name=\"username\" maxlength=\"30\" autocomplete=\"username\" ")
             .append("required autofocus></label>")
             .append("<label>Password <span class=\"muted\">(at least 8 characters)</span>")
             .append("<input name=\"password\" type=\"password\" minlength=\"8\" maxlength=\"100\" ")
             .append("autocomplete=\"new-password\" required></label>")
             .append("<label>Repeat password <input name=\"confirm\" type=\"password\" minlength=\"8\" ")
             .append("maxlength=\"100\" autocomplete=\"new-password\" required></label>")
             .append("<button type=\"submit\">Create account</button></form>");
        } else {
            h.append("<h2>Log in</h2>")
             .append(formStart(request, "login", "form-stack"))
             .append("<label>User name <input name=\"username\" maxlength=\"30\" autocomplete=\"username\" ")
             .append("required autofocus></label>")
             .append("<label>Password <input name=\"password\" type=\"password\" maxlength=\"100\" ")
             .append("autocomplete=\"current-password\" required></label>")
             .append("<button type=\"submit\">Log in</button></form>");
        }
        h.append("</div>");
        return h.toString();
    }
}
