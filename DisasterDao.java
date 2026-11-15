import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Database access for the "disasters" table. */
public class DisasterDao {

    private static final String SELECT =
            "SELECT id, disaster_type, location, latitude, longitude, severity, status, "
                    + "affected_people, description, reported_at FROM disasters";

    private final AlertDao alerts = new AlertDao();

    public List<Disaster> findAll() throws SQLException {
        return list(SELECT + " ORDER BY id DESC");
    }

    /** Disasters that are not resolved yet. */
    public List<Disaster> findOpen() throws SQLException {
        return list(SELECT + " WHERE status <> 'RESOLVED' ORDER BY id DESC");
    }

    /**
     * Disasters that match the filters of the search form. A filter that is null is ignored.
     * The WHERE clause is built from fixed text only; the values are passed as parameters (?),
     * which protects against SQL injection.
     */
    public List<Disaster> search(String locationText, DisasterStatus status, Severity severity)
            throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT + " WHERE 1 = 1");
        List<String> parameters = new ArrayList<>();
        if (locationText != null) {
            // ! % and _ have a special meaning in LIKE, so they are escaped with !
            String escaped = locationText.toLowerCase().replace("!", "!!").replace("%", "!%").replace("_", "!_");
            sql.append(" AND LOWER(location) LIKE ? ESCAPE '!'");
            parameters.add("%" + escaped + "%");
        }
        if (status != null) {
            sql.append(" AND status = ?");
            parameters.add(status.name());
        }
        if (severity != null) {
            sql.append(" AND severity = ?");
            parameters.add(severity.name());
        }
        sql.append(" ORDER BY id DESC");

        List<Disaster> disasters = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) ps.setString(i + 1, parameters.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) disasters.add(read(rs));
            }
        }
        return disasters;
    }

    private List<Disaster> list(String sql) throws SQLException {
        List<Disaster> disasters = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) disasters.add(read(rs));
        }
        return disasters;
    }

    /** Finds one disaster inside a running transaction; fails with a message if it does not exist. */
    Disaster find(Connection connection, int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT + " WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return read(rs);
            }
        }
        throw new AppException("That disaster no longer exists.");
    }

    private static Disaster read(ResultSet rs) throws SQLException {
        return new Disaster(
                rs.getInt("id"),
                rs.getString("disaster_type"),
                rs.getString("location"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"),
                Severity.valueOf(rs.getString("severity")),
                DisasterStatus.valueOf(rs.getString("status")),
                rs.getInt("affected_people"),
                rs.getString("description"),
                Times.format(rs.getTimestamp("reported_at")));
    }

    /** Saves a new disaster and raises an alert about it. */
    public void add(String type, String location, double latitude, double longitude,
                    Severity severity, int affectedPeople, String description) throws SQLException {
        String sql = "INSERT INTO disasters (disaster_type, location, latitude, longitude, severity, status, "
                + "affected_people, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Database.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, type);
                ps.setString(2, location);
                ps.setDouble(3, latitude);
                ps.setDouble(4, longitude);
                ps.setString(5, severity.name());
                ps.setString(6, DisasterStatus.ACTIVE.name());
                ps.setInt(7, affectedPeople);
                ps.setString(8, description);
                ps.executeUpdate();
            }
            AlertLevel level = severity == Severity.CRITICAL ? AlertLevel.CRITICAL
                    : severity == Severity.HIGH ? AlertLevel.WARNING : AlertLevel.INFO;
            alerts.add(connection, level,
                    "New " + severity + " severity disaster reported: " + type + " at " + location);
        });
    }

    /** Changes the status. When a disaster is resolved, its rescue teams become available again. */
    public void updateStatus(int id, DisasterStatus status) throws SQLException {
        Database.inTransaction(connection -> {
            Disaster disaster = find(connection, id);
            try (PreparedStatement ps = connection.prepareStatement("UPDATE disasters SET status = ? WHERE id = ?")) {
                ps.setString(1, status.name());
                ps.setInt(2, id);
                ps.executeUpdate();
            }
            if (status == DisasterStatus.RESOLVED) {
                try (PreparedStatement ps = connection.prepareStatement(
                        "UPDATE teams SET disaster_id = NULL WHERE disaster_id = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
            }
            alerts.add(connection, AlertLevel.INFO, disaster.getLabel() + " is now marked " + status);
        });
    }

    /** Deletes a disaster. Teams working on it are released by the foreign key (ON DELETE SET NULL). */
    public void delete(int id) throws SQLException {
        Database.inTransaction(connection -> {
            find(connection, id);
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM disasters WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        });
    }
}
