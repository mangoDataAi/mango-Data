package com.mango.test.parser.impl;

import com.mango.test.database.entity.FileDataSource;
import com.mango.test.database.entity.FileField;
import com.mango.test.database.model.FileParseResult;
import com.mango.test.parser.FileParser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

@Component
public class ExcelFileParser implements FileParser {

    @Override
    public List<FileField> parseHeader(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new RuntimeException("Excel文件格式错误：没有表头");
            }

            List<FileField> fields = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                String fieldName = getCellValue(headerRow.getCell(i));
                if (fieldName != null && !fieldName.trim().isEmpty()) {
                    FileField field = new FileField();
                    field.setFieldName(fieldName);
                    field.setOrderNum(i);
                    fields.add(field);
                }
            }
            return fields;
        } catch (IOException e) {
            throw new RuntimeException("解析Excel表头失败", e);
        }
    }

    @Override
    public List<FileField> parseFields(InputStream inputStream, FileDataSource dataSource) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            if (headerRow == null) {
                throw new RuntimeException("Excel文件格式错误：没有表头");
            }

            List<FileField> fields = new ArrayList<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell == null) continue;

                FileField field = new FileField();
                field.setSourceId(dataSource.getId());
                field.setFieldName(cell.getStringCellValue());
                field.setFieldType("VARCHAR");
                field.setFieldLength(255);
                field.setNullable(true);
                field.setOrderNum(i);
                fields.add(field);
            }

            return fields;
        } catch (IOException e) {
            throw new RuntimeException("解析Excel文件失败", e);
        }
    }


    @Override
    public List<Map<String, Object>> parseData(InputStream inputStream, List<FileField> fields) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            List<Map<String, Object>> dataList = new ArrayList<>();

            // 从第二行开始读取数据（跳过表头）
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, Object> data = new HashMap<>();
                for (int j = 0; j < fields.size(); j++) {
                    Cell cell = row.getCell(j);
                    String value = getCellValue(cell);
                    data.put(fields.get(j).getFieldName(), value);
                }
                dataList.add(data);
            }

            return dataList;
        } catch (IOException e) {
            throw new RuntimeException("解析Excel数据失败", e);
        }
    }

    @Override
    public byte[] generateTemplate(List<FileField> fields) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);

            // 创建表头
            for (int i = 0; i < fields.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(fields.get(i).getFieldName());
            }

            // 写入到字节数组
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        } catch (IOException e) {
            throw new RuntimeException("生成Excel模板失败", e);
        }
    }

    @Override
    public String getFileType() {
        return "xlsx";
    }

    @Override
    public FileParseResult parseTemplate(InputStream inputStream) {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            FileParseResult result = new FileParseResult();

            // 读取"字段定义"sheet
            Sheet fieldSheet = workbook.getSheet("字段定义");
            if (fieldSheet == null) {
                throw new RuntimeException("Excel模板格式错误：找不到字段定义sheet");
            }

            // 读取表信息（第1行）
            Row tableInfoRow = fieldSheet.getRow(0);
            if (tableInfoRow != null) {
                String tableName = getCellValue(tableInfoRow.getCell(1));  // B1单元格
                String tableComment = getCellValue(tableInfoRow.getCell(3));  // D1单元格
                
                // 检查表名是否填写
                if (tableName == null || tableName.trim().isEmpty()) {
                    throw new RuntimeException("请填写表名");
                }
                result.setTableName(tableName.trim());
                result.setTableComment(tableComment != null ? tableComment.trim() : "");
            }

            // 读取字段信息（从第4行开始，第3行是表头）
            List<FileField> fields = new ArrayList<>();
            int startRow = 3;  // 从第4行开始读取字段定义
            Row row;
            while ((row = fieldSheet.getRow(startRow)) != null) {
                String fieldName = getCellValue(row.getCell(0));  // 字段名称
                if (fieldName == null || fieldName.trim().isEmpty()) {
                    break;  // 遇到空行停止
                }

                String fieldType = getCellValue(row.getCell(1));  // 字段类型
                if (fieldType == null || fieldType.trim().isEmpty()) {
                    throw new RuntimeException("字段[" + fieldName + "]的类型不能为空");
                }

                FileField field = new FileField();
                field.setFieldName(fieldName.trim());
                field.setFieldType(fieldType.trim().toUpperCase());
                
                // 字段长度
                String lengthStr = getCellValue(row.getCell(2));
                if (lengthStr != null && !lengthStr.trim().isEmpty()) {
                    try {
                        field.setFieldLength(Integer.parseInt(lengthStr.trim()));
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("字段[" + fieldName + "]的长度格式不正确");
                    }
                }
                
                // 是否必填
                String required = getCellValue(row.getCell(3));
                field.setNullable(!"是".equals(required));
                
                // 字段说明
                String description = getCellValue(row.getCell(4));
                field.setDescription(description != null ? description.trim() : "");
                
                fields.add(field);
                startRow++;
            }

            if (fields.isEmpty()) {
                throw new RuntimeException("请至少定义一个字段");
            }
            result.setFields(fields);

            // 读取"数据内容"sheet中的示例数据
            Sheet dataSheet = workbook.getSheet("数据内容");
            if (dataSheet != null) {
                List<Map<String, Object>> data = new ArrayList<>();
                // 从第3行开始读取数据（跳过表头和字段名行）
                for (int i = 2; i <= dataSheet.getLastRowNum(); i++) {
                    Row dataRow = dataSheet.getRow(i);
                    if (dataRow == null) continue;

                    Map<String, Object> rowData = new HashMap<>();
                    for (int j = 0; j < fields.size(); j++) {
                        String value = getCellValue(dataRow.getCell(j));
                        if (value != null) {
                            rowData.put(fields.get(j).getFieldName(), value.trim());
                        }
                    }
                    if (!rowData.isEmpty()) {
                        data.add(rowData);
                    }
                }
                result.setData(data);
            }

            return result;
        } catch (IOException e) {
            throw new RuntimeException("解析Excel模板失败", e);
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                }
                // 避免数值被转为科学计数法
                return String.valueOf((long)cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    private Integer getIntValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int)cell.getNumericCellValue();
        }
        String value = getCellValue(cell);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
