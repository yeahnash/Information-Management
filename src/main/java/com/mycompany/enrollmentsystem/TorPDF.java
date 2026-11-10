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

        Document doc = new Document();
        PdfWriter writer = null;
        Connection tempCon = null;

        Font titleFont = new Font(FontFamily.TIMES_ROMAN, 16, Font.BOLD);
        Font subtitleFont = new Font(FontFamily.TIMES_ROMAN, 13, Font.BOLD);
        Font boldFont = new Font(FontFamily.TIMES_ROMAN, 10, Font.BOLD);
        Font normalFont = new Font(FontFamily.TIMES_ROMAN, 10);

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

            // Find all semester databases.
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

            // Sort semester databases for consistent report order.
            // Sort databases chronologically.
            semesterDbs.sort((a, b) ->
                    Long.compare(getSemesterSortKey(a), getSemesterSortKey(b))
            );

            if (semesterDbs.isEmpty()) {
                throw new Exception("No semester databases were found.");
            }

            // Student information.
            String studName = "";
            String studCourse = "";
            String studYear = "";
            List<String> schoolYears = new ArrayList<>();
            boolean studentFound = false;

            // Prepare the PDF.
            writer = PdfWriter.getInstance(
                    doc,
                    new FileOutputStream(pdfFile)
            );

            doc.open();

            // =========================================
            // 1. COLLEGE NAME AND LOGO
            // =========================================

            PdfPTable header = new PdfPTable(new float[]{1.2f, 5f});
            header.setWidthPercentage(85f);
            header.setHorizontalAlignment(Element.ALIGN_CENTER);

            // PASTE YOUR LOGO IMAGE PATH HERE.
            String logoPath = "C:\\Users\\ychiong\\Documents\\NetBeansProjects\\Information-Management\\src\\main\\java\\com\\mycompany\\enrollmentsystem\\Resources\\logo.png";

            PdfPCell logoCell;

            try {
                com.itextpdf.text.Image logo =
                        com.itextpdf.text.Image.getInstance(logoPath);

                logo.scaleToFit(65f, 65f);

                logoCell = new PdfPCell(logo);
            } catch (Exception ex) {
                // Leave the logo area blank if the image cannot be loaded.
                logoCell = new PdfPCell(new Phrase(""));
                System.err.println("Logo could not be loaded: "
                        + ex.getMessage());
            }

            logoCell.setBorder(Rectangle.NO_BORDER);
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell collegeCell = new PdfPCell(
                    new Phrase(
                            "Ateneo de Davao University",
                            titleFont
                    )
            );

            collegeCell.setBorder(Rectangle.NO_BORDER);
            collegeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            collegeCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            header.addCell(logoCell);
            header.addCell(collegeCell);

            doc.add(header);

            Paragraph subtitle = new Paragraph(
                    "Official Transcript of Records",
                    subtitleFont
            );

            subtitle.setAlignment(Element.ALIGN_CENTER);
            doc.add(subtitle);
            doc.add(new Paragraph("\n"));

            // =========================================
            // 2. GET STUDENT INFORMATION
            // =========================================

            for (String dbName : semesterDbs) {

                String studentSql =
                        "SELECT studname, studcrs, yrlvl " +
                        "FROM `" + dbName + "`.students " +
                        "WHERE studid = ?";

                try (PreparedStatement ps =
                             tempCon.prepareStatement(studentSql)) {

                    ps.setInt(1, studentId);

                    try (ResultSet rs = ps.executeQuery()) {

                        if (rs.next()) {
                            studName = rs.getString("studname");
                            studCourse = rs.getString("studcrs");
                            studYear = rs.getString("yrlvl");
                            studentFound = true;
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

            // Collect the school years represented by the databases.
            for (String dbName : semesterDbs) {
                java.util.regex.Matcher matcher =
                        java.util.regex.Pattern.compile(
                                "(?i)(?:1st|2nd|summer)_sy(\\d{4}_\\d{4})"
                        ).matcher(dbName);

                if (matcher.find()) {
                    String year = matcher.group(1).replace("_", "-");

                    if (!schoolYears.contains(year)) {
                        schoolYears.add(year);
                    }
                }
            }

            String schoolYearText = schoolYears.isEmpty()
                    ? "Not specified"
                    : String.join(", ", schoolYears);

            // =========================================
            // 3. STUDENT INFORMATION ABOVE THE TABLE
            // =========================================

            // This is a borderless two-column layout.
            // It is only used to align the information.

            PdfPTable infoLayout = new PdfPTable(new float[]{1f, 1f});
            infoLayout.setWidthPercentage(90f);

            String leftInfo =
                    "Student ID: " + studId + "\n\n"
                    + "Student Name: " + studName + "\n\n"
                    + "Student Year: " + studYear;

            String rightInfo =
                    "School Year: " + schoolYearText + "\n\n"
                    + "Student Course: " + studCourse;

            PdfPCell leftCell = new PdfPCell(
                    new Phrase(leftInfo, boldFont)
            );

            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            leftCell.setPadding(5);

            PdfPCell rightCell = new PdfPCell(
                    new Phrase(rightInfo, boldFont)
            );

            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            rightCell.setPadding(5);

            infoLayout.addCell(leftCell);
            infoLayout.addCell(rightCell);

            doc.add(infoLayout);
            doc.add(new Paragraph("\n"));

            // =========================================
            // 4. GRADES TABLE
            // =========================================

            PdfPTable gradesTable = new PdfPTable(
                    new float[]{1.2f, 1.8f, 4f, 1.5f, 1.2f}
            );

            gradesTable.setWidthPercentage(90f);

            addHeaderCell(gradesTable, "SubjID",
                    Element.ALIGN_CENTER, boldFont);

            addHeaderCell(gradesTable, "SubjCode",
                    Element.ALIGN_CENTER, boldFont);

            addHeaderCell(gradesTable, "Descriptive Title",
                    Element.ALIGN_CENTER, boldFont);

            addHeaderCell(gradesTable, "Final",
                    Element.ALIGN_CENTER, boldFont);

            addHeaderCell(gradesTable, "Credit",
                    Element.ALIGN_CENTER, boldFont);

            int totalSubjectsListed = 0;

            // =========================================
            // 5. RETRIEVE GRADES FROM EVERY SEMESTER
            // =========================================

            for (String dbName : semesterDbs) {

                String query =
                        "SELECT s.subjid, s.subjcode, s.subjdesc, " +
                        "g.final, s.subjunits " +
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

                            if (!hasSubjects) {
                                PdfPCell semesterCell = new PdfPCell(
                                        new Phrase(
                                                formatSemesterName(dbName),
                                                boldFont
                                        )
                                );

                                semesterCell.setColspan(5);
                                semesterCell.setHorizontalAlignment(
                                        Element.ALIGN_LEFT
                                );
                                semesterCell.setBackgroundColor(
                                        new BaseColor(230, 230, 230)
                                );
                                semesterCell.setPadding(6);

                                gradesTable.addCell(semesterCell);
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
                                    rs.getString("subjdesc") == null
                                            ? "-" : rs.getString("subjdesc"),
                                    Element.ALIGN_LEFT,
                                    normalFont
                            );

                            addBorderlessRowCell(
                                    gradesTable,
                                    rs.getString("final") == null
                                            ? "-" : rs.getString("final"),
                                    Element.ALIGN_CENTER,
                                    normalFont
                            );

                            addBorderlessRowCell(
                                    gradesTable,
                                    rs.getString("subjunits") == null
                                            ? "-" : rs.getString("subjunits"),
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
            doc.add(new Paragraph("\n"));
            
            
            // =========================================
            // 6. GRADING SYSTEM
            // =========================================

            Paragraph gradingTitle = new Paragraph(
                    "GRADING SYSTEM",
                    boldFont
            );

            gradingTitle.setAlignment(Element.ALIGN_CENTER);
            gradingTitle.setSpacingAfter(8f);
            doc.add(gradingTitle);

            // Two-column layout
            PdfPTable gradingTable = new PdfPTable(2);
            gradingTable.setWidthPercentage(100f);
            gradingTable.setWidths(new float[]{1f, 1f});

            // LEFT COLUMN: OLD GRADING SYSTEM
            PdfPCell oldSystemCell = new PdfPCell();
            oldSystemCell.setBorder(Rectangle.NO_BORDER);
            oldSystemCell.setPadding(5f);

            Paragraph oldTitle = new Paragraph(
                    "Grading System up to SY 1981-82",
                    boldFont
            );
            oldSystemCell.addElement(oldTitle);

            oldSystemCell.addElement(new Paragraph(
                    "95-100 = 1.0 = Excellent", normalFont));
            oldSystemCell.addElement(new Paragraph(
                    "90-94 = 1.5 = Very Good", normalFont));
            oldSystemCell.addElement(new Paragraph(
                    "85-89 = 2.0 = Good", normalFont));
            oldSystemCell.addElement(new Paragraph(
                    "80-84 = 2.5 = Fair", normalFont));
            oldSystemCell.addElement(new Paragraph(
                    "75-79 = 3.0 = Passed", normalFont));
            oldSystemCell.addElement(new Paragraph(
                    "Below 75 = 5.0 = Failed", normalFont));

            gradingTable.addCell(oldSystemCell);

            // RIGHT COLUMN: NEW GRADING SYSTEM
            PdfPCell newSystemCell = new PdfPCell();
            newSystemCell.setBorder(Rectangle.NO_BORDER);
            newSystemCell.setPadding(5f);

            Paragraph newTitle = new Paragraph(
                    "New Grading System (1st Sem. SY 1982-83 & up)",
                    boldFont
            );
            newSystemCell.addElement(newTitle);

            newSystemCell.addElement(new Paragraph(
                    "95-100 = 1.0 = Excellent", normalFont));
            newSystemCell.addElement(new Paragraph(
                    "90-94 = 1.5 = Very Good", normalFont));
            newSystemCell.addElement(new Paragraph(
                    "85-89 = 2.0 = Good", normalFont));
            newSystemCell.addElement(new Paragraph(
                    "80-84 = 2.5 = Fair", normalFont));
            newSystemCell.addElement(new Paragraph(
                    "75-79 = 3.0 = Passed", normalFont));
            newSystemCell.addElement(new Paragraph(
                    "Below 75 = 5.0 = Failed", normalFont));

            gradingTable.addCell(newSystemCell);

            doc.add(gradingTable);

            // FCF GRADES
            Paragraph fcfGrades = new Paragraph(
                    "FOR FCF Grades: O - Outstanding, HS - Highly Satisfactory, "
                    + "MS - Moderately Satisfactory, S - Satisfactory, "
                    + "F - Fair, P - Poor",
                    normalFont
            );

            fcfGrades.setSpacingBefore(5f);
            doc.add(fcfGrades);

            // QUALITY POINT EQUIVALENT
            Paragraph qpe = new Paragraph(
                    "Quality Point Equivalent: 1.0 = 4.0, 1.5 = 3.5, "
                    + "2.0 = 3.0, 2.5 = 2.5, 3.0 = 2.0",
                    normalFont
            );

            qpe.setSpacingBefore(3f);
            doc.add(qpe);

            doc.add(new Paragraph("\n"));


            // =========================================
            // 7. SCHOOL SEAL NOTICE
            // =========================================

            Paragraph sealNotice = new Paragraph(
                    "NOT VALID WITHOUT SCHOOL SEAL",
                    boldFont
            );

            sealNotice.setAlignment(Element.ALIGN_CENTER);
            doc.add(sealNotice);

            doc.add(new Paragraph("\n\n"));

            // =========================================
            // 8. PREPARATION AND REGISTRAR DETAILS
            // =========================================

            // Get the current date automatically.
            String currentDate = new SimpleDateFormat(
                    "MMMM dd, yyyy"
            ).format(new Date());

            PdfPTable footerTable = new PdfPTable(
                    new float[]{1f, 1f}
            );

            footerTable.setWidthPercentage(90f);

            // Prepared By
            PdfPCell preparedCell = new PdfPCell(
                    new Phrase("Prepared By: ____________________", normalFont)
            );
            preparedCell.setBorder(Rectangle.NO_BORDER);
            preparedCell.setPadding(8f);
            footerTable.addCell(preparedCell);

            // Date
            PdfPCell dateCell = new PdfPCell(
                    new Phrase("Date: " + currentDate, normalFont)
            );
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.setPadding(8f);
            footerTable.addCell(dateCell);

            // Checked By
            PdfPCell checkedCell = new PdfPCell(
                    new Phrase("Checked By: _____________________", normalFont)
            );
            checkedCell.setBorder(Rectangle.NO_BORDER);
            checkedCell.setPadding(8f);
            footerTable.addCell(checkedCell);

            // Registrar
            PdfPCell registrarCell = new PdfPCell(
                    new Phrase("Registrar: ________________________", normalFont)
            );
            registrarCell.setBorder(Rectangle.NO_BORDER);
            registrarCell.setPadding(8f);
            footerTable.addCell(registrarCell);

            doc.add(footerTable);

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

        // Open generated PDF.
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
    
    private static long getSemesterSortKey(String dbName) {

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile(
                        "(?i)(1st|2nd|summer)_sy(\\d{4})_(\\d{4})"
                ).matcher(dbName);

        if (matcher.find()) {

            String semester = matcher.group(1).toLowerCase();
            int year = Integer.parseInt(matcher.group(2));

            int semesterOrder;

            switch (semester) {
                case "1st":
                    semesterOrder = 1;
                    break;
                case "2nd":
                    semesterOrder = 2;
                    break;
                default:
                    semesterOrder = 3;
                    break;
            }

            return (long) year * 10 + semesterOrder;
        }

        return 0;
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

}

