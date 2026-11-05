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
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.swing.JOptionPane;

public class StudentPDF {

    public static void generateAndOpenPDF(String studId, String schoolYear) {
        Document doc = new Document();
        PdfWriter docWriter = null;
        EnrollmentSystem es = new EnrollmentSystem();
        boolean connected = false;

        Font bfBold14 = new Font(FontFamily.TIMES_ROMAN, 14, Font.BOLD);
        Font bfBold12 = new Font(FontFamily.TIMES_ROMAN, 12, Font.BOLD);
        Font bf12 = new Font(FontFamily.TIMES_ROMAN, 12);

        String fileName = "Student_Grade_Sheet_" + studId + ".pdf";
        File pdfFile = new File(System.getProperty("user.home"), fileName);

        try {
            // Connect using an EnrollmentSystem instance.
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
                    new Phrase("Ateneo de Davao \nUniversity", bfBold12));
            cellText.setBorder(Rectangle.NO_BORDER);
            cellText.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cellText.setHorizontalAlignment(Element.ALIGN_LEFT);

            PdfPCell cellLogo = new PdfPCell(new Phrase(""));
            cellLogo.setBorder(Rectangle.NO_BORDER);

            headerTable.addCell(cellLogo);
            headerTable.addCell(cellText);
            doc.add(headerTable);

            doc.add(new Paragraph("\n"));

            // 2. Title
            Paragraph title = new Paragraph("Student Grade Sheet", bfBold14);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph("\n"));

            // 3. Fetch student information
            String studName = "";
            String studCourse = "";
            String studYear = "";

            String studentSql =
                    "SELECT studname, studcrs, yrlvl FROM students WHERE studid = ?";

            try (PreparedStatement pstmtStud = es.con.prepareStatement(studentSql)) {
                pstmtStud.setInt(1, studentId);

                try (ResultSet rsStud = pstmtStud.executeQuery()) {
                    if (rsStud.next()) {
                        studName = rsStud.getString("studname") != null
                                ? rsStud.getString("studname") : "";
                        studCourse = rsStud.getString("studcrs") != null
                                ? rsStud.getString("studcrs") : "";
                        studYear = rsStud.getString("yrlvl") != null
                                ? rsStud.getString("yrlvl") : "";
                    } else {
                        JOptionPane.showMessageDialog(null, "Student not found.");
                        return;
                    }
                }
            }

            // 4. Student metadata
            float[] infoWidths = {5f, 5f};
            PdfPTable infoTable = new PdfPTable(infoWidths);
            infoTable.setWidthPercentage(90f);

            addNoBorderCell(infoTable, "Student ID: " + studId,
                    bfBold12, Element.ALIGN_LEFT);
            addNoBorderCell(infoTable, "School Year: " + schoolYear,
                    bfBold12, Element.ALIGN_LEFT);

            addNoBorderCell(infoTable, "Student Name: " + studName,
                    bfBold12, Element.ALIGN_LEFT);
            addNoBorderCell(infoTable, "Student Course: " + studCourse,
                    bfBold12, Element.ALIGN_LEFT);

            addNoBorderCell(infoTable, "Student Year: " + studYear,
                    bfBold12, Element.ALIGN_LEFT);
            addNoBorderCell(infoTable, "", bfBold12, Element.ALIGN_LEFT);

            doc.add(infoTable);
            doc.add(new Paragraph("\n"));

            // 5. Grades table
            float[] columnWidths = {2f, 3f, 2f, 2f, 2f, 2f};
            PdfPTable table = new PdfPTable(columnWidths);
            table.setWidthPercentage(90f);

            insertCell(table, "SubjID", Element.ALIGN_CENTER, 1, bfBold12);
            insertCell(table, "SubjCode", Element.ALIGN_CENTER, 1, bfBold12);
            insertCell(table, "Prelim", Element.ALIGN_CENTER, 1, bfBold12);
            insertCell(table, "Midterm", Element.ALIGN_CENTER, 1, bfBold12);
            insertCell(table, "Prefinal", Element.ALIGN_CENTER, 1, bfBold12);
            insertCell(table, "Final", Element.ALIGN_CENTER, 1, bfBold12);

            // 6. Fetch enrolled subjects and grades
            String gradesSql =
                    "SELECT s.subjid, s.subjcode, g.prelim, g.midterm, "
                    + "g.prefinal, g.final "
                    + "FROM enroll e "
                    + "JOIN subjects s ON e.subjid = s.subjid "
                    + "LEFT JOIN grades g ON e.eid = g.enroll_eid "
                    + "WHERE e.studid = ?";

            int subjectCount = 0;

            try (PreparedStatement pstmtGrades = es.con.prepareStatement(gradesSql)) {
                pstmtGrades.setInt(1, studentId);

                try (ResultSet rsGrades = pstmtGrades.executeQuery()) {
                    while (rsGrades.next()) {
                        subjectCount++;

                        String subjid = rsGrades.getString("subjid");
                        String subjcode = rsGrades.getString("subjcode");
                        String prelim = rsGrades.getString("prelim");
                        String midterm = rsGrades.getString("midterm");
                        String prefinal = rsGrades.getString("prefinal");
                        String fin = rsGrades.getString("final");

                        insertCell(table, subjid, Element.ALIGN_CENTER, 1, bf12);
                        insertCell(table, subjcode == null ? "" : subjcode,
                                Element.ALIGN_CENTER, 1, bf12);
                        insertCell(table, prelim == null ? "No Grade" : prelim,
                                Element.ALIGN_CENTER, 1, bf12);
                        insertCell(table, midterm == null ? "No Grade" : midterm,
                                Element.ALIGN_CENTER, 1, bf12);
                        insertCell(table, prefinal == null ? "No Grade" : prefinal,
                                Element.ALIGN_CENTER, 1, bf12);
                        insertCell(table, fin == null ? "No Grade" : fin,
                                Element.ALIGN_CENTER, 1, bf12);
                    }
                }
            }

            doc.add(table);
            doc.add(new Paragraph("\n"));

            // 7. Subject count
            Paragraph subjectCountPara =
                    new Paragraph("Number of Subjects Listed: " + subjectCount, bfBold12);
            subjectCountPara.setIndentationLeft(30f);
            doc.add(subjectCountPara);

            doc.add(new Paragraph("\n\n\n"));

            // 8. Registrar signature
            PdfPTable sigTable = new PdfPTable(1);
            sigTable.setWidthPercentage(40f);
            sigTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

            PdfPCell sigCell = new PdfPCell(
                    new Phrase("___________________________\nRegistrar", bfBold12));
            sigCell.setBorder(Rectangle.NO_BORDER);
            sigCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            sigTable.addCell(sigCell);

            doc.add(sigTable);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null,
                    "Failed to generate student grade sheet: " + e.getMessage());
        } finally {
            if (doc.isOpen()) {
                doc.close();
            }

            if (docWriter != null) {
                docWriter.close();
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
                    "PDF created, but it could not be opened automatically.");
        }
    }

    private static void insertCell(
            PdfPTable table, String text, int align, int colspan, Font font) {

        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text.trim(), font));
        cell.setHorizontalAlignment(align);
        cell.setColspan(colspan);
        cell.setBorder(Rectangle.BOX);
        cell.setBackgroundColor(new BaseColor(255, 255, 255));
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

