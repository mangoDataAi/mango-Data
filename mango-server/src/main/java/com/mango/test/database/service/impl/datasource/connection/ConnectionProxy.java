package com.mango.test.database.service.impl.datasource.connection;

import com.mango.test.database.service.impl.datasource.AbstractDataSourceHandler;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * 连接代理类，为不同类型的数据源提供统一的Connection接口
 * 即使是不支持标准JDBC连接的数据源也能返回此对象
 */
public class ConnectionProxy implements Connection {
    // 原生客户端对象
    private final Object nativeClient;
    // 对应的数据源处理器
    private final AbstractDataSourceHandler handler;
    // 是否已关闭
    private boolean closed = false;

    public ConnectionProxy(Object nativeClient, AbstractDataSourceHandler handler) {
        this.nativeClient = nativeClient;
        this.handler = handler;
    }

    /**
     * 获取原生客户端对象
     * @param <T> 客户端类型
     * @return 原生客户端
     */
    @SuppressWarnings("unchecked")
    public <T> T getNativeClient() {
        return (T) nativeClient;
    }

    /**
     * 获取关联的数据源处理器
     * @return 数据源处理器
     */
    public AbstractDataSourceHandler getHandler() {
        return handler;
    }

    @Override
    public Statement createStatement() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).createStatement();
        }
        return new StatementProxy(this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).prepareStatement(sql);
        }
        return new PreparedStatementProxy(this, sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).prepareCall(sql);
        }
        throw new SQLFeatureNotSupportedException("不支持存储过程调用");
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).nativeSQL(sql);
        }
        return sql;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (nativeClient instanceof Connection) {
            ((Connection) nativeClient).setAutoCommit(autoCommit);
        }
        // 非JDBC数据源忽略此操作
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).getAutoCommit();
        }
        return true;
    }

    @Override
    public void commit() throws SQLException {
        if (nativeClient instanceof Connection) {
            ((Connection) nativeClient).commit();
        }
        // 非JDBC数据源忽略此操作
    }

    @Override
    public void rollback() throws SQLException {
        if (nativeClient instanceof Connection) {
            ((Connection) nativeClient).rollback();
        }
        // 非JDBC数据源忽略此操作
    }

    @Override
    public void close() throws SQLException {
        if (!closed) {
            try {
                if (nativeClient instanceof Connection) {
                    ((Connection) nativeClient).close();
                } else if (nativeClient instanceof AutoCloseable) {
                    ((AutoCloseable) nativeClient).close();
                }
                // 调用处理器的关闭方法
                handler.closeClient(nativeClient);
            } catch (Exception e) {
                throw new SQLException("关闭连接失败", e);
            } finally {
                closed = true;
            }
        }
    }

    @Override
    public boolean isClosed() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).isClosed();
        }
        return closed;
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).getMetaData();
        }
        throw new SQLFeatureNotSupportedException("此数据源类型不支持DatabaseMetaData");
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        if (nativeClient instanceof Connection) {
            ((Connection) nativeClient).setReadOnly(readOnly);
        }
        // 非JDBC数据源忽略此操作
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).isReadOnly();
        }
        return false;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        if (nativeClient instanceof Connection) {
            ((Connection) nativeClient).setCatalog(catalog);
        }
        // 非JDBC数据源忽略此操作
    }

    @Override
    public String getCatalog() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).getCatalog();
        }
        return null;
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        if (nativeClient instanceof Connection) {
            ((Connection) nativeClient).setTransactionIsolation(level);
        }
        // 非JDBC数据源忽略此操作
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).getTransactionIsolation();
        }
        return Connection.TRANSACTION_NONE;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).getWarnings();
        }
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {
        if (nativeClient instanceof Connection) {
            ((Connection) nativeClient).clearWarnings();
        }
        // 非JDBC数据源忽略此操作
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).createStatement(resultSetType, resultSetConcurrency);
        }
        throw new SQLFeatureNotSupportedException("不支持此功能");
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).prepareStatement(sql, resultSetType, resultSetConcurrency);
        }
        throw new SQLFeatureNotSupportedException("不支持此功能");
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).prepareCall(sql, resultSetType, resultSetConcurrency);
        }
        throw new SQLFeatureNotSupportedException("不支持此功能");
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).getTypeMap();
        }
        throw new SQLFeatureNotSupportedException("不支持此功能");
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        if (nativeClient instanceof Connection) {
            ((Connection) nativeClient).setTypeMap(map);
        }
        throw new SQLFeatureNotSupportedException("不支持此功能");
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        if (nativeClient instanceof Connection) {
            ((Connection) nativeClient).setHoldability(holdability);
        }
        // 非JDBC数据源忽略此操作
    }

    @Override
    public int getHoldability() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).getHoldability();
        }
        return ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).setSavepoint();
        }
        throw new SQLFeatureNotSupportedException("不支持保存点");
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).setSavepoint(name);
        }
        throw new SQLFeatureNotSupportedException("不支持保存点");
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        if (nativeClient instanceof Connection) {
            ((Connection) nativeClient).rollback(savepoint);
        }
        throw new SQLFeatureNotSupportedException("不支持保存点");
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        if (nativeClient instanceof Connection) {
            ((Connection) nativeClient).releaseSavepoint(savepoint);
        }
        throw new SQLFeatureNotSupportedException("不支持保存点");
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
        }
        throw new SQLFeatureNotSupportedException("不支持此功能");
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }
        throw new SQLFeatureNotSupportedException("不支持此功能");
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }
        throw new SQLFeatureNotSupportedException("不支持此功能");
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).prepareStatement(sql, autoGeneratedKeys);
        }
        throw new SQLFeatureNotSupportedException("不支持自动生成键");
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).prepareStatement(sql, columnIndexes);
        }
        throw new SQLFeatureNotSupportedException("不支持自动生成键");
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).prepareStatement(sql, columnNames);
        }
        throw new SQLFeatureNotSupportedException("不支持自动生成键");
    }

    @Override
    public Clob createClob() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).createClob();
        }
        throw new SQLFeatureNotSupportedException("不支持此功能");
    }

    @Override
    public Blob createBlob() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).createBlob();
        }
        throw new SQLFeatureNotSupportedException("不支持此功能");
    }

    @Override
    public NClob createNClob() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).createNClob();
        }
        throw new SQLFeatureNotSupportedException("不支持此功能");
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).createSQLXML();
        }
        throw new SQLFeatureNotSupportedException("不支持此功能");
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).isValid(timeout);
        }
        return !closed;
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        if (nativeClient instanceof Connection) {
            try {
                ((Connection) nativeClient).setClientInfo(name, value);
            } catch (SQLClientInfoException e) {
                throw e;
            } catch (SQLException e) {
                SQLClientInfoException exception = new SQLClientInfoException();
                exception.initCause(e);
                throw exception;
            }
        }
        // 非JDBC数据源忽略此操作
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        if (nativeClient instanceof Connection) {
            try {
                ((Connection) nativeClient).setClientInfo(properties);
            } catch (SQLClientInfoException e) {
                throw e;
            } catch (SQLException e) {
                SQLClientInfoException exception = new SQLClientInfoException();
                exception.initCause(e);
                throw exception;
            }
        }
        // 非JDBC数据源忽略此操作
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).getClientInfo(name);
        }
        return null;
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).getClientInfo();
        }
        return new Properties();
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).createArrayOf(typeName, elements);
        }
        throw new SQLFeatureNotSupportedException("不支持此功能");
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).createStruct(typeName, attributes);
        }
        throw new SQLFeatureNotSupportedException("不支持此功能");
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        if (nativeClient instanceof Connection) {
            ((Connection) nativeClient).setSchema(schema);
        }
        // 非JDBC数据源忽略此操作
    }

    @Override
    public String getSchema() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).getSchema();
        }
        return null;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        if (nativeClient instanceof Connection) {
            ((Connection) nativeClient).abort(executor);
        } else {
            close();
        }
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        if (nativeClient instanceof Connection) {
            ((Connection) nativeClient).setNetworkTimeout(executor, milliseconds);
        }
        // 非JDBC数据源忽略此操作
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).getNetworkTimeout();
        }
        return 0;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).unwrap(iface);
        }
        throw new SQLException("无法转换为 " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return true;
        }
        if (nativeClient instanceof Connection) {
            return ((Connection) nativeClient).isWrapperFor(iface);
        }
        return false;
    }
} 