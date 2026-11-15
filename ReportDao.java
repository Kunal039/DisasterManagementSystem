import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Summary queries (GROUP BY, SUM, COUNT, JOIN) for the Reports page. */
public class ReportDao {

    /** Columns of the disasters table that a summary may be grouped by. */
    public enum GroupBy {
        TYPE("disaster_type"), SEVERITY("severity"), STATUS("status");

        private final String column;

        GroupBy(String column) {
            this.column = column;
        }
    }

    /** Rows of {group, number of disasters, people affected}, the largest group first. */
    public List<String[]> disastersBy(GroupBy group) throws SQLException {
        // group.column is one of the fixed names above, never user input, so it is safe to put into the SQL text
        return rows("SELECT " + group.column + ", COUNT(*), COALESCE(SUM(affected_people), 0) "
                + "FROM disasters GROUP BY " + group.column + " ORDER BY COUNT(*) DESC, " + group.column, 3);
    }

    /** Rows of {disaster, resource, total quantity, unit}. */
    public List<String[]> resourcesSent() throws SQLException {
        return rows("SELECT disaster_label, resource_name, SUM(quantity), unit FROM allocations "
                + "GROUP BY disaster_label, resource_name, unit ORDER BY disaster_label, resource_name", 4);
    }

    /** Rows of {disaster type, location, number of teams, number of people in those teams}. */
    public List<String[]> teamsDeployed() throws SQLException {
        return rows("SELECT d.disaster_type, d.location, COUNT(t.id), COALESCE(SUM(t.members), 0) "
                + "FROM disasters d JOIN teams t ON t.disaster_id = d.id "
                + "GROUP BY d.id, d.disaster_type, d.location ORDER BY COUNT(t.id) DESC, d.location", 4);
    }

    private static List<String[]> rows(String sql, int columns) throws SQLException {
        List<String[]> rows = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String[] row = new String[columns];
                for (int i = 0; i < columns; i++) row[i] = rs.getString(i + 1);
                rows.add(row);
            }
        }
        return rows;
    }
}
