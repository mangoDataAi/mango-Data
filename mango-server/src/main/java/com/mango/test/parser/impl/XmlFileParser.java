package com.mango.test.parser.impl;

import com.mango.test.database.entity.FileDataSource;
import com.mango.test.database.entity.FileField;
import com.mango.test.database.model.FileParseResult;
import com.mango.test.parser.FileParser;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.springframework.stereotype.Component;
import org.dom4j.DocumentException;

import java.io.*;
import java.util.*;

@Component
public class XmlFileParser implements FileParser {

    @Override
    public List<FileField> parseHeader(InputStream inputStream) {
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(inputStream);
            Element root = document.getRootElement();
            Element fields = root.element("fields");

            if (fields == null) {
                throw new RuntimeException("XML文件格式错误：缺少fields节点");
            }

            List<FileField> fieldList = new ArrayList<>();
            int orderNum = 0;
            for (Element field : fields.elements("field")) {
                String fieldName = field.attributeValue("name");
                if (fieldName != null && !fieldName.trim().isEmpty()) {
                    FileField fileField = new FileField();
                    fileField.setFieldName(fieldName);
                    fileField.setOrderNum(orderNum++);
                    fieldList.add(fileField);
                }
            }
            return fieldList;
        } catch (DocumentException e) {
            throw new RuntimeException("解析XML表头失败", e);
        }
    }

    @Override
    public FileParseResult parseTemplate(InputStream inputStream) {
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(inputStream);
            Element root = document.getRootElement();

            FileParseResult result = new FileParseResult();

            // 解析表信息
            result.setTableName(root.attributeValue("name"));
            result.setTableComment(root.attributeValue("comment"));

            // 解析字段信息
            List<FileField> fields = new ArrayList<>();
            Element fieldsElement = root.element("fields");
            if (fieldsElement != null) {
                for (Element fieldElement : fieldsElement.elements("field")) {
                    FileField field = new FileField();
                    field.setFieldName(fieldElement.attributeValue("name"));
                    field.setFieldType(fieldElement.attributeValue("type"));
                    String length = fieldElement.attributeValue("length");
                    field.setFieldLength(length != null ? Integer.parseInt(length) : null);
                    field.setNullable(!"false".equals(fieldElement.attributeValue("nullable")));
                    field.setDescription(fieldElement.attributeValue("description"));
                    fields.add(field);
                }
            }
            result.setFields(fields);

            return result;
        } catch (DocumentException e) {
            throw new RuntimeException("解析XML模板失败", e);
        }
    }

    @Override
    public List<FileField> parseFields(InputStream inputStream, FileDataSource dataSource) {
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(inputStream);
            Element root = document.getRootElement();

            // 获取第一个数据节点
            Element firstRow = root.element("row");
            if (firstRow == null) {
                throw new RuntimeException("XML文件格式错误：没有找到数据行");
            }

            List<FileField> fields = new ArrayList<>();
            int orderNum = 0;

            // 遍历第一行的所有元素作为字段
            for (Element element : firstRow.elements()) {
                FileField field = new FileField();
                field.setSourceId(dataSource.getId());
                field.setFieldName(element.getName());
                field.setFieldType("VARCHAR");
                field.setFieldLength(255);
                field.setNullable(true);
                field.setOrderNum(orderNum++);
                fields.add(field);
            }

            return fields;
        } catch (Exception e) {
            throw new RuntimeException("解析XML文件失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> parseData(InputStream inputStream, List<FileField> fields) {
        try {
            SAXReader reader = new SAXReader();
            Document document = reader.read(inputStream);
            Element root = document.getRootElement();

            List<Map<String, Object>> dataList = new ArrayList<>();

            // 遍历所有行
            for (Element row : root.elements("row")) {
                Map<String, Object> data = new HashMap<>();
                for (FileField field : fields) {
                    Element element = row.element(field.getFieldName());
                    data.put(field.getFieldName(), element != null ? element.getText() : null);
                }
                dataList.add(data);
            }

            return dataList;
        } catch (Exception e) {
            throw new RuntimeException("解析XML数据失败", e);
        }
    }

    @Override
    public byte[] generateTemplate(List<FileField> fields) {
        try {
            Document document = DocumentHelper.createDocument();
            Element root = document.addElement("data");
            Element row = root.addElement("row");

            // 添加字段
            for (FileField field : fields) {
                row.addElement(field.getFieldName());
            }

            // 格式化输出
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            XMLWriter writer = new XMLWriter(out, format);
            writer.write(document);

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("生成XML模板失败", e);
        }
    }

    @Override
    public String getFileType() {
        return "xml";
    }
}
