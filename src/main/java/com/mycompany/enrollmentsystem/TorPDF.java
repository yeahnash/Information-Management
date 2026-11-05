/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.mycompany.enrollmentsystem;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.Chunk;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JOptionPane;

public class TorPDF {

    
    public static void generateAndOpenTOR(String studId) {

        Document doc = new Document(
                com.itextpdf.text.PageSize.A4,
                36, 36, 36, 36
        );

        PdfWriter writer = null;
        Connection tempCon = null;

        Font titleFont = new Font(FontFamily.TIMES_ROMAN, 16, Font.BOLD);
        Font subtitleFont = new Font(FontFamily.TIMES_ROMAN, 13, Font.BOLD);
        Font headerFont = new Font(FontFamily.TIMES_ROMAN, 9, Font.BOLD);
        Font normalFont = new Font(FontFamily.TIMES_ROMAN, 9);
        Font boldFont = new Font(FontFamily.TIMES_ROMAN, 9, Font.BOLD);

        Font underlineFont = new Font(FontFamily.TIMES_ROMAN, 9);
        underlineFont.setStyle(Font.UNDERLINE);

        File pdfFile = new File(
                System.getProperty("user.home"),
                "TOR_Student_" + studId + ".pdf"
        );

        try {
            int studentId = Integer.parseInt(studId);

            // Connect using the logged-in Admin credentials.
            tempCon = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/?zeroDateTimeBehavior=CONVERT_TO_NULL",
                    EnrollmentSystem.currentUser,
                    EnrollmentSystem.currentPassword
            );

            // =========================================
            // 1. FIND ALL SEMESTER DATABASES
            // =========================================

            List<String> semesterDbs = new ArrayList<>();

            try (Statement st = tempCon.createStatement();
                 ResultSet rs = st.executeQuery("SHOW DATABASES")) {

                while (rs.next()) {
                    String dbName = rs.getString(1);
                    String lower = dbName.toLowerCase();

                    if (lower.startsWith("1st_sy")
                            || lower.startsWith("2nd_sy")
                            || lower.startsWith("summer_sy")) {

                        semesterDbs.add(dbName);
                    }
                }
            }

            java.util.Collections.sort(
                    semesterDbs,
                    String.CASE_INSENSITIVE_ORDER
            );

            if (semesterDbs.isEmpty()) {
                throw new Exception("No semester databases were found.");
            }

            // =========================================
            // 2. GET STUDENT INFORMATION
            // =========================================

            String studName = "";
            String studCourse = "";
            String studGender = "";
            boolean studentFound = false;

            for (String dbName : semesterDbs) {

                String studentSql =
                        "SELECT studname, studcrs, studgender " +
                        "FROM `" + dbName + "`.students " +
                        "WHERE studid = ?";

                try (PreparedStatement ps =
                             tempCon.prepareStatement(studentSql)) {

                    ps.setInt(1, studentId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (rs.next()) {
                            studName = rs.getString("studname");
                            studCourse = rs.getString("studcrs");
                            studGender = rs.getString("studgender");

                            studentFound = true;
                            break;
                        }
                    }
                }
            }

            if (!studentFound) {
                throw new Exception(
                        "Student ID " + studId
                        + " was not found in any semester database."
                );
            }

            // =========================================
            // 3. CREATE PDF
            // =========================================

            writer = PdfWriter.getInstance(
                    doc,
                    new FileOutputStream(pdfFile)
            );

            doc.open();

            // =========================================
            // 4. MAIN TITLE WITH LOGO
            // =========================================

            PdfPTable header = new PdfPTable(new float[]{1.2f, 5f});
            header.setWidthPercentage(90f);
            header.setHorizontalAlignment(Element.ALIGN_CENTER);

            String logoPath =
                    "C:\\Users\\ychiong\\Documents\\NetBeansProjects\\Information-Managements\\src\\main\\java\\com\\mycompany\\enrollmentsystem\\Resources\\logo.png";

            PdfPCell logoCell;

            try {
                com.itextpdf.text.Image logo =
                        com.itextpdf.text.Image.getInstance(logoPath);

                logo.scaleToFit(65f, 65f);

                logoCell = new PdfPCell(logo);

            } catch (Exception ex) {
                logoCell = new PdfPCell(new Phrase(""));
                System.err.println(
                        "Logo could not be loaded: " + ex.getMessage()
                );
            }

            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            PdfPCell titleCell = new PdfPCell(
                    new Phrase(
                            "ATENEO DE DAVAO UNIVERSITY",
                            titleFont
                    )
            );

            titleCell.setBorder(Rectangle.NO_BORDER);
            titleCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            header.addCell(logoCell);
            header.addCell(titleCell);

            doc.add(header);

            // Official Transcript of Records
            Paragraph subtitle = new Paragraph(
                    "Official Transcript of Records",
                    subtitleFont
            );

            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingBefore(5f);
            subtitle.setSpacingAfter(20f);

            doc.add(subtitle);

            // =========================================
            // 5. STUDENT INFORMATION
            // =========================================

            // These are Paragraphs with tab stops, NOT tables.

            addTwoColumnParagraph(
                    doc,
                    "Student ID: " + studId,
                    "Student Name: " + studName,
                    boldFont
            );

            addTwoColumnParagraph(
                    doc,
                    "Student Course: " + studCourse,
                    "Gender: " + studGender,
                    boldFont
            );

            doc.add(new Paragraph("\n"));

            // =========================================
            // 6. GRADES TABLE
            // =========================================

            PdfPTable gradesTable = new PdfPTable(
                    new float[]{1.1f, 1.3f, 3.8f, 1f, 0.9f}
            );

            gradesTable.setWidthPercentage(100f);
            gradesTable.setSpacingBefore(5f);

            addHeaderCell(
                    gradesTable, "SubjID",
                    Element.ALIGN_CENTER, headerFont
            );

            addHeaderCell(
                    gradesTable, "SubjCode",
                    Element.ALIGN_CENTER, headerFont
            );

            addHeaderCell(
                    gradesTable, "Descriptive Title",
                    Element.ALIGN_CENTER, headerFont
            );

            addHeaderCell(
                    gradesTable, "Final",
                    Element.ALIGN_CENTER, headerFont
            );

            addHeaderCell(
                    gradesTable, "Credit",
                    Element.ALIGN_CENTER, headerFont
            );

            int totalSubjectsListed = 0;

            // =========================================
            // 7. RETRIEVE SUBJECTS FROM EVERY SEMESTER
            // =========================================

            for (String dbName : semesterDbs) {

                String query =
                        "SELECT s.subjid, s.subjcode, " +
                        "s.subjdesc, s.subjunits, g.final " +
                        "FROM `" + dbName + "`.enroll e " +
                        "JOIN `" + dbName + "`.subjects s " +
                        "ON e.subjid = s.subjid " +
                        "LEFT JOIN `" + dbName + "`.grades g " +
                        "ON e.eid = g.enroll_eid " +
                        "WHERE e.studid = ? " +
                        "ORDER BY s.subjcode";

                try (PreparedStatement ps =
                             tempCon.prepareStatement(query)) {

                    ps.setInt(1, studentId);

                    try (ResultSet rs = ps.executeQuery()) {

                        boolean hasSubjects = false;

                        while (rs.next()) {

                            // Display semester name in the SubjID column.
                            if (!hasSubjects) {

                                addSemesterCell(
                                        gradesTable,
                                        formatSemesterName(dbName),
                                        headerFont
                                );

                                // Fill the remaining four columns.
                                for (int i = 0; i < 4; i++) {
                                    addSemesterBlankCell(gradesTable);
                                }

                                hasSubjects = true;
                            }

                            totalSubjectsListed++;

                            addBorderlessRowCell(
                                    gradesTable,
                                    rs.getString("subjid"),
                                    Element.ALIGN_CENTER,
                                    normalFont
                            );

                            addBorderlessRowCell(
                                    gradesTable,
                                    rs.getString("subjcode"),
                                    Element.ALIGN_CENTER,
                                    normalFont
                            );

                            addBorderlessRowCell(
                                    gradesTable,
                                    rs.getString("subjdesc"),
                                    Element.ALIGN_LEFT,
                                    normalFont
                            );

                            addBorderlessRowCell(
                                    gradesTable,
                                    rs.getObject("final") == null
                                            ? "—"
                                            : rs.getString("final"),
                                    Element.ALIGN_CENTER,
                                    normalFont
                            );

                            addBorderlessRowCell(
                                    gradesTable,
                                    rs.getObject("subjunits") == null
                                            ? "—"
                                            : rs.getString("subjunits"),
                                    Element.ALIGN_CENTER,
                                    normalFont
                            );
                        }
                    }

                } catch (Exception ex) {
                    throw new Exception(
                            "Could not retrieve grades from database "
                            + dbName + ": " + ex.getMessage(),
                            ex
                    );
                }
            }

            doc.add(gradesTable);

            // =========================================
            // 8. NUMBER OF SUBJECTS
            // =========================================

            Paragraph subjectCount = new Paragraph(
                    "Number of Subjects Listed: " + totalSubjectsListed,
                    boldFont
            );

            subjectCount.setSpacingBefore(10f);
            subjectCount.setSpacingAfter(10f);

            doc.add(subjectCount);

            // =========================================
            // 9. HORIZONTAL SEPARATOR
            // =========================================

            doc.add(new com.itextpdf.text.pdf.draw.LineSeparator());

            doc.add(new Paragraph("\n"));

            // =========================================
            // 10. GRADING SYSTEM
            // =========================================

            PdfPTable gradingTable = new PdfPTable(2);
            gradingTable.setWidthPercentage(100f);

            Font gradingTitleFont =
                    new Font(FontFamily.TIMES_ROMAN, 9, Font.BOLD);

            Font gradingFont =
                    new Font(FontFamily.TIMES_ROMAN, 9);

            String oldSystem =
                    "Grading System up to SY 1981-82\n\n"
                    + "95-100 = 1.0 = Excellent\n"
                    + "90-94 = 1.5 = Very Good\n"
                    + "85-89 = 2.0 = Good\n"
                    + "80-84 = 2.5 = Fair\n"
                    + "75-79 = 3.0 = Passed\n"
                    + "Below 75 = 5.0 = Failed";

            String newSystem =
                    "New Grading System (1st Sem. SY 1982-83 & up)\n\n"
                    + "95-100 = 1.0 = Excellent\n"
                    + "90-94 = 1.5 = Very Good\n"
                    + "85-89 = 2.0 = Good\n"
                    + "80-84 = 2.5 = Fair\n"
                    + "75-79 = 3.0 = Passed\n"
                    + "Below 75 = 5.0 = Failed";

            PdfPCell oldSystemCell = new PdfPCell(
                    new Phrase(oldSystem, gradingFont)
            );

            oldSystemCell.setBorder(Rectangle.NO_BORDER);
            oldSystemCell.setPadding(3f);

            PdfPCell newSystemCell = new PdfPCell(
                    new Phrase(newSystem, gradingFont)
            );

            newSystemCell.setBorder(Rectangle.NO_BORDER);
            newSystemCell.setPadding(3f);

            gradingTable.addCell(oldSystemCell);
            gradingTable.addCell(newSystemCell);

            doc.add(gradingTable);

            Paragraph fcfGrades = new Paragraph(
                    "FOR FCF Grades: O - Outstanding, "
                    + "HS - Highly Satisfactory, "
                    + "MS - Moderately Satisfactory, "
                    + "S - Satisfactory, F - Fair, P - Poor",
                    gradingFont
            );

            fcfGrades.setSpacingBefore(5f);
            doc.add(fcfGrades);

            Paragraph qualityPoints = new Paragraph(
                    "Quality Point Equivalent: "
                    + "1.0 = 4.0, 1.5 = 3.5, 2.0 = 3.0, "
                    + "2.5 = 2.5, 3.0 = 2.0",
                    gradingFont
            );

            qualityPoints.setSpacingBefore(3f);
            doc.add(qualityPoints);

            // =========================================
            // 11. NOT VALID WITHOUT SCHOOL SEAL
            // =========================================

            Paragraph validity = new Paragraph(
                    "NOT VALID WITHOUT SCHOOL SEAL",
                    boldFont
            );

            validity.setAlignment(Element.ALIGN_CENTER);
            validity.setSpacingBefore(15f);
            validity.setSpacingAfter(20f);

            doc.add(validity);

            // =========================================
            // 12. PREPARED BY AND CHECKED BY
            // =========================================

            addTwoColumnParagraph(
                    doc,
                    "Prepared By: JOHN D. DOE",
                    "Checked By: JANE F. DOE",
                    underlineFont
            );

            // =========================================
            // 13. DATE AND REGISTRAR
            // =========================================

            String currentDate =
                    new SimpleDateFormat("MMMM dd, yyyy")
                            .format(new Date());

            addTwoColumnParagraph(
                    doc,
                    "Date: " + currentDate,
                    "Registrar: ATTY. EDGAR ALLEN POE",
                    normalFont
            );

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    "Failed to generate TOR: " + e.getMessage(),
                    "TOR Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return;

        } finally {

            if (doc.isOpen()) {
                doc.close();
            }

            if (writer != null) {
                writer.close();
            }

            if (tempCon != null) {
                try {
                    tempCon.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // =========================================
        // 14. OPEN GENERATED PDF
        // =========================================

        try {
            if (pdfFile.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdfFile);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    null,
                    "TOR was created, but could not be opened automatically."
            );

            e.printStackTrace();
        }
    }
    
    private static void addTwoColumnParagraph(
        Document doc,
        String leftText,
        String rightText,
        Font font) throws Exception {

    PdfPTable layout = new PdfPTable(new float[]{1f, 1f});
    layout.setWidthPercentage(100f);

    PdfPCell leftCell = new PdfPCell(
            new Phrase(leftText, font)
    );

    leftCell.setBorder(Rectangle.NO_BORDER);
    leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);
    leftCell.setPadding(3f);

    PdfPCell rightCell = new PdfPCell(
            new Phrase(rightText, font)
    );

    rightCell.setBorder(Rectangle.NO_BORDER);
    rightCell.setHorizontalAlignment(Element.ALIGN_LEFT);
    rightCell.setPadding(3f);

    layout.addCell(leftCell);
    layout.addCell(rightCell);

    doc.add(layout);
}


// Creates the semester heading in the first column.
private static void addSemesterCell(
        PdfPTable table,
        String semester,
        Font font) {

    PdfPCell cell = new PdfPCell(
            new Phrase(semester, font)
    );

    cell.setBackgroundColor(new BaseColor(230, 230, 230));
    cell.setHorizontalAlignment(Element.ALIGN_LEFT);
    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
    cell.setPadding(5f);

    table.addCell(cell);
}


// Adds the remaining blank cells beside the semester name.
private static void addSemesterBlankCell(PdfPTable table) {

    PdfPCell cell = new PdfPCell(new Phrase(""));

    cell.setBackgroundColor(new BaseColor(230, 230, 230));
    cell.setPadding(5f);

    table.addCell(cell);
}


// Converts database names into readable semester names.
private static String formatSemesterName(String dbName) {

    String lower = dbName.toLowerCase();

    String semester;

    if (lower.startsWith("1st_sy")) {
        semester = "1st Semester";
    } else if (lower.startsWith("2nd_sy")) {
        semester = "2nd Semester";
    } else if (lower.startsWith("summer_sy")) {
        semester = "Summer";
    } else {
        semester = dbName;
    }

    java.util.regex.Matcher matcher =
            java.util.regex.Pattern.compile(
                    "(?i)(?:1st|2nd|summer)_sy(\\d{4}_\\d{4})"
            ).matcher(dbName);

    if (matcher.find()) {
        String schoolYear = matcher.group(1).replace("_", "-");
        return semester + " - SY " + schoolYear;
    }

    return semester;
}


