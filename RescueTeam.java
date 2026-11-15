/** A rescue team (one row of the "teams" table). It can work on one disaster at a time. */
public class RescueTeam {

    private final int id;
    private final String name;
    private final String specialization;
    private final int members;
    private final String baseLocation;
    private final Integer disasterId;
    private final String disasterLabel;

    public RescueTeam(int id, String name, String specialization, int members, String baseLocation,
                      Integer disasterId, String disasterLabel) {
        this.id = id;
        this.name = name;
        this.specialization = specialization;
        this.members = members;
        this.baseLocation = baseLocation;
        this.disasterId = disasterId;
        this.disasterLabel = disasterLabel;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getSpecialization() { return specialization; }
    public int getMembers() { return members; }
    public String getBaseLocation() { return baseLocation; }
    public Integer getDisasterId() { return disasterId; }
    public String getDisasterLabel() { return disasterLabel; }

    public boolean isDeployed() {
        return disasterId != null;
    }

    public String getStatus() {
        return isDeployed() ? "DEPLOYED" : "AVAILABLE";
    }
}
