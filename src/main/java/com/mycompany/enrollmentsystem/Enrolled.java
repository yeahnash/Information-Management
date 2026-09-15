/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.enrollmentsystem;

/**
 *
 * @author Admin
 */
public class Enrolled extends EnrollmentSystem{
    static int subjID;
    public void setsubjID(int a){
        subjID = a;
    }
    public int getsubjID(){
        return subjID;
    }
    
    
    
    
    
    public String enrollStud(int studID){
        DBConnect();
        int neweID = 0;
        String query1 = "select max(eID) + 1 as maxID from enroll";
        try {
            rs = st.executeQuery(query1);
            if (rs.next() && rs.getInt("maxID") > 0) {
                neweID = rs.getInt("maxID");
            } else {
                neweID = 1;
            }
        } catch (Exception e) {
            System.out.println("Failed to get enrollment ID " + e);
        }

        
        
        String enrollQuery = "insert into enroll(studID, subjID, evaluation)" + "values('"+studID+"','"+subjID+"',' ')";
        try {
            st.executeUpdate(enrollQuery);
        } catch (java.sql.SQLIntegrityConstraintViolationException e) {
            return "Student is already Enrolled in this subject.";
        } catch (Exception e) {
            System.out.println("Failed to insert" + e);
            return "Enrollment Failed";
        }
        return "Student "+ studID + " Enrolled to " + subjID;               
    }
    
    
    
    public String dropSubject(int studID){
        DBConnect();
        
        String query = "delete from enroll " + "where studID = " + studID + " and subjID = " + subjID;
        
        System.out.println("studID = " + studID);
System.out.println("subjID = " + subjID);
System.out.println("query = " + query);
        try {
            int rows = st.executeUpdate(query);
            System.out.println("rows deleted = " + rows);

            if(rows>0){
                return "Subject "+ subjID + " dropped from student " +studID;
            }else{
                return "Student is not Enrolled in this subject.";
            }
            
        } catch (Exception e) {
            System.out.println("Failed to drop subject "+ e);
            return "Drop Failed";
        }
    }
}
