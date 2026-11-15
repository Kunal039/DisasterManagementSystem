import java.sql.SQLException;

/** Made-up demonstration records, inserted only when the database is completely empty. */
public final class SampleData {

    private SampleData() { }

    public static void insertIfEmpty() throws SQLException {
        if (!new StatsDao().isDatabaseEmpty()) return;

        DisasterDao disasters = new DisasterDao();
        disasters.add("Flood", "Guwahati, Assam", 26.1445, 91.7362, Severity.CRITICAL, 12000,
                "River water has entered low-lying residential areas.");
        disasters.add("Cyclone", "Puri, Odisha", 19.8135, 85.8312, Severity.HIGH, 8000,
                "Strong winds have damaged houses and power lines along the coast.");
        disasters.add("Landslide", "Joshimath, Uttarakhand", 30.5550, 79.5650, Severity.MEDIUM, 600,
                "Road to the town is blocked by debris.");

        TeamDao teams = new TeamDao();
        teams.add("Alpha Rescue Unit", "Flood rescue", 25, "Kolkata");
        teams.add("Medical Response Team 1", "Medical aid", 12, "Bhubaneswar");
        teams.add("Mountain Rescue Squad", "Landslide and search", 18, "Dehradun");
        teams.add("Fire and Safety Unit", "Fire fighting", 20, "Delhi");

        ResourceDao resources = new ResourceDao();
        resources.add("Food packets", "Food", "packets", 5000, 500);
        resources.add("Drinking water", "Water", "litres", 20000, 2000);
        resources.add("Tents", "Shelter", "units", 300, 50);
        resources.add("First aid kits", "Medical", "kits", 400, 80);
        resources.add("Rescue boats", "Equipment", "boats", 15, 3);
    }
}
