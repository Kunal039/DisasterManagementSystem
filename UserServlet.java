import jakarta.servlet.http.HttpServletRequest;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/** The "Users" page: add accounts, delete accounts and change your own password. */
public class UserServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private final transient UserDao userDao = new UserDao();

    @Override protected String path() { return "/users"; }
    @Override protected String title() { return "Users"; }

    @Override
    protected String handlePost(HttpServletRequest request) throws SQLException {
        String me = currentUser(request);
        switch (action(request)) {
            case "add":
                String username = text(request, "username", "User name", 30).toLowerCase(Locale.ROOT);
                String password = LoginServlet.password(request, "password");
                UserDao.checkUsername(username);
                UserDao.checkNewPassword(password, LoginServlet.password(request, "confirm"));
                userDao.addUser(username, password);
                return "Account \"" + username + "\" created.";

            case "password":
                String newPassword = LoginServlet.password(request, "password");
                UserDao.checkNewPassword(newPassword, LoginServlet.password(request, "confirm"));
                userDao.changePassword(me, LoginServlet.password(request, "current"), newPassword);
                return "Your password was changed.";

            case "delete":
                String target = text(request, "username", "User name", 30);
                if (target.equals(me)) throw new AppException("You cannot delete your own account.");
                userDao.delete(target);
                return "Account \"" + target + "\" deleted.";

            default:
                throw new AppException("Unknown action.");
        }
    }

    @Override
    protected String body(HttpServletRequest request) throws SQLException {
        String me = currentUser(request);
        List<User> users = userDao.findAll();
        String passwordFields =
                "<label>New password <span class=\"muted\">(at least 8 characters)</span>"
                + "<input name=\"password\" type=\"password\" minlength=\"8\" maxlength=\"100\" "
                + "autocomplete=\"new-password\" required></label>"
                + "<label>Repeat password <input name=\"confirm\" type=\"password\" minlength=\"8\" "
                + "maxlength=\"100\" autocomplete=\"new-password\" required></label>";

        StringBuilder h = new StringBuilder();

        // ---- add account
        h.append("<div class=\"panel\"><h2>Add an account</h2>")
         .append(formStart(request, "add", "form-grid"))
         .append("<label>User name <input name=\"username\" maxlength=\"30\" autocomplete=\"off\" required></label>")
         .append(passwordFields)
         .append("<button type=\"submit\">Add account</button></form></div>");

        // ---- change own password
        h.append("<div class=\"panel\"><h2>Change my password</h2>")
         .append(formStart(request, "password", "form-grid"))
         .append("<label>Current password <input name=\"current\" type=\"password\" maxlength=\"100\" ")
         .append("autocomplete=\"current-password\" required></label>")
         .append(passwordFields)
         .append("<button type=\"submit\">Change password</button></form></div>");

        // ---- list
        h.append("<div class=\"panel\"><h2>Accounts</h2><div class=\"table-wrap\"><table><thead><tr>")
         .append("<th>User name</th><th>Created</th><th></th></tr></thead><tbody>");
        for (User u : users) {
            h.append("<tr><td>").append(Html.esc(u.getUsername()));
            if (u.getUsername().equals(me)) h.append(" <span class=\"badge INFO\">you</span>");
            h.append("</td><td>").append(Html.esc(u.getCreatedAt())).append("</td><td class=\"actions\">");
            if (!u.getUsername().equals(me)) {
                h.append(formStart(request, "delete", "inline"))
                 .append("<input type=\"hidden\" name=\"username\" value=\"").append(Html.esc(u.getUsername()))
                 .append("\"><button class=\"small danger\">Delete</button></form>");
            }
            h.append("</td></tr>");
        }
        h.append("</tbody></table></div></div>");
        return h.toString();
    }
}
