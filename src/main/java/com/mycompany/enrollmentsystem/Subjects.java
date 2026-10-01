package com.mycompany.enrollmentsystem;
import java.sql.PreparedStatement;

public class Subjects {
    public void newsubject(String subjCode, String subjDesc, int subjUnits, String subjSched){
        EnrollmentSystem b = new EnrollmentSystem();
        if (!b.DBConnect()) return;
        
        String query = "INSERT INTO subjects (subjCode, subjDesc, subjUnits, subjSched) VALUES (?, ?, ?, ?)"; 
        
        try (PreparedStatement ps = b.con.prepareStatement(query)) {
            ps.setString(1, subjCode);
            ps.setString(2, subjDesc);
            ps.setInt(3, subjUnits);
            ps.setString(4, subjSched);
            int rows = ps.executeUpdate();
            
            if (rows > 0) System.out.println("Subject inserted successfully!");
        } catch (Exception e) {
            System.out.println("Not successful!");
            e.printStackTrace();
        }
    }
    
    public void delete_subject(int subjID){
        EnrollmentSystem b = new EnrollmentSystem();
        if (!b.DBConnect()) return;
        
        try {
            // Drop dependencies to satisfy Foreign Key constraints
            try (PreparedStatement ps1 = b.con.prepareStatement("DELETE FROM assign WHERE SubjID = ?")) {
                ps1.setInt(1, subjID);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = b.con.prepareStatement("DELETE FROM enroll WHERE subjid = ?")) {
                ps2.setInt(1, subjID);
                ps2.executeUpdate();
            }
            
            // Delete the subject
            try (PreparedStatement ps3 = b.con.prepareStatement("DELETE FROM subjects WHERE subjid = ?")) {
                ps3.setInt(1, subjID);
                ps3.executeUpdate();
                System.out.println("Subject deleted successfully!");
            }
        } catch (Exception e) {
            System.out.println("Not successful!");
            e.printStackTrace();
        }
    }
    
    public void edit_subject(int subjID, String subjCode, String subjDesc, int subjUnits, String subjSched){
        EnrollmentSystem b = new EnrollmentSystem();
        if (!b.DBConnect()) return;
        
        String query = "UPDATE subjects SET subjCode = ?, subjDesc = ?, subjUnits = ?, subjSched = ? WHERE subjID = ?";
        
        try (PreparedStatement ps = b.con.prepareStatement(query)) {
            ps.setString(1, subjCode);
            ps.setString(2, subjDesc);
            ps.setInt(3, subjUnits);
            ps.setString(4, subjSched);
            ps.setInt(5, subjID);
            int rows = ps.executeUpdate();
            
            if (rows > 0) System.out.println("Subject updated successfully!");
        } catch (Exception e) {
            System.out.println("Not successful.");
            e.printStackTrace();
        }
    }
}