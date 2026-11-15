/** Numbers shown on the dashboard. */
public class Stats {

    private final int openDisasters;
    private final int criticalDisasters;
    private final long affectedPeople;
    private final int teamsTotal;
    private final int teamsAvailable;
    private final int lowStockResources;
    private final int allocations;

    public Stats(int openDisasters, int criticalDisasters, long affectedPeople, int teamsTotal,
                 int teamsAvailable, int lowStockResources, int allocations) {
        this.openDisasters = openDisasters;
        this.criticalDisasters = criticalDisasters;
        this.affectedPeople = affectedPeople;
        this.teamsTotal = teamsTotal;
        this.teamsAvailable = teamsAvailable;
        this.lowStockResources = lowStockResources;
        this.allocations = allocations;
    }

    public int getOpenDisasters() { return openDisasters; }
    public int getCriticalDisasters() { return criticalDisasters; }
    public long getAffectedPeople() { return affectedPeople; }
    public int getTeamsTotal() { return teamsTotal; }
    public int getTeamsAvailable() { return teamsAvailable; }
    public int getLowStockResources() { return lowStockResources; }
    public int getAllocations() { return allocations; }
}
