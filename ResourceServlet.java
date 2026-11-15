import jakarta.servlet.http.HttpServletRequest;

import java.sql.SQLException;
import java.util.List;

/** The "Resources" page: stock of relief material, sending stock to a disaster, restocking, history. */
public class ResourceServlet extends BaseServlet {
    private static final long serialVersionUID = 1L;

    private static final String[] CATEGORIES = {"Food", "Water", "Shelter", "Medical", "Equipment", "Other"};
    private static final int MAX_QUANTITY = 1_000_000_000;

    private final transient ResourceDao resourceDao = new ResourceDao();
    private final transient DisasterDao disasterDao = new DisasterDao();

    @Override protected String path() { return "/resources"; }
    @Override protected String title() { return "Resources"; }

    @Override
    protected String handlePost(HttpServletRequest request) throws SQLException {
        switch (action(request)) {
            case "add":
                String category = text(request, "category", "Category", 50);
                if (!List.of(CATEGORIES).contains(category)) throw new AppException("That is not a valid category.");
                resourceDao.add(text(request, "name", "Name", 200), category,
                        text(request, "unit", "Unit", 50),
                        integer(request, "quantity", "Quantity", 0, MAX_QUANTITY),
                        integer(request, "lowStockLevel", "Low stock level", 0, MAX_QUANTITY));
                return "Resource added.";
            case "allocate":
                resourceDao.allocate(id(request, "id"), id(request, "disasterId"),
                        integer(request, "quantity", "Quantity", 1, MAX_QUANTITY));
                return "Resources sent.";
            case "restock":
                resourceDao.restock(id(request, "id"), integer(request, "quantity", "Quantity", 1, MAX_QUANTITY));
                return "Stock updated.";
            case "delete":
                resourceDao.delete(id(request, "id"));
                return "Resource deleted.";
            default:
                throw new AppException("Unknown action.");
        }
    }

    @Override
    protected String body(HttpServletRequest request) throws SQLException {
        List<Resource> resources = resourceDao.findAll();
        List<Allocation> allocations = resourceDao.findAllocations();
        String disasterOptions = TeamServlet.disasterOptions(disasterDao.findOpen());

        StringBuilder h = new StringBuilder();

        // ---- form
        h.append("<div class=\"panel\"><h2>Add a resource</h2>")
         .append(formStart(request, "add", "form-grid"))
         .append("<label>Name <input name=\"name\" maxlength=\"200\" placeholder=\"e.g. Blankets\" required></label>")
         .append("<label>Category <select name=\"category\" required>").append(Html.options(CATEGORIES, "Food"))
         .append("</select></label>")
         .append("<label>Unit <input name=\"unit\" maxlength=\"50\" placeholder=\"e.g. pieces\" required></label>")
         .append("<label>Quantity in stock <input name=\"quantity\" type=\"number\" min=\"0\" step=\"1\" ")
         .append("value=\"100\" required></label>")
         .append("<label>Low stock warning at <input name=\"lowStockLevel\" type=\"number\" min=\"0\" step=\"1\" ")
         .append("value=\"10\" required></label>")
         .append("<button type=\"submit\">Add resource</button></form></div>");

        // ---- stock table
        h.append("<div class=\"panel\"><h2>Stock</h2><div class=\"table-wrap\"><table><thead><tr>")
         .append("<th>Resource</th><th>Category</th><th>In stock</th><th>Send to a disaster</th>")
         .append("<th>Restock</th><th></th></tr></thead><tbody>");
        if (resources.isEmpty()) {
            h.append("<tr><td colspan=\"6\" class=\"empty\">No resources added.</td></tr>");
        }
        for (Resource r : resources) {
            h.append("<tr><td>").append(Html.esc(r.getName())).append("</td>")
             .append("<td>").append(Html.esc(r.getCategory())).append("</td>")
             .append("<td>").append(Html.number(r.getQuantity())).append(" ").append(Html.esc(r.getUnit()))
             .append(r.isLowStock() ? " <span class=\"badge WARNING\">LOW STOCK</span>" : "").append("</td>")
             .append("<td class=\"actions\">");
            if (disasterOptions.isEmpty()) {
                h.append("<span class=\"muted\">no open disasters</span>");
            } else {
                h.append(rowForm(request, "allocate", r.getId()))
                 .append("<input type=\"number\" name=\"quantity\" min=\"1\" step=\"1\" placeholder=\"Qty\" ")
                 .append("aria-label=\"Quantity to send\" required> ")
                 .append("<select name=\"disasterId\" aria-label=\"Disaster\">").append(disasterOptions)
                 .append("</select> <button class=\"small\">Send</button></form>");
            }
            h.append("</td><td class=\"actions\">").append(rowForm(request, "restock", r.getId()))
             .append("<input type=\"number\" name=\"quantity\" min=\"1\" step=\"1\" placeholder=\"Qty\" ")
             .append("aria-label=\"Quantity to add\" required> ")
             .append("<button class=\"small plain\">Add</button></form></td>")
             .append("<td class=\"actions\">").append(rowForm(request, "delete", r.getId()))
             .append("<button class=\"small danger\">Delete</button></form></td></tr>");
        }
        h.append("</tbody></table></div></div>");

        // ---- history table
        h.append("<div class=\"panel\"><h2>Allocation history</h2><div class=\"table-wrap\"><table><thead><tr>")
         .append("<th>When</th><th>Resource</th><th>Quantity</th><th>Sent to</th></tr></thead><tbody>");
        if (allocations.isEmpty()) {
            h.append("<tr><td colspan=\"4\" class=\"empty\">No resources have been sent yet.</td></tr>");
        }
        for (Allocation a : allocations) {
            h.append("<tr><td>").append(Html.esc(a.getAllocatedAt())).append("</td>")
             .append("<td>").append(Html.esc(a.getResourceName())).append("</td>")
             .append("<td>").append(Html.number(a.getQuantity())).append(" ").append(Html.esc(a.getUnit())).append("</td>")
             .append("<td>").append(Html.esc(a.getDisasterLabel())).append("</td></tr>");
        }
        h.append("</tbody></table></div></div>");
        return h.toString();
    }
}
