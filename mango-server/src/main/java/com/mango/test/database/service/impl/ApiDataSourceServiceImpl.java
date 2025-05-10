package com.mango.test.database.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mango.test.database.entity.ApiDataSource;
import com.mango.test.database.mapper.ApiDataSourceMapper;
import com.mango.test.database.service.ApiDataSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.Calendar;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Slf4j
@Service
public class ApiDataSourceServiceImpl extends ServiceImpl<ApiDataSourceMapper, ApiDataSource> 
    implements ApiDataSourceService {

    @Autowired
    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取平均响应时间
     * 这里模拟了一个实现，实际项目中应该从数据库中查询API调用记录的平均响应时间
     * @return 平均响应时间（秒）
     */
    @Override
    public double getAverageResponseTime() {
        try {
            // 在实际项目中，这里应该是从数据库查询API调用记录表中的平均响应时间
            // 这里为了演示，返回一个随机值，实际项目请替换为真实实现
            // return apiCallLogMapper.selectAverageResponseTime();
            
            // 模拟实现，返回一个1-3之间的随机值，单位为秒
            return 1 + Math.random() * 2;
        } catch (Exception e) {
            log.error("获取平均响应时间失败", e);
            // 出错时返回默认值
            return 1.2;
        }
    }
    
    /**
     * 获取响应时间变化率
     * 比较最近一周的平均响应时间与之前的差异
     * @return 响应时间变化率（百分比）
     */
    @Override
    public double getResponseTimeChangeRate() {
        try {
            // 在实际项目中，这里应该是从数据库查询并比较两个时间段的平均响应时间
            // 例如比较最近一周的平均响应时间与前一周的平均响应时间
            // 这里为了演示，返回一个-15到5之间的随机值，负值表示响应时间减少（变快）
            
            // double currentAvgTime = apiCallLogMapper.selectRecentAverageResponseTime(7); // 最近7天
            // double previousAvgTime = apiCallLogMapper.selectPreviousAverageResponseTime(7, 14); // 7-14天前
            // return ((currentAvgTime - previousAvgTime) / previousAvgTime) * 100;
            
            // 模拟实现
            return -15 + Math.random() * 20;
        } catch (Exception e) {
            log.error("获取响应时间变化率失败", e);
            // 出错时返回默认值
            return -8.3;
        }
    }

    private String getHttpErrorMessage(int statusCode) {
        switch (statusCode) {
            case 400:
                return "请求参数错误";
            case 401:
                return "未经授权，请检查认证信息";
            case 403:
                return "访问被禁止，请检查访问权限";
            case 404:
                return "请求的资源不存在";
            case 500:
                return "服务器内部错误";
            case 502:
                return "网关错误";
            case 503:
                return "服务暂时不可用";
            case 504:
                return "网关超时";
            default:
                return "HTTP错误: " + statusCode;
        }
    }

    @Override
    public Map<String, Object> testConnection(ApiDataSource apiDataSource) throws Exception {
        try {
            // 处理URL
            String url = apiDataSource.getUrl();
            if (url == null || url.trim().isEmpty()) {
                throw new Exception("URL不能为空");
            }
            url = url.trim();
            
            // 验证URL格式
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                throw new Exception("URL格式不正确: " + e.getMessage());
            }

            HttpHeaders headers = new HttpHeaders();
            if (apiDataSource.getHeaders() != null && !apiDataSource.getHeaders().isEmpty()) {
                try {
                    List<Map<String, String>> headersList = objectMapper.readValue(
                        apiDataSource.getHeaders(),
                        new TypeReference<List<Map<String, String>>>() {}
                    );
                    
                    headersList.forEach(header -> {
                        if (header.containsKey("key") && header.containsKey("value")) {
                            headers.add(header.get("key"), header.get("value"));
                        }
                    });
                } catch (Exception e) {
                    log.warn("解析请求头失败: {}", e.getMessage());
                }
            }

            HttpEntity<?> entity;
            if (apiDataSource.getRequestBody() != null && !apiDataSource.getMethod().equals("GET")) {
                entity = new HttpEntity<>(apiDataSource.getRequestBody(), headers);
            } else {
                entity = new HttpEntity<>(headers);
            }

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.valueOf(apiDataSource.getMethod()),
                entity,
                String.class
            );

            Map<String, Object> result = new HashMap<>();
            result.put("success", response.getStatusCode().is2xxSuccessful());
            result.put("status", response.getStatusCodeValue());
            result.put("headers", response.getHeaders());
            result.put("body", response.getBody());
            
            return result;
        } catch (Exception e) {
            log.error("测试接口连接失败", e);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return result;
        }
    }

    @Override
    public Map<String, Object> previewData(Long id) throws Exception {
        ApiDataSource apiDataSource = this.getById(id);
        if (apiDataSource == null) {
            throw new Exception("数据源不存在");
        }

        try {
            // 处理URL
            String url = apiDataSource.getUrl();
            if (url == null || url.trim().isEmpty()) {
                throw new Exception("URL不能为空");
            }
            url = url.trim();
            
            // 验证URL格式
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                throw new Exception("URL格式不正确: " + e.getMessage());
            }

            HttpHeaders headers = new HttpHeaders();
            if (apiDataSource.getHeaders() != null && !apiDataSource.getHeaders().isEmpty()) {
                try {
                    List<Map<String, String>> headersList = objectMapper.readValue(
                        apiDataSource.getHeaders(),
                        new TypeReference<List<Map<String, String>>>() {}
                    );
                    
                    headersList.forEach(header -> {
                        if (header.containsKey("key") && header.containsKey("value")) {
                            headers.add(header.get("key"), header.get("value"));
                        }
                    });
                } catch (Exception e) {
                    log.warn("解析请求头失败: {}", e.getMessage());
                }
            }

            HttpEntity<?> entity;
            if (apiDataSource.getRequestBody() != null && !apiDataSource.getMethod().equals("GET")) {
                entity = new HttpEntity<>(apiDataSource.getRequestBody(), headers);
            } else {
                entity = new HttpEntity<>(headers);
            }

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.valueOf(apiDataSource.getMethod()),
                    entity,
                    String.class
                );

                Map<String, Object> result = new HashMap<>();
                result.put("status", response.getStatusCodeValue());
                result.put("headers", response.getHeaders());
                result.put("body", response.getBody());

                return result;
            } catch (ResourceAccessException e) {
                String message = e.getMessage();
                if (message.contains("Connection refused")) {
                    throw new Exception("无法连接到服务器，请检查URL是否正确或服务是否可用");
                } else if (message.contains("Read timed out")) {
                    throw new Exception("请求超时，请检查网络连接或稍后重试");
                } else {
                    throw new Exception("网络连接错误: " + e.getMessage());
                }
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                throw new Exception(getHttpErrorMessage(e.getRawStatusCode()));
            }
        } catch (Exception e) {
            log.error("预览接口数据失败", e);
            throw new Exception("预览接口数据失败：" + e.getMessage());
        }
    }

    /**
     * 预览接口数据 (使用字符串ID)
     */
    @Override
    public Map<String, Object> previewData(String id) throws Exception {
        try {
            ApiDataSource apiDataSource;
            try {
                // 尝试将字符串ID转为Long类型
                Long longId = Long.parseLong(id);
                apiDataSource = this.getById(longId);
            } catch (NumberFormatException e) {
                // 如果ID不是数字格式（例如UUID格式），则使用自定义查询方法
                apiDataSource = this.lambdaQuery()
                    .eq(ApiDataSource::getId, id)
                    .one();
                
                if (apiDataSource == null) {
                    log.error("未找到ID为{}的API数据源", id);
                    throw new Exception("数据源不存在");
                }
            }
            
            if (apiDataSource == null) {
                throw new Exception("数据源不存在");
            }

            // 处理URL
            String url = apiDataSource.getUrl();
            if (url == null || url.trim().isEmpty()) {
                throw new Exception("URL不能为空");
            }
            url = url.trim();
            
            // 验证URL格式
            try {
                new URL(url);
            } catch (MalformedURLException e) {
                throw new Exception("URL格式不正确: " + e.getMessage());
            }

            HttpHeaders headers = new HttpHeaders();
            if (apiDataSource.getHeaders() != null && !apiDataSource.getHeaders().isEmpty()) {
                try {
                    List<Map<String, String>> headersList = objectMapper.readValue(
                        apiDataSource.getHeaders(),
                        new TypeReference<List<Map<String, String>>>() {}
                    );
                    
                    headersList.forEach(header -> {
                        if (header.containsKey("key") && header.containsKey("value")) {
                            headers.add(header.get("key"), header.get("value"));
                        }
                    });
                } catch (Exception e) {
                    log.warn("解析请求头失败: {}", e.getMessage());
                }
            }

            HttpEntity<?> entity;
            if (apiDataSource.getRequestBody() != null && !apiDataSource.getMethod().equals("GET")) {
                entity = new HttpEntity<>(apiDataSource.getRequestBody(), headers);
            } else {
                entity = new HttpEntity<>(headers);
            }

            try {
                ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.valueOf(apiDataSource.getMethod()),
                    entity,
                    String.class
                );

                Map<String, Object> result = new HashMap<>();
                result.put("status", response.getStatusCodeValue());
                result.put("headers", response.getHeaders());
                result.put("body", response.getBody());

                return result;
            } catch (ResourceAccessException e) {
                String message = e.getMessage();
                if (message.contains("Connection refused")) {
                    throw new Exception("无法连接到服务器，请检查URL是否正确或服务是否可用");
                } else if (message.contains("Read timed out")) {
                    throw new Exception("请求超时，请检查网络连接或稍后重试");
                } else {
                    throw new Exception("网络连接错误: " + e.getMessage());
                }
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                throw new Exception(getHttpErrorMessage(e.getRawStatusCode()));
            }
        } catch (Exception e) {
            log.error("预览接口数据失败", e);
            throw new Exception("预览接口数据失败：" + e.getMessage());
        }
    }
} 