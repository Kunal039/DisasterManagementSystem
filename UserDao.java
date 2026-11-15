import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Database access for the "users" table, and password hashing.
 *
 * A password is never saved. Instead a hash is saved, made with PBKDF2 (HMAC-SHA256),
 * a random salt and many iterations, so the password cannot be read back from the database.
 * Stored format:  iterations:salt:hash   (salt and hash in Base64)
 */
public class UserDao {

    private static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Hash that is checked when the user name does not exist, so that case takes just as long. */
    private static final String DUMMY_HASH = hash("no-such-user");

    /** True while no account has been created yet (first start of the application). */
    public boolean noUsersExist() throws SQLException {
        try (Connection connection = Database.getConnection()) {
            return countUsers(connection) == 0;
        }
    }

    /** Creates the administrator account. Allowed only while no account exists. */
    public void createFirstUser(String username, String password) throws SQLException {
        String passwordHash = hash(password);
        Database.inTransaction(connection -> {
            if (countUsers(connection) > 0) {
                throw new AppException("An account already exists. Please log in.");
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (username, password_hash) VALUES (?, ?)")) {
                ps.setString(1, username);
                ps.setString(2, passwordHash);
                ps.executeUpdate();
            }
        });
    }

    /** Checks a user name and password typed into the login form. */
    public boolean passwordMatches(String username, String password) throws SQLException {
        String stored = null;
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT password_hash FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stored = rs.getString(1);
            }
        }
        if (stored == null) {
            verify(password, DUMMY_HASH);
            return false;
        }
        return verify(password, stored);
    }

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        try (Connection connection = Database.getConnection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT username, created_at FROM users ORDER BY username");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(new User(rs.getString("username"), Times.format(rs.getTimestamp("created_at"))));
            }
        }
        return users;
    }

    /** Adds another account. The user name must not be taken. */
    public void addUser(String username, String password) throws SQLException {
        String passwordHash = hash(password);
        Database.inTransaction(connection -> {
            if (exists(connection, username)) {
                throw new AppException("The user name \"" + username + "\" is already taken.");
            }
            try (PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (username, password_hash) VALUES (?, ?)")) {
                ps.setString(1, username);
                ps.setString(2, passwordHash);
                ps.executeUpdate();
            }
        });
    }

    /** Changes a password. The current password must be given, so nobody else can change it. */
    public void changePassword(String username, String currentPassword, String newPassword) throws SQLException {
        if (!passwordMatches(username, currentPassword)) {
            throw new AppException("The current password is wrong.");
        }
        String passwordHash = hash(newPassword);
        Database.inTransaction(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE users SET password_hash = ? WHERE username = ?")) {
                ps.setString(1, passwordHash);
                ps.setString(2, username);
                ps.executeUpdate();
            }
        });
    }

    public void delete(String username) throws SQLException {
        Database.inTransaction(connection -> {
            if (!exists(connection, username)) throw new AppException("That account no longer exists.");
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM users WHERE username = ?")) {
                ps.setString(1, username);
                ps.executeUpdate();
            }
        });
    }

    /** True if the account exists. Used to end the session of an account that was deleted. */
    public boolean exists(String username) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            return exists(connection, username);
        }
    }

    private static boolean exists(Connection connection, String username) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // ------------------------------------------------- rules for new accounts

    /** Checks a user name chosen for a new account. */
    public static void checkUsername(String username) {
        if (!username.matches("[a-z0-9._-]{3,30}")) {
            throw new AppException(
                    "User name: 3 to 30 characters; small letters, digits, dot, dash and underscore only.");
        }
    }

    /** Checks a newly chosen password and its repetition. */
    public static void checkNewPassword(String password, String repeated) {
        if (password.length() < 8) throw new AppException("The password must have at least 8 characters.");
        if (password.length() > 100) throw new AppException("The password is too long (maximum 100 characters).");
        if (!password.equals(repeated)) throw new AppException("The two passwords are not the same.");
    }

    private static int countUsers(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // --------------------------------------------------------- password hashing

    private static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password, salt, ITERATIONS);
        Base64.Encoder base64 = Base64.getEncoder();
        return ITERATIONS + ":" + base64.encodeToString(salt) + ":" + base64.encodeToString(hash);
    }

    private static boolean verify(String password, String stored) {
        String[] parts = stored.split(":");
        if (parts.length != 3) return false;
        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            // isEqual compares in constant time, so the comparison leaks nothing about the hash
            return MessageDigest.isEqual(expected, pbkdf2(password, salt, iterations));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is not available in this Java installation", e);
        } finally {
            spec.clearPassword();
        }
    }
}
