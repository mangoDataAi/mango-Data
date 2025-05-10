package com.mango.test.database.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.test.database.entity.ColumnInfo;
import com.mango.test.database.service.DataSourceService;
import com.mango.test.database.service.ExportService;
import com.mango.test.vo.R;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class ExportServiceImpl implements ExportService {
    
    @Autowired
    private DataSourceService dataSourceService;
    
    private String currentTableName;
    private String currentDataSourceId;
    
    public void setTableInfo(String dataSourceId, String tableName) {
        this.currentDataSourceId = dataSourceId;
        this.currentTableName = tableName;
    }
    
    @Override
    public byte[] exportToCSV(List<Map<String, Object>> data) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (data.isEmpty()) return new byte[0];
            
            // 获取字段名和注释
            List<String> headers = new ArrayList<>();
            for (String field : data.get(0).keySet()) {
                String comment = getFieldComment(field);
                headers.add(comment != null ? field + "（" + comment + "）" : field);
            }
            
            CSVPrinter printer = new CSVPrinter(
                new OutputStreamWriter(out, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.withHeader(headers.toArray(new String[0]))
            );
            
            for (Map<String, Object> row : data) {
                printer.printRecord(row.values());
            }
            printer.flush();
            
            return out.toByteArray();
        } catch (IOException e) {
            log.error("导出CSV失败", e);
            throw new RuntimeException("导出CSV失败", e);
        }
    }
    
    @Override
    public byte[] exportToExcel(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return new byte[0];
        }

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            Sheet sheet = workbook.createSheet("Data");
            
            // 创建标题样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);  // 设置左对齐
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);  // 垂直居中
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setWrapText(true);  // 允许换行显示
            
            // 创建数据样式
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.LEFT);  // 数据也左对齐
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStyle.setWrapText(true);
            
            // 创建表头
            Row headerRow = sheet.createRow(0);
            List<String> fieldNames = new ArrayList<>(data.get(0).keySet());
            
            for (int i = 0; i < fieldNames.size(); i++) {
                Cell cell = headerRow.createCell(i);
                String fieldName = fieldNames.get(i);
                String comment = getFieldComment(fieldName);
                cell.setCellValue(comment != null ? fieldName + "（" + comment + "）" : fieldName);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }
            
            // 填充数据
            for (int i = 0; i < data.size(); i++) {
                Row row = sheet.createRow(i + 1);
                Map<String, Object> rowData = data.get(i);
                
                for (int j = 0; j < fieldNames.size(); j++) {
                    Cell cell = row.createCell(j);
                    Object value = rowData.get(fieldNames.get(j));
                    
                    if (value == null) {
                        cell.setCellValue("");
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value instanceof Boolean) {
                        cell.setCellValue((Boolean) value);
                    } else if (value instanceof Date) {
                        cell.setCellValue((Date) value);
                    } else {
                        cell.setCellValue(value.toString());
                    }
                    
                    cell.setCellStyle(dataStyle);
                }
            }
            
            // 再次调整列宽
            for (int i = 0; i < fieldNames.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("导出Excel失败", e);
            throw new RuntimeException("导出Excel失败", e);
        }
    }
    
    @Override
    public byte[] exportToJSON(List<Map<String, Object>> data) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsBytes(data);
        } catch (IOException e) {
            throw new RuntimeException("导出JSON失败", e);
        }
    }
    
    @Override
    public byte[] exportToXML(List<Map<String, Object>> data) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            
            Element rootElement = doc.createElement("data");
            doc.appendChild(rootElement);
            
            for (Map<String, Object> row : data) {
                Element rowElement = doc.createElement("row");
                rootElement.appendChild(rowElement);
                
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    Element field = doc.createElement(entry.getKey());
                    field.setTextContent(entry.getValue() != null ? entry.getValue().toString() : "");
                    rowElement.appendChild(field);
                }
            }
            
            // 设置XML输出格式
            javax.xml.transform.TransformerFactory transformerFactory = javax.xml.transform.TransformerFactory.newInstance();
            javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();
            
            // 设置输出属性
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            transformer.transform(
                new javax.xml.transform.dom.DOMSource(doc),
                new javax.xml.transform.stream.StreamResult(out)
            );
            
            return out.toByteArray();
        } catch (Exception e) {
            log.error("导出XML失败", e);
            throw new RuntimeException("导出XML失败", e);
        }
    }
    
    @Override
    public byte[] exportToSQL(List<Map<String, Object>> data, String tableName) {
        StringBuilder sql = new StringBuilder();
        if (data.isEmpty()) return new byte[0];
        
        // 生成INSERT语句
        for (Map<String, Object> row : data) {
            sql.append("INSERT INTO ").append(tableName).append(" (");
            sql.append(String.join(", ", row.keySet()));
            sql.append(") VALUES (");
            sql.append(row.values().stream()
                .map(this::formatSQLValue)
                .collect(java.util.stream.Collectors.joining(", ")));
            sql.append(");\n");
        }
        
        return sql.toString().getBytes(StandardCharsets.UTF_8);
    }
    
    private String formatSQLValue(Object value) {
        if (value == null) return "NULL";
        if (value instanceof Number) return value.toString();
        return "'" + value.toString().replace("'", "''") + "'";
    }
    
    @Override
    public byte[] exportToDMP(List<Map<String, Object>> data, String tableName) {
        // DMP格式可以根据具体需求定制，这里简单实现
        return exportToSQL(data, tableName);
    }
    
    @Override
    public byte[] exportToTXT(List<Map<String, Object>> data) {
        StringBuilder txt = new StringBuilder();
        if (data.isEmpty()) return new byte[0];
        
        // 添加表头
        txt.append(String.join("\t", data.get(0).keySet())).append("\n");
        
        // 添加数据
        for (Map<String, Object> row : data) {
            txt.append(row.values().stream()
                .map(v -> v != null ? v.toString() : "")
                .collect(java.util.stream.Collectors.joining("\t")))
                .append("\n");
        }
        
        return txt.toString().getBytes(StandardCharsets.UTF_8);
    }

    // 获取字段注释的辅助方法
    private String getFieldComment(String fieldName) {
        try {
            // 获取当前表的字段信息
            if (currentTableName != null) {
                String dataSourceId = String.valueOf(currentDataSourceId);
                // 处理 R 包装类的返回值
                R<List<ColumnInfo>> response = dataSourceService.getTableColumns(dataSourceId, currentTableName);
                if (response != null && response.getData() != null) {
                    List<ColumnInfo> columns = response.getData();
                    for (ColumnInfo column : columns) {
                        if (column.getName().equals(fieldName)) {
                            return column.getComment();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取字段注释失败: {}", fieldName, e);
        }
        return null;
    }

    @Override
    public void exportData(List<String> types, List<Map<String, Object>> data, HttpServletResponse response) throws Exception {
        if (types.size() == 1) {
            // 单一格式导出
            String type = types.get(0).toLowerCase();
            byte[] content;
            String contentType;
            String extension;
            
            switch (type) {
                case "csv":
                    content = exportToCSV(data);
                    contentType = "text/csv";
                    extension = "csv";
                    break;
                case "xlsx":
                case "excel":
                    content = exportToExcel(data);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    extension = "xlsx";
                    break;
                case "json":
                    content = exportToJSON(data);
                    contentType = "application/json";
                    extension = "json";
                    break;
                case "xml":
                    content = exportToXML(data);
                    contentType = "application/xml";
                    extension = "xml";
                    break;
                case "sql":
                    content = exportToSQL(data, currentTableName);
                    contentType = "text/plain";
                    extension = "sql";
                    break;
                case "dmp":
                    content = exportToDMP(data, currentTableName);
                    contentType = "application/octet-stream";
                    extension = "dmp";
                    break;
                case "txt":
                    content = exportToTXT(data);
                    contentType = "text/plain";
                    extension = "txt";
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported export type: " + type);
            }
            
            // 设置响应头
            response.setContentType(contentType);
            response.setHeader("Content-Disposition", "attachment; filename=export." + extension);
            
            // 写入响应
            try (OutputStream out = response.getOutputStream()) {
                out.write(content);
                out.flush();
            }
        } else {
            // 多格式导出为ZIP
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=export.zip");
            
            try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
                for (String type : types) {
                    byte[] content;
                    String extension;
                    
                    switch (type.toLowerCase()) {
                        case "csv":
                            content = exportToCSV(data);
                            extension = "csv";
                            break;
                        case "xlsx":
                        case "excel":
                            content = exportToExcel(data);
                            extension = "xlsx";
                            break;
                        case "json":
                            content = exportToJSON(data);
                            extension = "json";
                            break;
                        case "xml":
                            content = exportToXML(data);
                            extension = "xml";
                            break;
                        case "sql":
                            content = exportToSQL(data, currentTableName);
                            extension = "sql";
                            break;
                        case "dmp":
                            content = exportToDMP(data, currentTableName);
                            extension = "dmp";
                            break;
                        case "txt":
                            content = exportToTXT(data);
                            extension = "txt";
                            break;
                        default:
                            continue;
                    }
                    
                    ZipEntry entry = new ZipEntry("export." + extension);
                    zipOut.putNextEntry(entry);
                    zipOut.write(content);
                    zipOut.closeEntry();
                }
            }
        }
    }
} 