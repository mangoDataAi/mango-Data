package com.mango.test.database.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mango.test.database.entity.FileDataSource;
import com.mango.test.database.service.FileDataSourceService;
import com.mango.test.database.service.FileTemplateService;
import com.mango.test.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "文件数据源管理")
@RestController
@RequestMapping("/api/file-source")
public class FileDataSourceController {

    @Autowired
    private FileDataSourceService fileDataSourceService;

    @Autowired
    private FileTemplateService fileTemplateService;

    @GetMapping("/list")
    @ApiOperation("获取数据源列表")
    public R<IPage<FileDataSource>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            FileDataSource query) {
        try {
            Page<FileDataSource> page = new Page<>(pageNum, pageSize);
            return R.ok(fileDataSourceService.page(page, query));
        } catch (Exception e) {
            log.error("获取数据源列表失败", e);
            return R.fail("获取数据源列表失败：" + e.getMessage());
        }
    }

    @PostMapping("/upload")
    @ApiOperation("上传文件")
    public R<FileDataSource> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "override", defaultValue = "false") boolean override
    ) {
        try {
            FileDataSource fileDataSource = fileDataSourceService.uploadFile(file, override);
            return R.ok(fileDataSource);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            return R.fail("上传文件失败：" + e.getMessage());
        }
    }


    @DeleteMapping("/{id}")
    @ApiOperation("删除数据源")
    public R<Void> delete(@PathVariable Long id) {
        try {
            fileDataSourceService.delete(id);
            return R.ok();
        } catch (Exception e) {
            log.error("删除数据源失败", e);
            return R.fail("删除数据源失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/batch")
    @ApiOperation("批量删除数据源")
    public R<Void> deleteBatch(@RequestBody List<Long> ids) {
        try {
            fileDataSourceService.deleteBatch(ids);
            return R.ok();
        } catch (Exception e) {
            log.error("批量删除数据源失败", e);
            return R.fail("批量删除数据源失败：" + e.getMessage());
        }
    }

    @GetMapping("/template")
    @ApiOperation("下载模板")
    public void downloadTemplate(@RequestParam String fileType, HttpServletResponse response) {
        try {
            // 生成模板文件
            byte[] content = fileTemplateService.generateTemplate(fileType);

            // 设置响应头
            response.setContentType(getContentType(fileType));
            response.setCharacterEncoding("UTF-8");
            
            // 设置文件名，使用URLEncoder编码避免中文问题
            String fileName = "template." + fileType.toLowerCase();
            String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

            // 设置不缓存
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            // 写入响应
            IOUtils.write(content, response.getOutputStream());
        } catch (Exception e) {
            log.error("下载模板失败", e);
            throw new RuntimeException("下载模板失败：" + e.getMessage());
        }
    }

    private String getContentType(String fileType) {
        String type = fileType.toLowerCase();
        if ("csv".equals(type) || "txt".equals(type)) {
            return "text/plain";
        } else if ("xlsx".equals(type)) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else if ("json".equals(type)) {
            return "application/json";
        } else if ("xml".equals(type)) {
            return "application/xml";
        } else if ("dmp".equals(type) || "sql".equals(type)) {
            return "text/plain";
        } else {
            return "application/octet-stream";
        }
    }

    @GetMapping("/{id}/data")
    @ApiOperation("获取数据源数据")
    public R<Map<String, Object>> getData(
            @PathVariable String id,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            Map<String, Object> data = fileDataSourceService.getData(id, pageNum, pageSize);
            return R.ok(data);
        } catch (Exception e) {
            log.error("获取数据失败", e);
            return R.fail("获取数据失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}/data")
    @ApiOperation("更新数据")
    public R<Void> updateData(
            @PathVariable String id,
            @RequestBody List<Map<String, Object>> data) {
        try {
            fileDataSourceService.updateData(id, data);
            return R.ok();
        } catch (Exception e) {
            log.error("更新数据失败", e);
            return R.fail("更新数据失败：" + e.getMessage());
        }
    }

    @GetMapping("/stats")
    @ApiOperation("获取文件数据源统计信息")
    public R<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = fileDataSourceService.getStatistics();
            return R.ok(stats);
        } catch (Exception e) {
            log.error("获取文件数据源统计信息失败", e);
            return R.fail("获取文件数据源统计信息失败：" + e.getMessage());
        }
    }
}
