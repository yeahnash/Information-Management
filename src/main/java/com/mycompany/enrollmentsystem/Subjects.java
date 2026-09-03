package com.mycompany.enrollmentsystem;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author Admin
 */
public class Subjects {
    public void newsubject(int subjID, String subjCode, String subjDesc, int subjUnits, String subjSched){
        EnrollmentSystem b = new EnrollmentSystem();
        b.DBConnect();
        
        try {
            String query = "INSERT INTO subjects VALUES (" + 
                    subjID + ", '" + 
                    subjCode + "', '" + 
                    subjDesc + "', '" + 
                    subjUnits + "', '" + 
                    subjSched + "')"; 
            int rows = b.st.executeUpdate(query);
            
            if (rows > 0) {
                System.out.println("Subject inserted successfully!");
            }
        } catch (Exception e) {
            System.out.println("Not successful!");
            e.printStackTrace();
        }
    }
    
    public void delete_subject(int subjID){
        EnrollmentSystem b = new EnrollmentSystem();
        b.DBConnect();
        String query = "delete from subjects where subjID =" + subjID;
        
        try {
            int rows = b.st.executeUpdate(query);
        } catch (Exception e) {
            System.out.println("Not successful!");
            e.printStackTrace();
        }
    }
    
    public void edit_subject(int subjID, String subjCode, String subjDesc, int subjUnits, String subjSched){
        EnrollmentSystem b = new EnrollmentSystem();
        b.DBConnect();
        String query = "UPDATE subjects SET subjCode = ?, subjDesc = ?, subjUnits = ?, "
                + "subjSched = ? WHERE subjID = ?";
        try {
            java.sql.PreparedStatement ps = b.con.prepareStatement(query);
            ps.setString(1, subjCode);
            ps.setString(2, subjDesc);
            ps.setInt(3, subjUnits);
            ps.setString(4, subjSched);
            ps.setInt(5, subjID);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                System.out.println("Subject updated successfully!");
            }
        } catch (Exception e) {
            System.out.println("Not successful.");
            e.printStackTrace();
        }
    }
}
