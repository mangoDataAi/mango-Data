package com.mango.test.database.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mango.test.database.entity.ApiDataSource;
import java.util.Map;

public interface ApiDataSourceService extends IService<ApiDataSource> {
    
    /**
     * 测试接口连接
     */
    Map<String, Object> testConnection(ApiDataSource apiDataSource) throws Exception;

    /**
     * 预览接口数据
     */
    Map<String, Object> previewData(Long id) throws Exception;
    
    /**
     * 预览接口数据 (使用字符串ID)
     */
    Map<String, Object> previewData(String id) throws Exception;
    
    /**
     * 获取平均响应时间
     * @return 平均响应时间（秒）
     */
    double getAverageResponseTime();
    
    /**
     * 获取响应时间变化率
     * @return 响应时间变化率（百分比）
     */
    double getResponseTimeChangeRate();
} 