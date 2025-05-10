package com.mango.test.config.mybatis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义JSON类型处理器，用于处理Map<String, Object>类型与数据库TEXT类型的转换
 */
@MappedTypes({Map.class})
public class JsonTypeHandler extends BaseTypeHandler<Map<String, Object>> {
    
    private static final Logger logger = LoggerFactory.getLogger(JsonTypeHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType) throws SQLException {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(parameter);
            ps.setString(i, json);
        } catch (JsonProcessingException e) {
            logger.error("Error converting Map to JSON string", e);
            throw new SQLException("Error converting Map to JSON string", e);
        }
    }
    
    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }
    
    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJson(json);
    }
    
    @Override
    public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJson(json);
    }
    
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> result = OBJECT_MAPPER.readValue(json, Map.class);
            return result;
        } catch (JsonProcessingException e) {
            logger.error("Error parsing JSON string to Map", e);
            return new HashMap<>();
        }
    }
} 