    private static void addHeaderCell(
            PdfPTable table, String text, int align, Font font) {

        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(align);
        cell.setBorder(Rectangle.BOX);
        cell.setBackgroundColor(new BaseColor(255, 255, 255));
        table.addCell(cell);
    }

    
    private static void addBorderlessRowCell(
            PdfPTable table, String text, int align, Font font) {

        PdfPCell cell = new PdfPCell(
                new Phrase(text == null ? "" : text, font)
        );

        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(5);

        table.addCell(cell);
    }


    private static void addNoBorderCell(
            PdfPTable table, String text, Font font, int align) {

        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }
    
    public static void generateAndOpenTORForSelectedDatabase(String selectedDb, String studID)
            throws Exception {

        // Validate the selected database name before using it in SQL
        if (selectedDb == null
                || !selectedDb.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Invalid database name.");
        }

        // Connect to the selected semester database
        EnrollmentSystem es = new EnrollmentSystem();
        es.currentDB(selectedDb);

        if (!es.DBConnect(
                EnrollmentSystem.currentUser,
                EnrollmentSystem.currentPassword,
                selectedDb)) {
            throw new Exception("Could not connect to " + selectedDb);
        }

        // Output PDF file
        java.io.File pdfFile = new java.io.File(
                System.getProperty("user.home"),
                "TOR_AllStudents_" + selectedDb + ".pdf"
        );

        com.itextpdf.text.Document document =
                new com.itextpdf.text.Document(
                        com.itextpdf.text.PageSize.A4.rotate(),
                        30, 30, 30, 30
                );

        try {
            com.itextpdf.text.pdf.PdfWriter.getInstance(
                    document,
                    new java.io.FileOutputStream(pdfFile)
            );

            document.open();

            // Title
            com.itextpdf.text.Font titleFont =
                    com.itextpdf.text.FontFactory.getFont(
                            com.itextpdf.text.FontFactory.HELVETICA_BOLD,
                            18
                    );

            com.itextpdf.text.Font normalFont =
                    com.itextpdf.text.FontFactory.getFont(
                            com.itextpdf.text.FontFactory.HELVETICA,
                            10
                    );

            com.itextpdf.text.Paragraph title =
                    new com.itextpdf.text.Paragraph(
                            "OFFICIAL TRANSCRIPT OF RECORDS",
                            titleFont
                    );

            title.setAlignment(
                    com.itextpdf.text.Element.ALIGN_CENTER
            );
            document.add(title);

            com.itextpdf.text.Paragraph semester =
                    new com.itextpdf.text.Paragraph(
                            "Selected Database: " + selectedDb,
                            normalFont
                    );

            semester.setAlignment(
                    com.itextpdf.text.Element.ALIGN_CENTER
            );
            document.add(semester);

            document.add(new com.itextpdf.text.Paragraph(" "));

            // Grade table
            com.itextpdf.text.pdf.PdfPTable table =
                    new com.itextpdf.text.pdf.PdfPTable(
                            new float[]{1.2f, 2.2f, 1.4f, 3.2f,
                                        1f, 1f, 1f, 1f, 0.8f}
                    );

            table.setWidthPercentage(100);

            String[] headers = {
                "Student ID",
                "Student Name",
                "Subject Code",
                "Descriptive Title",
                "Prelim",
                "Midterm",
                "Prefinal",
                "Final",
                "Credits"
            };

            for (String header : headers) {
                com.itextpdf.text.pdf.PdfPCell cell =
                        new com.itextpdf.text.pdf.PdfPCell(
                                new com.itextpdf.text.Phrase(
                                        header,
                                        com.itextpdf.text.FontFactory.getFont(
                                                com.itextpdf.text.FontFactory.HELVETICA_BOLD,
                                                9
                                        )
                                )
                        );

                cell.setHorizontalAlignment(
                        com.itextpdf.text.Element.ALIGN_CENTER
                );

                cell.setBackgroundColor(
                        new com.itextpdf.text.BaseColor(220, 220, 220)
                );

                cell.setPadding(5);
                table.addCell(cell);
            }

            // Query every student and their enrolled subjects

            String sql =
                    "SELECT st.studid, st.studname, " +
                    "s.subjcode, s.subjdesc, s.subjunits, " +
                    "g.prelim, g.midterm, g.prefinal, g.final " +
                    "FROM `" + selectedDb + "`.students st " +
                    "LEFT JOIN `" + selectedDb + "`.enroll e " +
                    "ON st.studid = e.studid " +
                    "LEFT JOIN `" + selectedDb + "`.subjects s " +
                    "ON e.subjid = s.subjid " +
                    "LEFT JOIN `" + selectedDb + "`.grades g " +
                    "ON e.eid = g.enroll_eid " +
                    "WHERE st.studid = ? " +
                    "ORDER BY s.subjcode";


            try (java.sql.PreparedStatement ps =
                    es.con.prepareStatement(sql)) {

                ps.setString(1, studID);

                try (java.sql.ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    table.addCell(String.valueOf(rs.getInt("studid")));
                    table.addCell(
                            rs.getString("studname") == null
                                    ? ""
                                    : rs.getString("studname")
                    );
                    table.addCell(
                            rs.getString("subjcode") == null
                                    ? "—"
                                    : rs.getString("subjcode")
                    );
                    table.addCell(
                            rs.getString("subjdesc") == null
                                    ? "No enrolled subjects"
                                    : rs.getString("subjdesc")
                    );

                    table.addCell(
                            rs.getObject("prelim") == null
                                    ? "—"
                                    : rs.getString("prelim")
                    );
                    table.addCell(
                            rs.getObject("midterm") == null
                                    ? "—"
                                    : rs.getString("midterm")
                    );
                    table.addCell(
                            rs.getObject("prefinal") == null
                                    ? "—"
                                    : rs.getString("prefinal")
                    );
                    table.addCell(
                            rs.getObject("final") == null
                                    ? "—"
                                    : rs.getString("final")
                    );
                    table.addCell(
                            rs.getObject("subjunits") == null
                                    ? "—"
                                    : rs.getString("subjunits")
                    );
                }
                }
            }

            document.add(table);

            document.add(new com.itextpdf.text.Paragraph(" "));
            document.add(new com.itextpdf.text.Paragraph(
                    "Note: — indicates that no grade or subject information is available.",
                    normalFont
            ));

        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        // Open the generated PDF
        if (java.awt.Desktop.isDesktopSupported()) {
            java.awt.Desktop.getDesktop().open(pdfFile);
        } else {
            throw new Exception(
                    "PDF created, but your computer cannot open it automatically: "
                    + pdfFile.getAbsolutePath()
            );
        }
    }

}

