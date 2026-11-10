
package com.mycompany.enrollmentsystem;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.swing.JOptionPane;

public class StudentPDF {

    public static void generateAndOpenPDF(
            String studId, String selectedDb) {

        Document doc = new Document(PageSize.A4, 35, 35, 35, 35);
        PdfWriter writer = null;
        EnrollmentSystem es = new EnrollmentSystem();
        boolean connected = false;
        boolean generated = false;

        Font titleFont =
                new Font(FontFamily.TIMES_ROMAN, 16, Font.BOLD);

        Font subtitleFont =
                new Font(FontFamily.TIMES_ROMAN, 13, Font.BOLD);

        Font boldFont =
                new Font(FontFamily.TIMES_ROMAN, 10, Font.BOLD);

        Font normalFont =
                new Font(FontFamily.TIMES_ROMAN, 10);

        File pdfFile = new File(
                System.getProperty("user.home"),
                "Student_TOR_" + studId + "_" + selectedDb + ".pdf"
        );

        try {
            // Validate student ID and database name.
            if (studId == null || !studId.matches("\\d+")) {
                throw new Exception("Invalid student ID.");
            }

            if (selectedDb == null
                    || !selectedDb.matches("[A-Za-z0-9_]+")) {
                throw new Exception("Invalid database name.");
            }

            int studentId = Integer.parseInt(studId);

            // Connect ONLY to the selected semester database.
            es.currentDB(selectedDb);

            if (!es.DBConnect(
                    EnrollmentSystem.currentUser,
                    EnrollmentSystem.currentPassword,
                    selectedDb)) {

                throw new Exception(
                        "Could not connect to " + selectedDb
                );
            }

            connected = true;

            writer = PdfWriter.getInstance(
                    doc,
                    new FileOutputStream(pdfFile)
            );

            doc.open();

            // =========================================
            // 1. COLLEGE NAME AND LOGO
            // =========================================

            PdfPTable header = new PdfPTable(
                    new float[]{1.2f, 5f}
            );

            header.setWidthPercentage(85f);

            String logoPath =
                    "C:\\Users\\ychiong\\Documents\\NetBeansProjects\\Information-Management\\src\\main\\java\\com\\mycompany\\enrollmentsystem\\Resources\\logo.png";

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
            logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell collegeCell = new PdfPCell(
                    new Phrase(
                            "Ateneo De Davao University",
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
                    "Student Grade Sheet",
                    subtitleFont
            );

            subtitle.setAlignment(Element.ALIGN_CENTER);
            doc.add(subtitle);
            doc.add(new Paragraph("\n"));

            // =========================================
            // 2. STUDENT INFORMATION
            // =========================================

            String studName = "";
            String studCourse = "";
            String studYear = "";

            String studentSql =
                    "SELECT studname, studcrs, yrlvl "
                    + "FROM students "
                    + "WHERE studid = ?";

            try (PreparedStatement ps =
                    es.con.prepareStatement(studentSql)) {

                ps.setInt(1, studentId);

                try (ResultSet rs = ps.executeQuery()) {

                    if (rs.next()) {
                        studName = rs.getString("studname");
                        studCourse = rs.getString("studcrs");
                        studYear = rs.getString("yrlvl");

                    } else {
                        throw new Exception(
                                "Student ID " + studId
                                + " was not found in " + selectedDb
                        );
                    }
                }
            }

            String schoolYear = formatSchoolYear(selectedDb);

            PdfPTable infoLayout = new PdfPTable(
                    new float[]{1f, 1f}
            );

            infoLayout.setWidthPercentage(90f);

            String leftInfo =
                    "Student ID: " + studId + "\n\n"
                    + "Student Name: " + studName + "\n\n"
                    + "Student Year: " + studYear;

            String rightInfo =
                    "School Year: " + schoolYear + "\n\n"
                    + "Student Course: " + studCourse;

            addInfoCell(infoLayout, leftInfo, boldFont);
            addInfoCell(infoLayout, rightInfo, boldFont);

            doc.add(infoLayout);
            doc.add(new Paragraph("\n"));

            // =========================================
            // 3. GRADES TABLE
            // =========================================

            PdfPTable gradesTable = new PdfPTable(
                    new float[]{1.5f, 2.5f, 1.5f, 1.5f, 1.5f, 1.5f}
            );

            gradesTable.setWidthPercentage(90f);

            addTableCell(gradesTable, "SubjID", boldFont);
            addTableCell(gradesTable, "SubjCode", boldFont);
            addTableCell(gradesTable, "Prelim", boldFont);
            addTableCell(gradesTable, "Midterm", boldFont);
            addTableCell(gradesTable, "Prefinal", boldFont);
            addTableCell(gradesTable, "Final", boldFont);

            String gradesSql =
                    "SELECT s.subjid, s.subjcode, "
                    + "g.prelim, g.midterm, g.prefinal, g.final "
                    + "FROM enroll e "
                    + "JOIN subjects s ON e.subjid = s.subjid "
                    + "LEFT JOIN grades g ON e.eid = g.enroll_eid "
                    + "WHERE e.studid = ? "
                    + "ORDER BY s.subjcode";

            int subjectCount = 0;

            try (PreparedStatement ps =
                    es.con.prepareStatement(gradesSql)) {

                ps.setInt(1, studentId);

                try (ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {

                        subjectCount++;

                        addTableCell(
                                gradesTable,
                                rs.getString("subjid"),
                                normalFont
                        );

                        addTableCell(
                                gradesTable,
                                rs.getString("subjcode"),
                                normalFont
                        );

                        addTableCell(
                                gradesTable,
                                getGrade(rs, "prelim"),
                                normalFont
                        );

                        addTableCell(
                                gradesTable,
                                getGrade(rs, "midterm"),
                                normalFont
                        );

                        addTableCell(
                                gradesTable,
                                getGrade(rs, "prefinal"),
                                normalFont
                        );

                        addTableCell(
                                gradesTable,
                                getGrade(rs, "final"),
                                normalFont
                        );
                    }
                }
            }

            doc.add(gradesTable);
            doc.add(new Paragraph("\n"));

            // =========================================
            // 4. NUMBER OF SUBJECTS
            // =========================================

            Paragraph subjectCountPara = new Paragraph(
                    "Number of Subjects Listed: " + subjectCount,
                    boldFont
            );

            subjectCountPara.setIndentationLeft(35f);
            doc.add(subjectCountPara);

            doc.add(new Paragraph("\n\n"));

            // =========================================
            // 5. REGISTRAR SIGNATURE
            // =========================================

            PdfPTable signatureTable = new PdfPTable(1);
            signatureTable.setWidthPercentage(90f);

            PdfPCell signatureCell = new PdfPCell(
                    new Phrase(
                            "\n\n"
                            + "_____________________\n"
                            + "REGISTRAR",
                            boldFont
                    )
            );

            signatureCell.setBorder(Rectangle.NO_BORDER);
            signatureCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            signatureCell.setPaddingRight(25f);

            signatureTable.addCell(signatureCell);
            doc.add(signatureTable);

            generated = true;

        } catch (Exception e) {

            e.printStackTrace();

            JOptionPane.showMessageDialog(
                    null,
                    "Failed to generate Student TOR: "
                    + e.getMessage(),
                    "Student TOR Error",
                    JOptionPane.ERROR_MESSAGE
            );

        } finally {

            if (doc.isOpen()) {
                doc.close();
            }

            if (writer != null) {
                writer.close();
            }

            if (connected && es.con != null) {
                try {
                    es.con.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Open the PDF only if generation succeeded.
        if (generated) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(pdfFile);
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null,
                        "Student TOR was created, but could not be opened."
                );
            }
        }
    }

    // =========================================
    // HELPER METHODS
    // =========================================

    private static void addInfoCell(
            PdfPTable table, String text, Font font) {

        PdfPCell cell = new PdfPCell(
                new Phrase(text, font)
        );

        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        cell.setPadding(5f);

        table.addCell(cell);
    }

    private static void addTableCell(
            PdfPTable table, String text, Font font) {

        PdfPCell cell = new PdfPCell(
                new Phrase(text == null ? "" : text, font)
        );

        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(5f);

        table.addCell(cell);
    }

    private static String getGrade(
            ResultSet rs, String column) throws Exception {

        String grade = rs.getString(column);

        return grade == null ? "—" : grade;
    }

    private static String formatSchoolYear(String dbName) {

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile(
                        "(?i)(?:1st|2nd|summer)_sy(\\d{4}_\\d{4})"
                ).matcher(dbName);

        if (matcher.find()) {
            return matcher.group(1).replace("_", "-");
        }

        return dbName;
    }
}


