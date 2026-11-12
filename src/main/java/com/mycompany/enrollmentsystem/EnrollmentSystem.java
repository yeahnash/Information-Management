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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;

public class EnrollmentSystem {

    private static final String SCHEDULE_PROCEDURE_VERSION = "schedule-format-v2";

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
            boolean databaseExists = rs.next();

            if (databaseExists) {
                System.out.println("Database already exists: " + dbName);
                st.executeUpdate("USE `" + dbName + "`");
            } else {
                // Create and select the new database
                st.executeUpdate("CREATE DATABASE `" + dbName + "`");
                st.executeUpdate("USE `" + dbName + "`");
            }

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
            if (!databaseExists) st.executeUpdate(query3);

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
            if (!databaseExists) st.executeUpdate(query4);

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
            if (!databaseExists) st.executeUpdate(query5);

            String query6 = """
                                CREATE TABLE assign (
                                    SubjID INT NOT NULL UNIQUE,
                                    TID INT NOT NULL,
                                    FOREIGN KEY (SubjID) REFERENCES subjects(subjid),
                                    FOREIGN KEY (TID) REFERENCES teachers(tid)
                                );
                                """;
            if (!databaseExists) st.executeUpdate(query6);

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
            if (!databaseExists) st.executeUpdate(query7);

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
            if (!databaseExists) st.executeUpdate(query8);

            ensureScheduleProcedures();

            if (databaseExists) {
                db = oldDB;
                return null;
            }

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

    void ensureScheduleProcedures() throws java.sql.SQLException {
        ensureScheduleProcedure("enroll_student_with_schedule_check", enrollmentProcedureSql());
        ensureScheduleProcedure("assign_teacher_with_schedule_check", assignmentProcedureSql());
    }

