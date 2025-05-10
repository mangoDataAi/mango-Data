package com.mango.test.database.service.impl.datasource.handlers.file;

import com.jcraft.jsch.*;
import com.mango.test.database.entity.DataSource;
import com.mango.test.database.service.impl.datasource.AbstractFileSystemHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

/**
 * SFTP文件系统处理器
 */
@Slf4j
public class SftpHandler extends AbstractFileSystemHandler {
    
    private static final String SFTP_DRIVER = "com.jcraft.jsch.JSch";
    private static final String DEFAULT_PORT = "22";
    private static final int FILE_ALREADY_EXISTS = 4;
    private Session session;
    private ChannelSftp sftp;
    
    public SftpHandler(DataSource dataSource) {
        super(dataSource);
    }
    
    /**
     * 初始化SFTP连接
     */
    private synchronized boolean initConnection() {
        if (session != null && session.isConnected() && sftp != null && sftp.isConnected()) {
            return true;
        }
        
        try {
            log.info("初始化SFTP连接...");
            JSch jsch = new JSch();
            
            // 获取端口，默认为22
            int port = DEFAULT_PORT.equals("22") ? 22 : 22;
            if (dataSource.getPort() != null && !dataSource.getPort().isEmpty()) {
                try {
                    port = Integer.parseInt(dataSource.getPort());
                } catch (NumberFormatException e) {
                    log.error("无效的端口号: {}, 使用默认端口: 22", dataSource.getPort());
                }
            }
            
            log.info("连接到SFTP服务器: {}:{} 用户: {}", dataSource.getHost(), port, dataSource.getUsername());
            
            session = jsch.getSession(
                dataSource.getUsername(),
                dataSource.getHost(),
                port
            );
            
            // 设置密码
            session.setPassword(dataSource.getPassword());
            
            // 严格主机密钥检查
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            
            // 连接
            session.connect(30000);
            
            // 打开SFTP通道
            Channel channel = session.openChannel("sftp");
            channel.connect(30000);
            
            sftp = (ChannelSftp) channel;
            
            if (sftp == null || !sftp.isConnected()) {
                log.error("SFTP通道连接失败");
                closeConnection();
                return false;
            }
            
            log.info("SFTP连接成功");
            return true;
        } catch (Exception e) {
            log.error("初始化SFTP连接失败: {}", e.getMessage(), e);
            closeConnection();
            return false;
        }
    }
    
