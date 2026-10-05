package com.mycompany.enrollmentsystem;
import java.sql.PreparedStatement;

public class Enrolled extends EnrollmentSystem {
    private int subjID;

    public void setsubjID(int a) {
        subjID = a;
    }

    public int getsubjID() {
        return subjID;
    }

    public String enrollStud(int studID) {
        if (!DBConnect()) {
            return "Database connection failed";
        }
        String enrollQuery =
                "INSERT INTO enroll(studid, subjid, evaluation) VALUES(?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(enrollQuery)) {
            pstmt.setInt(1, studID);
            pstmt.setInt(2, subjID);
            pstmt.setString(3, "");
            pstmt.executeUpdate();
            return "Student " + studID + " enrolled in subject " + subjID;
        } catch (java.sql.SQLException e) {
            if (e.getErrorCode() == 1062) {
                return "Student " + studID
                        + " is already enrolled in subject " + subjID + ".";
            }
            e.printStackTrace();
            return "Enrollment Failed: " + e.getMessage();
        } 
    } // FIX: Added missing closing brace for the enrollStud method

    public String dropSubject(int studID) {
        if (!DBConnect()) return "Database connection failed";
        String query = "DELETE FROM enroll WHERE studID = ? AND subjID = ?";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setInt(1, studID);
            pstmt.setInt(2, subjID);
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                return "Subject " + subjID + " dropped from student " + studID;
            } else {
                return "Student is not Enrolled in this subject.";
            }
        } catch (Exception e) {
            System.out.println("Failed to drop subject " + e);
            return "Drop Failed";
        }
    }
}