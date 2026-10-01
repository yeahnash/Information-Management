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

public class Teachers {
    public void newteacher(String tName, String tDept, String tAdd,
                            String tContact, String tStatus) {

         EnrollmentSystem b = new EnrollmentSystem();

         if (!b.DBConnect()) {
             System.out.println("Database connection failed!");
             return;
         }

         try {

             String insertQuery =
                     "INSERT INTO teachers " +
                     "(tname, tdept, tadd, tcontact, tstatus) " +
                     "VALUES (?, ?, ?, ?, ?)";

             PreparedStatement pstmt =
                     b.con.prepareStatement(
                             insertQuery,
                             Statement.RETURN_GENERATED_KEYS
                     );

             pstmt.setString(1, tName);
             pstmt.setString(2, tDept);
             pstmt.setString(3, tAdd);
             pstmt.setString(4, tContact);
             pstmt.setString(5, tStatus);

             int rows = pstmt.executeUpdate();

             if (rows > 0) {

                 ResultSet generatedKeys = pstmt.getGeneratedKeys();

                 if (generatedKeys.next()) {

                     int newID = generatedKeys.getInt(1);

                     // Create the teacher's MySQL account
                     boolean accountCreated = b.createDatabaseUser(
                             String.valueOf(newID),
                             tName,
                             "Teacher",
                             EnrollmentSystem.db
                     );

                     if (accountCreated) {

                         System.out.println(
                                 "Teacher inserted successfully!"
                         );

                         System.out.println(
                                 "Teacher ID: " + newID
                         );

                     } else {

                         System.out.println(
                                 "Teacher was added, but MySQL user creation failed."
                         );
                     }
                 }
             }

         } catch (Exception e) {

             System.out.println("Not successful! (TEACHER)");
             e.printStackTrace();
         }
     }
    
    public boolean delete_teacher(int tID){
        EnrollmentSystem b = new EnrollmentSystem();
        if (!b.DBConnect()) return false;

        try {
            try (java.sql.PreparedStatement ps = b.con.prepareStatement("DELETE FROM assign WHERE TID = ?")) {
                ps.setInt(1, tID);
                ps.executeUpdate();
            }

            try (java.sql.PreparedStatement ps = b.con.prepareStatement("DELETE FROM teachers WHERE TID = ?")) {
                ps.setInt(1, tID);
                int rows = ps.executeUpdate();

                if (rows > 0) {
                    System.out.println("Teacher deleted successfully!");
                    b.dropDatabaseUser(String.valueOf(tID)); // Now only uses ID
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("Not successful!");
            e.printStackTrace();
        }
        return false;
    }
    
    public void edit_teacher(int tID, String tName, String tDept,
                            String tAdd, String tContact, String tStatus) {

       EnrollmentSystem b = new EnrollmentSystem();

       if (!b.DBConnect()) {
           System.out.println("Database connection failed!");
           return;
       }

       String query = "UPDATE teachers SET tName = ?, tDept = ?, tAdd = ?, "
               + "tContact = ?, tStatus = ? WHERE tID = ?";

       try {
           java.sql.PreparedStatement ps =
                   b.con.prepareStatement(query);

           ps.setString(1, tName);
           ps.setString(2, tDept);
           ps.setString(3, tAdd);
           ps.setString(4, tContact);
           ps.setString(5, tStatus);
           ps.setInt(6, tID);

           int rows = ps.executeUpdate();

           if (rows > 0) {
               System.out.println("Teacher updated successfully!");
           }

       } catch (Exception e) {
           System.out.println("Not successful.");
           e.printStackTrace();
       }
   }
}
