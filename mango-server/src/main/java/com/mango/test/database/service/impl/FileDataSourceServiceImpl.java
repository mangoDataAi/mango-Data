package com.mango.test.database.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mango.test.database.entity.FileDataSource;
import com.mango.test.database.entity.FileField;
import com.mango.test.exception.BusinessException;
import com.mango.test.database.mapper.FileDataSourceMapper;
import com.mango.test.database.mapper.FileFieldMapper;
import com.mango.test.database.model.FileParseResult;
import com.mango.test.parser.FileParser;
import com.mango.test.database.service.DataSourceService;
import com.mango.test.database.service.FileDataSourceService;
import com.mango.test.util.JdbcUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import java.util.ArrayList;

@Slf4j
@Service
public class FileDataSourceServiceImpl extends ServiceImpl<FileDataSourceMapper, FileDataSource> implements FileDataSourceService {

    @Autowired
    private FileDataSourceMapper fileDataSourceMapper;

    @Autowired
    private FileFieldMapper fileFieldMapper;

    @Autowired
    private List<FileParser> fileParsers;

    @Autowired
    private DataSourceService dataSourceService;

    private Map<String, FileParser> fileParserMap;

    @PostConstruct
    public void init() {
        fileParserMap = new HashMap<>();
        for (FileParser parser : fileParsers) {
            fileParserMap.put(parser.getFileType().toLowerCase(), parser);
        }
    }