    private void ensureScheduleProcedure(String procedureName, String createSql)
            throws java.sql.SQLException {
        String query = """
                SELECT ROUTINE_COMMENT
                FROM INFORMATION_SCHEMA.ROUTINES
                WHERE ROUTINE_SCHEMA = DATABASE()
                    AND ROUTINE_NAME = ?
                    AND ROUTINE_TYPE = 'PROCEDURE'
                """;
        boolean procedureExists = false;
        String routineComment = null;
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, procedureName);
            try (ResultSet result = pstmt.executeQuery()) {
                if (result.next()) {
                    procedureExists = true;
                    routineComment = result.getString("ROUTINE_COMMENT");
                }
            }
        }

        if (SCHEDULE_PROCEDURE_VERSION.equals(routineComment)) {
            return;
        }
        if (procedureExists) {
            st.executeUpdate("DROP PROCEDURE `" + procedureName + "`");
        }
        st.executeUpdate(createSql);
    }

    private String enrollmentProcedureSql() {
        return """
                CREATE PROCEDURE enroll_student_with_schedule_check(
                    IN p_studid INT,
                    IN p_subjid INT
                )
                COMMENT 'schedule-format-v2'
                BEGIN
                    DECLARE v_schedule VARCHAR(100);
                    DECLARE v_days VARCHAR(3);
                    DECLARE v_time_range VARCHAR(100);
                    DECLARE v_start TIME;
                    DECLARE v_end TIME;
                    DECLARE v_conflict_schedule VARCHAR(100) DEFAULT NULL;
                    DECLARE v_error_message VARCHAR(128);

                    SELECT subjsched INTO v_schedule
                    FROM subjects
                    WHERE subjid = p_subjid;

                    IF v_schedule IS NULL THEN
                        SIGNAL SQLSTATE '45000'
                            SET MESSAGE_TEXT = 'Error, subject schedule is missing.';
                    END IF;

                    SET v_days = UPPER(LEFT(TRIM(v_schedule), 3));
                    SET v_time_range = TRIM(SUBSTRING(TRIM(v_schedule), 4));
                    SET v_start = CASE
                        WHEN RIGHT(UPPER(REPLACE(
                            SUBSTRING_INDEX(v_time_range, '-', 1), ' ', ''
                        )), 2) IN ('AM', 'PM')
                        THEN TIME(STR_TO_DATE(
                            UPPER(REPLACE(SUBSTRING_INDEX(v_time_range, '-', 1), ' ', '')),
                            '%h:%i%p'
                        ))
                        ELSE TIME(STR_TO_DATE(
                            UPPER(REPLACE(SUBSTRING_INDEX(v_time_range, '-', 1), ' ', '')),
                            '%H:%i'
                        ))
                    END;
                    SET v_end = CASE
                        WHEN RIGHT(UPPER(REPLACE(
                            SUBSTRING_INDEX(v_time_range, '-', -1), ' ', ''
                        )), 2) IN ('AM', 'PM')
                        THEN TIME(STR_TO_DATE(
                            UPPER(REPLACE(SUBSTRING_INDEX(v_time_range, '-', -1), ' ', '')),
                            '%h:%i%p'
                        ))
                        ELSE TIME(STR_TO_DATE(
                            UPPER(REPLACE(SUBSTRING_INDEX(v_time_range, '-', -1), ' ', '')),
                            '%H:%i'
                        ))
                    END;

                    IF v_days NOT IN ('MWF', 'TTH')
                        OR LOCATE('-', v_time_range) = 0
                        OR v_start IS NULL
                        OR v_end IS NULL
                        OR v_start >= v_end THEN
                        SIGNAL SQLSTATE '45000'
                            SET MESSAGE_TEXT =
                                'Error, invalid schedule. Use MWF or TTH followed by STARTTIME-ENDTIME.';
                    END IF;

                    SELECT s.subjsched INTO v_conflict_schedule
                    FROM enroll e
                    JOIN subjects s ON s.subjid = e.subjid
                    WHERE e.studid = p_studid
                        AND e.subjid <> p_subjid
                        AND UPPER(SUBSTRING_INDEX(TRIM(s.subjsched), ' ', 1)) = v_days
                        AND v_start < COALESCE(
                            TIME(STR_TO_DATE(
                                REPLACE(SUBSTRING_INDEX(
                                    TRIM(SUBSTRING(TRIM(s.subjsched), 4)), '-', -1
                                ), ' ', ''),
                                '%h:%i%p'
                            )),
                            TIME(STR_TO_DATE(
                                REPLACE(SUBSTRING_INDEX(
                                    TRIM(SUBSTRING(TRIM(s.subjsched), 4)), '-', -1
                                ), ' ', ''),
                                '%H:%i'
                            ))
                        )
                        AND v_end > COALESCE(
                            TIME(STR_TO_DATE(
                                REPLACE(SUBSTRING_INDEX(
                                    TRIM(SUBSTRING(TRIM(s.subjsched), 4)), '-', 1
                                ), ' ', ''),
                                '%h:%i%p'
                            )),
                            TIME(STR_TO_DATE(
                                REPLACE(SUBSTRING_INDEX(
                                    TRIM(SUBSTRING(TRIM(s.subjsched), 4)), '-', 1
                                ), ' ', ''),
                                '%H:%i'
                            ))
                        )
                    LIMIT 1;

                    IF v_conflict_schedule IS NOT NULL THEN
                        SET v_error_message =
                            CONCAT('Error, Conflict with ', v_conflict_schedule);
                        SIGNAL SQLSTATE '45000'
                            SET MESSAGE_TEXT = v_error_message;
                    END IF;

                    INSERT INTO enroll(studid, subjid, evaluation)
                    VALUES (p_studid, p_subjid, '');
                END
                """;
    }

    private String assignmentProcedureSql() {
        return """
                CREATE PROCEDURE assign_teacher_with_schedule_check(
                    IN p_tid INT,
                    IN p_subjid INT
                )
                COMMENT 'schedule-format-v2'
                BEGIN
                    DECLARE v_schedule VARCHAR(100);
                    DECLARE v_days VARCHAR(3);
                    DECLARE v_time_range VARCHAR(100);
                    DECLARE v_start TIME;
                    DECLARE v_end TIME;
                    DECLARE v_conflict_schedule VARCHAR(100) DEFAULT NULL;
                    DECLARE v_error_message VARCHAR(128);

                    SELECT subjsched INTO v_schedule
                    FROM subjects
                    WHERE subjid = p_subjid;

                    IF v_schedule IS NULL THEN
                        SIGNAL SQLSTATE '45000'
                            SET MESSAGE_TEXT = 'Error, subject schedule is missing.';
                    END IF;

                    SET v_days = UPPER(LEFT(TRIM(v_schedule), 3));
                    SET v_time_range = TRIM(SUBSTRING(TRIM(v_schedule), 4));
                    SET v_start = CASE
                        WHEN RIGHT(UPPER(REPLACE(
                            SUBSTRING_INDEX(v_time_range, '-', 1), ' ', ''
                        )), 2) IN ('AM', 'PM')
                        THEN TIME(STR_TO_DATE(
                            UPPER(REPLACE(SUBSTRING_INDEX(v_time_range, '-', 1), ' ', '')),
                            '%h:%i%p'
                        ))
                        ELSE TIME(STR_TO_DATE(
                            UPPER(REPLACE(SUBSTRING_INDEX(v_time_range, '-', 1), ' ', '')),
                            '%H:%i'
                        ))
                    END;
                    SET v_end = CASE
                        WHEN RIGHT(UPPER(REPLACE(
                            SUBSTRING_INDEX(v_time_range, '-', -1), ' ', ''
                        )), 2) IN ('AM', 'PM')
                        THEN TIME(STR_TO_DATE(
                            UPPER(REPLACE(SUBSTRING_INDEX(v_time_range, '-', -1), ' ', '')),
                            '%h:%i%p'
                        ))
                        ELSE TIME(STR_TO_DATE(
                            UPPER(REPLACE(SUBSTRING_INDEX(v_time_range, '-', -1), ' ', '')),
                            '%H:%i'
                        ))
                    END;

                    IF v_days NOT IN ('MWF', 'TTH')
                        OR LOCATE('-', v_time_range) = 0
                        OR v_start IS NULL
                        OR v_end IS NULL
                        OR v_start >= v_end THEN
                        SIGNAL SQLSTATE '45000'
                            SET MESSAGE_TEXT =
                                'Error, invalid schedule. Use MWF or TTH followed by STARTTIME-ENDTIME.';
                    END IF;

                    SELECT s.subjsched INTO v_conflict_schedule
                    FROM assign a
                    JOIN subjects s ON s.subjid = a.subjid
                    WHERE a.tid = p_tid
                        AND a.subjid <> p_subjid
                        AND UPPER(SUBSTRING_INDEX(TRIM(s.subjsched), ' ', 1)) = v_days
                        AND v_start < COALESCE(
                            TIME(STR_TO_DATE(
                                REPLACE(SUBSTRING_INDEX(
                                    TRIM(SUBSTRING(TRIM(s.subjsched), 4)), '-', -1
                                ), ' ', ''),
                                '%h:%i%p'
                            )),
                            TIME(STR_TO_DATE(
                                REPLACE(SUBSTRING_INDEX(
                                    TRIM(SUBSTRING(TRIM(s.subjsched), 4)), '-', -1
                                ), ' ', ''),
                                '%H:%i'
                            ))
                        )
                        AND v_end > COALESCE(
                            TIME(STR_TO_DATE(
                                REPLACE(SUBSTRING_INDEX(
                                    TRIM(SUBSTRING(TRIM(s.subjsched), 4)), '-', 1
                                ), ' ', ''),
                                '%h:%i%p'
                            )),
                            TIME(STR_TO_DATE(
                                REPLACE(SUBSTRING_INDEX(
                                    TRIM(SUBSTRING(TRIM(s.subjsched), 4)), '-', 1
                                ), ' ', ''),
                                '%H:%i'
                            ))
                        )
                    LIMIT 1;

                    IF v_conflict_schedule IS NOT NULL THEN
                        SET v_error_message =
                            CONCAT('Error, Conflict with ', v_conflict_schedule);
                        SIGNAL SQLSTATE '45000'
                            SET MESSAGE_TEXT = v_error_message;
                    END IF;

                    INSERT INTO assign(TID, SubjID)
                    VALUES (p_tid, p_subjid);
                END
                """;
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
                rootSt.executeUpdate(
                        "GRANT EXECUTE ON `" + targetDB + "`.* TO '" + newUsername + "'@'localhost'"
                );
            } else if (role.equalsIgnoreCase("Teacher")) {
                String grantTeacher =
                        "GRANT SELECT, INSERT, UPDATE ON `" + targetDB + "`.* TO '" + newUsername + "'@'localhost'";
                rootSt.executeUpdate(grantTeacher);
                rootSt.executeUpdate(
                        "GRANT EXECUTE ON `" + targetDB + "`.* TO '" + newUsername + "'@'localhost'"
                );
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
}
