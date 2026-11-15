import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Database access for the "alerts" table. */
public class AlertDao {

    /** Returns the newest alerts first. */
    public List<Alert> findLatest(int limit) throws SQLException {
        String sql = "SELECT alert_level, message, created_at FROM alerts ORDER BY id DESC LIMIT ?";
        List<Alert> alerts = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    alerts.add(new Alert(
                            AlertLevel.valueOf(rs.getString("alert_level")),
                            rs.getString("message"),
                            Times.format(rs.getTimestamp("created_at"))));
                }
            }
        }
        return alerts;
    }

    /** Adds an alert as part of a transaction that is already running. */
    public void add(Connection connection, AlertLevel level, String message) throws SQLException {
        String sql = "INSERT INTO alerts (alert_level, message) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, level.name());
            ps.setString(2, message);
            ps.executeUpdate();
        }
    }

    /** Adds an alert typed in by a user. */
    public void broadcast(AlertLevel level, String message) throws SQLException {
        Database.inTransaction(connection -> add(connection, level, message));
    }
}
