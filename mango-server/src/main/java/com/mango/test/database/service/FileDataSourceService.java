package com.mango.test.database.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.database.entity.FileDataSource;
import com.mango.test.database.model.FileParseResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface FileDataSourceService extends IService<FileDataSource> {

    /**
     * 分页查询
     */
    IPage<FileDataSource> page(Page<FileDataSource> page, FileDataSource query);


    /**
     * 删除数据源
     */
    void delete(Long id);

    /**
     * 批量删除
     */
    void deleteBatch(List<Long> ids);


    /**
     * 获取数据源数据
     * @param id 数据源ID
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @return 包含数据源信息、字段信息和数据的Map
     */
    Map<String, Object> getData(String id, Integer pageNum, Integer pageSize);

    /**
     * 更新数据
     */
    void updateData(String id, List<Map<String, Object>> data);


    FileDataSource uploadFile(MultipartFile file, boolean override) throws Exception;

    /**
     * 解析模板文件
     */
    FileParseResult parseTemplate(MultipartFile file) throws Exception;

    /**
     * 获取文件数据源统计信息
     * @return 包含统计信息的Map
     */
    Map<String, Object> getStatistics();
}