    @Override
    public IPage<FileDataSource> page(Page<FileDataSource> page, FileDataSource query) {
        LambdaQueryWrapper<FileDataSource> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotBlank(query.getFileName())) {
            wrapper.like(FileDataSource::getFileName, query.getFileName());
        }
        if (StringUtils.isNotBlank(query.getFileType())) {
            wrapper.eq(FileDataSource::getFileType, query.getFileType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(FileDataSource::getStatus, query.getStatus());
        }

        wrapper.orderByDesc(FileDataSource::getCreateTime);
        return fileDataSourceMapper.selectPage(page, wrapper);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FileDataSource dataSource = fileDataSourceMapper.selectById(id);
        if (dataSource == null) {
            return;
        }

        // 删除物理文件
        deleteFile(dataSource.getFilePath());

        // 删除数据表
        dropTable(dataSource.getTableName());

        // 删除字段记录
        fileFieldMapper.delete(new LambdaQueryWrapper<FileField>()
                .eq(FileField::getSourceId, id));

        // 删除数据源记录
        fileDataSourceMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(List<Long> ids) {
        ids.forEach(this::delete);
    }



    @Override
    public Map<String, Object> getData(String id, Integer pageNum, Integer pageSize) {
        // 获取数据源信息
        FileDataSource dataSource = this.getById(id);
        if (dataSource == null) {
            throw new BusinessException("数据源不存在");
        }

        // 获取字段信息
        List<FileField> fields = fileFieldMapper.selectList(
            new LambdaQueryWrapper<FileField>()
                .eq(FileField::getSourceId, id)
                .orderByAsc(FileField::getOrderNum)
        );
        JdbcTemplate jdbcTemplate = ((DataSourceServiceImpl) dataSourceService).getFileJdbcTemplate();
        // 计算分页偏移量
        int offset = (pageNum - 1) * pageSize;

        // 获取数据总数
        String countSql = String.format("SELECT COUNT(*) FROM %s", dataSource.getTableName());
        int total = jdbcTemplate.queryForObject(countSql, Integer.class);

        // 获取分页数据
        String dataSql = String.format("SELECT * FROM %s LIMIT %d, %d",
            dataSource.getTableName(), offset, pageSize);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(dataSql);

        // 构造返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("dataSource", dataSource);
        result.put("fields", fields);
        result.put("records", records);
        result.put("total", total);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateData(String id, List<Map<String, Object>> data) {
        FileDataSource dataSource = fileDataSourceMapper.selectById(id);
        if (dataSource == null) {
            throw new RuntimeException("数据源不存在");
        }

        // 清空原有数据
        JdbcUtils.truncateTable(dataSource.getTableName());

        // 获取字段信息
        List<FileField> fields = fileFieldMapper.selectList(
                new LambdaQueryWrapper<FileField>()
                        .eq(FileField::getSourceId, id)
                        .orderByAsc(FileField::getOrderNum)
        );

        // 插入新数据
        insertData(dataSource, fields, data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileDataSource uploadFile(MultipartFile file, boolean override) throws Exception {
        String fileName = file.getOriginalFilename();
        String fileType = getFileType(fileName);

        // 检查文件类型是否支持
        FileParser parser = fileParserMap.get(fileType.toLowerCase());
        if (parser == null) {
            throw new RuntimeException("不支持的文件类型：" + fileType);
        }

        // 1. 解析文件
        FileParseResult parseResult = parseFile(file, fileType);

        // 2. 获取表名
        String tableName = parseResult.getTableName();
        
        // 3. 开启事务
        TransactionStatus transaction = null;

        // 4. 保存文件数据源记录
        FileDataSource dataSource = new FileDataSource();
        dataSource.setFileName(fileName);
        dataSource.setFileType(fileType);
        dataSource.setFileSize(file.getSize());
        dataSource.setTableName(tableName);
        dataSource.setTableComment(parseResult.getTableComment());
        dataSource.setFieldCount(parseResult.getFields().size());
        dataSource.setDataCount(parseResult.getData().size());
        dataSource.setStatus(1);  // 已解析
        dataSource.setCreateTime(new Date());
        dataSource.setUpdateTime(new Date());

        fileDataSourceMapper.insert(dataSource);

        // 设置字段的数据源ID并保存字段信息
        saveFields(dataSource.getId(), parseResult.getFields());

        try {
            transaction = dataSourceService.beginTransaction();

            // 如果表已存在，则直接删除表，不管是否选择了覆盖选项
            if (dataSourceService.isTableExists(tableName)) {
                dropTable(tableName);
            }

            // 5. 在目标数据源创建表
            dataSourceService.createTable(
                    tableName,
                    parseResult.getTableComment(),
                    parseResult.getFields()
            );

            // 6. 导入数据
            dataSourceService.batchInsertData(
                    tableName,
                    parseResult.getFields(),
                    parseResult.getData()
            );

            // 7. 提交事务
            dataSourceService.commit(transaction);

            return dataSource;
        } catch (Exception e) {
            if (transaction != null) {
                dataSourceService.rollback(transaction);
            }
            throw e;
        }
    }

    /**
     * 解析文件
     */
    private FileParseResult parseFile(MultipartFile file, String fileType) throws Exception {
        String type = fileType.toLowerCase();
        if ("csv".equals(type)) {
            return parseTemplate(file);
        } else if ("xlsx".equals(type)) {
            return parseTemplate(file);
        } else if ("json".equals(type)) {
            return parseTemplate(file);
        } else if ("xml".equals(type)) {
            return parseTemplate(file);
        } else if ("txt".equals(type)) {
            return parseTemplate(file);
        } else if ("dmp".equals(type)) {
            return parseTemplate(file);
        } else if ("sql".equals(type)) {
            return parseTemplate(file);
        } else {
            throw new IllegalArgumentException("不支持的文件类型");
        }
    }

    /**
     * 生成新表名
     */
    private String generateNewTableName(String tableName) {
        int i = 1;
        String newTableName = tableName;
        while (dataSourceService.isTableExists(newTableName)) {
            newTableName = tableName + "_" + i++;
        }
        return newTableName;
    }

    /**
     * 获取文件类型
     */
    private String getFileType(String fileName) {
        if (fileName == null) return null;
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if ("csv".equals(extension)) {
            return "csv";
        } else if ("xlsx".equals(extension) || "xls".equals(extension)) {
            return "xlsx";
        } else if ("json".equals(extension)) {
            return "json";
        } else if ("xml".equals(extension)) {
            return "xml";
        } else if ("txt".equals(extension)) {
            return "txt";
        } else if ("dmp".equals(extension)) {
            return "dmp";
        } else if ("sql".equals(extension)) {
            return "sql";
        } else {
            return null;
        }
    }



    /**
     * 保存上传的文件
     */
    private String saveFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        String extension = fileName.substring(fileName.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString() + extension;

        // 确保目录存在
        File uploadDir = new File("uploads");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        File dest = new File(uploadDir, newFileName);
        file.transferTo(dest);
        return dest.getPath();
    }

    /**
     * 删除物理文件
     */
    private void deleteFile(String filePath) {
        if (StringUtils.isNotBlank(filePath)) {
            File file = new File(filePath);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    /**
     * 删除数据表
     */
    private void dropTable(String tableName) {
        if (StringUtils.isNotBlank(tableName)) {
            try {
                String sql = "DROP TABLE IF EXISTS " + tableName;
                JdbcTemplate jdbcTemplate = ((DataSourceServiceImpl) dataSourceService).getFileJdbcTemplate();
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                log.error("删除表失败: " + tableName, e);
            }
        }
    }

    /**
     * 创建数据表
     */
    private void createTable(FileDataSource dataSource, List<FileField> fields) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(dataSource.getTableName()).append(" (");

        for (int i = 0; i < fields.size(); i++) {
            FileField field = fields.get(i);
            sql.append(field.getFieldName())
                    .append(" ")
                    .append(field.getFieldType());

            if (field.getFieldLength() != null) {
                sql.append("(").append(field.getFieldLength()).append(")");
            }

            if (!field.getNullable()) {
                sql.append(" NOT NULL");
            }

            if (StringUtils.isNotBlank(field.getDescription())) {
                sql.append(" COMMENT '").append(field.getDescription()).append("'");
            }

            if (i < fields.size() - 1) {
                sql.append(", ");
            }
        }

        sql.append(")");

        if (StringUtils.isNotBlank(dataSource.getTableComment())) {
            sql.append(" COMMENT '").append(dataSource.getTableComment()).append("'");
        }

        JdbcUtils.execute(sql.toString());
    }

    /**
     * 批量插入数据
     */
    private void insertData(FileDataSource dataSource, List<FileField> fields, List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return;
        }

        // 构建插入SQL
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(dataSource.getTableName()).append(" (");

        // 添加字段名
        for (int i = 0; i < fields.size(); i++) {
            sql.append(fields.get(i).getFieldName());
            if (i < fields.size() - 1) {
                sql.append(", ");
            }
        }

        sql.append(") VALUES ");

        // 添加占位符
        String placeholder = "(";
        for (int i = 0; i < fields.size(); i++) {
            placeholder += "?";
            if (i < fields.size() - 1) {
                placeholder += ", ";
            }
        }
        placeholder += ")";

        // 批量添加数据
        int batchSize = 1000;
        List<Object[]> batchArgs = new ArrayList<>();

        for (Map<String, Object> row : data) {
            Object[] args = new Object[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                args[i] = row.get(fields.get(i).getFieldName());
            }
            batchArgs.add(args);

            // 达到批处理大小时执行
            if (batchArgs.size() >= batchSize) {
                JdbcUtils.batchUpdate(sql.toString() + placeholder, batchArgs);
                batchArgs.clear();
            }
        }

        // 处理剩余数据
        if (!batchArgs.isEmpty()) {
            JdbcUtils.batchUpdate(sql.toString() + placeholder, batchArgs);
        }
    }


    @Override
    public FileParseResult parseTemplate(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        String fileType = getFileType(fileName);

        // 获取对应的解析器
        FileParser parser = getFileParser(fileType);
        if (parser == null) {
            throw new RuntimeException("不支持的文件类型: " + fileType);
        }

        // 解析模板
        try (InputStream inputStream = file.getInputStream()) {
            return parser.parseTemplate(inputStream);
        } catch (Exception e) {
            log.error("解析模板文件失败", e);
            throw new RuntimeException("解析模板文件失败: " + e.getMessage());
        }
    }

    private FileParser getFileParser(String fileType) {
        return fileParsers.stream()
                .filter(parser -> parser.getFileType().equalsIgnoreCase(fileType))
                .findFirst()
                .orElse(null);
    }

    private void saveFields(String sourceId, List<FileField> fields) {
        if (fields == null || fields.isEmpty()) {
            log.warn("没有字段需要保存");
            return;
        }
        
        // 设置字段的顺序号
        for (int i = 0; i < fields.size(); i++) {
            FileField field = fields.get(i);
            if (field == null) {
                log.warn("跳过空字段，索引: {}", i);
                continue;
            }
            
            // 设置ID
            field.setId(UUID.randomUUID().toString());
            field.setSourceId(sourceId);
            field.setOrderNum(i + 1); // 设置orderNum，从1开始
            
            // 确保必要字段不为空
            if (field.getFieldName() == null) {
                field.setFieldName("field_" + (i + 1));
            }
            if (field.getFieldType() == null) {
                field.setFieldType("VARCHAR");
            }
            if (field.getNullable() == null) {
                field.setNullable(true);
            }
            
            // 保存字段
            try {
                fileFieldMapper.insert(field);
            } catch (Exception e) {
                log.error("保存字段失败: {}, 错误: {}", field.getFieldName(), e.getMessage());
                throw e;
            }
        }
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 获取文件总数
            long totalFiles = count();
            stats.put("totalFiles", totalFiles);
            
            // 获取解析成功率
            LambdaQueryWrapper<FileDataSource> successQuery = new LambdaQueryWrapper<>();
            successQuery.eq(FileDataSource::getStatus, 1);
            long successCount = count(successQuery);
            double successRate = totalFiles > 0 ? (successCount * 100.0 / totalFiles) : 0;
            stats.put("successRate", Math.round(successRate * 10) / 10.0);
            
            // 获取解析失败数
            LambdaQueryWrapper<FileDataSource> failedQuery = new LambdaQueryWrapper<>();
            failedQuery.eq(FileDataSource::getStatus, 2);
            long failedCount = count(failedQuery);
            stats.put("failedCount", failedCount);
            
            // 计算平均文件大小
            LambdaQueryWrapper<FileDataSource> wrapper = new LambdaQueryWrapper<>();
            List<FileDataSource> allFiles = list(wrapper);
            long totalSize = 0;
            for (FileDataSource file : allFiles) {
                totalSize += file.getFileSize();
            }
            double avgFileSize = totalFiles > 0 ? (totalSize / (totalFiles * 1024.0 * 1024.0)) : 0;
            stats.put("avgFileSize", Math.round(avgFileSize * 10) / 10.0);
            
            // 计算增长率（假设与一周前比较）
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -7);
            Date lastWeek = cal.getTime();
            
            LambdaQueryWrapper<FileDataSource> newFilesQuery = new LambdaQueryWrapper<>();
            newFilesQuery.ge(FileDataSource::getCreateTime, lastWeek);
            long newFilesCount = count(newFilesQuery);
            
            LambdaQueryWrapper<FileDataSource> oldFilesQuery = new LambdaQueryWrapper<>();
            oldFilesQuery.lt(FileDataSource::getCreateTime, lastWeek);
            long oldFilesCount = count(oldFilesQuery);
            
            double fileGrowthRate = oldFilesCount > 0 ? ((newFilesCount * 100.0) / oldFilesCount) - 100 : 0;
            stats.put("fileGrowthRate", Math.round(fileGrowthRate * 10) / 10.0);
            
            // 计算解析失败率的变化
            LambdaQueryWrapper<FileDataSource> newFailedQuery = new LambdaQueryWrapper<>();
            newFailedQuery.eq(FileDataSource::getStatus, 2).ge(FileDataSource::getCreateTime, lastWeek);
            long newFailedCount = count(newFailedQuery);
            
            LambdaQueryWrapper<FileDataSource> oldFailedQuery = new LambdaQueryWrapper<>();
            oldFailedQuery.eq(FileDataSource::getStatus, 2).lt(FileDataSource::getCreateTime, lastWeek);
            long oldFailedCount = count(oldFailedQuery);
            
            double failedChangeRate = 0;
            if (oldFailedCount > 0) {
                failedChangeRate = ((newFailedCount * 100.0) / oldFailedCount) - 100;
            } else if (newFailedCount > 0) {
                failedChangeRate = 100; // 如果过去没有失败记录，但现在有，增长率为100%
            }
            stats.put("failedChangeRate", Math.round(failedChangeRate * 10) / 10.0);
            
            // 计算平均文件大小的变化
            LambdaQueryWrapper<FileDataSource> newSizeQuery = new LambdaQueryWrapper<>();
            newSizeQuery.ge(FileDataSource::getCreateTime, lastWeek);
            List<FileDataSource> newFiles = list(newSizeQuery);
            
            long newTotalSize = 0;
            for (FileDataSource file : newFiles) {
                newTotalSize += file.getFileSize();
            }
            
            double newAvgSize = newFilesCount > 0 ? (newTotalSize / (newFilesCount * 1024.0 * 1024.0)) : 0;
            
            LambdaQueryWrapper<FileDataSource> oldSizeQuery = new LambdaQueryWrapper<>();
            oldSizeQuery.lt(FileDataSource::getCreateTime, lastWeek);
            List<FileDataSource> oldFiles = list(oldSizeQuery);
            
            long oldTotalSize = 0;
            for (FileDataSource file : oldFiles) {
                oldTotalSize += file.getFileSize();
            }
            
            double oldAvgSize = oldFilesCount > 0 ? (oldTotalSize / (oldFilesCount * 1024.0 * 1024.0)) : 0;
            
            double sizeChangeRate = oldAvgSize > 0 ? ((newAvgSize * 100.0) / oldAvgSize) - 100 : 0;
            stats.put("sizeChangeRate", Math.round(sizeChangeRate * 10) / 10.0);
            
        } catch (Exception e) {
            log.error("获取文件数据源统计信息失败", e);
        }
        
        return stats;
    }
}
