/** A user account (one row of the "users" table). The password hash is deliberately not included. */
public class User {

    private final String username;
    private final String createdAt;

    public User(String username, String createdAt) {
        this.username = username;
        this.createdAt = createdAt;
    }

    public String getUsername() { return username; }
    public String getCreatedAt() { return createdAt; }
}
