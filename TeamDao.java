import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Database access for the "teams" table. */
public class TeamDao {

    private final AlertDao alerts = new AlertDao();
    private final DisasterDao disasters = new DisasterDao();

    /** All teams, each with the disaster it is working on (if any). */
    public List<RescueTeam> findAll() throws SQLException {
        String sql = "SELECT t.id, t.name, t.specialization, t.members, t.base_location, t.disaster_id, "
                + "d.disaster_type, d.location "
                + "FROM teams t LEFT JOIN disasters d ON d.id = t.disaster_id ORDER BY t.id";
        List<RescueTeam> teams = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int disasterId = rs.getInt("disaster_id");
                boolean deployed = !rs.wasNull();
                teams.add(new RescueTeam(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("specialization"),
                        rs.getInt("members"),
                        rs.getString("base_location"),
                        deployed ? disasterId : null,
                        deployed ? rs.getString("disaster_type") + " at " + rs.getString("location") : null));
            }
        }
        return teams;
    }

    public void add(String name, String specialization, int members, String baseLocation) throws SQLException {
        String sql = "INSERT INTO teams (name, specialization, members, base_location) VALUES (?, ?, ?, ?)";
        Database.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, specialization);
                ps.setInt(3, members);
                ps.setString(4, baseLocation);
                ps.executeUpdate();
            }
        });
    }

    /** Sends a free team to an open disaster. */
    public void assign(int teamId, int disasterId) throws SQLException {
        Database.inTransaction(connection -> {
            String teamName = findName(connection, teamId);
            Disaster disaster = disasters.find(connection, disasterId);
            if (disaster.getStatus() == DisasterStatus.RESOLVED) {
                throw new AppException("A team cannot be deployed to a resolved disaster.");
            }
            // "AND disaster_id IS NULL" makes sure a team is never deployed twice
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE teams SET disaster_id = ? WHERE id = ? AND disaster_id IS NULL")) {
                ps.setInt(1, disasterId);
                ps.setInt(2, teamId);
                if (ps.executeUpdate() == 0) {
                    throw new AppException(teamName + " is already deployed. Release it first.");
                }
            }
            alerts.add(connection, AlertLevel.INFO, teamName + " deployed to " + disaster.getLabel());
        });
    }

    /** Brings a deployed team back so it can be used again. */
    public void release(int teamId) throws SQLException {
        Database.inTransaction(connection -> {
            String teamName = findName(connection, teamId);
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE teams SET disaster_id = NULL WHERE id = ? AND disaster_id IS NOT NULL")) {
                ps.setInt(1, teamId);
                if (ps.executeUpdate() == 0) {
                    throw new AppException(teamName + " is not deployed.");
                }
            }
            alerts.add(connection, AlertLevel.INFO, teamName + " is back and available");
        });
    }

    public void delete(int teamId) throws SQLException {
        Database.inTransaction(connection -> {
            findName(connection, teamId);
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM teams WHERE id = ?")) {
                ps.setInt(1, teamId);
                ps.executeUpdate();
            }
        });
    }

    private static String findName(Connection connection, int teamId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM teams WHERE id = ?")) {
            ps.setInt(1, teamId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString(1);
            }
        }
        throw new AppException("That team no longer exists.");
    }
}
