package com.mango.test.parser.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mango.test.database.entity.FileDataSource;
import com.mango.test.database.entity.FileField;
import com.mango.test.database.model.FileParseResult;
import com.mango.test.parser.FileParser;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
public class JsonFileParser implements FileParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<FileField> parseHeader(InputStream inputStream) {
        try {
            JsonNode root = objectMapper.readTree(inputStream);
            if (!root.has("fields")) {
                throw new RuntimeException("JSON文件格式错误：缺少fields字段");
            }

            List<FileField> fields = new ArrayList<>();
            JsonNode fieldsNode = root.get("fields");
            if (fieldsNode.isArray()) {
                int orderNum = 0;
                for (JsonNode fieldNode : fieldsNode) {
                    if (fieldNode.has("fieldName")) {
                        FileField field = new FileField();
                        field.setFieldName(fieldNode.get("fieldName").asText());
                        field.setOrderNum(orderNum++);
                        fields.add(field);
                    }
                }
            }
            return fields;
        } catch (IOException e) {
            throw new RuntimeException("解析JSON表头失败", e);
        }
    }

    @Override
    public FileParseResult parseTemplate(InputStream inputStream) {
        try {
            JsonNode root = objectMapper.readTree(inputStream);
            FileParseResult result = new FileParseResult();

            // 解析表信息
            result.setTableName(root.path("tableName").asText());
            result.setTableComment(root.path("tableComment").asText());

            // 解析字段信息
            List<FileField> fields = new ArrayList<>();
            JsonNode fieldsNode = root.path("fields");
            if (fieldsNode.isArray()) {
                for (JsonNode fieldNode : fieldsNode) {
                    FileField field = new FileField();
                    field.setFieldName(fieldNode.path("fieldName").asText());
                    field.setFieldType(fieldNode.path("fieldType").asText());
                    field.setFieldLength(fieldNode.has("fieldLength") ? fieldNode.get("fieldLength").asInt() : null);
                    field.setNullable(!fieldNode.path("required").asBoolean(false));
                    field.setDescription(fieldNode.path("description").asText());
                    fields.add(field);
                }
            }
            result.setFields(fields);

            return result;
        } catch (IOException e) {
            throw new RuntimeException("解析JSON模板失败", e);
        }
    }

    @Override
    public List<FileField> parseFields(InputStream inputStream, FileDataSource dataSource) {
        try {
            JsonNode root = objectMapper.readTree(inputStream);
            if (!root.isArray() || root.size() == 0) {
                throw new RuntimeException("JSON文件格式错误：需要是对象数组格式");
            }

            // 使用第一个对象来获取字段信息
            JsonNode firstObject = root.get(0);
            List<FileField> fields = new ArrayList<>();
            Iterator<String> fieldNames = firstObject.fieldNames();
            int orderNum = 0;

            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                FileField field = new FileField();
                field.setSourceId(dataSource.getId());
                field.setFieldName(fieldName);
                field.setFieldType("VARCHAR");
                field.setFieldLength(255);
                field.setNullable(true);
                field.setOrderNum(orderNum++);
                fields.add(field);
            }

            return fields;
        } catch (IOException e) {
            throw new RuntimeException("解析JSON文件失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> parseData(InputStream inputStream, List<FileField> fields) {
        try {
            JsonNode root = objectMapper.readTree(inputStream);
            List<Map<String, Object>> dataList = new ArrayList<>();

            for (JsonNode node : root) {
                Map<String, Object> data = new HashMap<>();
                for (FileField field : fields) {
                    JsonNode value = node.get(field.getFieldName());
                    data.put(field.getFieldName(), value != null ? value.asText() : null);
                }
                dataList.add(data);
            }

            return dataList;
        } catch (IOException e) {
            throw new RuntimeException("解析JSON数据失败", e);
        }
    }

    @Override
    public byte[] generateTemplate(List<FileField> fields) {
        try {
            ArrayNode template = objectMapper.createArrayNode();
            ObjectNode sampleObject = template.addObject();

            // 添加示例字段
            for (FileField field : fields) {
                sampleObject.put(field.getFieldName(), "");
            }

            return objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(template);
        } catch (IOException e) {
            throw new RuntimeException("生成JSON模板失败", e);
        }
    }

    @Override
    public String getFileType() {
        return "json";
    }
}