    /**
     * 关闭SFTP连接
     */
    private synchronized void closeConnection() {
        try {
            if (sftp != null && sftp.isConnected()) {
                try {
                    sftp.disconnect();
                    log.debug("SFTP通道已断开");
                } catch (Exception e) {
                    log.error("关闭SFTP通道失败: {}", e.getMessage(), e);
                } finally {
                    sftp = null;
                }
            }
            
            if (session != null && session.isConnected()) {
                try {
                    session.disconnect();
                    log.debug("SFTP会话已断开");
                } catch (Exception e) {
                    log.error("关闭SFTP会话失败: {}", e.getMessage(), e);
                } finally {
                    session = null;
                }
            }
        } catch (Exception e) {
            log.error("关闭SFTP连接时发生异常: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public boolean testConnection() {
        try {
            if (initConnection()) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("测试SFTP连接失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public List<Map<String, Object>> listFiles(String path) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            if (!initConnection()) {
                return result;
            }
            
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> files = sftp.ls(path);
            
            for (ChannelSftp.LsEntry entry : files) {
                // 跳过"."和".."
                if (".".equals(entry.getFilename()) || "..".equals(entry.getFilename())) {
                    continue;
                }
                
                SftpATTRS attrs = entry.getAttrs();
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("name", entry.getFilename());
                fileInfo.put("path", path + "/" + entry.getFilename());
                fileInfo.put("type", attrs.isDir() ? "directory" : "file");
                fileInfo.put("size", attrs.getSize());
                fileInfo.put("lastModified", attrs.getMTime() * 1000L);
                fileInfo.put("permissions", attrs.getPermissionsString());
                
                result.add(fileInfo);
            }
        } catch (Exception e) {
            log.error("获取SFTP文件列表失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        return result;
    }
    
    /**
     * 列出指定路径下的文件和目录
     * @param path 路径
     * @param pattern 文件名匹配模式
     * @return 文件和目录列表
     */
    @Override
    public List<Map<String, Object>> listFiles(String path, String pattern) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            if (!initConnection()) {
                return result;
            }
            
            String fullPath = getFullPath(path);
            
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> files = sftp.ls(fullPath);
            
            for (ChannelSftp.LsEntry entry : files) {
                // 跳过"."和".."
                if (".".equals(entry.getFilename()) || "..".equals(entry.getFilename())) {
                    continue;
                }
                
                // 如果有匹配模式，检查文件名是否匹配
                if (pattern != null && !pattern.isEmpty() && !entry.getFilename().contains(pattern)) {
                    continue;
                }
                
                SftpATTRS attrs = entry.getAttrs();
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("name", entry.getFilename());
                fileInfo.put("path", path + "/" + entry.getFilename());
                fileInfo.put("type", attrs.isDir() ? "directory" : "file");
                fileInfo.put("size", attrs.getSize());
                fileInfo.put("lastModified", attrs.getMTime() * 1000L);
                fileInfo.put("permissions", attrs.getPermissionsString());
                
                result.add(fileInfo);
            }
        } catch (Exception e) {
            log.error("获取SFTP文件列表失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        return result;
    }
    
    @Override
    public boolean createDirectory(String path) {
        try {
            if (path == null || path.trim().isEmpty()) {
                log.error("创建目录失败: 路径为空");
                return false;
            }
            
            if (!initConnection()) {
                log.error("创建目录失败: 无法连接到SFTP服务器");
                return false;
            }
            
            // 再次检查sftp对象是否为null
            if (sftp == null || !sftp.isConnected()) {
                log.error("创建目录失败: SFTP连接为空或未连接");
                return false;
            }
            
            String fullPath = getFullPath(path);
            log.info("开始创建目录: {}", fullPath);
            
            try {
                // 检查目录是否已存在
                try {
                    SftpATTRS attrs = sftp.stat(fullPath);
                    if (attrs != null && attrs.isDir()) {
                        log.info("目录已存在: {}", fullPath);
                        return true;
                    }
                } catch (SftpException e) {
                    if (e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                        throw e;
                    }
                    // 目录不存在，继续创建
                }
                
                // 递归创建目录层次结构
                String[] folders = fullPath.split("/");
                StringBuilder currentPath = new StringBuilder();
                
                for (String folder : folders) {
                    if (folder.isEmpty()) {
                        continue; // 跳过空目录名（通常是路径开头的/或连续的//）
                    }
                    
                    currentPath.append("/").append(folder);
                    
                    // 创建当前层级的目录
                    try {
                        sftp.stat(currentPath.toString());
                        log.debug("目录已存在，跳过创建: {}", currentPath);
                    } catch (SftpException e) {
                        if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                            // 目录不存在，创建它
                            try {
                                log.debug("创建目录: {}", currentPath);
                                sftp.mkdir(currentPath.toString());
                            } catch (SftpException ex) {
                                // 如果是因为已经存在而失败，则忽略错误
                                if (ex.id == FILE_ALREADY_EXISTS) {
                                    log.debug("目录已存在(并发创建): {}", currentPath);
                                } else {
                                    throw ex;
                                }
                            }
                        } else {
                            throw e;
                        }
                    }
                }
                
                log.info("目录创建成功: {}", fullPath);
                return true;
            } catch (SftpException e) {
                log.error("创建SFTP目录失败: {} (错误码: {})", e.getMessage(), e.id, e);
                return false;
            }
        } catch (Exception e) {
            log.error("创建SFTP目录失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public boolean deleteFile(String path) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            sftp.rm(path);
            return true;
        } catch (Exception e) {
            log.error("删除SFTP文件失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public boolean deleteDirectory(String path) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            // 删除目录下的所有文件和子目录
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> files = sftp.ls(path);
            
            for (ChannelSftp.LsEntry entry : files) {
                String filename = entry.getFilename();
                if (!".".equals(filename) && !"..".equals(filename)) {
                    String childPath = path + "/" + filename;
                    if (entry.getAttrs().isDir()) {
                        deleteDirectory(childPath);
                    } else {
                        sftp.rm(childPath);
                    }
                }
            }
            
            // 删除空目录
            sftp.rmdir(path);
            return true;
        } catch (Exception e) {
            log.error("删除SFTP目录失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public byte[] downloadFile(String path) {
        try {
            if (!initConnection()) {
                return null;
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            sftp.get(path, outputStream);
            
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("下载SFTP文件失败: {}", e.getMessage(), e);
            return null;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public boolean uploadFile(String path, byte[] content) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            // 检查父目录是否存在，不存在则创建
            int lastSeparator = path.lastIndexOf('/');
            if (lastSeparator > 0) {
                String parentDir = path.substring(0, lastSeparator);
                try {
                    sftp.stat(parentDir);
                } catch (SftpException e) {
                    if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                        createDirectory(parentDir);
                    }
                }
            }
            
            ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
            sftp.put(inputStream, path);
            inputStream.close();
            
            return true;
        } catch (Exception e) {
            log.error("上传SFTP文件失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public boolean renameFile(String oldPath, String newPath) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            sftp.rename(oldPath, newPath);
            return true;
        } catch (Exception e) {
            log.error("重命名SFTP文件失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public Map<String, Object> getFileInfo(String path) {
        try {
            if (!initConnection()) {
                return null;
            }
            
            SftpATTRS attrs = sftp.stat(path);
            if (attrs == null) {
                return null;
            }
            
            Map<String, Object> fileInfo = new HashMap<>();
            String filename = path.substring(path.lastIndexOf('/') + 1);
            fileInfo.put("name", filename);
            fileInfo.put("path", path);
            fileInfo.put("type", attrs.isDir() ? "directory" : "file");
            fileInfo.put("size", attrs.getSize());
            fileInfo.put("lastModified", attrs.getMTime() * 1000L);
            fileInfo.put("permissions", attrs.getPermissionsString());
            
            return fileInfo;
        } catch (Exception e) {
            log.error("获取SFTP文件信息失败: {}", e.getMessage(), e);
            return null;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public boolean exists(String path) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            try {
                sftp.stat(path);
                return true;
            } catch (SftpException e) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    return false;
                }
                throw e;
            }
        } catch (Exception e) {
            log.error("检查SFTP文件是否存在失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    @Override
    public String getDriverClassName() {
        return SFTP_DRIVER;
    }
    
    @Override
    public String getDefaultPort() {
        return DEFAULT_PORT;
    }
    
    /**
     * 获取文件的完整路径
     * @param relativePath 相对路径
     * @return 完整路径
     */
    @Override
    public String getFullPath(String relativePath) {
        try {
            if (relativePath == null) {
                log.error("相对路径为null");
                return "/";
            }
            
            // 确保路径以/开始
            String normalizedPath = relativePath;
            if (!normalizedPath.startsWith("/")) {
                normalizedPath = "/" + normalizedPath;
            }
            
            // 避免路径重复
            normalizedPath = normalizedPath.replaceAll("/+", "/");
            
            // 如果有根目录配置，将其与相对路径组合
            String rootDirectory = "";
            if (dataSource != null && StringUtils.isNotEmpty(dataSource.getPath())) {
                rootDirectory = dataSource.getPath();
                // 确保根目录不以/结尾
                if (rootDirectory.endsWith("/")) {
                    rootDirectory = rootDirectory.substring(0, rootDirectory.length() - 1);
                }
            }
            
            String result = rootDirectory + normalizedPath;
            log.debug("相对路径: {} 转换为完整路径: {}", relativePath, result);
            return result;
        } catch (Exception e) {
            log.error("获取完整路径时出错: {}", e.getMessage(), e);
            // 返回安全值
            return "/";
        }
    }
    
    @Override
    public boolean moveFile(String sourcePath, String targetPath) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            // 检查源文件是否存在
            if (!exists(sourcePath)) {
                log.error("SFTP源文件不存在: {}", sourcePath);
                return false;
            }
            
            // 检查目标目录是否存在，如果不存在则创建
            String targetDir = getParent(targetPath);
            if (!exists(targetDir)) {
                if (!createDirectory(targetDir)) {
                    log.error("创建SFTP目标目录失败: {}", targetDir);
                    return false;
                }
            }
            
            // 移动文件
            sftp.rename(sourcePath, targetPath);
            return true;
        } catch (Exception e) {
            log.error("移动SFTP文件失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 获取路径的父目录
     * @param path 路径
     * @return 父目录
     */
    @Override
    public String getParent(String path) {
        if (path == null || path.isEmpty() || "/".equals(path)) {
            return "/";
        }
        
        String fullPath = getFullPath(path);
        int lastSlashIndex = fullPath.lastIndexOf('/');
        
        // 如果没有斜杠或者斜杠在第一个位置且路径长度为1，返回根目录
        if (lastSlashIndex <= 0) {
            return "/";
        }
        
        // 返回斜杠之前的部分，如果为空则返回根目录
        String parent = fullPath.substring(0, lastSlashIndex);
        return parent.isEmpty() ? "/" : parent;
    }
    
    /**
     * 获取元数据
     * @param path 路径
     * @return 元数据
     */
    @Override
    public Map<String, Object> getMetadata(String path) {
        Map<String, Object> metadata = new HashMap<>();
        try {
            ChannelSftp sftp = getSftpChannel();
            if (sftp == null) {
                return metadata;
            }
            
            try {
                // 获取文件属性
                String fullPath = getFullPath(path);
                SftpATTRS attrs = sftp.stat(fullPath);
                
                if (attrs != null) {
                    metadata.put("path", path);
                    metadata.put("fullPath", fullPath);
                    metadata.put("size", attrs.getSize());
                    metadata.put("modificationTime", attrs.getMTime() * 1000L); // 转换为毫秒
                    metadata.put("accessTime", attrs.getATime() * 1000L); // 转换为毫秒
                    metadata.put("permissions", Integer.toOctalString(attrs.getPermissions() & 0777)); // 转换为八进制表示
                    metadata.put("uid", attrs.getUId());
                    metadata.put("gid", attrs.getGId());
                    metadata.put("isDirectory", attrs.isDir());
                    metadata.put("isFile", !attrs.isDir());
                    metadata.put("isLink", attrs.isLink());
                    
                    // 如果是目录，获取子项数量
                    if (attrs.isDir()) {
                        try {
                            Vector<ChannelSftp.LsEntry> list = sftp.ls(fullPath);
                            if (list != null) {
                                // 排除.和..
                                int count = 0;
                                for (ChannelSftp.LsEntry entry : list) {
                                    if (!".".equals(entry.getFilename()) && !"..".equals(entry.getFilename())) {
                                        count++;
                                    }
                                }
                                metadata.put("childCount", count);
                            }
                        } catch (Exception e) {
                            log.error("获取目录内容数量失败: {}", e.getMessage(), e);
                        }
                    }
                }
            } finally {
                releaseSftpChannel(sftp);
            }
        } catch (Exception e) {
            log.error("获取SFTP元数据失败: {}", e.getMessage(), e);
        }
        
        return metadata;
    }
    
    /**
     * 目录是否存在
     * @param path 路径
     * @return 是否存在
     */
    @Override
    public boolean directoryExists(String path) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            try {
                SftpATTRS attrs = sftp.stat(path);
                return attrs != null && attrs.isDir();
            } catch (SftpException e) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    return false;
                }
                throw e;
            }
        } catch (Exception e) {
            log.error("检查SFTP目录是否存在失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 获取SFTP通道（如果已连接则返回现有的，否则创建新的）
     * @return SFTP通道实例
     */
    private ChannelSftp getSftpChannel() {
        if (sftp != null && sftp.isConnected()) {
            return sftp;
        }
        
        try {
            initConnection();
            return sftp;
        } catch (Exception e) {
            log.error("获取SFTP通道失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 释放SFTP通道（这里我们不真正关闭它，而是保持连接池模式）
     * @param channel SFTP通道实例
     */
    private void releaseSftpChannel(ChannelSftp channel) {
        // 由于我们使用的是连接池模式，这里不做任何操作
        // 实际连接将在closeConnection()方法中关闭
    }
    
    /**
     * 检查文件是否存在
     * @param path 文件路径
     * @return 文件是否存在
     */
    @Override
    public boolean fileExists(String path) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            // 获取完整路径
            String fullPath = getFullPath(path);
            
            try {
                // 获取文件属性
                SftpATTRS attrs = sftp.stat(fullPath);
                // 存在且不是目录则为文件
                return attrs != null && !attrs.isDir();
            } catch (SftpException e) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    // 文件不存在
                    return false;
                }
                throw e; // 其他异常继续抛出
            }
        } catch (Exception e) {
            log.error("检查SFTP文件是否存在失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 写入文本文件到SFTP服务器
     * @param content 文本内容
     * @param targetPath 目标路径
     * @return 是否写入成功
     */
    @Override
    public boolean writeTextFile(String content, String targetPath) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            // 将文本内容转换为字节数组
            byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
            
            // 获取完整路径
            String fullPath = getFullPath(targetPath);
            
            // 确保父目录存在
            String parentDir = getParent(fullPath);
            if (!parentDir.equals("/")) {
                try {
                    sftp.stat(parentDir);
                } catch (SftpException e) {
                    if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                        // 父目录不存在，创建它
                        createDirectoryHierarchy(parentDir);
                    } else {
                        throw e;
                    }
                }
            }
            
            // 创建输入流
            ByteArrayInputStream inputStream = new ByteArrayInputStream(contentBytes);
            
            // 上传文件
            sftp.put(inputStream, fullPath);
            
            return true;
        } catch (Exception e) {
            log.error("写入SFTP文本文件失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 递归创建目录层次结构
     * @param path 目录路径
     * @throws SftpException 如果创建失败
     */
    private void createDirectoryHierarchy(String path) throws SftpException {
        String[] folders = path.split("/");
        String currentPath = "";
        
        // 从根目录开始创建每个子目录
        for (String folder : folders) {
            if (folder.isEmpty()) {
                continue; // 跳过空目录名（通常是路径开头的/）
            }
            
            currentPath += "/" + folder;
            
            try {
                sftp.stat(currentPath);
            } catch (SftpException e) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    // 目录不存在，创建它
                    sftp.mkdir(currentPath);
                } else {
                    throw e;
                }
            }
        }
    }
    
    /**
     * 读取文本文件
     * @param path 文件路径
     * @return 文件内容
     */
    @Override
    public String readTextFile(String path) {
        try {
            if (!initConnection()) {
                return null;
            }
            
            String fullPath = getFullPath(path);
            
            // 检查文件是否存在
            try {
                SftpATTRS attrs = sftp.stat(fullPath);
                if (attrs.isDir()) {
                    log.error("路径是一个目录，无法读取为文本文件: {}", fullPath);
                    return null;
                }
            } catch (SftpException e) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    log.error("文件不存在: {}", fullPath);
                    return null;
                }
                throw e;
            }
            
            // 读取文件内容
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                sftp.get(fullPath, outputStream);
                return outputStream.toString(StandardCharsets.UTF_8.name());
            }
        } catch (Exception e) {
            log.error("读取SFTP文本文件失败: {}", e.getMessage(), e);
            return null;
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 下载文件到指定的File对象
     * @param sourcePath SFTP服务器上的源文件路径
     * @param destFile 本地目标File对象
     * @return 是否下载成功
     */
    @Override
    public boolean downloadFile(String sourcePath, File destFile) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            // 获取完整的源路径
            String fullSourcePath = getFullPath(sourcePath);
            
            // 检查源文件是否存在
            try {
                SftpATTRS attrs = sftp.stat(fullSourcePath);
                if (attrs.isDir()) {
                    log.error("源路径是一个目录，不是文件: {}", fullSourcePath);
                    return false;
                }
            } catch (SftpException e) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    log.error("源文件不存在: {}", fullSourcePath);
                    return false;
                }
                throw e;
            }
            
            // 确保目标文件的父目录存在
            File parentDir = destFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    log.error("无法创建目标文件的父目录: {}", parentDir.getAbsolutePath());
                    return false;
                }
            }
            
            // 下载文件
            try (FileOutputStream fos = new FileOutputStream(destFile)) {
                sftp.get(fullSourcePath, fos);
            }
            
            return true;
        } catch (Exception e) {
            log.error("下载SFTP文件失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 下载文件到输出流
     * @param path 文件路径
     * @param outputStream 输出流
     * @return 是否下载成功
     */
    @Override
    public boolean downloadFile(String path, OutputStream outputStream) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            // 获取完整的源路径
            String fullPath = getFullPath(path);
            
            // 检查文件是否存在
            try {
                SftpATTRS attrs = sftp.stat(fullPath);
                if (attrs.isDir()) {
                    log.error("路径是一个目录，不是文件: {}", fullPath);
                    return false;
                }
            } catch (SftpException e) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    log.error("文件不存在: {}", fullPath);
                    return false;
                }
                throw e;
            }
            
            // 下载文件到输出流
            sftp.get(fullPath, outputStream);
            
            return true;
        } catch (Exception e) {
            log.error("下载SFTP文件到输出流失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 从本地文件上传到SFTP服务器
     * @param file 本地文件
     * @param remotePath 远程路径
     * @return 是否上传成功
     */
    @Override
    public boolean uploadFile(File file, String remotePath) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            // 检查本地文件是否存在
            if (!file.exists() || !file.isFile()) {
                log.error("本地文件不存在或不是文件: {}", file.getAbsolutePath());
                return false;
            }
            
            // 获取完整的远程路径
            String fullRemotePath = getFullPath(remotePath);
            
            // 确保远程目录存在
            String remoteDir = fullRemotePath.substring(0, fullRemotePath.lastIndexOf('/'));
            if (!remoteDir.isEmpty()) {
                try {
                    sftp.stat(remoteDir);
                } catch (SftpException e) {
                    if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                        // 远程目录不存在，创建它
                        createDirectoryHierarchy(remoteDir);
                    } else {
                        throw e;
                    }
                }
            }
            
            // 上传文件
            sftp.put(file.getAbsolutePath(), fullRemotePath);
            
            return true;
        } catch (Exception e) {
            log.error("从本地文件上传到SFTP服务器失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }

    @Override
    public boolean uploadFile(InputStream inputStream, String remotePath) {
        ChannelSftp sftpChannel = null;

        try {
            // 参数检查
            if (inputStream == null) {
                log.error("上传失败: 文件内容为空");
                return false;
            }

            if (remotePath == null || remotePath.trim().isEmpty()) {
                log.error("上传失败: 目标路径为空");
                return false;
            }

            // 获取完整路径
            String fullPath = getFullPath(remotePath);
            log.info("准备上传文件到SFTP路径: {}", fullPath);

            // 重要：创建一个新的SFTP连接，专用于上传，避免共享连接的问题
            JSch jsch = new JSch();
            int port = DEFAULT_PORT.equals("22") ? 22 : Integer.parseInt(dataSource.getPort());

            Session uploadSession = jsch.getSession(
                    dataSource.getUsername(),
                    dataSource.getHost(),
                    port
            );

            uploadSession.setPassword(dataSource.getPassword());

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            uploadSession.setConfig(config);

            // 设置更长的超时时间
            uploadSession.connect(60000);

            Channel channel = uploadSession.openChannel("sftp");
            channel.connect(60000);

            sftpChannel = (ChannelSftp) channel;

            if (sftpChannel == null) {
                log.error("上传失败: 无法创建SFTP通道");
                return false;
            }

            // 确保父目录存在
            String parentDir = getParent(fullPath);
            if (!directoryExists(parentDir)) {
                log.info("创建父目录: {}", parentDir);
                if (!createDirectory(parentDir)) {
                    log.error("创建SFTP目标目录失败: {}", parentDir);
                    return false;
                }
            }

            // 上传文件
            log.info("开始上传文件...");
            sftpChannel.put(inputStream, fullPath);
            log.info("文件上传成功: {}", fullPath);

            return true;
        } catch (Exception e) {
            log.error("上传文件到SFTP失败: {}", e.getMessage(), e);
            return false;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                log.error("关闭输入流失败: {}", e.getMessage());
            }

            // 关闭专用的SFTP连接
            try {
                if (sftpChannel != null && sftpChannel.isConnected()) {
                    sftpChannel.disconnect();
                }
            } catch (Exception e) {
                log.error("关闭SFTP通道失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 检查路径是否可读
     * @param path 路径
     * @return 是否可读
     */
    @Override
    public boolean isReadable(String path) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            // 检查文件是否存在
            try {
                SftpATTRS attrs = sftp.stat(path);
                
                // 检查用户是否有读权限 (r--) 检查第1位是否为r
                String permissions = attrs.getPermissionsString();
                boolean ownerReadable = permissions.length() > 1 && permissions.charAt(1) == 'r';
                
                // 也可以检查组和其他人权限 (权限格式 drwxrwxrwx)
                boolean groupReadable = permissions.length() > 4 && permissions.charAt(4) == 'r';
                boolean othersReadable = permissions.length() > 7 && permissions.charAt(7) == 'r';
                
                return ownerReadable || groupReadable || othersReadable;
            } catch (SftpException e) {
                log.error("检查SFTP路径是否可读失败: {}", e.getMessage(), e);
                return false;
            }
        } catch (Exception e) {
            log.error("检查SFTP路径是否可读失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 检查路径是否可写
     * @param path 路径
     * @return 是否可写
     */
    @Override
    public boolean isWritable(String path) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            // 检查文件是否存在
            try {
                SftpATTRS attrs = sftp.stat(path);
                
                // 检查用户是否有写权限 (-w-) 检查第2位是否为w
                String permissions = attrs.getPermissionsString();
                boolean ownerWritable = permissions.length() > 2 && permissions.charAt(2) == 'w';
                
                // 也可以检查组和其他人权限 (权限格式 drwxrwxrwx)
                boolean groupWritable = permissions.length() > 5 && permissions.charAt(5) == 'w';
                boolean othersWritable = permissions.length() > 8 && permissions.charAt(8) == 'w';
                
                return ownerWritable || groupWritable || othersWritable;
            } catch (SftpException e) {
                log.error("检查SFTP路径是否可写失败: {}", e.getMessage(), e);
                return false;
            }
        } catch (Exception e) {
            log.error("检查SFTP路径是否可写失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 获取已用空间
     * @return 已用空间大小(字节)
     */
    @Override
    public long getUsedSpace() {
        try {
            if (!initConnection()) {
                return -1;
            }
            
            try {
                // SFTP协议本身不直接提供已用空间信息，尝试通过执行df命令获取
                Channel channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand("df -k . | tail -1 | awk '{print $3}'");
                
                InputStream in = channel.getInputStream();
                channel.connect();
                
                byte[] buffer = new byte[1024];
                int bytesRead = in.read(buffer);
                
                if (bytesRead > 0) {
                    String output = new String(buffer, 0, bytesRead).trim();
                    try {
                        // 结果是以KB为单位，需要转换为字节
                        return Long.parseLong(output) * 1024;
                    } catch (NumberFormatException e) {
                        log.error("解析SFTP已用空间失败: {}", e.getMessage(), e);
                    }
                }
                
                channel.disconnect();
                return -1;
            } catch (Exception e) {
                log.error("获取SFTP已用空间失败: {}", e.getMessage(), e);
                return -1;
            }
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 获取文件系统总空间
     * @return 总空间大小（字节）
     */
    @Override
    public long getTotalSpace() {
        try {
            if (!initConnection()) {
                return -1;
            }
            
            // SFTP协议并没有直接提供获取总空间的方法
            // 尝试通过执行命令获取文件系统信息
            try {
                // 创建exec通道执行df命令
                Channel channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand("df -k " + (StringUtils.isNotEmpty(dataSource.getPath()) ? dataSource.getPath() : "/"));
                
                InputStream in = channel.getInputStream();
                channel.connect();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                
                // 跳过标题行
                reader.readLine();
                
                if ((line = reader.readLine()) != null) {
                    // 分析df命令输出
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        try {
                            // 总空间，以KB为单位，需要转换为字节
                            long totalSpace = Long.parseLong(parts[1]) * 1024;
                            return totalSpace;
                        } catch (NumberFormatException e) {
                            log.error("解析文件系统大小失败: {}", e.getMessage());
                        }
                    }
                }
                
                channel.disconnect();
            } catch (Exception e) {
                log.error("执行df命令获取总空间失败: {}", e.getMessage());
            }
            
            // 如果无法获取，返回一个默认大小
            return 1024L * 1024L * 1024L * 1024L; // 1TB
        } catch (Exception e) {
            log.error("获取SFTP文件系统总空间失败: {}", e.getMessage(), e);
            return -1;
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 获取可用空间
     * @return 可用空间大小（字节）
     */
    @Override
    public long getAvailableSpace() {
        try {
            if (!initConnection()) {
                return -1;
            }
            
            // SFTP协议并没有直接提供获取可用空间的方法
            // 尝试通过执行命令获取文件系统信息
            try {
                // 创建exec通道执行df命令
                Channel channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand("df -k " + (StringUtils.isNotEmpty(dataSource.getPath()) ? dataSource.getPath() : "/"));
                
                InputStream in = channel.getInputStream();
                channel.connect();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                
                // 跳过标题行
                reader.readLine();
                
                if ((line = reader.readLine()) != null) {
                    // 分析df命令输出
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 4) {
                        try {
                            // 可用空间，以KB为单位，需要转换为字节
                            long availableSpace = Long.parseLong(parts[3]) * 1024;
                            return availableSpace;
                        } catch (NumberFormatException e) {
                            log.error("解析文件系统可用空间失败: {}", e.getMessage());
                        }
                    }
                }
                
                channel.disconnect();
            } catch (Exception e) {
                log.error("执行df命令获取可用空间失败: {}", e.getMessage());
            }
            
            // 如果无法获取，返回-1表示未知
            return -1;
        } catch (Exception e) {
            log.error("获取SFTP文件系统可用空间失败: {}", e.getMessage(), e);
            return -1;
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 获取存储统计信息
     * @param path 路径
     * @return 统计信息
     */
    @Override
    public Map<String, Object> getStorageStats(String path) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            if (!initConnection()) {
                return stats;
            }
            
            // 获取完整路径
            String fullPath = getFullPath(path);
            stats.put("path", fullPath);
            
            try {
                // 创建exec通道执行df命令获取存储信息
                Channel channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand("df -h " + fullPath);
                
                InputStream in = channel.getInputStream();
                channel.connect();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                String line;
                
                // 跳过标题行
                reader.readLine();
                
                if ((line = reader.readLine()) != null) {
                    // 分析df命令输出：Filesystem Size Used Avail Use% Mounted on
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 6) {
                        stats.put("filesystem", parts[0]);
                        stats.put("size", parts[1]);
                        stats.put("used", parts[2]);
                        stats.put("available", parts[3]);
                        stats.put("usePercentage", parts[4]);
                        stats.put("mountPoint", parts[5]);
                    }
                }
                
                channel.disconnect();
                
                // 尝试获取目录内容计数
                List<Map<String, Object>> files = listFiles(fullPath);
                
                long totalSize = 0;
                int fileCount = 0;
                int dirCount = 0;
                
                for (Map<String, Object> file : files) {
                    String type = (String) file.get("type");
                    
                    if ("directory".equals(type)) {
                        dirCount++;
                    } else {
                        fileCount++;
                        Object size = file.get("size");
                        if (size instanceof Number) {
                            totalSize += ((Number) size).longValue();
                        }
                    }
                }
                
                stats.put("fileCount", fileCount);
                stats.put("directoryCount", dirCount);
                stats.put("totalItemCount", fileCount + dirCount);
                stats.put("totalSize", totalSize);
                stats.put("averageFileSize", fileCount > 0 ? totalSize / fileCount : 0);
                
            } catch (Exception e) {
                log.error("获取SFTP存储统计信息失败: {}", e.getMessage(), e);
            }
            
        } catch (Exception e) {
            log.error("获取SFTP存储统计信息失败: {}", e.getMessage(), e);
        } finally {
            closeConnection();
        }
        
        return stats;
    }
    
    /**
     * 是否为目录
     * @param path 路径
     * @return 是否为目录
     */
    @Override
    public boolean isDirectory(String path) {
        return directoryExists(path);
    }
    
    /**
     * 获取文件大小
     * @param path 路径
     * @return 文件大小(字节)
     */
    @Override
    public long getFileSize(String path) {
        try {
            if (!initConnection()) {
                return -1;
            }
            
            try {
                SftpATTRS attrs = sftp.stat(getFullPath(path));
                return attrs.getSize();
            } catch (SftpException e) {
                log.error("获取SFTP文件大小失败: {}", e.getMessage(), e);
                return -1;
            }
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 获取文件最后修改时间
     * @param path 路径
     * @return 最后修改时间(毫秒)
     */
    @Override
    public long getLastModifiedTime(String path) {
        try {
            if (!initConnection()) {
                return -1;
            }
            
            try {
                SftpATTRS attrs = sftp.stat(getFullPath(path));
                // SFTP返回的是秒，需要转换为毫秒
                return attrs.getMTime() * 1000L;
            } catch (SftpException e) {
                log.error("获取SFTP文件修改时间失败: {}", e.getMessage(), e);
                return -1;
            }
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 读取文件内容
     * @param path 文件路径
     * @return 文件内容字节数组
     */
    @Override
    public byte[] readFile(String path) {
        try {
            if (!initConnection()) {
                return null;
            }
            
            try {
                String fullPath = getFullPath(path);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                sftp.get(fullPath, baos);
                return baos.toByteArray();
            } catch (SftpException e) {
                log.error("读取SFTP文件内容失败: {}", e.getMessage(), e);
                return null;
            }
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 读取文件片段
     * @param path 路径
     * @param offset 偏移量
     * @param length 长度
     * @return 文件片段
     */
    @Override
    public byte[] readFilePart(String path, long offset, int length) {
        try {
            // SFTP协议不直接支持部分读取，读取全部内容后再截取
            byte[] content = readFile(path);
            if (content == null || offset >= content.length) {
                return null;
            }
            
            int actualLength = (int) Math.min(length, content.length - offset);
            byte[] result = new byte[actualLength];
            System.arraycopy(content, (int) offset, result, 0, actualLength);
            return result;
        } catch (Exception e) {
            log.error("读取SFTP文件片段失败: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 写入文件内容
     * @param path 文件路径
     * @param content 文件内容字节数组
     * @return 是否写入成功
     */
    @Override
    public boolean writeFile(String path, byte[] content) {
        return uploadFile(path, content);
    }
    
    /**
     * 上传文件
     * @param localPath 本地路径
     * @param remotePath 远程路径
     * @return 是否上传成功
     */
    @Override
    public boolean uploadFile(String localPath, String remotePath) {
        try {
            File file = new File(localPath);
            if (!file.exists() || !file.isFile()) {
                log.error("本地文件不存在或不是文件: {}", localPath);
                return false;
            }
            return uploadFile(file, remotePath);
        } catch (Exception e) {
            log.error("上传文件到SFTP失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取文件权限
     * @param path 路径
     * @return 文件权限
     */
    @Override
    public String getFilePermission(String path) {
        try {
            if (!initConnection()) {
                return null;
            }
            
            try {
                SftpATTRS attrs = sftp.stat(getFullPath(path));
                return Integer.toOctalString(attrs.getPermissions() & 0777); // 转换为八进制权限表示
            } catch (SftpException e) {
                log.error("获取SFTP文件权限失败: {}", e.getMessage(), e);
                return null;
            }
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 设置文件权限
     * @param path 路径
     * @param permission 权限
     * @return 是否设置成功
     */
    @Override
    public boolean setFilePermission(String path, String permission) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            try {
                // 将八进制权限字符串转为整数
                int permissionInt = 0;
                if (permission != null && !permission.isEmpty()) {
                    try {
                        permissionInt = Integer.parseInt(permission, 8);
                    } catch (NumberFormatException e) {
                        log.error("无效的权限格式: {}", permission);
                        return false;
                    }
                } else {
                    log.error("权限不能为空");
                    return false;
                }
                
                // 确保路径有效
                String fullPath = getFullPath(path);
                if (fullPath == null || fullPath.isEmpty()) {
                    log.error("无效的文件路径");
                    return false;
                }
                
                try {
                    // 检查文件是否存在
                    SftpATTRS attrs = sftp.stat(fullPath);
                    if (attrs == null) {
                        log.error("文件不存在: {}", fullPath);
                        return false;
                    }
                    
                    // 使用int类型的权限值直接调用chmod
                    sftp.chmod(permissionInt, fullPath);
                    return true;
                } catch (SftpException e) {
                    log.error("设置文件权限失败: {}", e.getMessage());
                    return false;
                }
            } catch (Exception e) {
                log.error("设置SFTP文件权限失败: {}", e.getMessage(), e);
                return false;
            }
        } finally {
            closeConnection();
        }
    }
    
    /**
     * 获取文件写入流
     * @param path 路径
     * @return 写入流
     */
    @Override
    public OutputStream getOutputStream(String path) {
        try {
            if (!initConnection()) {
                return null;
            }
            
            final String fullPath = getFullPath(path);
            
            // 确保父目录存在
            String parentDir = getParent(fullPath);
            if (!directoryExists(parentDir)) {
                createDirectory(parentDir);
            }
            
            // 返回一个ByteArrayOutputStream，它将在close时写入SFTP服务器
            return new ByteArrayOutputStream() {
                @Override
                public void close() throws IOException {
                    super.close();
                    
                    try {
                        byte[] data = this.toByteArray();
                        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
                        
                        try {
                            sftp.put(inputStream, fullPath);
                        } catch (SftpException e) {
                            throw new IOException("无法将数据上传到SFTP服务器: " + e.getMessage(), e);
                        } finally {
                            inputStream.close();
                        }
                    } finally {
                        closeConnection();
                    }
                }
            };
        } catch (Exception e) {
            log.error("获取SFTP输出流失败: {}", e.getMessage(), e);
            closeConnection();
            return null;
        }
    }
    
    /**
     * 下载文件
     * @param remotePath 远程路径
     * @param localPath 本地路径
     * @return 是否下载成功
     */
    @Override
    public boolean downloadFile(String remotePath, String localPath) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            // 获取完整的源路径
            String fullSourcePath = getFullPath(remotePath);
            
            // 确保本地目录存在
            File localFile = new File(localPath);
            File parentDir = localFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    log.error("无法创建本地目录: {}", parentDir.getAbsolutePath());
                    return false;
                }
            }
            
            // 下载文件
            try (FileOutputStream fos = new FileOutputStream(localFile)) {
                sftp.get(fullSourcePath, fos);
                return true;
            } catch (SftpException e) {
                log.error("下载SFTP文件失败: {}", e.getMessage(), e);
                return false;
            }
        } catch (Exception e) {
            log.error("下载SFTP文件失败: {}", e.getMessage(), e);
            return false;
        } finally {
            closeConnection();
        }
    }
} 