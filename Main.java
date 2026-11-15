import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;

import jakarta.servlet.http.HttpServlet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Starting point of the Disaster Management System.
 *
 * It prepares the database (JDBC), then starts the Tomcat servlet container from Java code
 * and registers the servlets. No web.xml or other configuration file is needed.
 *
 * Start it with:   java Run.java          (Run.java sets up the classpath and then starts this class)
 * Then open:       http://localhost:8080
 */
public class Main {

    // Keeps Tomcat's start-up messages short (a field, so the setting is not garbage-collected)
    private static final Logger TOMCAT_LOG = Logger.getLogger("org.apache");

    public static void main(String[] args) throws Exception {
        int port;
        try {
            port = Settings.port(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage() + "  Example: java Run.java 9090");
            return;
        }

        Database.init();
        SampleData.insertIfEmpty();

        TOMCAT_LOG.setLevel(Level.WARNING);
        Path workFolder = Paths.get(System.getProperty("java.io.tmpdir"), "dms-tomcat-" + port);
        Files.createDirectories(workFolder);

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(workFolder.toString());
        tomcat.setPort(port);

        Connector connector = tomcat.getConnector();
        connector.setProperty("address", Settings.bindAddress());   // default: reachable only from this computer
        connector.setThrowOnFailure(true);               // fail clearly if the port is already in use

        if (Settings.isPublic()) {
            // A hosting service puts an HTTPS proxy in front of the application. This valve reads the
            // proxy's X-Forwarded-Proto header, so Tomcat knows the visitor uses HTTPS and marks the
            // session cookie "Secure" (sent over HTTPS only).
            RemoteIpValve proxy = new RemoteIpValve();
            proxy.setProtocolHeader("X-Forwarded-Proto");
            tomcat.getHost().getPipeline().addValve(proxy);
        }

        Context context = tomcat.addContext("", workFolder.toString());
        Rfc6265CookieProcessor cookies = new Rfc6265CookieProcessor();
        cookies.setSameSiteCookies("strict");
        context.setCookieProcessor(cookies);

        register(context, new DashboardServlet(), "", "/dashboard");
        register(context, new DisasterServlet(), "/disasters");
        register(context, new TeamServlet(), "/teams");
        register(context, new ResourceServlet(), "/resources");
        register(context, new AlertServlet(), "/alerts");
        register(context, new ReportServlet(), "/reports");
        register(context, new UserServlet(), "/users");
        register(context, new LoginServlet(), "/login");
        register(context, new LogoutServlet(), "/logout");

        try {
            tomcat.start();
        } catch (Exception e) {
            System.err.println("Could not start the server on port " + port
                    + ". If the port is in use, choose another one: java Run.java 9090");
            System.exit(1);
        }

        System.out.println();
        System.out.println("Disaster Management System is running.");
        if (Settings.isPublic()) {
            System.out.println("Listening on " + Settings.bindAddress() + ":" + port + " (reachable from the network).");
        } else {
            System.out.println("Open http://localhost:" + port + " in your browser.");
            System.out.println("Press Ctrl+C to stop.");
        }
        tomcat.getServer().await();
    }

    /** Adds a servlet to the web application and connects it to one or more addresses. */
    private static void register(Context context, HttpServlet servlet, String... addresses) {
        String name = servlet.getClass().getSimpleName();
        Tomcat.addServlet(context, name, servlet);
        for (String address : addresses) {
            context.addServletMappingDecoded(address, name);
        }
    }
}
