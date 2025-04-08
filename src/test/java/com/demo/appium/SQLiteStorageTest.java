package com.demo.appium;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.demo.appium.util.SQLiteStorage;

public class SQLiteStorageTest {
    

    public static void main(String[] arges)
    {
        SQLiteStorage sqLiteStorage = null;

        try
        {
            sqLiteStorage = new SQLiteStorage("futu.db");
            //sqLiteStorage.createTables("futuAgencies", new String[]{"机构名称", "股票名称","股票代码","持仓比例:real","变动股份:real","变动比例:real"},null);

            //sqLiteStorage.storeData("futuAgencies", new String[]{"机构名称", "股票名称","股票代码","持仓比例","变动股份","变动比例"}, new String[]{"先锋领航", "苹果","APPL","+4.8","4916.88万","-0.33"});

            // 优化查询语句，只选择需要的列，提高查询效率
            String query = "SELECT 机构名称, 股票名称, 股票代码, 持仓比例, 变动股份, 变动比例 FROM futuAgencies WHERE 机构名称 = ?";
            ResultSet result = sqLiteStorage.queryWithParams(query, "先锋领航");
            
            // 逐行读取并打印结果
            while (result.next()) {
                System.out.println("机构名称: " + result.getString("机构名称"));
                System.out.println("股票名称: " + result.getString("股票名称"));
                System.out.println("股票代码: " + result.getString("股票代码"));
                System.out.println("持仓比例: " + result.getDouble("持仓比例"));
                System.out.println("变动股份: " + result.getDouble("变动股份"));
                System.out.println("变动比例: " + result.getDouble("变动比例"));
                System.out.println("-----------------------------");
            }

            sqLiteStorage.dropTable("futuAgencies");

            

        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
        finally
        {
            if (sqLiteStorage != null)
            {
                try {
                    sqLiteStorage.close();
                } catch (SQLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
                
        }
    
        
    }

    
}
