package com.mango.test.parser.impl;

import com.mango.test.database.entity.FileDataSource;
import com.mango.test.database.entity.FileField;
import com.mango.test.database.model.FileParseResult;
import com.mango.test.parser.FileParser;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SqlFileParser implements FileParser {

    private boolean isNotNullColumn(ColumnDefinition column) {
        List<String> specs = column.getColumnSpecs();
        if (specs == null) return false;
        return specs.stream()
                .anyMatch(spec -> spec.toUpperCase().contains("NOT NULL"));
    }

    @Override
    public List<FileField> parseFields(InputStream inputStream, FileDataSource dataSource) {
        try {
            String sqlContent = new BufferedReader(new InputStreamReader(inputStream))
                    .lines()
                    .collect(Collectors.joining("\n"));

            Statement statement = CCJSqlParserUtil.parse(sqlContent);
            if (!(statement instanceof CreateTable)) {
                throw new RuntimeException("SQL文件必须是CREATE TABLE语句");
            }

            CreateTable createTable = (CreateTable) statement;
            List<FileField> fields = new ArrayList<>();
            int orderNum = 0;

            for (ColumnDefinition column : createTable.getColumnDefinitions()) {
                FileField field = new FileField();
                field.setSourceId(dataSource.getId());
                field.setFieldName(column.getColumnName());
                field.setFieldType(column.getColDataType().getDataType().toUpperCase());

                // 解析字段长度
                if (column.getColDataType().getArgumentsStringList() != null &&
                    !column.getColDataType().getArgumentsStringList().isEmpty()) {
                    field.setFieldLength(Integer.parseInt(
                        column.getColDataType().getArgumentsStringList().get(0)));
                }

                field.setNullable(!isNotNullColumn(column));
                field.setOrderNum(orderNum++);
                fields.add(field);
            }

            return fields;
        } catch (Exception e) {
            throw new RuntimeException("解析SQL文件失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> parseData(InputStream inputStream, List<FileField> fields) {
        // SQL文件不支持数据解析，返回空列表
        return new ArrayList<>();
    }

    @Override
    public byte[] generateTemplate(List<FileField> fields) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE table_name (\n");

        for (int i = 0; i < fields.size(); i++) {
            FileField field = fields.get(i);
            sql.append("    ")
               .append(field.getFieldName())
               .append(" ")
               .append(field.getFieldType());

            if (field.getFieldLength() != null) {
                sql.append("(").append(field.getFieldLength()).append(")");
            }

            if (!field.getNullable()) {
                sql.append(" NOT NULL");
            }

            if (i < fields.size() - 1) {
                sql.append(",");
            }
            sql.append("\n");
        }

        sql.append(");");
        return sql.toString().getBytes();
    }

    @Override
    public String getFileType() {
        return "sql";
    }

    @Override
    public List<FileField> parseHeader(InputStream inputStream) {
        try {
            String sql = readInputStream(inputStream);
            List<FileField> fields = new ArrayList<>();

            // 解析CREATE TABLE语句中的字段
            Pattern createTablePattern = Pattern.compile(
                "CREATE TABLE [^(]+ \\(([^)]+)\\)");
            Matcher matcher = createTablePattern.matcher(sql);

            if (matcher.find()) {
                String[] fieldDefinitions = matcher.group(1).split(",");
                int orderNum = 0;
                for (String fieldDef : fieldDefinitions) {
                    Pattern fieldPattern = Pattern.compile(
                        "\"([^\"]+)\"\\s+([^\\s]+)");
                    Matcher fieldMatcher = fieldPattern.matcher(fieldDef.trim());

                    if (fieldMatcher.find()) {
                        FileField field = new FileField();
                        field.setFieldName(fieldMatcher.group(1));
                        field.setOrderNum(orderNum++);
                        fields.add(field);
                    }
                }
            }
            return fields;
        } catch (IOException e) {
            throw new RuntimeException("解析SQL表头失败", e);
        }
    }

    @Override
    public FileParseResult parseTemplate(InputStream inputStream) {
        try {
            String sql = readInputStream(inputStream);
            FileParseResult result = new FileParseResult();

            // 解析表名和注释
            Pattern tablePattern = Pattern.compile(
                "CREATE TABLE \"([^\"]+)\".*COMMENT\\s+'([^']+)'");
            Matcher tableMatcher = tablePattern.matcher(sql);

            if (tableMatcher.find()) {
                result.setTableName(tableMatcher.group(1));
                result.setTableComment(tableMatcher.group(2));
            }

            // 解析字段
            List<FileField> fields = new ArrayList<>();
            Pattern fieldPattern = Pattern.compile(
                "\"([^\"]+)\"\\s+([^\\s]+)(\\s+NOT NULL)?.*COMMENT\\s+'([^']+)'");
            Matcher fieldMatcher = fieldPattern.matcher(sql);

            while (fieldMatcher.find()) {
                FileField field = new FileField();
                field.setFieldName(fieldMatcher.group(1));
                field.setFieldType(fieldMatcher.group(2));
                field.setNullable(fieldMatcher.group(3) == null);
                field.setDescription(fieldMatcher.group(4));
                fields.add(field);
            }

            result.setFields(fields);
            return result;
        } catch (IOException e) {
            throw new RuntimeException("解析SQL模板失败", e);
        }
    }

    /**
     * 读取输入流内容
     */
    private String readInputStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[1024];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                content.append(buffer, 0, length);
            }
            return content.toString();
        }
    }
}
