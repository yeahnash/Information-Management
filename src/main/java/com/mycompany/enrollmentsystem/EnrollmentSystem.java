/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.mycompany.enrollmentsystem;
import java.util.ArrayList;

/**
 *
 * @author Admin
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;

public class EnrollmentSystem {

    Connection con;
    Statement st;
    public ResultSet rs;
    
    public static String currentUser = "";
    public static String currentPassword = "";
    public static String currentRole = "";
    public static String currentSemesterDB = "";
    public static String db = "";
    
    public static int globalSubjID = 0;

    public void currentDB(String dbName) {
        db = dbName;
        currentSemesterDB = dbName;
    }
    
    public String newdb(String term) {
        // Save the currently selected database
        String oldDB = db;

        // Connect to MySQL server without selecting a database
        db = "";

        if (!DBConnect()) {
            db = oldDB;
            return null;
        }

        int year = Calendar.getInstance().get(Calendar.YEAR);
        String schyear = "SY" + year + "_" + (year + 1);
        String dbName = term + "_" + schyear;
        
        try {

            String checkDB = "SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA "
                    + "WHERE SCHEMA_NAME = '" + dbName + "'";

            rs = st.executeQuery(checkDB);

            if (rs.next()) {
                System.out.println("Database already exists: " + dbName);

                // Restore the previously selected database
                db = oldDB;

                return null;
            }

            // Create database
            String query = "CREATE DATABASE `" + dbName + "`";
            st.executeUpdate(query);

            // Use database
            String query2 = "USE `" + dbName + "`";
            st.executeUpdate(query2);

            String query3 = """
                                CREATE TABLE students (
                                    studid INT NOT NULL AUTO_INCREMENT,
                                    studname VARCHAR(100) NOT NULL,
                                    studadd VARCHAR(255) NULL,
                                    studcrs VARCHAR(100) NULL,
                                    studgender VARCHAR(20) NULL,
                                    yrlvl VARCHAR(20) NULL,
                                    PRIMARY KEY (studid)
                                ) AUTO_INCREMENT = 1001;
                                """;
            st.executeUpdate(query3);

            String query4 = """
                                CREATE TABLE subjects (
                                    subjid INT NOT NULL AUTO_INCREMENT,
                                    subjcode VARCHAR(50) NULL DEFAULT NULL,
                                    subjdesc VARCHAR(255) NULL DEFAULT NULL,
                                    subjunits INT NULL DEFAULT NULL,
                                    subjsched VARCHAR(100) NULL,
                                    PRIMARY KEY (subjid)
                                ) AUTO_INCREMENT = 2001;
                                """;
            st.executeUpdate(query4);

            String query5 = """
                                CREATE TABLE teachers (
                                    tid INT NOT NULL AUTO_INCREMENT,
                                    tname VARCHAR(100) NULL DEFAULT NULL,
                                    tdept VARCHAR(100) NULL DEFAULT NULL,
                                    tadd VARCHAR(255) NULL,
                                    tcontact VARCHAR(50) NULL,
                                    tstatus VARCHAR(50) NULL,
                                    PRIMARY KEY (tid)
                                ) AUTO_INCREMENT = 3001;
                                """;
            st.executeUpdate(query5);

            String query6 = """
                                CREATE TABLE assign (
                                    SubjID INT NOT NULL UNIQUE,
                                    TID INT NOT NULL,
                                    FOREIGN KEY (SubjID) REFERENCES subjects(subjid),
                                    FOREIGN KEY (TID) REFERENCES teachers(tid)
                                );
                                """;
            st.executeUpdate(query6);

            String query7 = """
                                CREATE TABLE enroll (
                                    eid INT NOT NULL AUTO_INCREMENT,
                                    studid INT NULL DEFAULT NULL,
                                    subjid INT NULL DEFAULT NULL,
                                    evaluation VARCHAR(255) DEFAULT NULL,
                                    PRIMARY KEY (eid),
                                    UNIQUE (studid, subjid),
                                    FOREIGN KEY (studid) REFERENCES students(studid),
                                    FOREIGN KEY (subjid) REFERENCES subjects(subjid)
                                );
                                """;
            st.executeUpdate(query7);

            String query8 = """
                                CREATE TABLE grades (
                                    gradeid INT NOT NULL AUTO_INCREMENT,
                                    enroll_eid INT NOT NULL UNIQUE,
                                    prelim VARCHAR(10) NULL DEFAULT NULL,
                                    midterm VARCHAR(10) NULL DEFAULT NULL,
                                    prefinal VARCHAR(10) NULL DEFAULT NULL,
                                    final VARCHAR(10) NULL DEFAULT NULL,
                                    PRIMARY KEY (gradeid),
                                    FOREIGN KEY (enroll_eid) REFERENCES enroll(eid)
                                );
                                """;
            st.executeUpdate(query8);

            db = dbName;

            System.out.println("Database created successfully: " + dbName);

            return dbName;

        } catch (Exception e) {
            // Restore the previous database if creation fails
            db = oldDB;
            
            System.out.println("Failed to create database: " + e);
            return null;
        }
    }
    
    public boolean createDatabaseUser(
            String id,
            String name,
            String role,
            String targetDB) {
        String cleanName = name.replaceAll("\\s+", "").toLowerCase();
        String newUsername = id + cleanName;
        String newPassword = cleanName;

        // Use the logged-in admin's credentials dynamically
        String adminUser = (currentUser != null && !currentUser.isEmpty()) ? currentUser : "root";
        String adminPass = (currentPassword != null && !currentPassword.isEmpty()) ? currentPassword : "root";

        try (Connection rootCon = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/?zeroDateTimeBehavior=CONVERT_TO_NULL",
                    adminUser,
                    adminPass
            ); Statement rootSt = rootCon.createStatement()) {

            String createUser =
                    "CREATE USER IF NOT EXISTS '" + newUsername + "'@'localhost' IDENTIFIED BY '" + newPassword + "'";
            rootSt.executeUpdate(createUser);

            if (role.equalsIgnoreCase("Student")) {
                String grantStudent =
                        "GRANT SELECT ON `" + targetDB + "`.* TO '" + newUsername + "'@'localhost'";
                rootSt.executeUpdate(grantStudent);
                
                // FIX: Grant students permission to enroll and drop (INSERT and DELETE) 
//                String grantStudentEnroll = 
//                        "GRANT INSERT, DELETE ON `" + targetDB + "`.enroll TO '" + newUsername + "'@'localhost'";
//                rootSt.executeUpdate(grantStudentEnroll);
                
            } else if (role.equalsIgnoreCase("Teacher")) {
                String grantTeacher =
                        "GRANT SELECT, INSERT, UPDATE ON `" + targetDB + "`.* TO '" + newUsername + "'@'localhost'";
                rootSt.executeUpdate(grantTeacher);
                
                // FIX: Grant teachers permission to drop their assignments
//                String grantDeleteAssign =
//                        "GRANT DELETE ON `" + targetDB + "`.assign TO '" + newUsername + "'@'localhost'";
//                rootSt.executeUpdate(grantDeleteAssign);
//                
//                String grantDeleteEnroll =
//                        "GRANT DELETE ON `" + targetDB + "`.enroll TO '" + newUsername + "'@'localhost'";
//                rootSt.executeUpdate(grantDeleteEnroll);
            }

            rootSt.executeUpdate("FLUSH PRIVILEGES");
            System.out.println("MySQL user created: " + newUsername);
            return true;

        } catch (Exception e) {
            System.out.println("Failed to create MySQL user: " + e);
            return false;
        }
    }

    public static void main(String[] args) {
        Login a = new Login();
        a.setVisible(true);
//        a.showRecords();
    }

    public boolean DBConnect() {

        String username =
                (currentUser != null && !currentUser.isEmpty())
                        ? currentUser
                        : "root";

        String password =
                (currentPassword != null && !currentPassword.isEmpty())
                        ? currentPassword
                        : "root";

        String database =
                (db != null && !db.trim().isEmpty())
                        ? db.trim()
                        : "";

        return DBConnect(username, password, database);
    }
    
    public boolean DBConnect(String username, String password) {

        String database =
                (db != null && !db.trim().isEmpty())
                        ? db.trim()
                        : "";

        return DBConnect(username, password, database);
    }
    
    public boolean DBConnect(
            String username,
            String password,
            String database) {

        try {

            Class.forName("com.mysql.cj.jdbc.Driver");

            String url;

            if (database == null || database.trim().isEmpty()) {

                url = "jdbc:mysql://localhost:3306/"
                        + "?zeroDateTimeBehavior=CONVERT_TO_NULL";

            } else {

                url = "jdbc:mysql://localhost:3306/"
                        + database
                        + "?zeroDateTimeBehavior=CONVERT_TO_NULL";
            }

            con = DriverManager.getConnection(
                    url,
                    username,
                    password
            );

            st = con.createStatement();

            currentUser = username;
            currentPassword = password;

            if (database != null && !database.trim().isEmpty()) {
                db = database;
                currentSemesterDB = database;
            }

            System.out.println(
                    "Connected to database: "
                    + (database == null || database.trim().isEmpty()
                            ? "MySQL Server"
                            : database)
                    + " as user: "
                    + username
            );

            return true;

        } catch (Exception e) {

            System.out.println(
                    "Connection failed for user: " + username
            );

            e.printStackTrace();

            return false;
        }
    }
    
    public static ArrayList<String> getDatabases() {
        ArrayList<String> dbList = new ArrayList<>();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Fetch the currently logged-in user credentials, default to root if empty
            String activeUser = (currentUser != null && !currentUser.isEmpty()) ? currentUser : "root";
            String activePass = (currentPassword != null && !currentPassword.isEmpty()) ? currentPassword : "root";
            
            // Connect using the active user's credentials instead of hardcoded "root"
            Connection tempCon = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/?zeroDateTimeBehavior=CONVERT_TO_NULL",
                    activeUser,
                    activePass
            );
            Statement tempSt = tempCon.createStatement();
            ResultSet tempRs = tempSt.executeQuery("SHOW DATABASES");
            
            while (tempRs.next()) {
                String dbName = tempRs.getString(1);
                String lowerDbName = dbName.toLowerCase();
                
                if (lowerDbName.startsWith("1st_sy")
                        || lowerDbName.startsWith("2nd_sy")
                        || lowerDbName.startsWith("summer_sy")) {
                    dbList.add(dbName);
                }
            }
            tempRs.close();
            tempSt.close();
            tempCon.close();
        } catch (Exception e) {
            System.err.println(
                    "Failed to fetch semester databases: " + e.getMessage()
            );
        }
        return dbList;
    }
    
    public boolean dropDatabaseUser(String id) {
        String adminUser = (currentUser != null && !currentUser.isEmpty()) ? currentUser : "root";
        String adminPass = (currentPassword != null && !currentPassword.isEmpty()) ? currentPassword : "root";

        // Connect specifically to the 'mysql' database to search for the user
        try (java.sql.Connection rootCon = java.sql.DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/mysql?zeroDateTimeBehavior=CONVERT_TO_NULL",
                adminUser,
                adminPass
        ); java.sql.Statement rootSt = rootCon.createStatement()) {

            String exactUsername = "";
            
            // Search for the exact MySQL username using the unique ID prefix
            try (java.sql.ResultSet rs = rootSt.executeQuery("SELECT user FROM user WHERE user LIKE '" + id + "%'")) {
                if (rs.next()) {
                    exactUsername = rs.getString("user");
                }
            }

            if (!exactUsername.isEmpty()) {
                rootSt.executeUpdate("DROP USER IF EXISTS '" + exactUsername + "'@'localhost'");
                rootSt.executeUpdate("FLUSH PRIVILEGES");
                System.out.println("MySQL user dropped: " + exactUsername);
                return true;
            } else {
                System.out.println("Could not find a MySQL user starting with ID: " + id);
                return false;
            }

        } catch (Exception e) {
            System.out.println("Failed to drop MySQL user: " + e);
            return false;
        }
    }
    
//    public ArrayList<String> getDatabasesAsRoot() {
//
//        ArrayList<String> databases = new ArrayList<>();
//
//        try {
//
//            Connection rootCon = DriverManager.getConnection(
//                    "jdbc:mysql://localhost:3306/?zeroDateTimeBehavior=CONVERT_TO_NULL",
//                    "root",
//                    "root"
//            );
//
//            Statement rootSt = rootCon.createStatement();
//
//            ResultSet rs = rootSt.executeQuery("SHOW DATABASES");
//
//            while (rs.next()) {
//
//                String dbName = rs.getString(1);
//                String lowerDbName = dbName.toLowerCase();
//
//                if (lowerDbName.startsWith("1st_sy")
//                        || lowerDbName.startsWith("2nd_sy")
//                        || lowerDbName.startsWith("summer_sy")) {
//
//                    databases.add(dbName);
//                }
//            }
//
//            rs.close();
//            rootSt.close();
//            rootCon.close();
//
//        } catch (Exception e) {
//
//            System.out.println(
//                    "Error getting semester databases: " + e
//            );
//        }
//
//        return databases;
//    }

}
