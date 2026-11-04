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
        PdfWriter docWriter = null;

        EnrollmentSystem es = new EnrollmentSystem();
        boolean connected = false;
        Connection tempCon = null;

        Font bfBold14 = new Font(FontFamily.TIMES_ROMAN, 14, Font.BOLD);
        Font bfBold11 = new Font(FontFamily.TIMES_ROMAN, 11, Font.BOLD);
        Font bfBold10 = new Font(FontFamily.TIMES_ROMAN, 10, Font.BOLD);
        Font bf10 = new Font(FontFamily.TIMES_ROMAN, 10);
        Font bf9 = new Font(FontFamily.TIMES_ROMAN, 9);

        String fileName = "TOR_Student_" + studId + ".pdf";
        File pdfFile = new File(System.getProperty("user.home"), fileName);

        try {
            // Connect to the current database.
            if (!es.DBConnect()) {
                JOptionPane.showMessageDialog(null, "Database connection failed.");
                return;
            }
            connected = true;

            int studentId = Integer.parseInt(studId);

            docWriter = PdfWriter.getInstance(doc, new FileOutputStream(pdfFile));
            doc.open();

            // 1. Header
            float[] headerWidths = {1.5f, 5f};
            PdfPTable headerTable = new PdfPTable(headerWidths);
            headerTable.setWidthPercentage(80f);
            headerTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell cellText = new PdfPCell(
                    new Phrase("International State College \nof The Philippinnes", bfBold11));
            cellText.setBorder(Rectangle.NO_BORDER);
            cellText.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cellText.setHorizontalAlignment(Element.ALIGN_LEFT);

            PdfPCell cellLogo = new PdfPCell(new Phrase("", bf10));
            cellLogo.setBorder(Rectangle.NO_BORDER);

            headerTable.addCell(cellLogo);
            headerTable.addCell(cellText);
            doc.add(headerTable);

            doc.add(new Paragraph("\n"));

            // 2. Title
            Paragraph title = new Paragraph("Official Transcript of Records", bfBold14);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph("\n"));

            // 3. Fetch student information from the current database.
            String studName = "";
            String studCourse = "";
            String studGender = "";

            String studentSql =
                    "SELECT studname, studcrs, studgender FROM students WHERE studid = ?";

            try (PreparedStatement pstmtStud = es.con.prepareStatement(studentSql)) {
                pstmtStud.setInt(1, studentId);

                try (ResultSet rsStud = pstmtStud.executeQuery()) {
                    if (rsStud.next()) {
                        studName = rsStud.getString("studname") != null
                                ? rsStud.getString("studname") : "";
                        studCourse = rsStud.getString("studcrs") != null
                                ? rsStud.getString("studcrs") : "";
                        studGender = rsStud.getString("studgender") != null
                                ? rsStud.getString("studgender") : "";
                    } else {
                        JOptionPane.showMessageDialog(null, "Student not found.");
                        return;
                    }
                }
            }

            // 4. Student information
            float[] infoWidths = {5f, 5f};
            PdfPTable infoTable = new PdfPTable(infoWidths);
            infoTable.setWidthPercentage(90f);

            addNoBorderCell(infoTable, "Student ID: " + studId,
                    bfBold11, Element.ALIGN_LEFT);
            addNoBorderCell(infoTable, "Student Name: " + studName,
                    bfBold11, Element.ALIGN_LEFT);

            addNoBorderCell(infoTable, "Student Course: " + studCourse,
                    bfBold11, Element.ALIGN_LEFT);
            addNoBorderCell(infoTable, "Gender: " + studGender,
                    bfBold11, Element.ALIGN_LEFT);

            doc.add(infoTable);
            doc.add(new Paragraph("\n"));

            // 5. Grades table
            float[] columnWidths = {1.8f, 2f, 4f, 2f, 1.5f};
            PdfPTable table = new PdfPTable(columnWidths);
            table.setWidthPercentage(90f);

            addHeaderCell(table, "SubjID", Element.ALIGN_CENTER, bfBold10);
            addHeaderCell(table, "Subj Code", Element.ALIGN_CENTER, bfBold10);
            addHeaderCell(table, "Descriptive Title", Element.ALIGN_CENTER, bfBold10);
            addHeaderCell(table, "Final", Element.ALIGN_CENTER, bfBold10);
            addHeaderCell(table, "Credit", Element.ALIGN_CENTER, bfBold10);

            // 6. Discover semester databases.
            List<String> semesterDbs = new ArrayList<>();

            String dbUser = EnrollmentSystem.currentUser;
            String dbPassword = EnrollmentSystem.currentPassword;

            if (dbUser == null || dbUser.isEmpty()) {
                dbUser = "root";
            }

            if (dbPassword == null) {
                dbPassword = "";
            }

            tempCon = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/?zeroDateTimeBehavior=CONVERT_TO_NULL",
                    dbUser,
                    dbPassword
            );

            try (Statement tempSt = tempCon.createStatement();
                 ResultSet rsDbs = tempSt.executeQuery("SHOW DATABASES")) {

                while (rsDbs.next()) {
                    String dbName = rsDbs.getString(1);
                    String lowerDb = dbName.toLowerCase();

                    if (lowerDb.startsWith("1st_sy")
                            || lowerDb.startsWith("2nd_sy")
                            || lowerDb.startsWith("summer_sy")) {
                        semesterDbs.add(dbName);
                    }
                }
            }

            int totalSubjectsListed = 0;

            // 7. Retrieve each semester's subjects and grades.
            for (String dbName : semesterDbs) {
                String query =
                        "SELECT s.subjid, s.subjcode, s.subjdesc, "
                        + "s.subjunits, g.final "
                        + "FROM `" + dbName + "`.enroll e "
                        + "JOIN `" + dbName + "`.subjects s "
                        + "ON e.subjid = s.subjid "
                        + "LEFT JOIN `" + dbName + "`.grades g "
                        + "ON e.eid = g.enroll_eid "
                        + "WHERE e.studid = ?";

                try (PreparedStatement pstmt = tempCon.prepareStatement(query)) {
                    pstmt.setInt(1, studentId);

                    try (ResultSet rs = pstmt.executeQuery()) {
                        boolean hasSemRows = false;

                        while (rs.next()) {
                            if (!hasSemRows) {
                                PdfPCell semCell = new PdfPCell(
                                        new Phrase(dbName, bfBold10));
                                semCell.setColspan(5);
                                semCell.setBorder(Rectangle.NO_BORDER);
                                semCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                                table.addCell(semCell);
                                hasSemRows = true;
                            }

                            totalSubjectsListed++;

                            String subjid = rs.getString("subjid");
                            String subjcode = rs.getString("subjcode");
                            String subjdesc = rs.getString("subjdesc");
                            String units = String.valueOf(rs.getInt("subjunits"));
                            String fin = rs.getString("final");

                            if (subjcode == null) {
                                subjcode = "";
                            }

                            if (subjdesc == null) {
                                subjdesc = "";
                            }

                            if (fin == null || fin.trim().isEmpty()
                                    || fin.equalsIgnoreCase("null")) {
                                fin = "No Grade";
                            }

                            addBorderlessRowCell(table, subjid,
                                    Element.ALIGN_CENTER, bf10);
                            addBorderlessRowCell(table, subjcode,
                                    Element.ALIGN_CENTER, bf10);
                            addBorderlessRowCell(table, subjdesc,
                                    Element.ALIGN_CENTER, bf10);
                            addBorderlessRowCell(table, fin,
                                    Element.ALIGN_CENTER, bf10);
                            addBorderlessRowCell(table, units,
                                    Element.ALIGN_CENTER, bf10);
                        }
                    }
                } catch (Exception e) {
                    // Skip semester databases that cannot be accessed.
                    System.err.println("Could not read database "
                            + dbName + ": " + e.getMessage());
                }
            }

            doc.add(table);
            doc.add(new Paragraph("\n"));

            // 8. Subject count
            Paragraph subjectCountPara = new Paragraph(
                    "Number of Subjects Listed: " + totalSubjectsListed, bfBold11);
            subjectCountPara.setIndentationLeft(30f);
            doc.add(subjectCountPara);

            doc.add(new Paragraph("\n"));

            // 9. Grading system
            float[] legendWidths = {5f, 5f};
            PdfPTable legendTable = new PdfPTable(legendWidths);
            legendTable.setWidthPercentage(90f);

            String oldGrading =
                    "Grading System up to SY 1981-82\n\n"
                    + "95-100 = 1.0 = Excellent\n"
                    + "90-94 = 1.5 = Very Good\n"
                    + "85-89 = 2.0 = Good\n"
                    + "80-84 = 2.5 = Fair\n"
                    + "75-79 = 3.0 = Passed\n"
                    + "Below 75 = 5.0 = Failed";

            String newGrading =
                    "New Grading System (1st Sem. SY1982-83 & up)\n\n"
                    + "95-100 = 1.0 = Excellent\n"
                    + "90-94 = 1.5 = Very Good\n"
                    + "85-89 = 2.0 = Good\n"
                    + "80-84 = 2.5 = Fair\n"
                    + "75-79 = 3.0 = Passed\n"
                    + "Below 75 = 5.0 = Failed";

            addNoBorderCell(legendTable, oldGrading, bf9, Element.ALIGN_LEFT);
            addNoBorderCell(legendTable, newGrading, bf9, Element.ALIGN_LEFT);

            doc.add(legendTable);

            Paragraph extraLegend = new Paragraph(
                    "FOR FCF Grades: O - Outstanding, HS - Highly Satisfactory, "
                    + "MS - Moderately Satisfactory, S - Satisfactory, "
                    + "F - Fair, P - Poor\n"
                    + "Quality Point Equivalent: 1.0 = 4.0, 1.5 = 3.5, "
                    + "2.0 = 3.0, 2.5 = 2.5, 3.0 = 2.0",
                    bf9);
            extraLegend.setIndentationLeft(30f);
            doc.add(extraLegend);

            doc.add(new Paragraph("\n\n"));

            // 10. Watermark disclaimer
            Paragraph sealNotice =
                    new Paragraph("NOT VALID WITHOUT SCHOOL SEAL", bfBold11);
            sealNotice.setIndentationLeft(30f);
            doc.add(sealNotice);

            doc.add(new Paragraph("\n\n"));

            // 11. Signatures
            float[] sigWidths = {5f, 5f};
            PdfPTable sigTable = new PdfPTable(sigWidths);
            sigTable.setWidthPercentage(90f);

            String currentDate =
                    new SimpleDateFormat("MM/dd/yyyy").format(new Date());

            addNoBorderCell(sigTable,
                    "Prepared By: MARY ANN D. MATURAN\n"
                    + "___________________________________\n\nDate: " + currentDate,
                    bfBold10, Element.ALIGN_LEFT);

            addNoBorderCell(sigTable,
                    "Checked By: MYRNA VILLACOSA\n"
                    + "___________________________________\n\n"
                    + "Registrar: ATTY. EDGAR ESCALANTE\n"
                    + "___________________________________",
                    bfBold10, Element.ALIGN_LEFT);

            doc.add(sigTable);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Failed to generate TOR: " + e.getMessage());
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }

            if (docWriter != null) {
                docWriter.close();
            }

            if (tempCon != null) {
                try {
                    tempCon.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (connected && es.con != null) {
                try {
                    es.con.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Open the generated PDF.
        try {
            if (pdfFile.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdfFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "TOR created, but it could not be opened automatically.");
        }
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

        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(align);
        cell.setBorder(Rectangle.NO_BORDER);
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

