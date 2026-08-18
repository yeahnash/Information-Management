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
    public void newstudent(int studID, String studName, String studAdd, String studCrs, String studGender, String yrLvl){
        String query = "insert into Students values ("+ studID + ",'"+ studName +"','"+ studAdd );
        b.st.executeUpdate(query);
        try {
            
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
