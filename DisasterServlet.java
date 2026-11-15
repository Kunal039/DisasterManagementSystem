import jakarta.servlet.http.HttpServletRequest;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The "Disasters" page: report a disaster, change its status, delete it. */
public class DisasterServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private static final String[] TYPES =
            {"Flood", "Earthquake", "Cyclone", "Landslide", "Fire", "Drought", "Other"};

    private final transient DisasterDao disasterDao = new DisasterDao();
    private final transient TeamDao teamDao = new TeamDao();

    @Override protected String path() { return "/disasters"; }
    @Override protected String title() { return "Disasters"; }

    @Override
    protected String handlePost(HttpServletRequest request) throws SQLException {
        switch (action(request)) {
            case "add":
                String type = text(request, "type", "Type", 50);
                if (!List.of(TYPES).contains(type)) throw new AppException("That is not a valid type.");
                disasterDao.add(type,
                        text(request, "location", "Location name", 200),
                        decimal(request, "latitude", "Latitude", -90, 90),
                        decimal(request, "longitude", "Longitude", -180, 180),
                        choice(request, "severity", "Severity", Severity.class),
                        integer(request, "affectedPeople", "People affected", 0, 100_000_000),
                        optionalText(request, "description", "Description", 1000));
                return "Disaster reported.";
            case "status":
                disasterDao.updateStatus(id(request, "id"), choice(request, "status", "Status", DisasterStatus.class));
                return "Status updated.";
            case "delete":
                disasterDao.delete(id(request, "id"));
                return "Disaster deleted.";
            default:
                throw new AppException("Unknown action.");
        }
    }

    /** Turns the text of a search filter into an enum value; empty or unknown text means "no filter". */
    private static <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    protected String body(HttpServletRequest request) throws SQLException {
        // ---- search filters from the address, e.g. /disasters?q=assam&status=ACTIVE
        String query = request.getParameter("q");
        query = query == null ? "" : query.trim();
        if (query.length() > 100) query = query.substring(0, 100);
        DisasterStatus status = parse(DisasterStatus.class, request.getParameter("status"));
        Severity severity = parse(Severity.class, request.getParameter("severity"));
        boolean filtered = !query.isEmpty() || status != null || severity != null;
        List<Disaster> disasters = disasterDao.search(query.isEmpty() ? null : query, status, severity);

        // Names of the teams working on each disaster, e.g. {3 -> "Alpha Unit, Medical Team 1"}
        Map<Integer, String> teamNames = new HashMap<>();
        for (RescueTeam team : teamDao.findAll()) {
            if (team.isDeployed()) {
                teamNames.merge(team.getDisasterId(), team.getName(), (a, b) -> a + ", " + b);
            }
        }

        StringBuilder h = new StringBuilder();

        // ---- form
        h.append("<div class=\"panel\"><h2>Report a disaster</h2>")
         .append(formStart(request, "add", "form-grid"))
         .append("<label>Type <select name=\"type\" required>").append(Html.options(TYPES, "Flood"))
         .append("</select></label>")
         .append("<label>Location name <input name=\"location\" maxlength=\"200\" placeholder=\"City, State\" required></label>")
         .append("<label>Latitude <input name=\"latitude\" type=\"number\" step=\"any\" min=\"-90\" max=\"90\" ")
         .append("placeholder=\"e.g. 28.6139\" required></label>")
         .append("<label>Longitude <input name=\"longitude\" type=\"number\" step=\"any\" min=\"-180\" max=\"180\" ")
         .append("placeholder=\"e.g. 77.2090\" required></label>")
         .append("<label>Severity <select name=\"severity\" required>")
         .append(Html.options(Severity.values(), Severity.HIGH)).append("</select></label>")
         .append("<label>People affected <input name=\"affectedPeople\" type=\"number\" min=\"0\" max=\"100000000\" ")
         .append("step=\"1\" value=\"0\" required></label>")
         .append("<label class=\"wide\">Description <input name=\"description\" maxlength=\"1000\" ")
         .append("placeholder=\"Short description of the situation\"></label>")
         .append("<button type=\"submit\">Report disaster</button></form></div>");

        // ---- table
        h.append("<div class=\"panel\"><h2>").append(filtered ? "Search results" : "All disasters")
         .append(" <span class=\"muted\">(").append(disasters.size()).append(")</span></h2>")
         .append("<form method=\"get\" action=\"/disasters\" class=\"form-grid\">")
         .append("<label>Location contains <input name=\"q\" maxlength=\"100\" value=\"")
         .append(Html.esc(query)).append("\"></label>")
         .append("<label>Status <select name=\"status\"><option value=\"\">Any</option>")
         .append(Html.options(DisasterStatus.values(), status)).append("</select></label>")
         .append("<label>Severity <select name=\"severity\"><option value=\"\">Any</option>")
         .append(Html.options(Severity.values(), severity)).append("</select></label>")
         .append("<div><button type=\"submit\">Search</button> ")
         .append(filtered ? "<a class=\"button plain\" href=\"/disasters\">Clear</a>" : "").append("</div></form>")
         .append("<div class=\"table-wrap\"><table><thead><tr>")
         .append("<th>Type</th><th>Location</th><th>Severity</th><th>Affected</th><th>Reported</th>")
         .append("<th>Teams</th><th>Status</th><th></th></tr></thead><tbody>");
        if (disasters.isEmpty()) {
            h.append("<tr><td colspan=\"8\" class=\"empty\">")
             .append(filtered ? "No disaster matches the search." : "No disasters reported.").append("</td></tr>");
        }
        for (Disaster d : disasters) {
            String teams = teamNames.get(d.getId());
            h.append("<tr><td>").append(Html.esc(d.getType())).append("</td>")
             .append("<td title=\"").append(Html.esc(d.getDescription())).append("\">")
             .append(Html.esc(d.getLocation())).append("</td>")
             .append("<td>").append(Html.badge(d.getSeverity())).append("</td>")
             .append("<td>").append(Html.number(d.getAffectedPeople())).append("</td>")
             .append("<td>").append(Html.esc(d.getReportedAt())).append("</td>")
             .append("<td>").append(teams == null ? "<span class=\"muted\">none</span>" : Html.esc(teams)).append("</td>")
             .append("<td class=\"actions\">").append(rowForm(request, "status", d.getId()))
             .append("<select name=\"status\" aria-label=\"Status\">")
             .append(Html.options(DisasterStatus.values(), d.getStatus())).append("</select> ")
             .append("<button class=\"small plain\">Update</button></form></td>")
             .append("<td class=\"actions\"><a class=\"button small plain\" href=\"/dashboard?map=")
             .append(d.getId()).append("\">Map</a> ")
             .append(rowForm(request, "delete", d.getId()))
             .append("<button class=\"small danger\">Delete</button></form></td></tr>");
        }
        h.append("</tbody></table></div></div>");
        return h.toString();
    }
}
