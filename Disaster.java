/** A reported disaster event at a specific place (one row of the "disasters" table). */
public class Disaster {

    private final int id;
    private final String type;
    private final String location;
    private final double latitude;
    private final double longitude;
    private final Severity severity;
    private final DisasterStatus status;
    private final int affectedPeople;
    private final String description;
    private final String reportedAt;

    public Disaster(int id, String type, String location, double latitude, double longitude,
                    Severity severity, DisasterStatus status, int affectedPeople,
                    String description, String reportedAt) {
        this.id = id;
        this.type = type;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.severity = severity;
        this.status = status;
        this.affectedPeople = affectedPeople;
        this.description = description;
        this.reportedAt = reportedAt;
    }

    public int getId() { return id; }
    public String getType() { return type; }
    public String getLocation() { return location; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Severity getSeverity() { return severity; }
    public DisasterStatus getStatus() { return status; }
    public int getAffectedPeople() { return affectedPeople; }
    public String getDescription() { return description; }
    public String getReportedAt() { return reportedAt; }

    /** Short name used in lists and alerts, e.g. "Flood at Guwahati". */
    public String getLabel() {
        return type + " at " + location;
    }

    /** Address of the embedded Google Map centred on this disaster. */
    public String getMapUrl() {
        return "https://maps.google.com/maps?q=" + latitude + "," + longitude + "&z=9&output=embed";
    }
}
