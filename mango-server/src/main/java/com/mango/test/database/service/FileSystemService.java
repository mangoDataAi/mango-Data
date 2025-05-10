package com.mango.test.database.service;

import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * 文件系统服务接口
 */
public interface FileSystemService {

    /**
     * 获取文件列表
     */
    List<Map<String, Object>> listFiles(String datasourceId, String path, String filter) throws Exception;

    /**
     * 获取文件信息
     */
    Map<String, Object> getFileInfo(String datasourceId, String path) throws Exception;

    /**
     * 获取文件内容
     */
    String getFileContent(String datasourceId, String path, String encoding) throws Exception;

    /**
     * 下载文件
     */
    void downloadFile(String datasourceId, String path, HttpServletResponse response) throws Exception;

    /**
     * 上传文件
     */
    Map<String, Object> uploadFile(String datasourceId, String path, MultipartFile file, boolean overwrite) throws Exception;

    /**
     * 删除文件
     */
    boolean deleteFile(String datasourceId, String path, boolean recursive) throws Exception;

    /**
     * 创建目录
     */
    Map<String, Object> createDirectory(String datasourceId, String path) throws Exception;

    /**
     * 复制文件
     */
    boolean copyFile(String datasourceId, String sourcePath, String destinationPath, boolean overwrite) throws Exception;

    /**
     * 移动文件
     */
    boolean moveFile(String datasourceId, String sourcePath, String destinationPath, boolean overwrite) throws Exception;

    /**
     * 重命名文件
     */
    boolean renameFile(String datasourceId, String path, String newName) throws Exception;

    /**
     * 搜索文件
     */
    List<Map<String, Object>> searchFiles(String datasourceId, String basePath, String query, boolean recursive, String fileType) throws Exception;

    /**
     * 获取文件系统统计信息
     */
    Map<String, Object> getFileSystemStats(String datasourceId) throws Exception;

    /**
     * 保存文件内容
     */
    boolean saveFileContent(String datasourceId, String path, String content, String encoding) throws Exception;
} 