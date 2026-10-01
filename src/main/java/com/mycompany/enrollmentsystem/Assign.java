package com.mycompany.enrollmentsystem;
import java.sql.PreparedStatement;

public class Assign extends EnrollmentSystem {
    private int subjID; // Removed static

    public void setsubjID(int a){
        subjID = a;
    }

    public int getsubjID(){
        return subjID;
    }

    public String assignTchr(int tID){
        if (!DBConnect()) return "Database connection failed";

        String query = "INSERT INTO assign(TID, SubjID) VALUES(?, ?)";

        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setInt(1, tID);
            pstmt.setInt(2, subjID);
            pstmt.executeUpdate();
            return "Teacher " + tID + " assigned to subject " + subjID;

        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            return "This subject is already assigned to a teacher.";
        } catch (Exception e) {
            System.out.println("Failed to assign teacher " + e);
            return "Assignment Failed";
        }
    }

    public String deleteSubject(int tID){
        if (!DBConnect()) return "Database connection failed";

        String query = "DELETE FROM assign WHERE TID = ? AND SubjID = ?";

        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setInt(1, tID);
            pstmt.setInt(2, subjID);
            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                return "Subject " + subjID + " unassigned from teacher " + tID;
            } else {
                return "Teacher is not assigned to this subject.";
            }
        } catch (Exception e) {
            System.out.println("Failed to drop subject " + e);
            return "Drop Failed";
        }
    }
}