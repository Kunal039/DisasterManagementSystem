import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Starts the Disaster Management System with one command (run it inside this folder):
 *
 *     java Run.java            starts the application on port 8080
 *     java Run.java 9090       starts it on another port
 *
 * What this class does:
 *   1. finds the Tomcat installation (the servlet library) and a JDBC driver jar,
 *   2. puts them on the classpath,
 *   3. starts Main.java. Java compiles the other .java files of this folder automatically,
 *      so there is no separate compile step and no .class files are created.
 *
 * Requirements: JDK 22 or newer, and Apache Tomcat 10.1 or newer (CATALINA_HOME must point to it).
 * JDBC driver: the H2 or MySQL driver jar is taken from this folder, or from the local Maven
 * repository (.m2 in your user home folder) if this folder contains no jar.
 */
public class Run {

    public static void main(String[] args) throws IOException, InterruptedException {
        if (Runtime.version().feature() < 22) {
            fail("JDK 22 or newer is needed. You are using Java " + Runtime.version().feature() + ".");
        }
        Path folder = Paths.get("").toAbsolutePath();
        if (!Files.exists(folder.resolve("Main.java"))) {
            fail("Run this command inside the project folder (the folder that contains Main.java).");
        }

        List<String> classpath = new ArrayList<>();

        // ---- Tomcat: servlet API and the servlet container
        Path tomcat = findTomcat();
        classpath.add(tomcat.resolve("lib") + java.io.File.separator + "*");   // "*" = every jar in the folder
        classpath.add(tomcat.resolve("bin").resolve("tomcat-juli.jar").toString());

        // ---- JDBC driver
        List<Path> drivers = jarsIn(folder);
        if (drivers.isEmpty()) {
            Path maven = Paths.get(System.getProperty("user.home"), ".m2", "repository");
            addNewestJar(drivers, maven.resolve(Paths.get("com", "h2database", "h2")));
            addNewestJar(drivers, maven.resolve(Paths.get("com", "mysql", "mysql-connector-j")));
        }
        if (drivers.isEmpty()) {
            fail("No JDBC driver found. Copy the H2 jar (h2-x.y.z.jar) or the MySQL Connector/J jar into this folder.");
        }
        for (Path driver : drivers) classpath.add(driver.toString());

        // ---- start: java -cp <classpath> Main.java [port]
        List<String> command = new ArrayList<>();
        command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
        String databaseUrl = System.getProperty("dms.db.url");
        if (databaseUrl != null) command.add("-Ddms.db.url=" + databaseUrl);
        command.add("-cp");
        command.add(String.join(System.getProperty("path.separator"), classpath));
        command.add("Main.java");
        for (String arg : args) command.add(arg);

        Process process = new ProcessBuilder(command).directory(folder.toFile()).inheritIO().start();
        System.exit(process.waitFor());
    }

    private static Path findTomcat() throws IOException {
        List<Path> candidates = new ArrayList<>();
        String home = System.getenv("CATALINA_HOME");
        if (home != null && !home.isBlank()) candidates.add(Paths.get(home));

        Path apache = Paths.get("C:\\Program Files\\Apache Software Foundation");
        if (Files.isDirectory(apache)) {
            try (DirectoryStream<Path> folders = Files.newDirectoryStream(apache, "Tomcat*")) {
                for (Path p : folders) candidates.add(p);
            }
        }
        for (Path candidate : candidates) {
            if (Files.exists(candidate.resolve("lib").resolve("servlet-api.jar"))) return candidate;
        }
        fail("Apache Tomcat was not found. Install Tomcat 10.1 or newer and set CATALINA_HOME to its folder.");
        return null;
    }

    private static List<Path> jarsIn(Path folder) throws IOException {
        List<Path> jars = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.jar")) {
            for (Path p : files) jars.add(p);
        }
        return jars;
    }

    /** Adds the jar of the highest version found below a Maven repository folder, if there is one. */
    private static void addNewestJar(List<Path> jars, Path artifactFolder) throws IOException {
        if (!Files.isDirectory(artifactFolder)) return;
        try (Stream<Path> files = Files.walk(artifactFolder, 2)) {
            files.filter(p -> p.getFileName().toString().endsWith(".jar"))
                 .filter(p -> !p.getFileName().toString().contains("-sources"))
                 .max((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()))
                 .ifPresent(jars::add);
        }
    }

    private static void fail(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
