package com.mango.test.parser.impl;

import com.mango.test.database.entity.FileDataSource;
import com.mango.test.database.entity.FileField;
import com.mango.test.database.model.FileParseResult;
import com.mango.test.parser.FileParser;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

@Component
public class CsvFileParser implements FileParser {

    @Override
    public List<FileField> parseHeader(InputStream inputStream) {
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] headers = reader.readNext();
            if (headers == null) {
                throw new RuntimeException("CSV文件格式错误：没有表头");
            }

            List<FileField> fields = new ArrayList<>();
            for (int i = 0; i < headers.length; i++) {
                if (headers[i] != null && !headers[i].trim().isEmpty()) {
                    FileField field = new FileField();
                    field.setFieldName(headers[i].trim());
                    field.setOrderNum(i);
                    fields.add(field);
                }
            }
            return fields;
        } catch (Exception e) {
            throw new RuntimeException("解析CSV表头失败", e);
        }
    }

    @Override
    public FileParseResult parseTemplate(InputStream inputStream) {
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            FileParseResult result = new FileParseResult();

            // 读取表信息(第一行)
            String[] tableInfo = reader.readNext();
            if (tableInfo != null && tableInfo.length >= 2) {
                result.setTableName(tableInfo[1]); // 表名在第二列
            }

            // 读取表注释(第二行)
            String[] tableComment = reader.readNext();
            if (tableComment != null && tableComment.length >= 2) {
                result.setTableComment(tableComment[1]); // 表注释在第二列
            }

            // 跳过空行
            reader.readNext();

            // 读取字段定义表头
            reader.readNext();

            // 读取字段定义
            List<FileField> fields = new ArrayList<>();
            String[] line;
            while ((line = reader.readNext()) != null && line.length >= 5) {
                if (line[0].trim().isEmpty()) {
                    break;
                }
                FileField field = new FileField();
                field.setFieldName(line[0].trim());
                field.setFieldType(line[1].trim());
                field.setFieldLength(parseInteger(line[2]));
                field.setNullable("否".equals(line[3].trim()));
                field.setDescription(line[4].trim());
                fields.add(field);
            }
            result.setFields(fields);

            return result;
        } catch (Exception e) {
            throw new RuntimeException("解析CSV模板失败", e);
        }
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<FileField> parseFields(InputStream inputStream, FileDataSource dataSource) {
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            String[] headers = reader.readNext();
            if (headers == null || headers.length == 0) {
                throw new RuntimeException("CSV文件格式错误：没有表头");
            }

            List<FileField> fields = new ArrayList<>();
            for (int i = 0; i < headers.length; i++) {
                FileField field = new FileField();
                field.setSourceId(dataSource.getId());
                field.setFieldName(headers[i]);
                field.setFieldType("VARCHAR"); // 默认类型
                field.setFieldLength(255);
                field.setNullable(true);
                field.setOrderNum(i);
                fields.add(field);
            }

            return fields;
        } catch (IOException | CsvValidationException e) {
            throw new RuntimeException("解析CSV文件失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> parseData(InputStream inputStream, List<FileField> fields) {
        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream))) {
            reader.readNext(); // 跳过表头

            List<Map<String, Object>> dataList = new ArrayList<>();
            String[] line;
            while ((line = reader.readNext()) != null) {
                Map<String, Object> data = new HashMap<>();
                for (int i = 0; i < Math.min(line.length, fields.size()); i++) {
                    data.put(fields.get(i).getFieldName(), line[i]);
                }
                dataList.add(data);
            }

            return dataList;
        } catch (IOException | CsvValidationException e) {
            throw new RuntimeException("解析CSV数据失败", e);
        }
    }

    @Override
    public byte[] generateTemplate(List<FileField> fields) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             CSVWriter writer = new CSVWriter(new OutputStreamWriter(out))) {

            String[] headers = fields.stream()
                    .map(FileField::getFieldName)
                    .toArray(String[]::new);

            writer.writeNext(headers);
            writer.flush();

            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("生成CSV模板失败", e);
        }
    }

    @Override
    public String getFileType() {
        return "csv";
    }
}
