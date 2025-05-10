package com.mango.test.database.service;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

public interface ExportService {
    /**
     * 导出数据
     */
    void exportData(List<String> types, List<Map<String, Object>> data, HttpServletResponse response) throws Exception;

    /**
     * 设置表信息
     */
    void setTableInfo(String dataSourceId, String tableName);
    
    byte[] exportToCSV(List<Map<String, Object>> data);
    byte[] exportToExcel(List<Map<String, Object>> data);
    byte[] exportToJSON(List<Map<String, Object>> data);
    byte[] exportToXML(List<Map<String, Object>> data);
    byte[] exportToSQL(List<Map<String, Object>> data, String tableName);
    byte[] exportToDMP(List<Map<String, Object>> data, String tableName);
    byte[] exportToTXT(List<Map<String, Object>> data);
} 