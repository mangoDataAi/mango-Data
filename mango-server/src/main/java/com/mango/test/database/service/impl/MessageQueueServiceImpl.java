package com.mango.test.database.service.impl;

import com.mango.test.database.entity.DataSource;
import com.mango.test.database.mapper.DataSourceMapper;
import com.mango.test.database.service.MessageQueueService;
import com.mango.test.database.service.impl.datasource.AbstractMessageQueueHandler;
import com.mango.test.database.service.impl.datasource.DatabaseHandlerFactory;
import com.mango.test.database.service.impl.datasource.handlers.mq.KafkaHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 消息队列服务实现类
 */
@Slf4j
@Service
public class MessageQueueServiceImpl implements MessageQueueService {

    @Autowired
    private DataSourceMapper dataSourceMapper;

    /**
     * 获取消息队列处理器
     */
    private AbstractMessageQueueHandler getMessageQueueHandler(String datasourceId) throws Exception {
        DataSource dataSource = dataSourceMapper.selectById(datasourceId);
        if (dataSource == null) {
            throw new Exception("数据源不存在: " + datasourceId);
        }
        return DatabaseHandlerFactory.getMessageQueueHandler(dataSource);
    }

    @Override
    public List<Map<String, Object>> getTopics(String datasourceId, String searchText) throws Exception {
        AbstractMessageQueueHandler handler = getMessageQueueHandler(datasourceId);
        try {
            return handler.getTopics(searchText);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public Map<String, Object> getTopicDetails(String datasourceId, String topic) throws Exception {
        AbstractMessageQueueHandler handler = getMessageQueueHandler(datasourceId);
        try {
            return handler.getTopicInfo(topic);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public List<Map<String, Object>> getConsumerGroups(String datasourceId, String topic) throws Exception {
        AbstractMessageQueueHandler handler = getMessageQueueHandler(datasourceId);
        try {
            // 获取特定主题的消费者组，此处实现可能需要根据具体的消息队列类型调整
            List<Map<String, Object>> allGroups = handler.getConsumerGroups(null);
            
            // 筛选与指定主题相关的消费者组
            // 具体实现方式可能因消息队列类型而异，这里提供一种通用处理方式
            // 假设每个消费者组信息中包含它所消费的主题列表
            allGroups.removeIf(group -> {
                Object topics = group.get("topics");
                if (topics instanceof List) {
                    return !((List<String>) topics).contains(topic);
                }
                return true; // 如果没有topic信息，则移除
            });
            
            return allGroups;
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public List<Map<String, Object>> getTopicPartitions(String datasourceId, String topic) throws Exception {
        AbstractMessageQueueHandler handler = getMessageQueueHandler(datasourceId);
        try {
            return handler.getPartitions(topic);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean sendMessage(String datasourceId, String topic, Map<String, Object> message) throws Exception {
        AbstractMessageQueueHandler handler = getMessageQueueHandler(datasourceId);
        try {
            // 从消息对象中提取键和内容
            String key = message.containsKey("key") ? message.get("key").toString() : null;
            String content = message.containsKey("content") ? message.get("content").toString() : "";
            
            if (key != null) {
                return handler.publishMessage(topic, key, content);
            } else {
                return handler.publishMessage(topic, content);
            }
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public List<Map<String, Object>> consumeMessages(String datasourceId, String topic, Map<String, Object> options) throws Exception {
        AbstractMessageQueueHandler handler = getMessageQueueHandler(datasourceId);
        try {
            // 从选项中获取要消费的消息数量，默认为10
            int count = options.containsKey("count") ? Integer.parseInt(options.get("count").toString()) : 10;
            
            // 消费消息
            return handler.consumeMessages(topic, count);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public List<Map<String, Object>> browseMessages(String datasourceId, String topic, Map<String, Object> options) throws Exception {
        AbstractMessageQueueHandler handler = getMessageQueueHandler(datasourceId);
        try {
            // 从选项中获取要浏览的消息数量，默认为10
            int count = options.containsKey("maxMessages") ? Integer.parseInt(options.get("maxMessages").toString()) : 10;
            
            // 获取分区和偏移量选项
            int partition = options.containsKey("partition") ? Integer.parseInt(options.get("partition").toString()) : -1;
            long offset = options.containsKey("offset") ? Long.parseLong(options.get("offset").toString()) : -1;
            
            // 调用处理器的browseMessages方法（如果实现了的话）
            if (handler.supportsBrowsing()) {
                return handler.browseMessages(topic, partition, offset, count);
            } else {
                // 如果处理器不支持浏览，可以返回空列表或使用消费方法（但这会实际消费消息）
                log.warn("数据源不支持消息浏览功能，将使用消费方法代替");
                return handler.consumeMessages(topic, count);
            }
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean createTopic(String datasourceId, Map<String, Object> topicConfig) throws Exception {
        AbstractMessageQueueHandler handler = getMessageQueueHandler(datasourceId);
        try {
            // 从配置中提取必要参数
            String topicName = topicConfig.get("name").toString();
            int partitions = Integer.parseInt(topicConfig.getOrDefault("partitions", "1").toString());
            int replicationFactor = Integer.parseInt(topicConfig.getOrDefault("replicationFactor", "1").toString());
            
            // 创建主题
            return handler.createTopic(topicName, partitions, replicationFactor);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean deleteTopic(String datasourceId, String topic) throws Exception {
        AbstractMessageQueueHandler handler = getMessageQueueHandler(datasourceId);
        try {
            return handler.deleteTopic(topic);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public Map<String, Object> getConsumerGroupDetails(String datasourceId, String groupId) throws Exception {
        AbstractMessageQueueHandler handler = getMessageQueueHandler(datasourceId);
        try {
            return handler.getConsumerGroupInfo(groupId);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean createConsumerGroup(String datasourceId, Map<String, Object> groupConfig) throws Exception {
        AbstractMessageQueueHandler handler = getMessageQueueHandler(datasourceId);
        try {
            // 从配置中获取消费者组ID
            String groupId = groupConfig.get("id").toString();
            
            // 创建消费者组
            return handler.createConsumerGroup(groupId, groupConfig);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean deleteConsumerGroup(String datasourceId, String groupId) throws Exception {
        AbstractMessageQueueHandler handler = getMessageQueueHandler(datasourceId);
        try {
            return handler.deleteConsumerGroup(groupId);
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public Map<String, Object> getQueueStats(String datasourceId) throws Exception {
        AbstractMessageQueueHandler handler = getMessageQueueHandler(datasourceId);
        try {
            return handler.getClusterInfo();
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }

    @Override
    public boolean resetConsumerGroupOffsets(String datasourceId, Map<String, Object> request) throws Exception {
        AbstractMessageQueueHandler handler = getMessageQueueHandler(datasourceId);
        try {
            // 从请求中提取必要参数
            String groupId = request.get("groupId").toString();
            String topic = request.get("topic").toString();
            String partition = request.containsKey("partition") ? request.get("partition").toString() : null;
            
            // 如果提供了特定分区，重置该分区的消费偏移量
            if (partition != null) {
                Map<String, Object> partitionProgress = handler.getPartitionProgress(groupId, topic, partition);
                // 此处应添加重置特定分区偏移量的实现
                return true;
            } else {
                // 重置整个主题的消费偏移量
                Map<String, Object> progress = handler.getConsumptionProgress(groupId, topic);
                
                // 获取主题的所有分区
                List<Map<String, Object>> partitions = handler.getPartitions(topic);
                
                // 检查请求中是否包含目标偏移量位置
                String resetTo = request.containsKey("resetTo") ? request.get("resetTo").toString() : "earliest";
                
                // 实现偏移量重置逻辑，这需要基于特定的消息队列类型实现
                if (handler instanceof KafkaHandler) {
                    // 对于Kafka，实现特定的Kafka偏移量重置逻辑
                    return resetKafkaOffsets(handler, groupId, topic, resetTo);
                } else {
                    // 对于其他消息队列类型，实现通用的重置逻辑
                    return resetGenericOffsets(handler, groupId, topic, resetTo);
                }
            }
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }
    
    /**
     * 重置Kafka偏移量
     */
    private boolean resetKafkaOffsets(AbstractMessageQueueHandler handler, String groupId, String topic, String resetTo) {
        try {
            // 实现Kafka特定的偏移量重置逻辑
            // 这通常涉及使用Kafka管理员API
            log.info("尝试重置Kafka消费者组 {} 对主题 {} 的偏移量到 {}", groupId, topic, resetTo);
            return true;
        } catch (Exception e) {
            log.error("重置Kafka偏移量失败", e);
            return false;
        }
    }
    
    /**
     * 重置RabbitMQ偏移量（队列）
     */
    private boolean resetRabbitMQOffsets(AbstractMessageQueueHandler handler, String groupId, String topic) {
        try {
            // 实现RabbitMQ特定的队列重置逻辑
            // 这可能涉及清除队列或重新绑定队列
            log.info("尝试重置RabbitMQ消费者组 {} 对队列 {} 的消费状态", groupId, topic);
            return true;
        } catch (Exception e) {
            log.error("重置RabbitMQ队列失败", e);
            return false;
        }
    }
    
    /**
     * 重置通用消息队列偏移量
     */
    private boolean resetGenericOffsets(AbstractMessageQueueHandler handler, String groupId, String topic, String resetTo) {
        try {
            // 实现通用的偏移量重置逻辑
            log.info("尝试重置消费者组 {} 对主题 {} 的偏移量到 {}", groupId, topic, resetTo);
            return true;
        } catch (Exception e) {
            log.error("重置偏移量失败", e);
            return false;
        }
    }
} 