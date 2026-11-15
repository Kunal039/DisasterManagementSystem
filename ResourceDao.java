import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Database access for the "resources" and "allocations" tables. */
public class ResourceDao {

    private static final String SELECT =
            "SELECT id, name, category, unit, quantity, low_stock_level FROM resources";

    private final AlertDao alerts = new AlertDao();
    private final DisasterDao disasters = new DisasterDao();

    public List<Resource> findAll() throws SQLException {
        List<Resource> resources = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT + " ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) resources.add(read(rs));
        }
        return resources;
    }

    /** History of resources sent to disasters, newest first. */
    public List<Allocation> findAllocations() throws SQLException {
        String sql = "SELECT resource_name, unit, quantity, disaster_label, allocated_at "
                + "FROM allocations ORDER BY id DESC";
        List<Allocation> allocations = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                allocations.add(new Allocation(
                        rs.getString("resource_name"),
                        rs.getString("unit"),
                        rs.getInt("quantity"),
                        rs.getString("disaster_label"),
                        Times.format(rs.getTimestamp("allocated_at"))));
            }
        }
        return allocations;
    }

    public void add(String name, String category, String unit, int quantity, int lowStockLevel)
            throws SQLException {
        String sql = "INSERT INTO resources (name, category, unit, quantity, low_stock_level) VALUES (?, ?, ?, ?, ?)";
        Database.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, category);
                ps.setString(3, unit);
                ps.setInt(4, quantity);
                ps.setInt(5, lowStockLevel);
                ps.executeUpdate();
            }
        });
    }

    /**
     * Sends stock to a disaster. Reducing the stock, saving the history record and raising
     * the alerts happen in one transaction, so the data can never be half-updated.
     */
    public void allocate(int resourceId, int disasterId, int quantity) throws SQLException {
        Database.inTransaction(connection -> {
            Resource before = find(connection, resourceId);
            Disaster disaster = disasters.find(connection, disasterId);
            if (disaster.getStatus() == DisasterStatus.RESOLVED) {
                throw new AppException("Resources cannot be sent to a resolved disaster.");
            }

            // "AND quantity >= ?" makes sure the stock can never go below zero
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE resources SET quantity = quantity - ? WHERE id = ? AND quantity >= ?")) {
                ps.setInt(1, quantity);
                ps.setInt(2, resourceId);
                ps.setInt(3, quantity);
                if (ps.executeUpdate() == 0) {
                    throw new AppException("Only " + before.getQuantity() + " " + before.getUnit()
                            + " of " + before.getName() + " in stock.");
                }
            }

            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO allocations (resource_name, unit, quantity, disaster_label) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, before.getName());
                ps.setString(2, before.getUnit());
                ps.setInt(3, quantity);
                ps.setString(4, disaster.getLabel());
                ps.executeUpdate();
            }

            alerts.add(connection, AlertLevel.INFO, quantity + " " + before.getUnit() + " of "
                    + before.getName() + " sent to " + disaster.getLabel());

            Resource after = find(connection, resourceId);
            if (after.isLowStock()) {
                alerts.add(connection, AlertLevel.WARNING, "Low stock: only " + after.getQuantity() + " "
                        + after.getUnit() + " of " + after.getName() + " left");
            }
        });
    }

    public void restock(int resourceId, int quantity) throws SQLException {
        Database.inTransaction(connection -> {
            Resource resource = find(connection, resourceId);
            if ((long) resource.getQuantity() + quantity > 1_000_000_000) {
                throw new AppException("Stock cannot be more than 1,000,000,000.");
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE resources SET quantity = quantity + ? WHERE id = ?")) {
                ps.setInt(1, quantity);
                ps.setInt(2, resourceId);
                ps.executeUpdate();
            }
        });
    }

    public void delete(int resourceId) throws SQLException {
        Database.inTransaction(connection -> {
            find(connection, resourceId);
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM resources WHERE id = ?")) {
                ps.setInt(1, resourceId);
                ps.executeUpdate();
            }
        });
    }

    private static Resource find(Connection connection, int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT + " WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return read(rs);
            }
        }
        throw new AppException("That resource no longer exists.");
    }

    private static Resource read(ResultSet rs) throws SQLException {
        return new Resource(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getString("unit"),
                rs.getInt("quantity"),
                rs.getInt("low_stock_level"));
    }
}
