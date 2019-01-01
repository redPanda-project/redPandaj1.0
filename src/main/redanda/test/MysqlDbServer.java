/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package main.redanda.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author rflohr
 */
public class MysqlDbServer {
    
    public static void main(String[] args) throws SQLException {
        
        Connection con = DriverManager.getConnection("jdbc:mysql://localhost/redpanda", 
        "redpanda", 
        "jfhezchuthre");
        
    }
    
}
