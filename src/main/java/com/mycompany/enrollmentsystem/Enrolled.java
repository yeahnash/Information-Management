package com.mycompany.enrollmentsystem;
import java.sql.PreparedStatement;

public class Enrolled extends EnrollmentSystem {
    private int subjID; // Removed static

    public void setsubjID(int a) {
        subjID = a;
    }

    public int getsubjID() {
        return subjID;
    }

    public String enrollStud(int studID) {
        if (!DBConnect()) return "Database connection failed";

        String enrollQuery = "INSERT INTO enroll(studID, subjID, evaluation) VALUES(?, ?, ' ')";

        try (PreparedStatement pstmt = con.prepareStatement(enrollQuery)) {
            pstmt.setInt(1, studID);
            pstmt.setInt(2, subjID);
            pstmt.executeUpdate();
            return "Student " + studID + " Enrolled to " + subjID;

        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            return "Student is already Enrolled in this subject.";
        } catch (Exception e) {
            System.out.println("Failed to insert " + e);
            return "Enrollment Failed";
        }
    }

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