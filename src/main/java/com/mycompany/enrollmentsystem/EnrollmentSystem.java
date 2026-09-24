/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */
package com.mycompany.enrollmentsystem;

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
    static ResultSet rs;
    
    public String newdb(String term){
        DBConnect();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        String schyear = "SY" + year + "_" + (year + 1);
        
        try {
            String query = "CREATE database IF NOT EXISTS " + term + "_" + schyear;
            st.executeUpdate(query);
            
            String query2 = "USE " + term + "_" + schyear;
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
            
        } catch (Exception e) {
            System.out.println(e);
        }
        
        return term + "_" + schyear;
    }

    public static void main(String[] args) {
        StudentsForm a = new StudentsForm();
        a.setVisible(true);
        a.showRecords();
    }

    public boolean DBConnect() {
        try {
            Class.forName("com.mysql.jdbc.Driver");

            con = DriverManager.getConnection("jdbc:mysql://localhost:3306/enrollmentsystem?zeroDateTimeBehavior=CONVERT_TO_NULL", "root", "root");

            st = con.createStatement();

            System.out.println("Connected to database:");

        } catch (Exception e) {
            System.out.print(e);
            System.out.println("Connection failed.");
            return false;
        }
        return true;
    }
}
