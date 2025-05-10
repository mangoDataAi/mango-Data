package com.mango.test.database.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.mango.test.database.model.excel.ExcelTemplateSheet;
import com.mango.test.database.service.FileTemplateService;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FileTemplateServiceImpl implements FileTemplateService {

    /**
     * 生成模板文件
     */
    public byte[] generateTemplate(String fileType) throws Exception {
        String type = fileType.toLowerCase();
        if ("csv".equals(type)) {
            return generateCsvTemplate();
        } else if ("xlsx".equals(type)) {
            return generateExcelTemplate();
        } else if ("json".equals(type)) {
            return generateJsonTemplate();
        } else if ("xml".equals(type)) {
            return generateXmlTemplate();
        } else if ("txt".equals(type)) {
            return generateTxtTemplate();
        } else if ("dmp".equals(type)) {
            return generateDmpTemplate();
        } else if ("sql".equals(type)) {
            return generateSqlTemplate();
        } else {
            throw new IllegalArgumentException("不支持的文件类型");
        }
    }

    /**
     * 生成CSV模板
     */
    private byte[] generateCsvTemplate() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // 显式指定使用UTF-8编码，并添加BOM标记
        byte[] bom = { (byte)0xEF, (byte)0xBB, (byte)0xBF };
        out.write(bom);
        
        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            // 写入表信息
            writer.writeNext(new String[]{"表名", "", "表注释", ""});
            writer.writeNext(new String[]{});  // 空行
            
            // 写入字段定义表头
            writer.writeNext(new String[]{"字段名称", "字段类型", "字段长度", "是否必填", "字段说明", "示例值"});
            
            // 写入示例字段
            writer.writeNext(new String[]{"id", "INTEGER", "", "是", "主键ID", "1"});
            writer.writeNext(new String[]{"name", "VARCHAR", "50", "是", "名称", "测试名称"});
            writer.writeNext(new String[]{"age", "INTEGER", "", "否", "年龄", "18"});
            writer.writeNext(new String[]{"birthday", "DATE", "", "否", "出生日期", "2000-01-01"});
            writer.writeNext(new String[]{"salary", "DECIMAL", "10", "否", "薪资", "8000.00"});
            
            // 空行分隔
            writer.writeNext(new String[]{});
            
            // 写入使用说明
            writer.writeNext(new String[]{"使用说明"});
            writer.writeNext(new String[]{"1. 请在上方填写表名和表注释"});
            writer.writeNext(new String[]{"2. 在字段定义区域定义表的字段结构"});
            writer.writeNext(new String[]{"3. 字段类型支持：VARCHAR、INTEGER、DECIMAL、DATETIME等"});
            writer.writeNext(new String[]{"4. 字段名称不能重复，必填字段不能为空"});
        }
        return out.toByteArray();
    }

    /**
     * 生成Excel模板
     */
    private byte[] generateExcelTemplate() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            // 创建样式
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle contentStyle = createContentStyle(workbook);

            // 创建"使用说明"sheet
            createInstructionSheet(workbook, headerStyle, contentStyle);

            // 创建"字段定义"sheet
            createFieldDefinitionSheet(workbook, headerStyle, contentStyle);

            // 创建"数据内容"sheet
            createDataSheet(workbook);

            // 输出
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createInstructionSheet(Workbook workbook, CellStyle headerStyle, CellStyle contentStyle) {
        Sheet sheet = workbook.createSheet("使用说明");
        sheet.setColumnWidth(0, 20 * 256);
        sheet.setColumnWidth(1, 60 * 256);

        // 添加标题
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("使用说明");
        titleCell.setCellStyle(headerStyle);

        // 添加说明内容
        int rowNum = 2;
        addInstructionRow(sheet, rowNum++, "1. 基本信息", "请在字段定义sheet中填写表名和表注释", contentStyle);
        addInstructionRow(sheet, rowNum++, "2. 字段定义", "在字段定义sheet中定义表的字段结构", contentStyle);
        addInstructionRow(sheet, rowNum++, "3. 数据填写", "在数据内容sheet中填写实际数据", contentStyle);
        addInstructionRow(sheet, rowNum++, "4. 字段类型", "支持的字段类型：VARCHAR、INTEGER、DECIMAL、DATETIME等", contentStyle);
        addInstructionRow(sheet, rowNum++, "5. 注意事项", "字段名称不能重复，必填字段不能为空", contentStyle);
    }

    private void createFieldDefinitionSheet(Workbook workbook, CellStyle headerStyle, CellStyle contentStyle) {
        Sheet sheet = workbook.createSheet("字段定义");

        // 设置列宽
        sheet.setColumnWidth(0, 15 * 256);  // 字段名称
        sheet.setColumnWidth(1, 15 * 256);  // 字段类型
        sheet.setColumnWidth(2, 12 * 256);  // 字段长度
        sheet.setColumnWidth(3, 12 * 256);  // 是否必填
        sheet.setColumnWidth(4, 30 * 256);  // 字段说明
        sheet.setColumnWidth(5, 20 * 256);  // 示例值

        // 添加表信息
        Row tableRow = sheet.createRow(0);
        createHeaderCell(tableRow, 0, "表名", headerStyle);
        createCell(tableRow, 1, "", contentStyle);
        createHeaderCell(tableRow, 2, "表注释", headerStyle);
        createCell(tableRow, 3, "", contentStyle);

        // 添加字段表头
        Row headerRow = sheet.createRow(2);
        String[] headers = {"字段名称", "字段类型", "字段长度", "是否必填", "字段说明", "示例值"};
        for (int i = 0; i < headers.length; i++) {
            createHeaderCell(headerRow, i, headers[i], headerStyle);
        }

        // 添加示例数据
        addExampleField(sheet, 3, "id", "INTEGER", null, "是", "主键ID", "1", contentStyle);
        addExampleField(sheet, 4, "name", "VARCHAR", 50, "是", "名称", "测试名称", contentStyle);
        addExampleField(sheet, 5, "age", "INTEGER", null, "否", "年龄", "18", contentStyle);
        addExampleField(sheet, 6, "birthday", "DATE", null, "否", "出生日期", "2000-01-01", contentStyle);
        addExampleField(sheet, 7, "salary", "DECIMAL", 10, "否", "薪资", "8000.00", contentStyle);
    }

    private void createDataSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet("数据内容");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle contentStyle = createContentStyle(workbook);

        // 数据sheet的表头会在实际导入数据时根据字段定义动态生成
        Row headerRow = sheet.createRow(0);
        createHeaderCell(headerRow, 0, "请在字段定义sheet定义表结构后再填写数据", headerStyle);

        // 添加示例数据
        Row exampleRow = sheet.createRow(1);
        createCell(exampleRow, 0, "id", contentStyle);
        createCell(exampleRow, 1, "name", contentStyle);
        createCell(exampleRow, 2, "age", contentStyle);
        createCell(exampleRow, 3, "birthday", contentStyle);
        createCell(exampleRow, 4, "salary", contentStyle);

        Row dataRow1 = sheet.createRow(2);
        createCell(dataRow1, 0, "1", contentStyle);
        createCell(dataRow1, 1, "张三", contentStyle);
        createCell(dataRow1, 2, "25", contentStyle);
        createCell(dataRow1, 3, "1998-01-01", contentStyle);
        createCell(dataRow1, 4, "8000.00", contentStyle);

        Row dataRow2 = sheet.createRow(3);
        createCell(dataRow2, 0, "2", contentStyle);
        createCell(dataRow2, 1, "李四", contentStyle);
        createCell(dataRow2, 2, "30", contentStyle);
        createCell(dataRow2, 3, "1993-05-15", contentStyle);
        createCell(dataRow2, 4, "12000.00", contentStyle);

        // 调整列宽
        for (int i = 0; i < 5; i++) {
            sheet.setColumnWidth(i, 15 * 256);
        }
    }

    private void addExampleField(Sheet sheet, int rowNum, String name, String type,
            Integer length, String required, String desc, String example, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        createCell(row, 0, name, style);
        createCell(row, 1, type, style);
        createCell(row, 2, length == null ? "" : length.toString(), style);
        createCell(row, 3, required, style);
        createCell(row, 4, desc, style);
        createCell(row, 5, example, style);
    }

    private void addInstructionRow(Sheet sheet, int rowNum, String title, String content, CellStyle style) {
        Row row = sheet.createRow(rowNum);
        createCell(row, 0, title, style);
        createCell(row, 1, content, style);
    }

    private void createHeaderCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void createCell(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);

        return style;
    }

    private CellStyle createContentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * 生成JSON模板
     */
    private byte[] generateJsonTemplate() {
        JSONObject template = new JSONObject();
        template.put("tableName", "user_info");
        template.put("tableComment", "用户信息表");

        // 添加数据
        List<Map<String, Object>> data = new ArrayList<>();
        data.add(createDataRow("1", "张三", 25, "zhangsan@example.com", "2024-01-01 12:00:00"));
        data.add(createDataRow("2", "李四", 30, "lisi@example.com", "2024-01-02 13:00:00"));
        data.add(createDataRow("3", "王五", 28, "wangwu@example.com", "2024-01-03 14:00:00"));

        template.put("data", data);

        return JSON.toJSONString(template, SerializerFeature.PrettyFormat)
                  .getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 生成XML模板
     */
    private byte[] generateXmlTemplate() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        // 创建根元素
        Element root = doc.createElement("table");
        doc.appendChild(root);
        root.setAttribute("name", "user_info");
        root.setAttribute("comment", "用户信息表");

        // 添加数据
        Element data = doc.createElement("data");
        root.appendChild(data);

        addXmlDataRow(doc, data, "1", "张三", 25, "zhangsan@example.com", "2024-01-01 12:00:00");
        addXmlDataRow(doc, data, "2", "李四", 30, "lisi@example.com", "2024-01-02 13:00:00");
        addXmlDataRow(doc, data, "3", "王五", 28, "wangwu@example.com", "2024-01-03 14:00:00");

        // 转换为字节数组
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(out));

        return out.toByteArray();
    }

    /**
     * 生成TXT模板
     */
    private byte[] generateTxtTemplate() {
        StringBuilder sb = new StringBuilder();
        sb.append("# 表名: user_info\n");
        sb.append("# 表注释: 用户信息表\n");
        sb.append("# 字段: id|name|age|email|create_time\n");
        sb.append("# ----------------------------------------\n");
        sb.append("1|张三|25|zhangsan@example.com|2024-01-01 12:00:00\n");
        sb.append("2|李四|30|lisi@example.com|2024-01-02 13:00:00\n");
        sb.append("3|王五|28|wangwu@example.com|2024-01-03 14:00:00");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 生成DMP模板
     */
    private byte[] generateDmpTemplate() {
        StringBuilder sb = new StringBuilder();
        sb.append("-- 表结构定义\n");
        sb.append("CREATE TABLE \"user_info\" (\n");
        sb.append("  \"id\" NUMBER(20) PRIMARY KEY,\n");
        sb.append("  \"name\" VARCHAR2(50) NOT NULL,\n");
        sb.append("  \"age\" NUMBER(3),\n");
        sb.append("  \"email\" VARCHAR2(100),\n");
        sb.append("  \"create_time\" DATE DEFAULT SYSDATE\n");
        sb.append(");\n\n");

        sb.append("-- 表注释\n");
        sb.append("COMMENT ON TABLE \"user_info\" IS '用户信息表';\n\n");

        sb.append("-- 插入数据\n");
        sb.append("INSERT INTO \"user_info\" (\"id\", \"name\", \"age\", \"email\", \"create_time\") VALUES \n");
        sb.append("(1, '张三', 25, 'zhangsan@example.com', TO_DATE('2024-01-01 12:00:00', 'YYYY-MM-DD HH24:MI:SS'));\n");
        sb.append("INSERT INTO \"user_info\" (\"id\", \"name\", \"age\", \"email\", \"create_time\") VALUES \n");
        sb.append("(2, '李四', 30, 'lisi@example.com', TO_DATE('2024-01-02 13:00:00', 'YYYY-MM-DD HH24:MI:SS'));\n");
        sb.append("INSERT INTO \"user_info\" (\"id\", \"name\", \"age\", \"email\", \"create_time\") VALUES \n");
        sb.append("(3, '王五', 28, 'wangwu@example.com', TO_DATE('2024-01-03 14:00:00', 'YYYY-MM-DD HH24:MI:SS'));");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 生成SQL模板
     */
    private byte[] generateSqlTemplate() {
        StringBuilder sb = new StringBuilder();
        sb.append("-- 表结构定义\n");
        sb.append("CREATE TABLE `user_info` (\n");
        sb.append("  `id` BIGINT PRIMARY KEY,\n");
        sb.append("  `name` VARCHAR(50) NOT NULL COMMENT '姓名',\n");
        sb.append("  `age` INT COMMENT '年龄',\n");
        sb.append("  `email` VARCHAR(100) COMMENT '邮箱',\n");
        sb.append("  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'\n");
        sb.append(") COMMENT='用户信息表';\n\n");

        sb.append("-- 插入数据\n");
        sb.append("INSERT INTO `user_info` (`id`, `name`, `age`, `email`, `create_time`) VALUES\n");
        sb.append("(1, '张三', 25, 'zhangsan@example.com', '2024-01-01 12:00:00'),\n");
        sb.append("(2, '李四', 30, 'lisi@example.com', '2024-01-02 13:00:00'),\n");
        sb.append("(3, '王五', 28, 'wangwu@example.com', '2024-01-03 14:00:00');");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Map<String, Object> createDataRow(String id, String name, int age, String email, String createTime) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("name", name);
        row.put("age", age);
        row.put("email", email);
        row.put("create_time", createTime);
        return row;
    }

    private void addXmlDataRow(Document doc, Element parent, String id, String name,
                             int age, String email, String createTime) {
        Element row = doc.createElement("row");
        row.setAttribute("id", String.valueOf(id));
        row.setAttribute("name", name);
        row.setAttribute("age", String.valueOf(age));
        row.setAttribute("email", email);
        row.setAttribute("create_time", createTime);
        parent.appendChild(row);
    }

    private List<ExcelTemplateSheet> generateTemplateData() {
        List<ExcelTemplateSheet> list = new ArrayList<>();

        // 示例1：用户ID
        ExcelTemplateSheet field1 = new ExcelTemplateSheet();
        field1.setFieldName("user_id");
        field1.setFieldType("INTEGER");
        field1.setLength(11);
        field1.setRequired("是");
        field1.setDescription("用户唯一标识，自增主键");
        field1.setExample("1001");
        list.add(field1);

        // 示例2：用户名
        ExcelTemplateSheet field2 = new ExcelTemplateSheet();
        field2.setFieldName("username");
        field2.setFieldType("VARCHAR");
        field2.setLength(50);
        field2.setRequired("是");
        field2.setDescription("用户登录名，不可重复");
        field2.setExample("zhangsan");
        list.add(field2);

        // 示例3：年龄
        ExcelTemplateSheet field3 = new ExcelTemplateSheet();
        field3.setFieldName("age");
        field3.setFieldType("INTEGER");
        field3.setLength(3);
        field3.setRequired("否");
        field3.setDescription("用户年龄，范围0-150");
        field3.setExample("25");
        list.add(field3);

        // 示例4：工资
        ExcelTemplateSheet field4 = new ExcelTemplateSheet();
        field4.setFieldName("salary");
        field4.setFieldType("DECIMAL(10,2)");
        field4.setLength(10);
        field4.setRequired("否");
        field4.setDescription("月薪，最多保留2位小数");
        field4.setExample("8800.50");
        list.add(field4);

        // 示例5：邮箱
        ExcelTemplateSheet field5 = new ExcelTemplateSheet();
        field5.setFieldName("email");
        field5.setFieldType("VARCHAR");
        field5.setLength(100);
        field5.setRequired("否");
        field5.setDescription("电子邮箱地址");
        field5.setExample("zhangsan@example.com");
        list.add(field5);

        // 示例6：创建时间
        ExcelTemplateSheet field6 = new ExcelTemplateSheet();
        field6.setFieldName("create_time");
        field6.setFieldType("DATETIME");
        field6.setLength(null);
        field6.setRequired("是");
        field6.setDescription("记录创建时间，格式：yyyy-MM-dd HH:mm:ss");
        field6.setExample("2024-02-14 12:00:00");
        list.add(field6);

        return list;
    }
}
