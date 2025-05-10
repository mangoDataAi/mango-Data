package com.mango.test.database.service.impl.datasource.connection;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * ResultSet代理类，用于非JDBC数据源查询结果
 * 通过继承ResultSetBase类简化实现
 */
public class ResultSetProxy extends ResultSetBase {
    private final List<Map<String, Object>> data;
    private final String[] columnNames;
    private int currentRowIndex = -1;
    private Map<String, Object> currentRow;
    private boolean closed = false;

    public ResultSetProxy(List<Map<String, Object>> data) {
        this.data = data;
        if (data != null && !data.isEmpty()) {
            Map<String, Object> firstRow = data.get(0);
            this.columnNames = firstRow.keySet().toArray(new String[0]);
        } else {
            this.columnNames = new String[0];
        }
    }

    @Override
    public boolean next() throws SQLException {
        if (data == null || data.isEmpty() || currentRowIndex >= data.size() - 1) {
            currentRow = null;
            return false;
        }

        currentRowIndex++;
        currentRow = data.get(currentRowIndex);
        return true;
    }

    @Override
    public void close() throws SQLException {
        closed = true;
        currentRow = null;
    }

    @Override
    public boolean wasNull() throws SQLException {
        return false;
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        checkClosed();
        checkRowAvailable();

        if (columnIndex < 1 || columnIndex > columnNames.length) {
            throw new SQLException("列索引超出范围: " + columnIndex);
        }

        String columnName = columnNames[columnIndex - 1];
        Object value = currentRow.get(columnName);
        return value != null ? value.toString() : null;
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value) || "1".equals(value) || "Y".equalsIgnoreCase(value) || "T".equalsIgnoreCase(value);
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        if (value == null) {
            return 0;
        }
        try {
            return Byte.parseByte(value);
        } catch (NumberFormatException e) {
            throw new SQLException("不能将值转换为byte: " + value);
        }
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        if (value == null) {
            return 0;
        }
        try {
            return Short.parseShort(value);
        } catch (NumberFormatException e) {
            throw new SQLException("不能将值转换为short: " + value);
        }
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new SQLException("不能将值转换为int: " + value);
        }
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new SQLException("不能将值转换为long: " + value);
        }
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        if (value == null) {
            return 0.0f;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new SQLException("不能将值转换为float: " + value);
        }
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new SQLException("不能将值转换为double: " + value);
        }
    }

    @Override
    @Deprecated
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        String value = getString(columnIndex);
        if (value == null) {
            return null;
        }
        try {
            BigDecimal bd = new BigDecimal(value);
            return bd.setScale(scale, BigDecimal.ROUND_HALF_UP);
        } catch (NumberFormatException e) {
            throw new SQLException("不能将值转换为BigDecimal: " + value);
        }
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        throw new SQLException("不能将值转换为byte[]: " + value);
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof java.util.Date) {
            return new Date(((java.util.Date) value).getTime());
        }
        try {
            return Date.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            throw new SQLException("不能将值转换为Date: " + value);
        }
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        if (value == null) {
            return null;
        }
        if (value instanceof Time) {
            return (Time) value;
        }
        if (value instanceof java.util.Date) {
            return new Time(((java.util.Date) value).getTime());
        }
        try {
            return Time.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            throw new SQLException("不能将值转换为Time: " + value);
        }
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        Object value = getObject(columnIndex);
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }
        if (value instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) value).getTime());
        }
        try {
            return Timestamp.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            throw new SQLException("不能将值转换为Timestamp: " + value);
        }
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持流操作");
    }

    @Override
    @Deprecated
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持流操作");
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持流操作");
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        checkClosed();
        checkRowAvailable();

        Object value = currentRow.get(columnLabel);
        return value != null ? value.toString() : null;
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        String value = getString(columnLabel);
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value) || "1".equals(value) || "Y".equalsIgnoreCase(value) || "T".equalsIgnoreCase(value);
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        String value = getString(columnLabel);
        if (value == null) {
            return 0;
        }
        try {
            return Byte.parseByte(value);
        } catch (NumberFormatException e) {
            throw new SQLException("不能将值转换为byte: " + value);
        }
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        String value = getString(columnLabel);
        if (value == null) {
            return 0;
        }
        try {
            return Short.parseShort(value);
        } catch (NumberFormatException e) {
            throw new SQLException("不能将值转换为short: " + value);
        }
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        String value = getString(columnLabel);
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new SQLException("不能将值转换为int: " + value);
        }
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        String value = getString(columnLabel);
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new SQLException("不能将值转换为long: " + value);
        }
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        String value = getString(columnLabel);
        if (value == null) {
            return 0.0f;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new SQLException("不能将值转换为float: " + value);
        }
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        String value = getString(columnLabel);
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new SQLException("不能将值转换为double: " + value);
        }
    }

    @Override
    @Deprecated
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        String value = getString(columnLabel);
        if (value == null) {
            return null;
        }
        try {
            BigDecimal bd = new BigDecimal(value);
            return bd.setScale(scale, BigDecimal.ROUND_HALF_UP);
        } catch (NumberFormatException e) {
            throw new SQLException("不能将值转换为BigDecimal: " + value);
        }
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        if (value == null) {
            return null;
        }
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        throw new SQLException("不能将值转换为byte[]: " + value);
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof java.util.Date) {
            return new Date(((java.util.Date) value).getTime());
        }
        try {
            return Date.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            throw new SQLException("不能将值转换为Date: " + value);
        }
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        if (value == null) {
            return null;
        }
        if (value instanceof Time) {
            return (Time) value;
        }
        if (value instanceof java.util.Date) {
            return new Time(((java.util.Date) value).getTime());
        }
        try {
            return Time.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            throw new SQLException("不能将值转换为Time: " + value);
        }
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        Object value = getObject(columnLabel);
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp) {
            return (Timestamp) value;
        }
        if (value instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) value).getTime());
        }
        try {
            return Timestamp.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            throw new SQLException("不能将值转换为Timestamp: " + value);
        }
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持流操作");
    }

    @Override
    @Deprecated
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持流操作");
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持流操作");
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
    public String getCursorName() throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持此操作");
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        // 这里应该实现一个ResultSetMetaData代理，但为简化，返回null
        throw new SQLFeatureNotSupportedException("不支持元数据操作");
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        checkClosed();
        checkRowAvailable();

        if (columnIndex < 1 || columnIndex > columnNames.length) {
            throw new SQLException("列索引超出范围: " + columnIndex);
        }

        String columnName = columnNames[columnIndex - 1];
        return currentRow.get(columnName);
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        checkClosed();
        checkRowAvailable();

        return currentRow.get(columnLabel);
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        for (int i = 0; i < columnNames.length; i++) {
            if (columnNames[i].equals(columnLabel)) {
                return i + 1;
            }
        }
        throw new SQLException("找不到列: " + columnLabel);
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持流操作");
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持流操作");
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        String value = getString(columnIndex);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new SQLException("不能将值转换为BigDecimal: " + value);
        }
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        String value = getString(columnLabel);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new SQLException("不能将值转换为BigDecimal: " + value);
        }
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        return currentRowIndex < 0 && data != null && !data.isEmpty();
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        return data != null && !data.isEmpty() && currentRowIndex >= data.size();
    }

    @Override
    public boolean isFirst() throws SQLException {
        return currentRowIndex == 0;
    }

    @Override
    public boolean isLast() throws SQLException {
        return data != null && !data.isEmpty() && currentRowIndex == data.size() - 1;
    }

    @Override
    public void beforeFirst() throws SQLException {
        currentRowIndex = -1;
        currentRow = null;
    }

    @Override
    public void afterLast() throws SQLException {
        if (data != null) {
            currentRowIndex = data.size();
            currentRow = null;
        }
    }

    @Override
    public boolean first() throws SQLException {
        if (data == null || data.isEmpty()) {
            return false;
        }

        currentRowIndex = 0;
        currentRow = data.get(0);
        return true;
    }

    @Override
    public boolean last() throws SQLException {
        if (data == null || data.isEmpty()) {
            return false;
        }

        currentRowIndex = data.size() - 1;
        currentRow = data.get(currentRowIndex);
        return true;
    }

    @Override
    public int getRow() throws SQLException {
        return currentRowIndex + 1;
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        if (data == null || data.isEmpty()) {
            return false;
        }

        if (row == 0) {
            beforeFirst();
            return false;
        }

        int targetRow;
        if (row > 0) {
            // 正向索引
            targetRow = row - 1;
        } else {
            // 负向索引（从末尾计算）
            targetRow = data.size() + row;
        }

        if (targetRow < 0) {
            beforeFirst();
            return false;
        } else if (targetRow >= data.size()) {
            afterLast();
            return false;
        }

        currentRowIndex = targetRow;
        currentRow = data.get(currentRowIndex);
        return true;
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        if (data == null || data.isEmpty()) {
            return false;
        }

        int targetRow = currentRowIndex + rows;

        if (targetRow < 0) {
            beforeFirst();
            return false;
        } else if (targetRow >= data.size()) {
            afterLast();
            return false;
        }

        currentRowIndex = targetRow;
        currentRow = data.get(currentRowIndex);
        return true;
    }

    @Override
    public boolean previous() throws SQLException {
        if (data == null || data.isEmpty() || currentRowIndex <= 0) {
            beforeFirst();
            return false;
        }

        currentRowIndex--;
        currentRow = data.get(currentRowIndex);
        return true;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        if (direction != ResultSet.FETCH_FORWARD) {
            throw new SQLException("仅支持FETCH_FORWARD方向");
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
    public int getType() throws SQLException {
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public int getConcurrency() throws SQLException {
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        return false;
    }

    @Override
    public boolean rowInserted() throws SQLException {
        return false;
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        return false;
    }

    @Override
    public void updateNull(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    private void checkClosed() throws SQLException {
        if (closed) {
            throw new SQLException("ResultSet已关闭");
        }
    }

    private void checkRowAvailable() throws SQLException {
        if (currentRow == null) {
            throw new SQLException("没有可用的当前行");
        }
    }

    // 为了简化代码，省略了所有更新操作的方法实现，全部抛出不支持异常

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    // ...其他所有更新方法类似...

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持此操作");
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持此操作");
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持此操作");
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持此操作");
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        return getString(columnIndex);
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        return getString(columnLabel);
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持此操作");
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持此操作");
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        Object value = getObject(columnIndex);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        throw new SQLException("不能将值转换为请求的类型: " + type.getName());
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        Object value = getObject(columnLabel);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        throw new SQLException("不能将值转换为请求的类型: " + type.getName());
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("不能将ResultSet转换为 " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this);
    }

    // 添加getHoldability方法实现
    @Override
    public int getHoldability() throws SQLException {
        return ResultSet.HOLD_CURSORS_OVER_COMMIT;
    }

    // 添加缺失的updateRowId方法
    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }

    // 省略了许多更新方法，为了简化代码展示，实际应该实现所有接口方法

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        throw new SQLFeatureNotSupportedException("不支持更新操作");
    }
} 