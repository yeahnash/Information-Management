/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.enrollmentsystem;

/**
 *
 * @author ychiong
 */
public class Students {
    public void newstudent(String studID, String studName, String studAdd, String studCrs, String studGender, String yrLvl){
        EnrollmentSystem b = new EnrollmentSystem();
        b.DBConnect();
        int newID = 0;
        String query = "insert into Students values ("+ studID + ",'"+ studName +"','"+ studAdd );
        b.st.executeUpdate(query);
        String query2 = "select max(studID) + 1 as maxID from students";
        try {
            b.executeUpdate(query2);
            if (b.rs.next() && b.rs.getInt("maxID") > 0) {
                newID = b.rs.getInt("maxID")
            } else {
                newID = 1001;
            }
        } catch (Exception e) {
        }
    }
    public void delete_student(){
        
    }
    public void update_student(){
        EnrollmentSystem b = new EnrollmentSystem();
        b.DBConnect();
    }
}
