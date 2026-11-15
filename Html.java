/**
 * Helper methods that build the web pages. The pages are produced entirely by Java code:
 * there are no separate HTML, CSS or JavaScript files in this project.
 */
public final class Html {

    private Html() { }

    /** The tabs of the navigation bar: address and label. */
    private static final String[][] TABS = {
        {"/dashboard", "Dashboard"},
        {"/disasters", "Disasters"},
        {"/teams", "Rescue Teams"},
        {"/resources", "Resources"},
        {"/alerts", "Alerts"},
        {"/reports", "Reports"},
        {"/users", "Users"}
    };

    /** Replaces the characters that have a special meaning in a web page, so user input is shown as plain text. */
    public static String esc(Object value) {
        if (value == null) return "";
        String text = value.toString();
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&': out.append("&amp;"); break;
                case '<': out.append("&lt;"); break;
                case '>': out.append("&gt;"); break;
                case '"': out.append("&quot;"); break;
                case '\'': out.append("&#39;"); break;
                default: out.append(c);
            }
        }
        return out.toString();
    }

    /** A small coloured label, e.g. for severity or status. */
    public static String badge(Object value) {
        return "<span class=\"badge " + esc(value) + "\">" + esc(value) + "</span>";
    }

    /** The option tags of a drop-down list; the entry equal to "selected" is pre-selected. */
    public static String options(Object[] values, Object selected) {
        StringBuilder out = new StringBuilder();
        for (Object value : values) {
            boolean isSelected = value.toString().equals(String.valueOf(selected));
            out.append("<option").append(isSelected ? " selected" : "").append(">")
               .append(esc(value)).append("</option>");
        }
        return out.toString();
    }

    public static String number(long value) {
        return String.format("%,d", value);
    }

    /**
     * Builds a complete page: header, navigation tabs, message and the given body.
     * userBar is the "Signed in as ..." part; when it is null (nobody logged in) the tabs are hidden.
     */
    public static String page(String title, String activePath, String userBar, String success, String error,
                              int refreshSeconds, String body) {
        StringBuilder out = new StringBuilder(body.length() + 6000);
        out.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\">")
           .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        if (refreshSeconds > 0) {
            out.append("<meta http-equiv=\"refresh\" content=\"").append(refreshSeconds).append("\">");
        }
        out.append("<title>").append(esc(title)).append(" - Disaster Management System</title>")
           .append("<style>").append(STYLE).append("</style></head><body>")
           .append("<header><div class=\"top\"><h1>Disaster Management System</h1>")
           .append(userBar == null ? "" : userBar).append("</div><nav>");
        if (userBar != null) {
            for (String[] tab : TABS) {
                out.append("<a href=\"").append(tab[0]).append("\"")
                   .append(tab[0].equals(activePath) ? " class=\"active\"" : "")
                   .append(">").append(tab[1]).append("</a>");
            }
        }
        out.append("</nav></header>");
        if (success != null) out.append("<div class=\"message\">").append(esc(success)).append("</div>");
        if (error != null) out.append("<div class=\"message error\">").append(esc(error)).append("</div>");
        out.append("<main>").append(body).append("</main></body></html>");
        return out.toString();
    }

    /** Look of the pages. */
    private static final String STYLE = String.join("\n",
        "* { box-sizing: border-box; }",
        "body { margin: 0; font-family: 'Segoe UI', Arial, sans-serif; font-size: 15px; color: #1f2933; background: #f2f4f7; }",
        "header { background: #12344d; color: #fff; padding: 14px 24px 0; }",
        "header .top { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }",
        "header h1 { margin: 0; font-size: 22px; }",
        "header .user { font-size: 13px; color: #c9d6e2; }",
        ".narrow { max-width: 420px; margin: 40px auto; }",
        ".form-stack { display: grid; gap: 12px; }",
        ".bar { display: inline-block; height: 10px; border-radius: 5px; background: #2f6f9f; vertical-align: middle; }",
        "nav { display: flex; flex-wrap: wrap; gap: 4px; }",
        "nav a { color: #c9d6e2; text-decoration: none; border-radius: 6px 6px 0 0; padding: 10px 16px; }",
        "nav a:hover { color: #fff; }",
        "nav a.active { background: #f2f4f7; color: #12344d; font-weight: 600; }",
        "main { max-width: 1200px; margin: 0 auto; padding: 20px 16px 40px; }",
        ".muted { color: #6b7785; font-size: 13px; font-weight: normal; }",
        ".message { max-width: 1168px; margin: 16px auto 0; padding: 10px 14px; border-radius: 6px;",
        "  background: #e3f6e8; color: #17622f; border: 1px solid #a9dfb8; }",
        ".message.error { background: #fde8e8; color: #9b1c1c; border-color: #f5b5b5; }",
        ".cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(160px, 1fr)); gap: 12px; margin-bottom: 16px; }",
        ".card { background: #fff; border-radius: 8px; padding: 14px 16px; border-left: 5px solid #2f6f9f;",
        "  box-shadow: 0 1px 2px rgba(0,0,0,.08); }",
        ".card.red { border-left-color: #c53030; }",
        ".card.orange { border-left-color: #dd6b20; }",
        ".card.green { border-left-color: #2f855a; }",
        ".card .number { font-size: 26px; font-weight: 700; }",
        ".card .label { color: #6b7785; font-size: 13px; }",
        ".panel { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 16px; box-shadow: 0 1px 2px rgba(0,0,0,.08); }",
        ".panel h2 { margin: 0 0 12px; font-size: 17px; }",
        ".panel h2:not(:first-child) { margin-top: 20px; }",
        ".two-columns { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; }",
        "@media (max-width: 850px) { .two-columns { grid-template-columns: 1fr; } }",
        "iframe { width: 100%; height: 380px; border: 1px solid #d5dbe1; border-radius: 6px; }",
        ".form-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(190px, 1fr)); gap: 12px; align-items: end; }",
        ".form-grid .wide { grid-column: span 2; }",
        "@media (max-width: 480px) { .form-grid .wide { grid-column: span 1; } }",
        "label { display: block; font-size: 13px; color: #4a5560; }",
        "input, select { width: 100%; margin-top: 4px; padding: 8px; font-size: 14px; border: 1px solid #c3cbd3;",
        "  border-radius: 5px; background: #fff; }",
        "form.inline { display: inline-block; margin: 0; }",
        "form.inline input, form.inline select { width: auto; margin-top: 0; padding: 5px; }",
        "form.inline input[type=number] { width: 80px; }",
        "button, a.button { display: inline-block; padding: 9px 14px; font-size: 14px; border: none; border-radius: 5px;",
        "  background: #2f6f9f; color: #fff; cursor: pointer; text-decoration: none; }",
        "button:hover, a.button:hover { background: #25597f; }",
        ".small { padding: 5px 9px !important; font-size: 13px !important; }",
        ".danger { background: #c53030 !important; }",
        ".danger:hover { background: #9b2424 !important; }",
        ".plain { background: #e4e9ee !important; color: #1f2933 !important; }",
        ".plain:hover { background: #d3dae1 !important; }",
        ".table-wrap { overflow-x: auto; }",
        "table { width: 100%; border-collapse: collapse; }",
        "th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #e6eaee; vertical-align: middle; }",
        "th { font-size: 13px; color: #6b7785; white-space: nowrap; }",
        "td.actions { white-space: nowrap; }",
        ".empty { color: #6b7785; padding: 12px 0; }",
        ".badge { display: inline-block; padding: 2px 8px; border-radius: 10px; font-size: 12px; font-weight: 600;",
        "  background: #e4e9ee; color: #36414d; }",
        ".badge.CRITICAL, .badge.ACTIVE { background: #fde8e8; color: #9b1c1c; }",
        ".badge.HIGH, .badge.WARNING, .badge.DEPLOYED { background: #feebc8; color: #9c4221; }",
        ".badge.MEDIUM { background: #fefcbf; color: #744210; }",
        ".badge.LOW, .badge.AVAILABLE, .badge.RESOLVED { background: #e3f6e8; color: #17622f; }",
        ".badge.CONTAINED, .badge.INFO { background: #dbeafe; color: #1e429f; }",
        ".item { display: flex; justify-content: space-between; align-items: center; gap: 10px; padding: 8px 0;",
        "  border-bottom: 1px solid #e6eaee; }",
        ".item:last-child { border-bottom: none; }",
        ".item .time { color: #6b7785; font-size: 12px; white-space: nowrap; }");
}
