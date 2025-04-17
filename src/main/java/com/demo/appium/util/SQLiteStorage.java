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

/**
 * SQLite存储工具类，用于操作SQLite数据库
 */
public class SQLiteStorage {

    private Connection conn;
    // 用于验证表名和列名的正则表达式，支持中文、字母、数字和下划线
    private static final Pattern VALID_IDENTIFIER = Pattern.compile("^[\\u4e00-\\u9fa5a-zA-Z_][\\u4e00-\\u9fa5a-zA-Z0-9_]*$");
    
    /**
     * 构造函数，初始化数据库连接
     * @param dbFile 数据库文件路径
     * @throws SQLException 如果连接数据库失败
     */
    public SQLiteStorage(String dbFile) throws SQLException {
        this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        this.conn.setAutoCommit(false); // 关闭自动提交，启用事务
    }
    
    /**
     * 列类型枚举，支持TEXT、INTEGER和REAL三种类型
     */
    public enum ColumnType {
        TEXT,
        INTEGER,
        REAL,
        DEFAULT_TIMESTAMP
    }

    /**
     * 创建数据表
     * @param tableName 表名
     * @param columnDefinitions 列定义数组
     * @param primaryKey 主键列名
     * @throws SQLException 如果创建表失败
     */
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

                if (columnType == ColumnType.DEFAULT_TIMESTAMP)
                    sqlBuilder.append(parts[0].trim()).append(" ").append("TEXT DEFAULT (strftime('%Y-%m-%d', 'now'))").append(", ");
                else
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

            System.out.println(sqlBuilder.toString());

            try (PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
                pstmt.execute();
                conn.commit();
            }
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        }
    }

    /**
     * 删除数据表
     * @param tableName 要删除的表名
     * @throws SQLException 如果删除表失败
     */
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

    /**
     * 批量保存CSV文件到数据库
     * @param tableName 目标表名
     * @param columnDefinition 列定义数组
     * @param directoryPath CSV文件所在目录
     * @param excludeString 要排除的文件名包含的字符串
     * @param skipFirstTitleLine 是否跳过第一行标题
     */
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
    
    /**
     * 存储单条数据到数据库
     * @param tableName 目标表名
     * @param columns 列名数组
     * @param values 值数组
     * @throws SQLException 如果插入数据失败
     */
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

    /**
     * 带参数的查询
     * @param query SQL查询语句
     * @param params 查询参数
     * @return 查询结果集
     * @throws SQLException 如果查询失败
     */
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
