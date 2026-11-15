import jakarta.servlet.http.HttpServletRequest;

import java.sql.SQLException;
import java.util.List;

/** The "Alerts" page: the alert feed and a form to broadcast a new alert. */
public class AlertServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private final transient AlertDao alertDao = new AlertDao();

    @Override protected String path() { return "/alerts"; }
    @Override protected String title() { return "Alerts"; }

    @Override
    protected String handlePost(HttpServletRequest request) throws SQLException {
        if (!action(request).equals("broadcast")) throw new AppException("Unknown action.");
        alertDao.broadcast(choice(request, "level", "Level", AlertLevel.class),
                text(request, "message", "Message", 200));
        return "Alert sent.";
    }

    @Override
    protected String body(HttpServletRequest request) throws SQLException {
        List<Alert> alerts = alertDao.findLatest(100);

        StringBuilder h = new StringBuilder();
        h.append("<div class=\"panel\"><h2>Broadcast an alert</h2>")
         .append(formStart(request, "broadcast", "form-grid"))
         .append("<label>Level <select name=\"level\" required>")
         .append(Html.options(AlertLevel.values(), AlertLevel.INFO)).append("</select></label>")
         .append("<label class=\"wide\">Message <input name=\"message\" maxlength=\"200\" required></label>")
         .append("<button type=\"submit\">Send alert</button></form></div>");

        h.append("<div class=\"panel\"><h2>Alert feed <span class=\"muted\">(latest 100) &nbsp;")
         .append("<a href=\"/alerts\">Refresh</a></span></h2>");
        if (alerts.isEmpty()) h.append("<div class=\"empty\">No alerts yet.</div>");
        for (Alert a : alerts) h.append(alertRow(a));
        h.append("</div>");
        return h.toString();
    }

    /** One line of the alert feed. Also used by the dashboard. */
    static String alertRow(Alert alert) {
        return "<div class=\"item\"><span>" + Html.badge(alert.getLevel()) + " " + Html.esc(alert.getMessage())
                + "</span><span class=\"time\">" + Html.esc(alert.getCreatedAt()) + "</span></div>";
    }
}
