package com.mycompany.enrollmentsystem;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
            return "Error, database connection failed.";
        }
        try {
            ensureScheduleProcedures();
        } catch (SQLException e) {
            e.printStackTrace();
            return "Error, unable to install schedule procedures: " + e.getMessage();
        }

        String enrollQuery = "{CALL enroll_student_with_schedule_check(?, ?)}";
        try (CallableStatement stmt = con.prepareCall(enrollQuery)) {
            stmt.setInt(1, studID);
            stmt.setInt(2, subjID);
            stmt.execute();
            return "Student " + studID + " enrolled in subject " + subjID;
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                return "Error, student " + studID
                        + " is already enrolled in subject " + subjID + ".";
            }
            if ("45000".equals(e.getSQLState())) {
                return e.getMessage();
            }
            e.printStackTrace();
            return "Error, enrollment failed: " + e.getMessage();
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