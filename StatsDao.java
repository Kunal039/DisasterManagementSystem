import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Calculates the dashboard numbers with SQL aggregate queries. */
public class StatsDao {

    public Stats load() throws SQLException {
        try (Connection connection = Database.getConnection()) {
            return new Stats(
                    (int) number(connection, "SELECT COUNT(*) FROM disasters WHERE status <> 'RESOLVED'"),
                    (int) number(connection, "SELECT COUNT(*) FROM disasters "
                            + "WHERE status <> 'RESOLVED' AND severity = 'CRITICAL'"),
                    number(connection, "SELECT COALESCE(SUM(affected_people), 0) FROM disasters "
                            + "WHERE status <> 'RESOLVED'"),
                    (int) number(connection, "SELECT COUNT(*) FROM teams"),
                    (int) number(connection, "SELECT COUNT(*) FROM teams WHERE disaster_id IS NULL"),
                    (int) number(connection, "SELECT COUNT(*) FROM resources WHERE quantity <= low_stock_level"),
                    (int) number(connection, "SELECT COUNT(*) FROM allocations"));
        }
    }

    /** True when there are no disasters, teams or resources at all (a brand-new database). */
    public boolean isDatabaseEmpty() throws SQLException {
        try (Connection connection = Database.getConnection()) {
            return number(connection, "SELECT COUNT(*) FROM disasters") == 0
                    && number(connection, "SELECT COUNT(*) FROM teams") == 0
                    && number(connection, "SELECT COUNT(*) FROM resources") == 0;
        }
    }

    private static long number(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
