package com.mycompany.enrollmentsystem;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Admin
 */
public class Teachers {
    public void newteacher(int tID, String tName, String tDept, String tAdd, String tContact, String tStatus){
        EnrollmentSystem b = new EnrollmentSystem();
        b.DBConnect();
        
        try {
            String query = "INSERT INTO Teachers VALUES (" + 
                    tID + ", '" + 
                    tName + "', '" + 
                    tDept + "', '" + 
                    tAdd + "', '" + 
                    tContact + "', '" + 
                    tStatus + "')";
            
            int rows = b.st.executeUpdate(query);
            
            if (rows > 0) {
                System.out.println("Teacher inserted successfully!");
            }
        } catch (Exception e) {
            System.out.println("Not successful!");
            e.printStackTrace();
        }
    }
    
    public void delete_teacher(int tID){
        EnrollmentSystem b = new EnrollmentSystem();
        b.DBConnect();
        String query = "delete from teachers where tID =" + tID;
        
        try {
            int rows = b.st.executeUpdate(query);
        } catch (Exception e) {
            System.out.println("Not successful!");
            e.printStackTrace();
        }
    }
    
    public void edit_teacher(int tID, String tName, String tDept, String tAdd, String tContact, String tStatus){
        EnrollmentSystem b = new EnrollmentSystem();
        b.DBConnect();
        String query = "UPDATE teachers SET tName = ?, tDept = ?, tAdd = ?, "
                + "tContact = ?, tStatus = ? WHERE tID = ?";
        try {
            java.sql.PreparedStatement ps = b.con.prepareStatement(query);
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
