import jakarta.servlet.http.HttpServletRequest;

import java.sql.SQLException;
import java.util.List;

/** The "Rescue Teams" page: add teams, deploy them to a disaster, release them. */
public class TeamServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private final transient TeamDao teamDao = new TeamDao();
    private final transient DisasterDao disasterDao = new DisasterDao();

    @Override protected String path() { return "/teams"; }
    @Override protected String title() { return "Rescue Teams"; }

    @Override
    protected String handlePost(HttpServletRequest request) throws SQLException {
        switch (action(request)) {
            case "add":
                teamDao.add(text(request, "name", "Team name", 200),
                        text(request, "specialization", "Specialization", 200),
                        integer(request, "members", "Members", 1, 100_000),
                        text(request, "baseLocation", "Base location", 200));
                return "Team added.";
            case "assign":
                teamDao.assign(id(request, "id"), id(request, "disasterId"));
                return "Team deployed.";
            case "release":
                teamDao.release(id(request, "id"));
                return "Team released.";
            case "delete":
                teamDao.delete(id(request, "id"));
                return "Team deleted.";
            default:
                throw new AppException("Unknown action.");
        }
    }

    @Override
    protected String body(HttpServletRequest request) throws SQLException {
        List<RescueTeam> teams = teamDao.findAll();
        String disasterOptions = disasterOptions(disasterDao.findOpen());

        StringBuilder h = new StringBuilder();

        // ---- form
        h.append("<div class=\"panel\"><h2>Add a rescue team</h2>")
         .append(formStart(request, "add", "form-grid"))
         .append("<label>Team name <input name=\"name\" maxlength=\"200\" required></label>")
         .append("<label>Specialization <input name=\"specialization\" maxlength=\"200\" ")
         .append("placeholder=\"e.g. Medical aid\" required></label>")
         .append("<label>Members <input name=\"members\" type=\"number\" min=\"1\" max=\"100000\" step=\"1\" ")
         .append("value=\"10\" required></label>")
         .append("<label>Base location <input name=\"baseLocation\" maxlength=\"200\" required></label>")
         .append("<button type=\"submit\">Add team</button></form></div>");

        // ---- table
        h.append("<div class=\"panel\"><h2>Rescue teams</h2><div class=\"table-wrap\"><table><thead><tr>")
         .append("<th>Name</th><th>Specialization</th><th>Members</th><th>Base</th><th>Status</th>")
         .append("<th>Assignment</th><th></th></tr></thead><tbody>");
        if (teams.isEmpty()) {
            h.append("<tr><td colspan=\"7\" class=\"empty\">No rescue teams added.</td></tr>");
        }
        for (RescueTeam t : teams) {
            h.append("<tr><td>").append(Html.esc(t.getName())).append("</td>")
             .append("<td>").append(Html.esc(t.getSpecialization())).append("</td>")
             .append("<td>").append(t.getMembers()).append("</td>")
             .append("<td>").append(Html.esc(t.getBaseLocation())).append("</td>")
             .append("<td>").append(Html.badge(t.getStatus())).append("</td>")
             .append("<td class=\"actions\">");
            if (t.isDeployed()) {
                h.append(Html.esc(t.getDisasterLabel())).append(" ")
                 .append(rowForm(request, "release", t.getId()))
                 .append("<button class=\"small plain\">Release</button></form>");
            } else if (!disasterOptions.isEmpty()) {
                h.append(rowForm(request, "assign", t.getId()))
                 .append("<select name=\"disasterId\" aria-label=\"Disaster\">").append(disasterOptions)
                 .append("</select> <button class=\"small\">Deploy</button></form>");
            } else {
                h.append("<span class=\"muted\">no open disasters</span>");
            }
            h.append("</td><td class=\"actions\">").append(rowForm(request, "delete", t.getId()))
             .append("<button class=\"small danger\">Delete</button></form></td></tr>");
        }
        h.append("</tbody></table></div></div>");
        return h.toString();
    }

    /** Drop-down entries for choosing an open disaster. Also used by the Resources page. */
    static String disasterOptions(List<Disaster> open) {
        StringBuilder options = new StringBuilder();
        for (Disaster d : open) {
            options.append("<option value=\"").append(d.getId()).append("\">")
                   .append(Html.esc(d.getLabel())).append("</option>");
        }
        return options.toString();
    }
}
