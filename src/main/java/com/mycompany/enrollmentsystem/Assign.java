/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.enrollmentsystem;

/**
 *
 * @author Admin
 */
public class Assign extends EnrollmentSystem {

    static int subjID;

    public void setsubjID(int a){
        subjID = a;
    }

    public int getsubjID(){
        return subjID;
    }

    public String assignTchr(int tID){
        DBConnect();

        String query = "INSERT INTO assign(TID, SubjID) "
                + "VALUES(" + tID + ", " + subjID + ")";

        try {
            st.executeUpdate(query);

        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            return "This subject is already assigned to a teacher.";

        } catch (Exception e) {
            System.out.println("Failed to assign teacher " + e);
            return "Assignment Failed";
        }

        return "Teacher " + tID + " assigned to subject " + subjID;
    }

    public String deleteSubject(int tID){
        DBConnect();

        String query = "DELETE FROM assign "
                + "WHERE TID = " + tID
                + " AND SubjID = " + subjID;

        System.out.println("tchrid = " + tID);
        System.out.println("subjID = " + subjID);
        System.out.println("query = " + query);

        try {
            int rows = st.executeUpdate(query);

            System.out.println("rows deleted = " + rows);

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