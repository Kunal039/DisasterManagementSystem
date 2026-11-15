/** A relief resource held in stock, such as food packets or tents (one row of the "resources" table). */
public class Resource {

    private final int id;
    private final String name;
    private final String category;
    private final String unit;
    private final int quantity;
    private final int lowStockLevel;

    public Resource(int id, String name, String category, String unit, int quantity, int lowStockLevel) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.quantity = quantity;
        this.lowStockLevel = lowStockLevel;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getUnit() { return unit; }
    public int getQuantity() { return quantity; }
    public int getLowStockLevel() { return lowStockLevel; }

    public boolean isLowStock() {
        return quantity <= lowStockLevel;
    }
}
