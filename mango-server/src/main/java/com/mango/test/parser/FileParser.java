package com.mango.test.parser;

import com.mango.test.database.entity.FileDataSource;
import com.mango.test.database.entity.FileField;
import com.mango.test.database.model.FileParseResult;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface FileParser {

    List<FileField> parseFields(InputStream inputStream, FileDataSource dataSource);

    /**
     * 解析文件头部获取字段信息
     */
    List<FileField> parseHeader(InputStream inputStream);

    /**
     * 解析文件数据
     */
    List<Map<String, Object>> parseData(InputStream inputStream, List<FileField> fields);

    /**
     * 生成模板文件
     */
    byte[] generateTemplate(List<FileField> fields);

    /**
     * 解析模板文件
     */
    FileParseResult parseTemplate(InputStream inputStream);

    /**
     * 获取支持的文件类型
     */
    String getFileType();
}
