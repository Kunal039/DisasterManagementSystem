/** A record of resources sent from stock to a disaster site (one row of the "allocations" table). */
public class Allocation {

    private final String resourceName;
    private final String unit;
    private final int quantity;
    private final String disasterLabel;
    private final String allocatedAt;

    public Allocation(String resourceName, String unit, int quantity, String disasterLabel, String allocatedAt) {
        this.resourceName = resourceName;
        this.unit = unit;
        this.quantity = quantity;
        this.disasterLabel = disasterLabel;
        this.allocatedAt = allocatedAt;
    }

    public String getResourceName() { return resourceName; }
    public String getUnit() { return unit; }
    public int getQuantity() { return quantity; }
    public String getDisasterLabel() { return disasterLabel; }
    public String getAllocatedAt() { return allocatedAt; }
}
