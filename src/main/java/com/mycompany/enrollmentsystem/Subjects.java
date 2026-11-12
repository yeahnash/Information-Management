package com.mycompany.enrollmentsystem;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
    
    public boolean delete_subject(int subjID){
        EnrollmentSystem b = new EnrollmentSystem();
        if (!b.DBConnect()) return false;
        
        try (Connection connection = b.con) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM assign WHERE SubjID = ?")) {
                    ps.setInt(1, subjID);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM grades WHERE enroll_eid IN "
                                + "(SELECT eid FROM enroll WHERE subjid = ?)")) {
                    ps.setInt(1, subjID);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM enroll WHERE subjid = ?")) {
                    ps.setInt(1, subjID);
                    ps.executeUpdate();
                }

                int rows;
                try (PreparedStatement ps = connection.prepareStatement(
                        "DELETE FROM subjects WHERE subjid = ?")) {
                    ps.setInt(1, subjID);
                    rows = ps.executeUpdate();
                }
                if (rows == 0) {
                    connection.rollback();
                    return false;
                }

                connection.commit();
                System.out.println("Subject deleted successfully!");
                return true;
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
                throw e;
            }
        } catch (SQLException e) {
            System.out.println("Not successful!");
            e.printStackTrace();
            return false;
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