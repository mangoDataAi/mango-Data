package com.mango.test.database.service.impl;

import com.mango.test.database.entity.DataSource;
import com.mango.test.database.mapper.DataSourceMapper;
import com.mango.test.database.service.FileSystemService;
import com.mango.test.database.service.impl.datasource.AbstractFileSystemHandler;
import com.mango.test.database.service.impl.datasource.DatabaseHandlerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件系统服务实现类
 */
@Slf4j
@Service
public class FileSystemServiceImpl implements FileSystemService {

    @Autowired
    private DataSourceMapper dataSourceMapper;

    /**
     * 获取文件系统处理器
     */
    private AbstractFileSystemHandler getFileSystemHandler(String datasourceId) throws Exception {
        DataSource dataSource = dataSourceMapper.selectById(datasourceId);
        if (dataSource == null) {
            throw new Exception("数据源不存在: " + datasourceId);
        }
        return DatabaseHandlerFactory.getFileSystemHandler(dataSource);
    }

    @Override
    public List<Map<String, Object>> listFiles(String datasourceId, String path, String filter) throws Exception {
        AbstractFileSystemHandler handler = getFileSystemHandler(datasourceId);
        try {
            return handler.listFiles(path, filter);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public Map<String, Object> getFileInfo(String datasourceId, String path) throws Exception {
        AbstractFileSystemHandler handler = getFileSystemHandler(datasourceId);
        try {
            return handler.getFileInfo(path);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public String getFileContent(String datasourceId, String path, String encoding) throws Exception {
        AbstractFileSystemHandler handler = getFileSystemHandler(datasourceId);
        try {
            return handler.readTextFile(path);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public void downloadFile(String datasourceId, String path, HttpServletResponse response) throws Exception {
        AbstractFileSystemHandler handler = getFileSystemHandler(datasourceId);
        try {
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            fileName = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
            
            byte[] fileContent = handler.readFile(path);
            if (fileContent != null) {
                response.getOutputStream().write(fileContent);
                response.getOutputStream().flush();
            }
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public Map<String, Object> uploadFile(String datasourceId, String path, MultipartFile file, boolean overwrite) throws Exception {
        AbstractFileSystemHandler handler = getFileSystemHandler(datasourceId);
        try {
            // 清理路径，移除可能存在的逗号和重复路径
            String cleanPath = path;
            if (cleanPath != null) {
                cleanPath = cleanPath.replace(",", "");
                
                // 处理重复路径问题
                // 检测是否存在路径重复，如 /opt/test//opt/test
                if (cleanPath.contains("//")) {
                    cleanPath = cleanPath.replaceAll("/+", "/");
                }
                
                // 如果路径出现两次相同的部分，只保留一次
                String[] pathParts = cleanPath.split("/");
                StringBuilder normalizedPath = new StringBuilder();
                for (int i = 0; i < pathParts.length; i++) {
                    if (pathParts[i].isEmpty()) continue;
                    // 检查这部分是否已经在路径中存在了
                    boolean isDuplicate = false;
                    for (int j = i + 1; j < pathParts.length; j++) {
                        if (pathParts[i].equals(pathParts[j])) {
                            isDuplicate = true;
                            break;
                        }
                    }
                    if (!isDuplicate) {
                        normalizedPath.append("/").append(pathParts[i]);
                    }
                }
                
                // 确保路径以/开头
                cleanPath = normalizedPath.toString();
                if (cleanPath.isEmpty()) {
                    cleanPath = "/";
                }
            }
            
            String targetPath = cleanPath;
            if (cleanPath.endsWith("/")) {
                targetPath = cleanPath + file.getOriginalFilename();
            } else {
                targetPath = cleanPath + "/" + file.getOriginalFilename();
            }
            
            log.info("上传文件到路径(处理后): {}, 文件名: {}", targetPath, file.getOriginalFilename());
            
            if (!overwrite && handler.exists(targetPath)) {
                throw new Exception("文件已存在，请使用覆盖选项或更改文件名");
            }
            
            boolean success = handler.uploadFile(file.getInputStream(), targetPath);
            if (!success) {
                throw new Exception("文件上传失败");
            }
            
            return handler.getFileInfo(targetPath);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean deleteFile(String datasourceId, String path, boolean recursive) throws Exception {
        AbstractFileSystemHandler handler = getFileSystemHandler(datasourceId);
        try {
            if (handler.isDirectory(path)) {
                return handler.deleteDirectory(path);
            } else {
                return handler.deleteFile(path);
            }
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public Map<String, Object> createDirectory(String datasourceId, String path) throws Exception {
        AbstractFileSystemHandler handler = getFileSystemHandler(datasourceId);
        try {
            boolean success = handler.createDirectory(path);
            if (!success) {
                throw new Exception("创建目录失败");
            }
            return handler.getFileInfo(path);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean copyFile(String datasourceId, String sourcePath, String destinationPath, boolean overwrite) throws Exception {
        AbstractFileSystemHandler handler = getFileSystemHandler(datasourceId);
        try {
            if (!handler.exists(sourcePath)) {
                throw new Exception("源文件不存在");
            }
            
            if (!overwrite && handler.exists(destinationPath)) {
                throw new Exception("目标文件已存在，请使用覆盖选项或更改目标路径");
            }
            
            return handler.copyFile(sourcePath, destinationPath);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean moveFile(String datasourceId, String sourcePath, String destinationPath, boolean overwrite) throws Exception {
        AbstractFileSystemHandler handler = getFileSystemHandler(datasourceId);
        try {
            if (!handler.exists(sourcePath)) {
                throw new Exception("源文件不存在");
            }
            
            if (!overwrite && handler.exists(destinationPath)) {
                throw new Exception("目标文件已存在，请使用覆盖选项或更改目标路径");
            }
            
            return handler.moveFile(sourcePath, destinationPath);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean renameFile(String datasourceId, String path, String newName) throws Exception {
        AbstractFileSystemHandler handler = getFileSystemHandler(datasourceId);
        try {
            if (!handler.exists(path)) {
                throw new Exception("文件不存在");
            }
            
            String parent = handler.getParent(path);
            String newPath = parent + (parent.endsWith("/") ? "" : "/") + newName;
            
            if (handler.exists(newPath)) {
                throw new Exception("已存在同名文件或目录");
            }
            
            return handler.renameFile(path, newPath);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public List<Map<String, Object>> searchFiles(String datasourceId, String basePath, String query, boolean recursive, String fileType) throws Exception {
        AbstractFileSystemHandler handler = getFileSystemHandler(datasourceId);
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            if (basePath == null || basePath.isEmpty()) {
                basePath = "/";
            }
            
            // 递归搜索文件
            searchFilesRecursive(handler, basePath, query, recursive, fileType, result);
            
            return result;
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }
    
    /**
     * 递归搜索文件
     * 
     * @param handler 文件系统处理器
     * @param path 当前路径
     * @param query 搜索关键词
     * @param recursive 是否递归
     * @param fileType 文件类型过滤
     * @param result 结果集合
     * @throws Exception 异常
     */
    private void searchFilesRecursive(AbstractFileSystemHandler handler, String path, String query, boolean recursive, String fileType, List<Map<String, Object>> result) throws Exception {
        List<Map<String, Object>> files = handler.listFiles(path);
        
        for (Map<String, Object> file : files) {
            String fileName = (String) file.get("name");
            String filePath = path.endsWith("/") ? path + fileName : path + "/" + fileName;
            boolean isDirectory = handler.isDirectory(filePath);
            
            // 检查是否匹配搜索条件
            if (query == null || query.isEmpty() || fileName.toLowerCase().contains(query.toLowerCase())) {
                // 检查文件类型
                if (fileType == null || fileType.isEmpty() || 
                    (isDirectory && "directory".equals(fileType)) || 
                    (!isDirectory && (fileType.equals("file") || fileName.toLowerCase().endsWith("." + fileType.toLowerCase())))) {
                    result.add(file);
                }
            }
            
            // 如果是目录且递归标志为true，则递归处理
            if (isDirectory && recursive) {
                searchFilesRecursive(handler, filePath, query, recursive, fileType, result);
            }
        }
    }

    @Override
    public Map<String, Object> getFileSystemStats(String datasourceId) throws Exception {
        AbstractFileSystemHandler handler = getFileSystemHandler(datasourceId);
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalSpace", handler.getTotalSpace());
            stats.put("usedSpace", handler.getUsedSpace());
            stats.put("availableSpace", handler.getAvailableSpace());
            // 可以添加更多统计信息
            return stats;
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean saveFileContent(String datasourceId, String path, String content, String encoding) throws Exception {
        AbstractFileSystemHandler handler = getFileSystemHandler(datasourceId);
        try {
            return handler.uploadTextFile(path, content, encoding);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }
} 