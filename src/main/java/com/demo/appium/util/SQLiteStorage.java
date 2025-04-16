package com.demo.appium.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SQLiteStorage {

    private Connection conn;
    private static final Pattern VALID_IDENTIFIER = Pattern.compile("^[\\u4e00-\\u9fa5a-zA-Z_][\\u4e00-\\u9fa5a-zA-Z0-9_]*$");
    
    public SQLiteStorage(String dbFile) throws SQLException {
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        this.conn.setAutoCommit(false); // 关闭自动提交，启用事务
    }
    
    public enum ColumnType {
        TEXT,
        INTEGER,
        REAL
    }

    public void createTables(String tableName, String[] columnDefinitions, String primaryKey) throws SQLException {
        try {
            if (tableName == null || tableName.isEmpty()) {
                throw new IllegalArgumentException("表名不能为空");
            }
            if (!VALID_IDENTIFIER.matcher(tableName).matches()) {
                throw new IllegalArgumentException("表名包含非法字符");
            }
            if (columnDefinitions == null || columnDefinitions.length == 0) {
                throw new IllegalArgumentException("至少需要定义一个列");
            }

            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("CREATE TABLE IF NOT EXISTS ")
                      .append(tableName)
                      .append(" (");

            for (String column : columnDefinitions) {
                String[] parts = column.split(":");
                if (parts.length < 1) {
                    throw new IllegalArgumentException("列定义必须包含列名: " + column);
                }
                if (!VALID_IDENTIFIER.matcher(parts[0].trim()).matches()) {
                    throw new IllegalArgumentException("列名包含非法字符: " + column);
                }
                ColumnType columnType = parts.length > 1 ? 
                    ColumnType.valueOf(parts[1].trim().toUpperCase()) : ColumnType.TEXT;
                sqlBuilder.append(parts[0].trim()).append(" ").append(columnType).append(", ");
            }

            if (primaryKey != null && !primaryKey.isEmpty()) {
                if (!VALID_IDENTIFIER.matcher(primaryKey).matches()) {
                    throw new IllegalArgumentException("主键名包含非法字符");
                }
                sqlBuilder.append("PRIMARY KEY (").append(primaryKey).append(")");
            } else {
                sqlBuilder.setLength(sqlBuilder.length() - 2);
            }

            sqlBuilder.append(")");

            try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
                pstmt.execute();
                conn.commit();
            }
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        }
    }

    public void dropTable(String tableName) throws SQLException {
        try {
            if (tableName == null || tableName.isEmpty()) {
                throw new IllegalArgumentException("表名不能为空");
            }
            if (!VALID_IDENTIFIER.matcher(tableName).matches()) {
                throw new IllegalArgumentException("表名包含非法字符");
            }

            String sql = "DROP TABLE IF EXISTS " + tableName;
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.execute();
                conn.commit();
            }
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        }
    }

    public void batchSaveCSVFilesToDB(String tableName,String[] columnDefinition,String directoryPath,String excludeString,boolean skipFirstTitleLine)
    {
        File currentDir = new File(directoryPath);
        // 获取所有.csv文件
        File[] csvFiles ;
        
        if (excludeString != null && !excludeString.equals(""))
            csvFiles = currentDir.listFiles((dir, name) -> name.endsWith(".csv") && !name.contains(excludeString));
        else
            csvFiles = currentDir.listFiles((dir, name) -> name.endsWith(".csv"));


        if (csvFiles != null) {
            for (File csvFile : csvFiles) 
            {
                List<String[]> data = new ArrayList<>();

                try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
                    String line;
                    boolean isFirstLine = true;

                    while ((line = reader.readLine()) != null) {

                        if (skipFirstTitleLine)
                            if (isFirstLine)
                            {
                                isFirstLine = false;
                                continue;
                            }

                        //line = line.replaceAll("%", "").replaceAll("<", "");

                        if (line.split(",").length == columnDefinition.length ) {

                            data.add(line.split(","));
                            System.out.println("add line:" + line);
                        } else {
                            System.out.println("ignore line:" + line);
                        }
                    }

                    // 直接插入数据，假设表已经存在
                    for (String[] row : data) {
                        storeData(tableName, columnDefinition, row);
                    }
                } catch (Exception e) {
                    System.err.println("读取CSV文件时出错：" + e.getMessage());
                }


            }
        }

    }
    
    public void storeData(String tableName, String[] columns, Object[] values) throws SQLException {
        try {
            if (tableName == null || tableName.isEmpty()) {
                throw new IllegalArgumentException("表名不能为空");
            }
            if (!VALID_IDENTIFIER.matcher(tableName).matches()) {
                throw new IllegalArgumentException("表名包含非法字符");
            }
            if (columns == null || columns.length == 0) {
                throw new IllegalArgumentException("至少需要定义一个列");
            }
            if (values == null || values.length == 0) {
                throw new IllegalArgumentException("至少需要提供一个值");
            }
            if (columns.length != values.length) {
                throw new IllegalArgumentException("列和值的数量必须匹配");
            }

            for (String column : columns) {
                if (!VALID_IDENTIFIER.matcher(column).matches()) {
                    throw new IllegalArgumentException("列名包含非法字符: " + column);
                }
            }

            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("INSERT INTO ")
                      .append(tableName)
                      .append(" (");
            for (String column : columns) {
                sqlBuilder.append(column).append(", ");
            }
            sqlBuilder.setLength(sqlBuilder.length() - 2);
            sqlBuilder.append(") VALUES (");
            for (int i = 0; i < values.length; i++) {
                sqlBuilder.append("?, ");
            }
            sqlBuilder.setLength(sqlBuilder.length() - 2);
            sqlBuilder.append(")");

            try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
                for (int i = 0; i < values.length; i++) {
                    pstmt.setObject(i + 1, values[i]);
                }
                pstmt.executeUpdate();
                conn.commit();
            }
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        }
    }

    public ResultSet queryWithParams(String query, Object... params) throws SQLException {
        try {
            if (query == null || query.isEmpty()) {
                throw new IllegalArgumentException("查询语句不能为空");
            }

            PreparedStatement pstmt = conn.prepareStatement(query);
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            return pstmt.executeQuery();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        }
    }
    
    public void close() throws SQLException {
        if (conn != null) {
            try {
                if (!conn.getAutoCommit()) {
                    conn.commit();
                }
                conn.close();
            } finally {
                conn = null;
            }
        }
    }
}
