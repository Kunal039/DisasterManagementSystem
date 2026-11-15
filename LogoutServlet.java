import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** Logs the user out. It is called by the "Log out" button in the page header. */
public class LogoutServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    @Override protected String path() { return "/logout"; }
    @Override protected String title() { return "Log out"; }
    @Override protected String body(HttpServletRequest request) { return ""; }

    @Override
    protected String redirectAfterPost(HttpServletRequest request) {
        return "/login";
    }

    /** Logging out is done with the button (a POST request); opening the address does nothing. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect("/dashboard");
    }

    @Override
    protected String handlePost(HttpServletRequest request) {
        request.getSession().invalidate();   // throws away the session and everything stored in it
        return "You have been logged out.";
    }
}
