package com.mycompany.enrollmentsystem;

import java.awt.Component;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class Grades {

    /**
     * Helper method to extract the numeric Teacher ID (tid) from currentUser username.
     */
    public int getTeacherID() {
        String user = EnrollmentSystem.currentUser;
        try {
            // Extract leading digits (e.g., "3001john" -> 3001)
            String digits = user.replaceAll("^(\\d+).*", "$1");
            if (!digits.isEmpty() && digits.matches("\\d+")) {
                return Integer.parseInt(digits);
            }
            
            // Search database by tname if username isn't numeric
            EnrollmentSystem sys = new EnrollmentSystem();
            if (sys.DBConnect()) {
                String sql = "SELECT tid FROM teachers WHERE LOWER(tname) = LOWER(?)";
                PreparedStatement pstmt = sys.con.prepareStatement(sql);
                pstmt.setString(1, user);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt("tid");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 3001; // Fallback default
    }

    /**
     * Populates all grade ComboBoxes with standard grading options.
     */
    public void setupGradeComboBoxes(JComboBox<String> prelim, JComboBox<String> midterm, JComboBox<String> prefinal, JComboBox<String> finalCmb) {
        String[] gradeOptions = {"", "A", "B+", "B", "C+", "C", "D", "F", "FD"};
        prelim.setModel(new DefaultComboBoxModel<>(gradeOptions));
        midterm.setModel(new DefaultComboBoxModel<>(gradeOptions));
        prefinal.setModel(new DefaultComboBoxModel<>(gradeOptions));
        finalCmb.setModel(new DefaultComboBoxModel<>(gradeOptions));
    }

    /**
     * Loads assigned subjects for the active teacher into assignTbl.
     */
    public void showAssign(JTable assignTbl, int teachid) {
        DefaultTableModel tblmodel = (DefaultTableModel) assignTbl.getModel();
        tblmodel.setRowCount(0);

        EnrollmentSystem sys = new EnrollmentSystem();
        if (sys.DBConnect()) {
            try {
                String query = "SELECT s.subjid, s.subjcode, s.subjdesc, s.subjunits, s.subjsched, COUNT(e.studid) AS Students "
                             + "FROM subjects s "
                             + "JOIN assign a ON s.subjid = a.subjid "
                             + "LEFT JOIN enroll e ON s.subjid = e.subjid "
                             + "WHERE a.tid = ? "
                             + "GROUP BY s.subjid, s.subjcode, s.subjdesc, s.subjunits, s.subjsched";

                PreparedStatement pstmt = sys.con.prepareStatement(query);
                pstmt.setInt(1, teachid);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    String id = rs.getString("subjid");
                    String code = rs.getString("subjcode");
                    String desc = rs.getString("subjdesc");
                    int units = rs.getInt("subjunits");
                    String sched = rs.getString("subjsched");
                    int students = rs.getInt("Students");

                    tblmodel.addRow(new Object[]{id, code, desc, units, sched, students});
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Loads enrolled students and their existing grades for a selected subject into gradesTbl.
     */
    public void loadEnrolledStudentsAndGrades(JTable gradesTbl, String subjid) {
        DefaultTableModel tblmodel = (DefaultTableModel) gradesTbl.getModel();
        tblmodel.setRowCount(0);

        EnrollmentSystem sys = new EnrollmentSystem();
        if (sys.DBConnect()) {
            try {
                String query = "SELECT st.studid, st.studname, g.prelim, g.midterm, g.prefinal, g.final "
                             + "FROM enroll e "
                             + "INNER JOIN students st ON e.studid = st.studid "
                             + "LEFT JOIN grades g ON e.eid = g.enroll_eid "
                             + "WHERE e.subjid = ?";

                PreparedStatement pstmt = sys.con.prepareStatement(query);
                pstmt.setInt(1, Integer.parseInt(subjid));
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    String id = rs.getString("studid");
                    String name = rs.getString("studname");
                    String prelim = rs.getString("prelim") != null ? rs.getString("prelim") : "";
                    String midterm = rs.getString("midterm") != null ? rs.getString("midterm") : "";
                    String prefinal = rs.getString("prefinal") != null ? rs.getString("prefinal") : "";
                    String fin = rs.getString("final") != null ? rs.getString("final") : "";

                    tblmodel.addRow(new Object[]{id, name, prelim, midterm, prefinal, fin});
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Saves or updates student grades in the database.
     */
    public void saveGrades(Component parent, String sid, String currentSubjId, String prelim, String midterm, String prefinal, String fin, JTable gradesTbl) {
        if (sid.isEmpty() || sid.equals("Student ID") || currentSubjId.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Please select a student from the table first!", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        EnrollmentSystem sys = new EnrollmentSystem();
        if (sys.DBConnect()) {
            try {
                String getEidSql = "SELECT eid FROM enroll WHERE studid = ? AND subjid = ?";
                PreparedStatement pstmtEid = sys.con.prepareStatement(getEidSql);
                pstmtEid.setInt(1, Integer.parseInt(sid));
                pstmtEid.setInt(2, Integer.parseInt(currentSubjId));
                ResultSet rsEid = pstmtEid.executeQuery();

                if (rsEid.next()) {
                    int eid = rsEid.getInt("eid");

                    String upsertSql = "INSERT INTO grades (enroll_eid, prelim, midterm, prefinal, final) "
                                     + "VALUES (?, ?, ?, ?, ?) "
                                     + "ON DUPLICATE KEY UPDATE prelim=?, midterm=?, prefinal=?, final=?";

                    PreparedStatement pstmt = sys.con.prepareStatement(upsertSql);
                    pstmt.setInt(1, eid);
                    pstmt.setString(2, prelim);
                    pstmt.setString(3, midterm);
                    pstmt.setString(4, prefinal);
                    pstmt.setString(5, fin);

                    pstmt.setString(6, prelim);
                    pstmt.setString(7, midterm);
                    pstmt.setString(8, prefinal);
                    pstmt.setString(9, fin);

                    pstmt.executeUpdate();

                    JOptionPane.showMessageDialog(parent, "Grades updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadEnrolledStudentsAndGrades(gradesTbl, currentSubjId);
                } else {
                    JOptionPane.showMessageDialog(parent, "Student enrollment record not found!", "Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(parent, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Helper to set ComboBox selection safely.
     */
    public void setComboValue(JComboBox<String> cmb, Object val) {
        if (val != null && !val.toString().isEmpty()) {
            cmb.setSelectedItem(val.toString());
        } else {
            cmb.setSelectedIndex(0);
        }
    }

    /**
     * Logs out and disposes all open windows.
     */
    public void logout(JFrame currentFrame) {
        int confirm = JOptionPane.showConfirmDialog(
            currentFrame, 
            "Are you sure you want to log out?", 
            "Confirm Logout", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            EnrollmentSystem.currentUser = "";
            EnrollmentSystem.currentPassword = "";
            EnrollmentSystem.currentRole = "";
            EnrollmentSystem.currentSemesterDB = "";
            EnrollmentSystem.db = "";

            for (java.awt.Window window : java.awt.Window.getWindows()) {
                window.dispose();
            }

            Login loginForm = new Login();
            loginForm.setVisible(true);
        }
    }
}