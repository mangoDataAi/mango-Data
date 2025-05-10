package com.mango.test.database.controller;

import com.mango.test.vo.R;
import com.mango.test.database.service.FileSystemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "文件系统")
@RestController
@RequestMapping("/api/filesystem")
public class FileSystemController {

    @Autowired
    private FileSystemService fileSystemService;

    @GetMapping("/{datasourceId}/list")
    @ApiOperation("获取文件列表")
    public R<List<Map<String, Object>>> listFiles(
            @PathVariable String datasourceId,
            @RequestParam(defaultValue = "/") String path,
            @RequestParam(required = false) String filter) {
        try {
            List<Map<String, Object>> files = fileSystemService.listFiles(datasourceId, path, filter);
            return R.ok(files);
        } catch (Exception e) {
            log.error("获取文件列表失败", e);
            return R.fail("获取文件列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{datasourceId}/info")
    @ApiOperation("获取文件或目录信息")
    public R<Map<String, Object>> getFileInfo(
            @PathVariable String datasourceId,
            @RequestParam String path) {
        try {
            Map<String, Object> info = fileSystemService.getFileInfo(datasourceId, path);
            return R.ok(info);
        } catch (Exception e) {
            log.error("获取文件信息失败", e);
            return R.fail("获取文件信息失败: " + e.getMessage());
        }
    }

    @GetMapping("/{datasourceId}/content")
    @ApiOperation("获取文件内容")
    public R<String> getFileContent(
            @PathVariable String datasourceId,
            @RequestParam String path,
            @RequestParam(required = false, defaultValue = "utf-8") String encoding) {
        try {
            String content = fileSystemService.getFileContent(datasourceId, path, encoding);
            return R.ok(content);
        } catch (Exception e) {
            log.error("获取文件内容失败", e);
            return R.fail("获取文件内容失败: " + e.getMessage());
        }
    }

    @GetMapping("/{datasourceId}/download")
    @ApiOperation("下载文件")
    public void downloadFile(
            @PathVariable String datasourceId,
            @RequestParam String path,
            HttpServletResponse response) {
        try {
            fileSystemService.downloadFile(datasourceId, path, response);
        } catch (Exception e) {
            log.error("下载文件失败", e);
            try {
                response.setContentType("application/json");
                response.setCharacterEncoding("utf-8");
                response.getWriter().write("{\"msg\":\"下载文件失败: " + e.getMessage() + "\"}");
            } catch (Exception ex) {
                log.error("返回错误信息失败", ex);
            }
        }
    }

    @PostMapping("/{datasourceId}/upload")
    @ApiOperation("上传文件")
    public R<Map<String, Object>> uploadFile(
            @PathVariable String datasourceId,
            @RequestParam String path,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "false") boolean overwrite) {
        try {
            Map<String, Object> info = fileSystemService.uploadFile(datasourceId, path, file, overwrite);
            return R.ok("文件上传成功", info);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return R.fail("上传文件失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{datasourceId}/delete")
    @ApiOperation("删除文件或目录")
    public R<Boolean> deleteFile(
            @PathVariable String datasourceId,
            @RequestParam String path,
            @RequestParam(required = false, defaultValue = "false") boolean recursive) {
        try {
            boolean success = fileSystemService.deleteFile(datasourceId, path, recursive);
            return R.ok("删除成功", success);
        } catch (Exception e) {
            log.error("删除文件失败", e);
            return R.fail("删除文件失败: " + e.getMessage());
        }
    }

    @PostMapping("/{datasourceId}/mkdir")
    @ApiOperation("创建目录")
    public R<Map<String, Object>> createDirectory(
            @PathVariable String datasourceId,
            @RequestBody Map<String, String> request) {
        try {
            Map<String, Object> info = fileSystemService.createDirectory(datasourceId, request.get("path"));
            return R.ok("目录创建成功", info);
        } catch (Exception e) {
            log.error("创建目录失败", e);
            return R.fail("创建目录失败: " + e.getMessage());
        }
    }

    @PostMapping("/{datasourceId}/copy")
    @ApiOperation("复制文件或目录")
    public R<Boolean> copyFile(
            @PathVariable String datasourceId,
            @RequestBody Map<String, Object> request) {
        try {
            boolean success = fileSystemService.copyFile(
                    datasourceId, 
                    (String) request.get("sourcePath"), 
                    (String) request.get("destinationPath"), 
                    (Boolean) request.getOrDefault("overwrite", false));
            return R.ok("复制成功", success);
        } catch (Exception e) {
            log.error("复制文件失败", e);
            return R.fail("复制文件失败: " + e.getMessage());
        }
    }

    @PostMapping("/{datasourceId}/move")
    @ApiOperation("移动文件或目录")
    public R<Boolean> moveFile(
            @PathVariable String datasourceId,
            @RequestBody Map<String, Object> request) {
        try {
            boolean success = fileSystemService.moveFile(
                    datasourceId, 
                    (String) request.get("sourcePath"), 
                    (String) request.get("destinationPath"), 
                    (Boolean) request.getOrDefault("overwrite", false));
            return R.ok("移动成功", success);
        } catch (Exception e) {
            log.error("移动文件失败", e);
            return R.fail("移动文件失败: " + e.getMessage());
        }
    }

    @PostMapping("/{datasourceId}/rename")
    @ApiOperation("重命名文件或目录")
    public R<Boolean> renameFile(
            @PathVariable String datasourceId,
            @RequestBody Map<String, Object> request) {
        try {
            boolean success = fileSystemService.renameFile(
                    datasourceId, 
                    (String) request.get("path"), 
                    (String) request.get("newName"));
            return R.ok("重命名成功", success);
        } catch (Exception e) {
            log.error("重命名文件失败", e);
            return R.fail("重命名文件失败: " + e.getMessage());
        }
    }

    @GetMapping("/{datasourceId}/search")
    @ApiOperation("搜索文件")
    public R<List<Map<String, Object>>> searchFiles(
            @PathVariable String datasourceId,
            @RequestParam String basePath,
            @RequestParam String query,
            @RequestParam(required = false) boolean recursive,
            @RequestParam(required = false) String fileType) {
        try {
            List<Map<String, Object>> files = fileSystemService.searchFiles(
                    datasourceId, 
                    basePath, 
                    query, 
                    recursive, 
                    fileType);
            return R.ok(files);
        } catch (Exception e) {
            log.error("搜索文件失败", e);
            return R.fail("搜索文件失败: " + e.getMessage());
        }
    }

    @GetMapping("/{datasourceId}/stats")
    @ApiOperation("获取文件系统统计信息")
    public R<Map<String, Object>> getFileSystemStats(@PathVariable String datasourceId) {
        try {
            Map<String, Object> stats = fileSystemService.getFileSystemStats(datasourceId);
            return R.ok(stats);
        } catch (Exception e) {
            log.error("获取文件系统统计信息失败", e);
            return R.fail("获取文件系统统计信息失败: " + e.getMessage());
        }
    }

    @PostMapping("/{datasourceId}/save")
    @ApiOperation("保存文件内容")
    public R<Boolean> saveFileContent(
            @PathVariable String datasourceId,
            @RequestBody Map<String, Object> request) {
        try {
            boolean success = fileSystemService.saveFileContent(
                    datasourceId, 
                    (String) request.get("path"), 
                    (String) request.get("content"), 
                    (String) request.getOrDefault("encoding", "utf-8"));
            return R.ok("文件保存成功", success);
        } catch (Exception e) {
            log.error("保存文件内容失败", e);
            return R.fail("保存文件内容失败: " + e.getMessage());
        }
    }

    @GetMapping("/{datasourceId}/preview")
    @ApiOperation("预览文件")
    public void previewFile(
            @PathVariable String datasourceId,
            @RequestParam String path,
            @RequestParam(required = false) String t,
            @RequestParam(required = false, defaultValue = "false") boolean noCache,
            HttpServletResponse response) {
        try {
            log.info("预览文件请求 - datasourceId: {}, path: {}", datasourceId, path);
            
            // 获取文件扩展名并设置适当的Content-Type
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            String extension = fileName.contains(".") ? fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase() : "";
            
            log.info("文件类型: {}, 扩展名: {}", fileName, extension);
            
            // 根据扩展名设置Content-Type
            switch (extension) {
                case "pdf":
                    response.setContentType("application/pdf");
                    break;
                case "jpg":
                case "jpeg":
                    response.setContentType("image/jpeg");
                    break;
                case "png":
                    response.setContentType("image/png");
                    break;
                case "gif":
                    response.setContentType("image/gif");
                    break;
                case "mp4":
                    response.setContentType("video/mp4");
                    break;
                case "mp3":
                    response.setContentType("audio/mpeg");
                    break;
                case "html":
                case "htm":
                    response.setContentType("text/html");
                    break;
                default:
                    response.setContentType("application/octet-stream");
                    break;
            }
            
            // 设置为内联显示，而不是下载
            response.setHeader("Content-Disposition", "inline; filename=\"" + fileName + "\"");
            
            // 设置跨域和缓存控制
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "GET, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type");
            
            // 对于PDF特殊处理，禁用缓存
            if ("pdf".equals(extension) || noCache) {
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
                log.info("已禁用PDF文件缓存");
            }
            
            // 调用现有的下载文件方法，但响应头已修改为inline
            fileSystemService.downloadFile(datasourceId, path, response);
            log.info("文件预览成功");
        } catch (Exception e) {
            log.error("预览文件失败: ", e);
            try {
                response.setContentType("application/json");
                response.setCharacterEncoding("utf-8");
                response.getWriter().write("{\"msg\":\"预览文件失败: " + e.getMessage() + "\"}");
            } catch (Exception ex) {
                log.error("返回错误信息失败", ex);
            }
        }
    }
} 