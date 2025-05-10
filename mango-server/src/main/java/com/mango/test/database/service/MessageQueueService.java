package com.mango.test.database.service;

import java.util.List;
import java.util.Map;

/**
 * 消息队列服务接口
 */
public interface MessageQueueService {

    /**
     * 获取主题列表
     */
    List<Map<String, Object>> getTopics(String datasourceId, String searchText) throws Exception;

    /**
     * 获取主题详情
     */
    Map<String, Object> getTopicDetails(String datasourceId, String topic) throws Exception;

    /**
     * 获取消费者组列表
     */
    List<Map<String, Object>> getConsumerGroups(String datasourceId, String topic) throws Exception;

    /**
     * 获取主题分区信息
     */
    List<Map<String, Object>> getTopicPartitions(String datasourceId, String topic) throws Exception;

    /**
     * 发送消息
     */
    boolean sendMessage(String datasourceId, String topic, Map<String, Object> message) throws Exception;

    /**
     * 消费消息
     */
    List<Map<String, Object>> consumeMessages(String datasourceId, String topic, Map<String, Object> options) throws Exception;

    /**
     * 浏览消息（不消费）
     */
    List<Map<String, Object>> browseMessages(String datasourceId, String topic, Map<String, Object> options) throws Exception;

    /**
     * 创建主题
     */
    boolean createTopic(String datasourceId, Map<String, Object> topicConfig) throws Exception;

    /**
     * 删除主题
     */
    boolean deleteTopic(String datasourceId, String topic) throws Exception;

    /**
     * 获取消费者组详情
     */
    Map<String, Object> getConsumerGroupDetails(String datasourceId, String groupId) throws Exception;

    /**
     * 创建消费者组
     */
    boolean createConsumerGroup(String datasourceId, Map<String, Object> groupConfig) throws Exception;

    /**
     * 删除消费者组
     */
    boolean deleteConsumerGroup(String datasourceId, String groupId) throws Exception;

    /**
     * 获取消息队列统计信息
     */
    Map<String, Object> getQueueStats(String datasourceId) throws Exception;

    /**
     * 重置消费者组偏移量
     */
    boolean resetConsumerGroupOffsets(String datasourceId, Map<String, Object> request) throws Exception;
} 