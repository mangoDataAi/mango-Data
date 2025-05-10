package com.mango.test.database.service.impl.datasource.connection;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Statement代理类，用于非JDBC数据源
 */
public class StatementProxy implements Statement {
    private final ConnectionProxy connection;
    private int maxRows = 0;
    private int queryTimeout = 0;
    private boolean closed = false;
    private ResultSet currentResultSet;
    
    public StatementProxy(ConnectionProxy connection) {
        this.connection = connection;
    }
    
    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        try {
            // 尝试通过处理器执行查询
            List<Map<String, Object>> results = connection.getHandler().executeQuery(sql);
            ResultSetProxy resultSet = new ResultSetProxy(results);
            this.currentResultSet = resultSet;
            return resultSet;
        } catch (Exception e) {
            throw new SQLException("执行查询失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public int executeUpdate(String sql) throws SQLException {
        try {
            // 尝试通过处理器执行更新
            return connection.getHandler().executeUpdate(sql);
        } catch (Exception e) {
            throw new SQLException("执行更新失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void close() throws SQLException {
        if (currentResultSet != null && !currentResultSet.isClosed()) {
            currentResultSet.close();
        }
        closed = true;
    }
    
    @Override
    public int getMaxFieldSize() throws SQLException {
        return 0;
    }
    
    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        // 忽略
    }
    
    @Override
    public int getMaxRows() throws SQLException {
        return maxRows;
    }
    
    @Override
    public void setMaxRows(int max) throws SQLException {
        this.maxRows = max;
    }
    
    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        // 忽略
    }
    
    @Override
    public int getQueryTimeout() throws SQLException {
        return queryTimeout;
    }
    
    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        this.queryTimeout = seconds;
    }
    
    @Override
    public void cancel() throws SQLException {
        // 无实现
    }
    
    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }
    
    @Override
    public void clearWarnings() throws SQLException {
        // 忽略
    }
    
    @Override
    public void setCursorName(String name) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持此功能");
    }
    
    @Override
    public boolean execute(String sql) throws SQLException {
        try {
            // 简单实现，尝试执行查询
            ResultSet rs = executeQuery(sql);
            return rs != null;
        } catch (SQLException e) {
            // 查询失败，尝试作为更新操作执行
            executeUpdate(sql);
            return false;
        }
    }
    
    @Override
    public ResultSet getResultSet() throws SQLException {
        return currentResultSet;
    }
    
    @Override
    public int getUpdateCount() throws SQLException {
        return -1;  // 默认无更新计数
    }
    
    @Override
    public boolean getMoreResults() throws SQLException {
        return false;
    }
    
    @Override
    public void setFetchDirection(int direction) throws SQLException {
        if (direction != ResultSet.FETCH_FORWARD) {
            throw new SQLFeatureNotSupportedException("仅支持FETCH_FORWARD");
        }
    }
    
    @Override
    public int getFetchDirection() throws SQLException {
        return ResultSet.FETCH_FORWARD;
    }
    
    @Override
    public void setFetchSize(int rows) throws SQLException {
        // 忽略
    }
    
    @Override
    public int getFetchSize() throws SQLException {
        return 0;
    }
    
    @Override
    public int getResultSetConcurrency() throws SQLException {
        return ResultSet.CONCUR_READ_ONLY;
    }
    
    @Override
    public int getResultSetType() throws SQLException {
        return ResultSet.TYPE_FORWARD_ONLY;
    }
    
    @Override
    public void addBatch(String sql) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持批处理");
    }
    
    @Override
    public void clearBatch() throws SQLException {
        // 忽略
    }
    
    @Override
    public int[] executeBatch() throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持批处理");
    }
    
    @Override
    public Connection getConnection() throws SQLException {
        return connection;
    }
    
    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return false;
    }
    
    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持自动生成的键");
    }
    
    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return executeUpdate(sql);
    }
    
    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return executeUpdate(sql);
    }
    
    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return executeUpdate(sql);
    }
    
    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return execute(sql);
    }
    
    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return execute(sql);
    }
    
    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return execute(sql);
    }
    
    @Override
    public int getResultSetHoldability() throws SQLException {
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }
    
    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }
    
    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        // 忽略
    }
    
    @Override
    public boolean isPoolable() throws SQLException {
        return false;
    }
    
    @Override
    public void closeOnCompletion() throws SQLException {
        // 忽略
    }
    
    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return false;
    }
    
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("无法转换为 " + iface.getName());
    }
    
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }
} 