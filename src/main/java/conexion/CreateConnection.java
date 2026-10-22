package conexion;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class CreateConnection {
    private static Properties config = new Properties();
    private String hostname;
    private String port;
    private String database;
    private String username;
    private String password;

    public CreateConnection() {
        try (InputStream in = CreateConnection.class
                .getResourceAsStream("/conexion/db_config.properties")) {
            if (in == null) {
                throw new IOException("No se encontró db_config.properties en resources");
            }
            config.load(in);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        loadProperties();
    }

    private void loadProperties() {
        hostname = config.getProperty("hostname");
        port = config.getProperty("port");
        database = config.getProperty("database");
        username = config.getProperty("username");
        password = config.getProperty("password");

        System.out.println("Propiedades cargadas:");
        System.out.println("Host: " + hostname);
        System.out.println("Puerto: " + port);
        System.out.println("Base: " + database);
        System.out.println("Usuario: " + username);
        // ⚠️ No imprimas la contraseña en producción
    }

    public Connection getConnection() throws SQLException {
        String jdbcUrl = "jdbc:postgresql://" + hostname + ":" + port + "/" + database;
        Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
        System.out.println("Conexión establecida");
        return conn;
    }
}

