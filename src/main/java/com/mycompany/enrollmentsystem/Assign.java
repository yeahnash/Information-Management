package com.mycompany.enrollmentsystem;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Assign extends EnrollmentSystem {
    private int subjID; // Removed static

    public void setsubjID(int a){
        subjID = a;
    }

    public int getsubjID(){
        return subjID;
    }

    public String assignTchr(int tID){
        if (!DBConnect()) return "Error, database connection failed.";

        String query = "{CALL assign_teacher_with_schedule_check(?, ?)}";

        try {
            ensureScheduleProcedures();
            try (CallableStatement pstmt = con.prepareCall(query)) {
                pstmt.setInt(1, tID);
                pstmt.setInt(2, subjID);
                pstmt.execute();
                return "Teacher " + tID + " assigned to subject " + subjID;
            }

        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            return "Error, this subject is already assigned to a teacher.";
        } catch (SQLException e) {
            if ("45000".equals(e.getSQLState())) {
                return e.getMessage();
            }
            System.out.println("Failed to assign teacher " + e);
            return "Error, assignment failed.";
        } catch (Exception e) {
            System.out.println("Failed to assign teacher " + e);
            return "Error, assignment failed.";
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