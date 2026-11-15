import jakarta.servlet.http.HttpServletRequest;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** The dashboard: summary numbers, a Google Map, the open disasters and the latest alerts. */
public class DashboardServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private final transient StatsDao statsDao = new StatsDao();
    private final transient DisasterDao disasterDao = new DisasterDao();
    private final transient AlertDao alertDao = new AlertDao();

    @Override protected String path() { return "/dashboard"; }
    @Override protected String title() { return "Dashboard"; }

    /** The dashboard reloads itself every 30 seconds, so new alerts appear without any action. */
    @Override protected int refreshSeconds() { return 30; }

    @Override
    protected String body(HttpServletRequest request) throws SQLException {
        Stats stats = statsDao.load();
        List<Disaster> all = disasterDao.findAll();
        List<Disaster> open = new ArrayList<>();
        for (Disaster d : all) {
            if (d.getStatus() != DisasterStatus.RESOLVED) open.add(d);
        }

        StringBuilder h = new StringBuilder();

        // ---- summary cards
        h.append("<div class=\"cards\">")
         .append(card("Open disasters", Html.number(stats.getOpenDisasters()),
                 stats.getOpenDisasters() > 0 ? "orange" : "green"))
         .append(card("Critical", Html.number(stats.getCriticalDisasters()),
                 stats.getCriticalDisasters() > 0 ? "red" : "green"))
         .append(card("People affected", Html.number(stats.getAffectedPeople()), ""))
         .append(card("Teams available", stats.getTeamsAvailable() + " / " + stats.getTeamsTotal(),
                 stats.getTeamsAvailable() > 0 ? "green" : "red"))
         .append(card("Low stock items", Html.number(stats.getLowStockResources()),
                 stats.getLowStockResources() > 0 ? "orange" : "green"))
         .append(card("Allocations made", Html.number(stats.getAllocations()), ""))
         .append("</div>");

        // ---- map
        Disaster onMap = chooseMapDisaster(request.getParameter("map"), all, open);
        h.append("<div class=\"two-columns\"><div class=\"panel\">");
        if (onMap == null) {
            h.append("<h2>Map</h2><div class=\"empty\">No disasters to show.</div>");
        } else {
            h.append("<h2>Map <span class=\"muted\">&ndash; ").append(Html.esc(onMap.getLabel())).append("</span></h2>")
             .append("<iframe title=\"Disaster location on Google Maps\" src=\"")
             .append(Html.esc(onMap.getMapUrl())).append("\"></iframe>")
             .append("<p class=\"muted\">").append(Html.esc(onMap.getDescription())).append("</p>");
        }
        h.append("</div>");

        // ---- open disasters
        h.append("<div class=\"panel\"><h2>Open disasters</h2>");
        if (open.isEmpty()) h.append("<div class=\"empty\">No open disasters.</div>");
        for (Disaster d : open) {
            h.append("<div class=\"item\"><span>").append(Html.badge(d.getSeverity())).append(" ")
             .append(Html.esc(d.getLabel())).append("</span>")
             .append("<a class=\"button small plain\" href=\"/dashboard?map=").append(d.getId())
             .append("\">Show on map</a></div>");
        }

        // ---- latest alerts
        h.append("<h2>Latest alerts</h2>");
        List<Alert> alerts = alertDao.findLatest(6);
        if (alerts.isEmpty()) h.append("<div class=\"empty\">No alerts yet.</div>");
        for (Alert a : alerts) h.append(AlertServlet.alertRow(a));
        h.append("</div></div>");

        return h.toString();
    }

    /** The disaster named in the address (?map=id) if it exists, otherwise the newest open one. */
    private static Disaster chooseMapDisaster(String parameter, List<Disaster> all, List<Disaster> open) {
        if (parameter != null) {
            for (Disaster d : all) {
                if (String.valueOf(d.getId()).equals(parameter)) return d;
            }
        }
        if (!open.isEmpty()) return open.get(0);
        return all.isEmpty() ? null : all.get(0);
    }

    private static String card(String label, String number, String color) {
        return "<div class=\"card " + color + "\"><div class=\"number\">" + Html.esc(number)
                + "</div><div class=\"label\">" + Html.esc(label) + "</div></div>";
    }
}
