import jakarta.servlet.http.HttpServletRequest;

import java.sql.SQLException;
import java.util.List;

/** The "Reports" page: summaries of disasters, resources sent and teams deployed. */
public class ReportServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private final transient ReportDao reportDao = new ReportDao();

    @Override protected String path() { return "/reports"; }
    @Override protected String title() { return "Reports"; }

    @Override
    protected String body(HttpServletRequest request) throws SQLException {
        StringBuilder h = new StringBuilder();

        h.append("<div class=\"two-columns\"><div>")
         .append(disasterSummary("Disasters by type", "Type", reportDao.disastersBy(ReportDao.GroupBy.TYPE), false))
         .append("</div><div>")
         .append(disasterSummary("Disasters by severity", "Severity",
                 reportDao.disastersBy(ReportDao.GroupBy.SEVERITY), true))
         .append(disasterSummary("Disasters by status", "Status",
                 reportDao.disastersBy(ReportDao.GroupBy.STATUS), true))
         .append("</div></div>");

        // ---- resources sent
        List<String[]> sent = reportDao.resourcesSent();
        h.append("<div class=\"panel\"><h2>Resources sent to each disaster</h2><div class=\"table-wrap\"><table>")
         .append("<thead><tr><th>Disaster</th><th>Resource</th><th>Total sent</th></tr></thead><tbody>");
        if (sent.isEmpty()) h.append("<tr><td colspan=\"3\" class=\"empty\">No resources have been sent yet.</td></tr>");
        for (String[] row : sent) {
            h.append("<tr><td>").append(Html.esc(row[0])).append("</td><td>").append(Html.esc(row[1]))
             .append("</td><td>").append(number(row[2])).append(" ").append(Html.esc(row[3])).append("</td></tr>");
        }
        h.append("</tbody></table></div></div>");

        // ---- teams deployed
        List<String[]> teams = reportDao.teamsDeployed();
        h.append("<div class=\"panel\"><h2>Teams currently deployed</h2><div class=\"table-wrap\"><table>")
         .append("<thead><tr><th>Disaster</th><th>Teams</th><th>Rescue workers</th></tr></thead><tbody>");
        if (teams.isEmpty()) h.append("<tr><td colspan=\"3\" class=\"empty\">No team is deployed at the moment.</td></tr>");
        for (String[] row : teams) {
            h.append("<tr><td>").append(Html.esc(row[0] + " at " + row[1])).append("</td><td>")
             .append(number(row[2])).append("</td><td>").append(number(row[3])).append("</td></tr>");
        }
        h.append("</tbody></table></div></div>");
        return h.toString();
    }

    /** A table of {group, count, people affected} with a bar that shows each count compared to the largest. */
    private static String disasterSummary(String heading, String column, List<String[]> rows, boolean asBadge) {
        long largest = 1;
        for (String[] row : rows) largest = Math.max(largest, Long.parseLong(row[1]));

        StringBuilder h = new StringBuilder();
        h.append("<div class=\"panel\"><h2>").append(heading).append("</h2><div class=\"table-wrap\"><table>")
         .append("<thead><tr><th>").append(column).append("</th><th>Disasters</th><th></th>")
         .append("<th>People affected</th></tr></thead><tbody>");
        if (rows.isEmpty()) h.append("<tr><td colspan=\"4\" class=\"empty\">No disasters reported.</td></tr>");
        for (String[] row : rows) {
            long count = Long.parseLong(row[1]);
            long barWidth = Math.max(4, count * 120 / largest);   // in pixels, the largest group gets 120
            h.append("<tr><td>").append(asBadge ? Html.badge(row[0]) : Html.esc(row[0])).append("</td>")
             .append("<td>").append(Html.number(count)).append("</td>")
             .append("<td><span class=\"bar\" style=\"width:").append(barWidth).append("px\"></span></td>")
             .append("<td>").append(number(row[2])).append("</td></tr>");
        }
        h.append("</tbody></table></div></div>");
        return h.toString();
    }

    /** Formats a whole number that came from the database as text, e.g. "12000" becomes "12,000". */
    private static String number(String value) {
        try {
            return Html.number(new java.math.BigDecimal(value).longValue());
        } catch (NumberFormatException | NullPointerException e) {
            return Html.esc(value);
        }
    }
}
