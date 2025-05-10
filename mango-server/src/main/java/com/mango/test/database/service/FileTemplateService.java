package com.mango.test.database.service;

public interface FileTemplateService {
    /**
     * 生成模板文件
     * @param fileType 文件类型
     * @return 模板文件字节数组
     */
    byte[] generateTemplate(String fileType) throws Exception;
} 