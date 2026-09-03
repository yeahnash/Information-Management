package com.mycompany.enrollmentsystem;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Admin
 */
public class Students {
    public void newstudent(int studID, String studName, String studAdd, String studCrs, String studGender, String yrLvl){
        EnrollmentSystem b = new EnrollmentSystem();
        b.DBConnect();
        
        try {
            String query = "INSERT INTO Students VALUES (" + 
                    studID + ", '" + 
                    studName + "', '" + 
                    studAdd + "', '" + 
                    studCrs + "', '" + 
                    studGender + "', '" + 
                    yrLvl + "')";
            
            int rows = b.st.executeUpdate(query);
            
            if (rows > 0) {
                System.out.println("Student inserted successfully!");
            }
        } catch (Exception e) {
            System.out.println("Not successful!");
            e.printStackTrace();
        }
    }
    
    public void delete_student(int studID){
        EnrollmentSystem b = new EnrollmentSystem();
        b.DBConnect();
        String query = "delete from students where studID =" + studID;
        
        try {
            int rows = b.st.executeUpdate(query);
        } catch (Exception e) {
            System.out.println("Not successful!");
            e.printStackTrace();
        }
    }
    
    public void edit_student(int studID, String studName, String studAdd, String studCrs, String studGender, String yrLvl){
        EnrollmentSystem b = new EnrollmentSystem();
        b.DBConnect();
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
