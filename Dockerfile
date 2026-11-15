# Builds and runs the Disaster Management System on a hosting service (for example Render).
# On your own computer you do not need this file: use "java Run.java".

# This image contains a JDK and Apache Tomcat 11 (in /usr/local/tomcat)
FROM tomcat:11.0-jdk21

# JDBC driver for the H2 database
ADD https://repo1.maven.org/maven2/com/h2database/h2/2.1.214/h2-2.1.214.jar /app/lib/h2.jar

# Compile the application
COPY *.java /app/src/
RUN javac -cp "/usr/local/tomcat/lib/*" -d /app/classes /app/src/*.java

# Run as an ordinary user, not as root. /data holds the database files.
RUN useradd --system --create-home dms \
    && mkdir -p /data \
    && chown dms /data \
    && chmod 644 /app/lib/h2.jar
USER dms
WORKDIR /home/dms

# Listen on all network addresses; the hosting service supplies PORT
ENV DMS_BIND=0.0.0.0 \
    DMS_DB_URL="jdbc:h2:file:/data/dms;MODE=MySQL;DB_CLOSE_DELAY=-1"
EXPOSE 10000

CMD ["java", "-XX:MaxRAMPercentage=70", "-cp", "/app/classes:/usr/local/tomcat/lib/*:/usr/local/tomcat/bin/tomcat-juli.jar:/app/lib/*", "Main"]
