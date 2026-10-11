/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package conexion;

import conexion.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

 
public class CreateConnection {
    static Properties config = new Properties();
    String hostname = null;
    String port = null;
    String database = null;
    String username = null;
    String password = null;
   
    
    public CreateConnection (){
        String path ="C:\\Users\\arten\\OneDrive\\Documentos\\NetBeansProjects\\parcial2\\src\\main\\java\\conexion\\db_config.properties";
        InputStream in = null;
        try {
            
            in = Files.newInputStream(Paths.get(path));
            config.load(in);
            in.close();
            
            }   catch (IOException ex) {
                        ex.printStackTrace();
            }   finally{
                    try{
                            in.close();
                        }catch (IOException ex ) {
                                ex.printStackTrace();
                                 }
                }
        loadProperties();
    }
    public void loadProperties(){
                hostname = config.getProperty("hostname");
                port  = config.getProperty("port");
                database = config.getProperty("database");
                username =   config.getProperty("username");
                password =   config.getProperty("password");
               
                System.out.println(hostname);
                System.out.println(port);
                System.out.println(database);
                System.out.println(username);
                System.out.println(password);
                
        }
    
    public Connection getConnection() throws SQLException{
    Connection conn = null;
    String jdbcUrl = "jdbc:postgresql://"+this.hostname+":"+
            this.port + "/" + this.database;
    conn = DriverManager.getConnection(jdbcUrl,username,password);
    System.out.println("Conexion establecida");
    
    return conn;
    }
}
