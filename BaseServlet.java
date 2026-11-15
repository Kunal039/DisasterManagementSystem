import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;

/**
 * Parent class of all servlets of the application. It contains what every page needs:
 *   - GET requests:  build the page and send it to the browser
 *   - POST requests: check the security token, run the action, then redirect back to the page
 *     (Post/Redirect/Get pattern, so pressing "refresh" never repeats an action)
 *   - helper methods to read and validate form fields
 */
public abstract class BaseServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TOKEN = "csrfToken";
    /** Name of the session attribute that holds the user name of the logged-in user. */
    protected static final String USER = "user";
    private static final String FLASH_SUCCESS = "flashSuccess";
    private static final String FLASH_ERROR = "flashError";

    /** Address of the page, e.g. "/disasters". */
    protected abstract String path();

    /** Title shown in the browser tab. */
    protected abstract String title();

    /** Builds the main part of the page. */
    protected abstract String body(HttpServletRequest request) throws SQLException;

    /** Runs a form action and returns the success message. Pages with forms override this. */
    protected String handlePost(HttpServletRequest request) throws SQLException {
        throw new AppException("This action is not supported.");
    }

    /** Every page needs a logged-in user, except the login page itself. */
    protected boolean requiresLogin() {
        return true;
    }

    /** Where the browser is sent after a form action. Normally back to the same page. */
    protected String redirectAfterPost(HttpServletRequest request) {
        return path();
    }

    /** User name of the logged-in user, or null if nobody is logged in. */
    protected static String currentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : (String) session.getAttribute(USER);
    }

    /**
     * Like currentUser, but also checks that the account still exists.
     * If it was deleted in the meantime, the session is ended.
     */
    private static String activeUser(HttpServletRequest request) {
        String user = currentUser(request);
        if (user == null) return null;
        try {
            if (new UserDao().exists(user)) return user;
        } catch (SQLException e) {
            return user;   // database problem: the page itself will report it
        }
        request.getSession().invalidate();
        return null;
    }

    /** Pages that should reload automatically return a number of seconds here. */
    protected int refreshSeconds() {
        return 0;
    }

    // ------------------------------------------------------------ GET and POST

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String user = activeUser(request);
        if (requiresLogin() && user == null) {
            response.sendRedirect("/login");
            return;
        }
        HttpSession session = request.getSession();
        String userBar = user == null ? null : userBar(user, token(session));
        String success = takeMessage(session, FLASH_SUCCESS);
        String error = takeMessage(session, FLASH_ERROR);

        String html;
        try {
            html = Html.page(title(), path(), userBar, success, error, refreshSeconds(), body(request));
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            html = Html.page("Error", "", userBar, null,
                    "A database error occurred. See the server console for details.", 0, "");
        }

        response.setContentType("text/html; charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        // The browser may load nothing except this page's own styles and the Google Map; scripts are not allowed
        response.setHeader("Content-Security-Policy",
                "default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'; "
                        + "frame-src https://maps.google.com https://www.google.com");
        response.getWriter().write(html);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();

        // The form must contain the secret token of this session.
        // This stops other web sites from submitting forms to this application (CSRF attack).
        String expected = (String) session.getAttribute(TOKEN);
        String given = request.getParameter("csrf");
        if (expected == null || given == null || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "The form has expired. Go back and reload the page.");
            return;
        }

        if (requiresLogin() && activeUser(request) == null) {
            response.sendRedirect("/login");
            return;
        }

        // request.getSession() is called again below because an action may replace the session (login, logout)
        try {
            String message = handlePost(request);
            request.getSession().setAttribute(FLASH_SUCCESS, message);
        } catch (AppException e) {
            request.getSession().setAttribute(FLASH_ERROR, e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            request.getSession().setAttribute(FLASH_ERROR,
                    "A database error occurred. See the server console for details.");
        }
        response.sendRedirect(redirectAfterPost(request));
    }

    private static String takeMessage(HttpSession session, String name) {
        String message = (String) session.getAttribute(name);
        session.removeAttribute(name);
        return message;
    }

    private static String token(HttpSession session) {
        synchronized (session) {
            String token = (String) session.getAttribute(TOKEN);
            if (token == null) {
                byte[] bytes = new byte[32];
                RANDOM.nextBytes(bytes);
                StringBuilder hex = new StringBuilder();
                for (byte b : bytes) hex.append(String.format("%02x", b));
                token = hex.toString();
                session.setAttribute(TOKEN, token);
            }
            return token;
        }
    }

    // ------------------------------------------------------- building forms

    /** The "Signed in as ..." text and the Log out button shown in the page header. */
    private static String userBar(String user, String token) {
        return "<div class=\"user\">Signed in as <b>" + Html.esc(user) + "</b> "
                + "<form method=\"post\" action=\"/logout\" class=\"inline\">"
                + "<input type=\"hidden\" name=\"csrf\" value=\"" + token + "\">"
                + "<button class=\"small plain\">Log out</button></form></div>";
    }

    /** Start of a normal form that posts back to this page, including the hidden security token. */
    protected String formStart(HttpServletRequest request, String action, String cssClass) {
        return "<form method=\"post\" action=\"" + path() + "\" class=\"" + cssClass + "\">"
                + "<input type=\"hidden\" name=\"csrf\" value=\"" + token(request.getSession()) + "\">"
                + "<input type=\"hidden\" name=\"action\" value=\"" + Html.esc(action) + "\">";
    }

    /** Start of a small form inside a table row that acts on the record with the given id. */
    protected String rowForm(HttpServletRequest request, String action, int id) {
        return formStart(request, action, "inline") + "<input type=\"hidden\" name=\"id\" value=\"" + id + "\">";
    }

    // ----------------------------------------------- reading form fields

    protected static String action(HttpServletRequest request) {
        String action = request.getParameter("action");
        return action == null ? "" : action;
    }

    protected static String text(HttpServletRequest request, String field, String label, int maxLength) {
        String value = optionalText(request, field, label, maxLength);
        if (value.isEmpty()) throw new AppException(label + " is required.");
        return value;
    }

    protected static String optionalText(HttpServletRequest request, String field, String label, int maxLength) {
        String value = request.getParameter(field);
        value = value == null ? "" : value.trim();
        if (value.length() > maxLength) {
            throw new AppException(label + " is too long (maximum " + maxLength + " characters).");
        }
        return value;
    }

    protected static int integer(HttpServletRequest request, String field, String label, int min, int max) {
        String value = request.getParameter(field);
        if (value == null || value.isBlank()) throw new AppException(label + " is required.");
        int number;
        try {
            number = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new AppException(label + " must be a whole number.");
        }
        if (number < min || number > max) {
            throw new AppException(label + " must be between " + Html.number(min) + " and " + Html.number(max) + ".");
        }
        return number;
    }

    protected static double decimal(HttpServletRequest request, String field, String label, double min, double max) {
        String value = request.getParameter(field);
        if (value == null || value.isBlank()) throw new AppException(label + " is required.");
        double number;
        try {
            number = Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new AppException(label + " must be a number.");
        }
        if (Double.isNaN(number) || number < min || number > max) {
            throw new AppException(label + " must be between " + min + " and " + max + ".");
        }
        return number;
    }

    protected static <E extends Enum<E>> E choice(HttpServletRequest request, String field, String label,
                                                  Class<E> type) {
        String value = request.getParameter(field);
        if (value == null || value.isBlank()) throw new AppException(label + " is required.");
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException("That is not a valid " + label.toLowerCase() + ".");
        }
    }

    /** The id of the record a table-row form acts on. */
    protected static int id(HttpServletRequest request, String field) {
        return integer(request, field, "Record", 1, Integer.MAX_VALUE);
    }
}
