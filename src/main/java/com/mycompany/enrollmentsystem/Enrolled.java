/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.enrollmentsystem;

public class Enrolled extends EnrollmentSystem {

    static int subjID;

    public void setsubjID(int a) {
        subjID = a;
    }

    public int getsubjID() {
        return subjID;
    }

    public String enrollStud(int studID) {

        DBConnect();

        String enrollQuery =
                "INSERT INTO enroll(studID, subjID, evaluation) "
                + "VALUES(" + studID + ", " + subjID + ", ' ')";

        try {

            st.executeUpdate(enrollQuery);

        } catch (java.sql.SQLIntegrityConstraintViolationException e) {

            return "Student is already Enrolled in this subject.";

        } catch (Exception e) {

            System.out.println("Failed to insert " + e);
            return "Enrollment Failed";
        }

        return "Student " + studID
                + " Enrolled to " + subjID;
    }

    public String dropSubject(int studID) {

        DBConnect();

        String query =
                "DELETE FROM enroll "
                + "WHERE studID = " + studID
                + " AND subjID = " + subjID;

        System.out.println("studID = " + studID);
        System.out.println("subjID = " + subjID);
        System.out.println("query = " + query);

        try {

            int rows = st.executeUpdate(query);

            System.out.println("rows deleted = " + rows);

            if (rows > 0) {

                return "Subject " + subjID
                        + " dropped from student " + studID;

            } else {

                return "Student is not Enrolled in this subject.";
            }

        } catch (Exception e) {

            System.out.println(
                    "Failed to drop subject " + e
            );

            return "Drop Failed";
        }
    }
}
