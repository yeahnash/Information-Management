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
    
    public void delete_student(int studID) {
        EnrollmentSystem b = new EnrollmentSystem();

        if (!b.DBConnect()) return;

        try {
            // Drop dependencies first to prevent constraint errors
            try (PreparedStatement ps = b.con.prepareStatement("DELETE FROM enroll WHERE studid = ?")) {
                ps.setInt(1, studID);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = b.con.prepareStatement("DELETE FROM students WHERE studID = ?")) {
                ps.setInt(1, studID);
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    System.out.println("Student deleted successfully!");
                    b.dropDatabaseUser(String.valueOf(studID)); // Now only uses ID
                }
            }
        } catch (Exception e) {
            System.out.println("Not successful!");
            e.printStackTrace();
        }
    }
    
    public void edit_student(int studID, String studName, String studAdd,
                            String studCrs, String studGender, String yrLvl) {

       EnrollmentSystem b = new EnrollmentSystem();

       if (!b.DBConnect()) {
           System.out.println("Database connection failed!");
           return;
       }

       String query = "UPDATE students SET studName = ?, studAdd = ?, studCrs = ?, "
               + "studGender = ?, yrLvl = ? WHERE studID = ?";

       try {
           java.sql.PreparedStatement ps = b.con.prepareStatement(query);

           ps.setString(1, studName);
           ps.setString(2, studAdd);
           ps.setString(3, studCrs);
           ps.setString(4, studGender);
           ps.setString(5, yrLvl);
           ps.setInt(6, studID);

           int rows = ps.executeUpdate();

           if (rows > 0) {
               System.out.println("Student updated successfully!");
           }

       } catch (Exception e) {
           System.out.println("Not successful.");
           e.printStackTrace();
       }
   }
}
