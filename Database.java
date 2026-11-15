import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens JDBC connections and creates the tables when the application starts.
 *
 * Default database: H2, a database written in Java. It needs no installation;
 * the data is stored in the folder "disaster-management-db" in your user home folder.
 *
 * To use MySQL instead:
 *   1. Start MySQL and run:  CREATE DATABASE disaster_management;
 *   2. Change URL and USER below to the MySQL values shown in the comments.
 *   3. Set the environment variable DMS_DB_PASSWORD to your MySQL password
 *      (the password is deliberately not written in the source code).
 */
public final class Database {

    private static final String URL = "jdbc:h2:file:~/disaster-management-db/dms;MODE=MySQL;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    // MySQL:
    // private static final String URL = "jdbc:mysql://localhost:3306/disaster_management";
    // private static final String USER = "root";

    /** The table definitions. They work on both H2 and MySQL. */
    private static final String[] TABLES = {
        "CREATE TABLE IF NOT EXISTS disasters ("
            + " id INT AUTO_INCREMENT PRIMARY KEY,"
            + " disaster_type VARCHAR(50) NOT NULL,"
            + " location VARCHAR(200) NOT NULL,"
            + " latitude DOUBLE NOT NULL,"
            + " longitude DOUBLE NOT NULL,"
            + " severity VARCHAR(10) NOT NULL,"
            + " status VARCHAR(10) NOT NULL,"
            + " affected_people INT NOT NULL,"
            + " description VARCHAR(1000) NOT NULL,"
            + " reported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)",

        "CREATE TABLE IF NOT EXISTS teams ("
            + " id INT AUTO_INCREMENT PRIMARY KEY,"
            + " name VARCHAR(200) NOT NULL,"
            + " specialization VARCHAR(200) NOT NULL,"
            + " members INT NOT NULL,"
            + " base_location VARCHAR(200) NOT NULL,"
            + " disaster_id INT NULL,"
            + " CONSTRAINT fk_team_disaster FOREIGN KEY (disaster_id)"
            + "   REFERENCES disasters (id) ON DELETE SET NULL)",

        "CREATE TABLE IF NOT EXISTS resources ("
            + " id INT AUTO_INCREMENT PRIMARY KEY,"
            + " name VARCHAR(200) NOT NULL,"
            + " category VARCHAR(50) NOT NULL,"
            + " unit VARCHAR(50) NOT NULL,"
            + " quantity INT NOT NULL,"
            + " low_stock_level INT NOT NULL)",

        "CREATE TABLE IF NOT EXISTS allocations ("
            + " id INT AUTO_INCREMENT PRIMARY KEY,"
            + " resource_name VARCHAR(200) NOT NULL,"
            + " unit VARCHAR(50) NOT NULL,"
            + " quantity INT NOT NULL,"
            + " disaster_label VARCHAR(300) NOT NULL,"
            + " allocated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)",

        "CREATE TABLE IF NOT EXISTS alerts ("
            + " id INT AUTO_INCREMENT PRIMARY KEY,"
            + " alert_level VARCHAR(10) NOT NULL,"
            + " message VARCHAR(600) NOT NULL,"
            + " created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)",

        // Passwords are never stored; only a salted PBKDF2 hash is (see UserDao)
        "CREATE TABLE IF NOT EXISTS users ("
            + " id INT AUTO_INCREMENT PRIMARY KEY,"
            + " username VARCHAR(30) NOT NULL UNIQUE,"
            + " password_hash VARCHAR(255) NOT NULL,"
            + " created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)"
    };

    /** A piece of work that must run inside one database transaction. */
    public interface Work {
        void run(Connection connection) throws SQLException;
    }

    private static String url;
    private static String user;
    private static String password;

    private Database() { }

    /** Loads the JDBC driver and creates the tables if they do not exist. */
    public static synchronized void init() throws SQLException {
        // -Ddms.db.url=... on the command line can replace the URL (useful for testing)
        // or the environment variable DMS_DB_URL (used on a hosting service)
        url = System.getProperty("dms.db.url", Settings.value("DMS_DB_URL", URL));
        user = Settings.value("DMS_DB_USER", USER);
        password = System.getenv().getOrDefault("DMS_DB_PASSWORD", "");

        String driver = url.startsWith("jdbc:mysql:") ? "com.mysql.cj.jdbc.Driver" : "org.h2.Driver";
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver " + driver + " is not on the classpath. Start the program with: java Run.java");
        }

        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            for (String table : TABLES) {
                statement.execute(table);
            }
        }
    }

    public static Connection getConnection() throws SQLException {
        if (url == null) throw new SQLException("Database.init() has not been called");
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Runs the work in a transaction: either every statement is saved (commit)
     * or, if anything fails, none of them is (rollback).
     */
    public static void inTransaction(Work work) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                work.run(connection);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }
}
