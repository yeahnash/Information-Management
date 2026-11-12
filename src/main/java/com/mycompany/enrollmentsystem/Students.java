package com.mycompany.enrollmentsystem;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Admin
 */
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.sql.DriverManager;

public class Students {
    public void newstudent(String studname, String studadd,
                            String studcrs, String studgender,
                            String yrlvl) {

         EnrollmentSystem b = new EnrollmentSystem();

         if (!b.DBConnect()) {
             System.out.println("Database connection failed!");
             return;
         }

         try {

             String insertQuery =
                     "INSERT INTO students " +
                     "(studname, studadd, studcrs, studgender, yrlvl) " +
                     "VALUES (?, ?, ?, ?, ?)";

             PreparedStatement pstmt =
                     b.con.prepareStatement(
                             insertQuery,
                             Statement.RETURN_GENERATED_KEYS
                     );

             pstmt.setString(1, studname);
             pstmt.setString(2, studadd);
             pstmt.setString(3, studcrs);
             pstmt.setString(4, studgender);
             pstmt.setString(5, yrlvl);

             int rows = pstmt.executeUpdate();

             if (rows > 0) {

                 ResultSet generatedKeys = pstmt.getGeneratedKeys();

                 if (generatedKeys.next()) {

                     int newID = generatedKeys.getInt(1);

                     // Create the student's MySQL account
                     boolean accountCreated = b.createDatabaseUser(
                             String.valueOf(newID),
                             studname,
                             "Student",
                             EnrollmentSystem.db
                     );

                     if (accountCreated) {

                         System.out.println(
                                 "Student inserted successfully!"
                         );

                         System.out.println(
                                 "Student ID: " + newID
                         );

                     } else {

                         System.out.println(
                                 "Student was added, but MySQL user creation failed."
                         );
                     }
                 }
             }

         } catch (Exception e) {

             System.out.println("Not successful! (STUDENT)");
             e.printStackTrace();
         }
     }
    
    public boolean delete_student(int studID) {
        EnrollmentSystem b = new EnrollmentSystem();

        if (!b.DBConnect()) return false;

        try (Connection connection = b.con) {
            connection.setAutoCommit(false);

            try {
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM grades WHERE enroll_eid IN "
                                + "(SELECT eid FROM enroll WHERE studid = ?)")) {
                    ps.setInt(1, studID);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM enroll WHERE studid = ?")) {
                    ps.setInt(1, studID);
                    ps.executeUpdate();
                }

                int rows;
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM students WHERE studid = ?")) {
                    ps.setInt(1, studID);
                    rows = ps.executeUpdate();
                }

                if (rows == 0) {
                    connection.rollback();
                    return false;
                }

                connection.commit();
            } catch (java.sql.SQLException e) {
                try {
                    connection.rollback();
                } catch (java.sql.SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        } catch (java.sql.SQLException e) {
            System.out.println("Failed to delete student " + studID + ".");
            e.printStackTrace();
            return false;
        }

        if (!b.dropDatabaseUser(String.valueOf(studID))) {
            System.out.println("Student was deleted, but their MySQL account could not be removed.");
        }
        return true;
    }
    
    
    public void edit_student(int studID, String studName, String studAdd,
                             String studCrs, String studGender, String yrLvl) {

        // Get the logged-in admin's MySQL credentials
        String adminUser = EnrollmentSystem.currentUser;
        String adminPass = EnrollmentSystem.currentPassword;

        if (adminUser == null || adminUser.isEmpty()) {
            adminUser = "root";
        }

        if (adminPass == null || adminPass.isEmpty()) {
            adminPass = "root";
        }

        // Get all existing semester databases
        java.util.ArrayList<String> semesterDBs =
                EnrollmentSystem.getDatabases();

        if (semesterDBs.isEmpty()) {
            System.out.println("No semester databases found!");
            return;
        }

        int updatedCount = 0;
        int errorCount = 0;

        // Visit each semester database
        for (String database : semesterDBs) {

            String url = "jdbc:mysql://localhost:3306/"
                    + database
                    + "?zeroDateTimeBehavior=CONVERT_TO_NULL";

            try (Connection con = DriverManager.getConnection(
                        url, adminUser, adminPass)) {

                // Check whether this student exists in this database
                String checkQuery =
                        "SELECT studid FROM students WHERE studid = ?";

                boolean studentExists;

                try (java.sql.PreparedStatement checkPS =
                             con.prepareStatement(checkQuery)) {

                    checkPS.setInt(1, studID);

                    try (ResultSet rs = checkPS.executeQuery()) {
                        studentExists = rs.next();
                    }
                }

                // Update only if the student exists
                if (studentExists) {

                    String updateQuery =
                            "UPDATE students SET "
                            + "studName = ?, "
                            + "studAdd = ?, "
                            + "studCrs = ?, "
                            + "studGender = ?, "
                            + "yrLvl = ? "
                            + "WHERE studID = ?";

                    try (java.sql.PreparedStatement ps =
                                 con.prepareStatement(updateQuery)) {

                        ps.setString(1, studName);
                        ps.setString(2, studAdd);
                        ps.setString(3, studCrs);
                        ps.setString(4, studGender);
                        ps.setString(5, yrLvl);
                        ps.setInt(6, studID);

                        ps.executeUpdate();

                        updatedCount++;

                        System.out.println(
                                "Student updated in: " + database
                        );
                    }
                }

            } catch (Exception e) {

                errorCount++;

                System.out.println(
                        "Failed to update database: " + database
                );

                e.printStackTrace();
            }
        }

        // Display the final result
        if (updatedCount > 0) {

            System.out.println(
                    "Student information updated successfully!"
            );

            System.out.println(
                    "Student ID: " + studID
            );

            System.out.println(
                    "Number of semester databases updated: "
                    + updatedCount
            );

        } else {

            System.out.println(
                    "Student ID " + studID
                    + " was not found in any semester database."
            );
        }

        if (errorCount > 0) {
            System.out.println(
                    "Number of databases with errors: " + errorCount
            );
        }
    }

}
