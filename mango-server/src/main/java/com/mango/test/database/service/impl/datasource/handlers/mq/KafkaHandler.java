package com.mango.test.database.service.impl.datasource.handlers.mq;

import com.mango.test.database.entity.DataSource;
import com.mango.test.database.service.impl.datasource.AbstractMessageQueueHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Kafka数据源处理器
 */
@Slf4j
public class KafkaHandler extends AbstractMessageQueueHandler {
    
    private AdminClient adminClient;
    private KafkaProducer<String, String> producer;
    private KafkaConsumer<String, String> consumer;
    private static final String KAFKA_DRIVER = "org.apache.kafka.clients.admin.AdminClient";
    
    public KafkaHandler(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * 初始化Kafka客户端
     */
    private synchronized boolean initConnection() {
        try {
            if (dataSource != null) {
                // 先关闭已有连接
                closeConnection();
                
                // 检查是否有有效的用户名和密码
                boolean hasAuth = StringUtils.hasText(dataSource.getUsername()) &&
                        StringUtils.hasText(dataSource.getPassword());

                // 初始化AdminClient
                Properties adminProps = new Properties();
                adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
                adminProps.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000");
                adminProps.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "10000");

                if (hasAuth) {
                    // 有认证信息，配置SASL
                    adminProps.put("sasl.jaas.config",
                            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" +
                                    dataSource.getUsername() + "\" password=\"" +
                                    dataSource.getPassword() + "\";");
                    adminProps.put("sasl.mechanism", "PLAIN");
                    adminProps.put("security.protocol", "SASL_PLAINTEXT");
                } else {
                    // 明确指定使用PLAINTEXT协议
                    adminProps.put("security.protocol", "PLAINTEXT");
                }

                adminClient = AdminClient.create(adminProps);

                // 初始化Producer
                Properties producerProps = new Properties();
                producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
                producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                producerProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000");
                producerProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "15000");

                if (hasAuth) {
                    producerProps.put("sasl.jaas.config",
                            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" +
                                    dataSource.getUsername() + "\" password=\"" +
                                    dataSource.getPassword() + "\";");
                    producerProps.put("sasl.mechanism", "PLAIN");
                    producerProps.put("security.protocol", "SASL_PLAINTEXT");
                } else {
                    producerProps.put("security.protocol", "PLAINTEXT");
                }

                producer = new KafkaProducer<>(producerProps);

                // 初始化Consumer
                Properties consumerProps = new Properties();
                consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
                consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, getGroupId());
                consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
                consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
                consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
                consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
                consumerProps.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000");
                consumerProps.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "10000");

                if (hasAuth) {
                    consumerProps.put("sasl.jaas.config",
                            "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" +
                                    dataSource.getUsername() + "\" password=\"" +
                                    dataSource.getPassword() + "\";");
                    consumerProps.put("sasl.mechanism", "PLAIN");
                    consumerProps.put("security.protocol", "SASL_PLAINTEXT");
                } else {
                    consumerProps.put("security.protocol", "PLAINTEXT");
                }

                consumer = new KafkaConsumer<>(consumerProps);
                
                return true;
            }
        } catch (Exception e) {
            log.error("初始化Kafka处理器失败: {}", e.getMessage(), e);
            closeConnection();
        }
        return false;
    }
    
    /**
     * 关闭Kafka连接
     */
    private synchronized void closeConnection() {
        // 关闭Consumer
        if (consumer != null) {
            try {
                consumer.close(Duration.ofSeconds(5));
            } catch (Exception e) {
                log.error("关闭Kafka Consumer失败: {}", e.getMessage(), e);
            } finally {
                consumer = null;
            }
        }
        
        // 关闭Producer
        if (producer != null) {
            try {
                producer.close(Duration.ofSeconds(5));
            } catch (Exception e) {
                log.error("关闭Kafka Producer失败: {}", e.getMessage(), e);
            } finally {
                producer = null;
            }
        }
        
        // 关闭AdminClient
        if (adminClient != null) {
            try {
                adminClient.close(Duration.ofSeconds(5));
            } catch (Exception e) {
                log.error("关闭Kafka AdminClient失败: {}", e.getMessage(), e);
            } finally {
                adminClient = null;
            }
        }
    }
    
    private String getBootstrapServers() {
        if (StringUtils.hasText(dataSource.getBootstrapServers())) {
            return dataSource.getBootstrapServers();
        } else {
            return dataSource.getHost() + ":" + getPort();
        }
    }
    
    private String getGroupId() {
        if (StringUtils.hasText(dataSource.getGroupId())) {
            return dataSource.getGroupId();
        } else {
            return "mango-consumer-group";
        }
    }
    
    private String getPort() {
        if (StringUtils.hasText(dataSource.getPort())) {
            return dataSource.getPort();
        } else {
            return getDefaultPort();
        }
    }

    @Override
    public boolean testConnection() {
        try {
            Properties props = new Properties();
            props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getBootstrapServers());
            props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
            props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "5000");

            // 如果有用户名密码，添加认证配置
            if (StringUtils.hasText(dataSource.getUsername()) && 
                StringUtils.hasText(dataSource.getPassword())) {
                props.put("security.protocol", "SASL_PLAINTEXT");
                props.put("sasl.mechanism", "PLAIN");
                props.put("sasl.jaas.config",
                        "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                                "username=\"" + dataSource.getUsername() + "\" " +
                                "password=\"" + dataSource.getPassword() + "\";");
            } else {
                props.put("security.protocol", "PLAINTEXT");
            }
            
            AdminClient testAdminClient = null;
            try {
                testAdminClient = AdminClient.create(props);
                ListTopicsResult topics = testAdminClient.listTopics();
                topics.names().get(5, TimeUnit.SECONDS); // 等待5秒获取topic列表
                return true;
            } finally {
                if (testAdminClient != null) {
                    testAdminClient.close(Duration.ofSeconds(5));
                }
            }
        } catch (Exception e) {
            log.error("测试Kafka连接失败: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public List<Map<String, Object>> getTopics(String pattern) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            if (!initConnection()) {
                return result;
            }
            
            try {
                Set<String> topicNames = adminClient.listTopics().names().get(10, TimeUnit.SECONDS);
                
                // 过滤主题
                if (pattern != null && !pattern.isEmpty()) {
                    topicNames = topicNames.stream()
                        .filter(name -> name.contains(pattern))
                        .collect(Collectors.toSet());
                }
                
                // 获取主题详细信息
                Map<String, TopicDescription> topicDescriptionMap = 
                    adminClient.describeTopics(topicNames).all().get(10, TimeUnit.SECONDS);
                
                for (Map.Entry<String, TopicDescription> entry : topicDescriptionMap.entrySet()) {
                    String topicName = entry.getKey();
                    TopicDescription description = entry.getValue();
                    
                    Map<String, Object> topicInfo = new HashMap<>();
                    topicInfo.put("id", topicName);
                    topicInfo.put("name", topicName);
                    topicInfo.put("partitions", description.partitions().size());
                    
                    // 获取副本因子
                    if (!description.partitions().isEmpty()) {
                        int replicationFactor = description.partitions().get(0).replicas().size();
                        topicInfo.put("replicationFactor", replicationFactor);
                    } else {
                        topicInfo.put("replicationFactor", 0);
                    }
                    
                    // 添加是否内部主题标记
                    topicInfo.put("internal", description.isInternal());
                    
                    result.add(topicInfo);
                }
            } catch (Exception e) {
                log.error("获取Kafka主题列表失败: {}", e.getMessage(), e);
            }
        } finally {
            closeConnection();
        }
        return result;
    }

    @Override
    public boolean publishMessage(String topic, String message) {
        return publishMessage(topic, null, message);
    }

    @Override
    public boolean publishMessage(String topic, String key, String message) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            try {
                ProducerRecord<String, String> record;
                if (key != null) {
                    record = new ProducerRecord<>(topic, key, message);
                } else {
                    record = new ProducerRecord<>(topic, message);
                }
                
                Future<RecordMetadata> future = producer.send(record);
                producer.flush();
                RecordMetadata metadata = future.get(10, TimeUnit.SECONDS);
                log.info("消息发送成功: 主题={}, 分区={}, 偏移量={}", topic, metadata.partition(), metadata.offset());
                return true;
            } catch (Exception e) {
                log.error("发布Kafka消息失败: {}", e.getMessage(), e);
            }
        } finally {
            closeConnection();
        }
        return false;
    }

    @Override
    public List<Map<String, Object>> consumeMessages(String topic, int count) {
        List<Map<String, Object>> messages = new ArrayList<>();
        try {
            if (!initConnection()) {
                return messages;
            }
            
            try {
                consumer.subscribe(Collections.singletonList(topic));
                
                // 尝试消费指定数量的消息
                int remainingCount = count;
                int retryCount = 3; // 最多尝试3次
                
                while (remainingCount > 0 && retryCount > 0) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                    
                    if (records.isEmpty()) {
                        retryCount--;
                        continue;
                    }
                    
                    for (ConsumerRecord<String, String> record : records) {
                        Map<String, Object> messageInfo = new HashMap<>();
                        messageInfo.put("topic", record.topic());
                        messageInfo.put("partition", record.partition());
                        messageInfo.put("offset", record.offset());
                        messageInfo.put("key", record.key());
                        messageInfo.put("value", record.value());
                        messageInfo.put("timestamp", record.timestamp());
                        
                        messages.add(messageInfo);
                        
                        remainingCount--;
                        if (remainingCount <= 0) {
                            break;
                        }
                    }
                }
                
                // 手动提交偏移量
                consumer.commitSync();
                // 取消订阅
                consumer.unsubscribe();
            } catch (Exception e) {
                log.error("消费Kafka消息失败: {}", e.getMessage(), e);
            }
        } finally {
            closeConnection();
        }
        return messages;
    }

    @Override
    public Map<String, Object> getTopicInfo(String topic) {
        Map<String, Object> info = new HashMap<>();
        
        try {
            AdminClient adminClient = getAdminClient();
            if (adminClient == null) {
                log.error("获取Kafka AdminClient失败");
                return info;
            }
            
            // 获取主题详情
            Map<String, TopicDescription> topicInfo = getTopicDescriptions(adminClient, Collections.singletonList(topic));
            TopicDescription topicDescription = topicInfo.get(topic);
            
            if (topicDescription != null) {
                info.put("name", topicDescription.name());
                info.put("partitions", topicDescription.partitions().size());
                info.put("isInternal", topicDescription.isInternal());
                
                // 获取主题配置
                ConfigResource configResource = new ConfigResource(ConfigResource.Type.TOPIC, topic);
                DescribeConfigsResult configsResult = adminClient.describeConfigs(Collections.singleton(configResource));
                Config config = configsResult.all().get(10, TimeUnit.SECONDS).get(configResource);
                
                Map<String, String> configs = new HashMap<>();
                for (ConfigEntry entry : config.entries()) {
                    configs.put(entry.name(), entry.value());
                }
                info.put("config", configs);
                
                // 获取消费者组
                List<Map<String, Object>> consumerGroups = new ArrayList<>();
                ListConsumerGroupsResult groupsResult = adminClient.listConsumerGroups();
                Collection<ConsumerGroupListing> groupListings = groupsResult.all().get(10, TimeUnit.SECONDS);
                
                for (ConsumerGroupListing groupListing : groupListings) {
                    try {
                        String groupId = groupListing.groupId();
                        Map<String, Object> offsets = getConsumptionProgress(groupId, topic);
                        
                        if (offsets != null && !offsets.isEmpty()) {
                            Map<String, Object> groupInfo = new HashMap<>();
                            groupInfo.put("groupId", groupId);
                            groupInfo.put("offsets", offsets);
                            consumerGroups.add(groupInfo);
                        }
                    } catch (Exception e) {
                        log.debug("获取消费者组 {} 的偏移量失败: {}", groupListing.groupId(), e.getMessage());
                    }
                }
                
                info.put("consumerGroups", consumerGroups);
            }
        } catch (Exception e) {
            log.error("获取Kafka主题信息失败: {}", e.getMessage(), e);
        }
        
        return info;
    }

    @Override
    public List<Map<String, Object>> getPartitions(String topic) {
        List<Map<String, Object>> partitions = new ArrayList<>();
        
        try {
            AdminClient adminClient = getAdminClient();
            if (adminClient == null) {
                log.error("获取Kafka AdminClient失败");
                return partitions;
            }
            
            // 获取主题详情
            Map<String, TopicDescription> topicInfo = getTopicDescriptions(adminClient, Collections.singletonList(topic));
            TopicDescription topicDescription = topicInfo.get(topic);
            
            if (topicDescription != null) {
                List<TopicPartitionInfo> partitionInfos = topicDescription.partitions();
                
                for (TopicPartitionInfo partitionInfo : partitionInfos) {
                    Map<String, Object> partition = new HashMap<>();
                    partition.put("id", partitionInfo.partition());
                    partition.put("leader", partitionInfo.leader() != null ? partitionInfo.leader().id() : null);
                    
                    List<Integer> replicas = new ArrayList<>();
                    for (Node replica : partitionInfo.replicas()) {
                        replicas.add(replica.id());
                    }
                    partition.put("replicas", replicas);
                    
                    List<Integer> isr = new ArrayList<>();
                    for (Node isrNode : partitionInfo.isr()) {
                        isr.add(isrNode.id());
                    }
                    partition.put("isr", isr);
                    
                    // 获取分区的开始offset和结束offset
                    try {
                        Properties props = new Properties();
                        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaServers());
                        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props, new StringDeserializer(), new StringDeserializer())) {
                            TopicPartition topicPartition = new TopicPartition(topic, partitionInfo.partition());
                            Map<TopicPartition, Long> beginningOffsets = consumer.beginningOffsets(Collections.singletonList(topicPartition));
                            Map<TopicPartition, Long> endOffsets = consumer.endOffsets(Collections.singletonList(topicPartition));
                            
                            partition.put("beginningOffset", beginningOffsets.get(topicPartition));
                            partition.put("endOffset", endOffsets.get(topicPartition));
                            partition.put("messageCount", endOffsets.get(topicPartition) - beginningOffsets.get(topicPartition));
                        }
                    } catch (Exception e) {
                        log.debug("获取分区 {} 的偏移量失败: {}", partitionInfo.partition(), e.getMessage());
                    }
                    
                    partitions.add(partition);
                }
            }
        } catch (Exception e) {
            log.error("获取Kafka分区信息失败: {}", e.getMessage(), e);
        }
        
        return partitions;
    }

    @Override
    public List<Map<String, Object>> getConsumerGroups(String pattern) {
        List<Map<String, Object>> consumerGroups = new ArrayList<>();
        try {
            if (!initConnection()) {
                return consumerGroups;
            }
            
            try {
                Collection<ConsumerGroupListing> groupListings = 
                    adminClient.listConsumerGroups()
                              .all()
                              .get(10, TimeUnit.SECONDS);
                
                // 过滤消费者组
                if (pattern != null && !pattern.isEmpty()) {
                    groupListings = groupListings.stream()
                        .filter(group -> group.groupId().contains(pattern))
                        .collect(Collectors.toList());
                }
                
                Map<String, ConsumerGroupDescription> groupDescriptions = 
                    adminClient.describeConsumerGroups(
                        groupListings.stream()
                            .map(ConsumerGroupListing::groupId)
                            .collect(Collectors.toList())
                    ).all().get(10, TimeUnit.SECONDS);
                
                for (ConsumerGroupListing listing : groupListings) {
                    Map<String, Object> group = new HashMap<>();
                    group.put("id", listing.groupId());
                    group.put("name", listing.groupId());
                    group.put("simple", listing.isSimpleConsumerGroup());
                    
                    // 添加消费者组状态
                    ConsumerGroupDescription description = groupDescriptions.get(listing.groupId());
                    
                    if (description != null) {
                        group.put("state", description.state().toString());
                        group.put("members", description.members().size());
                    }
                    
                    consumerGroups.add(group);
                }
            } catch (Exception e) {
                log.error("获取Kafka消费者组失败: {}", e.getMessage(), e);
            }
        } finally {
            closeConnection();
        }
        return consumerGroups;
    }

    @Override
    public Map<String, Object> getConsumerGroupInfo(String groupId) {
        Map<String, Object> groupInfo = new HashMap<>();
        try {
            if (!initConnection()) {
                return groupInfo;
            }
            
            try {
                ConsumerGroupDescription description = 
                    adminClient.describeConsumerGroups(Collections.singleton(groupId))
                              .all()
                              .get(10, TimeUnit.SECONDS)
                              .get(groupId);
                
                if (description != null) {
                    groupInfo.put("id", description.groupId());
                    groupInfo.put("name", description.groupId());
                    groupInfo.put("state", description.state().toString());
                    groupInfo.put("coordinator", description.coordinator().id());
                    groupInfo.put("partitionAssignor", description.partitionAssignor());
                    
                    List<Map<String, Object>> members = new ArrayList<>();
                    for (MemberDescription member : description.members()) {
                        Map<String, Object> memberInfo = new HashMap<>();
                        memberInfo.put("id", member.consumerId());
                        memberInfo.put("clientId", member.clientId());
                        memberInfo.put("host", member.host());
                        
                        // 获取分配的分区
                        Set<TopicPartition> assignedPartitions = member.assignment().topicPartitions();
                        Map<String, List<Integer>> topicPartitions = new HashMap<>();
                        
                        for (TopicPartition tp : assignedPartitions) {
                            topicPartitions.computeIfAbsent(tp.topic(), k -> new ArrayList<>())
                                .add(tp.partition());
                        }
                        
                        memberInfo.put("assignment", topicPartitions);
                        members.add(memberInfo);
                    }
                    
                    groupInfo.put("members", members);
                }
            } catch (Exception e) {
                log.error("获取Kafka消费者组信息失败: {}", e.getMessage(), e);
            }
        } finally {
            closeConnection();
        }
        return groupInfo;
    }

    @Override
    public Map<String, Object> getConsumptionProgress(String groupId, String topic) {
        Map<String, Object> progressInfo = new HashMap<>();
        try {
            if (!initConnection()) {
                return progressInfo;
            }
            
            try {
                // 获取主题分区信息
                TopicDescription topicDescription = 
                    adminClient.describeTopics(Collections.singleton(topic))
                              .all()
                              .get(10, TimeUnit.SECONDS)
                              .get(topic);
                
                if (topicDescription != null) {
                    // 创建TopicPartition列表
                    List<TopicPartition> partitions = topicDescription.partitions().stream()
                        .map(tp -> new TopicPartition(topic, tp.partition()))
                        .collect(Collectors.toList());
                    
                    // 获取消费者偏移量
                    Map<TopicPartition, OffsetAndMetadata> consumerOffsets;
                    try {
                        consumerOffsets = adminClient.listConsumerGroupOffsets(groupId)
                            .partitionsToOffsetAndMetadata()
                            .get(10, TimeUnit.SECONDS);
                    } catch (ExecutionException e) {
                        // 消费者组可能不存在或没有消费过该主题
                        consumerOffsets = new HashMap<>();
                    }
                    
                    // 获取主题结束偏移量
                    Map<TopicPartition, Long> endOffsets = getEndOffsets(partitions);
                    
                    // 计算进度
                    Map<Integer, Map<String, Object>> partitionProgress = new HashMap<>();
                    long totalLag = 0;
                    
                    for (TopicPartition partition : partitions) {
                        long consumerOffset = consumerOffsets.containsKey(partition) ? 
                            consumerOffsets.get(partition).offset() : 0;
                        long endOffset = endOffsets.getOrDefault(partition, 0L);
                        long lag = Math.max(0, endOffset - consumerOffset);
                        
                        totalLag += lag;
                        
                        Map<String, Object> partInfo = new HashMap<>();
                        partInfo.put("partition", partition.partition());
                        partInfo.put("consumerOffset", consumerOffset);
                        partInfo.put("endOffset", endOffset);
                        partInfo.put("lag", lag);
                        
                        partitionProgress.put(partition.partition(), partInfo);
                    }
                    
                    progressInfo.put("groupId", groupId);
                    progressInfo.put("topic", topic);
                    progressInfo.put("totalLag", totalLag);
                    progressInfo.put("partitions", partitionProgress);
                }
            } catch (Exception e) {
                log.error("获取Kafka消费进度失败: {}", e.getMessage(), e);
            }
        } finally {
            closeConnection();
        }
        return progressInfo;
    }

    @Override
    public Map<String, Object> getPartitionProgress(String groupId, String topic, String partition) {
        Map<String, Object> progress = new HashMap<>();
        
        try {
            int partitionId = Integer.parseInt(partition);
            
            AdminClient adminClient = getAdminClient();
            if (adminClient == null) {
                log.error("获取Kafka AdminClient失败");
                return progress;
            }
            
            // 获取主题详情
            Map<String, TopicDescription> topicInfo = getTopicDescriptions(adminClient, Collections.singletonList(topic));
            TopicDescription topicDescription = topicInfo.get(topic);
            
            if (topicDescription == null) {
                log.error("主题 {} 不存在", topic);
                return progress;
            }
            
            // 检查分区是否存在
            boolean partitionExists = false;
            for (TopicPartitionInfo partitionInfo : topicDescription.partitions()) {
                if (partitionInfo.partition() == partitionId) {
                    partitionExists = true;
                    break;
                }
            }
            
            if (!partitionExists) {
                log.error("分区 {} 不存在于主题 {}", partition, topic);
                return progress;
            }
            
            // 获取消费者组偏移量
            TopicPartition topicPartition = new TopicPartition(topic, partitionId);
            
            // 虽然topicPartitions方法已过时，但我们暂时仍使用此方法
            // 为兼容不同版本的Kafka客户端，后续可考虑升级
            ListConsumerGroupOffsetsOptions options = new ListConsumerGroupOffsetsOptions()
                    .topicPartitions(Collections.singletonList(topicPartition));
            
            ListConsumerGroupOffsetsResult offsetsResult = adminClient.listConsumerGroupOffsets(groupId, options);
            Map<TopicPartition, OffsetAndMetadata> offsets = offsetsResult.partitionsToOffsetAndMetadata().get(10, TimeUnit.SECONDS);
            
            OffsetAndMetadata offsetAndMetadata = offsets.get(topicPartition);
            if (offsetAndMetadata != null) {
                progress.put("consumedOffset", offsetAndMetadata.offset());
                progress.put("metadata", offsetAndMetadata.metadata());
            }
            
            // 获取分区的结束偏移量
            Properties consumerProps = new Properties();
            consumerProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaServers());
            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps, new StringDeserializer(), new StringDeserializer())) {
                Map<TopicPartition, Long> endOffsets = consumer.endOffsets(Collections.singletonList(topicPartition));
                Long endOffset = endOffsets.get(topicPartition);
                
                if (endOffset != null) {
                    progress.put("endOffset", endOffset);
                    
                    if (offsetAndMetadata != null) {
                        long lag = endOffset - offsetAndMetadata.offset();
                        progress.put("lag", lag);
                    }
                }
            }
            
            progress.put("topic", topic);
            progress.put("partition", partitionId);
            progress.put("groupId", groupId);
            
        } catch (Exception e) {
            log.error("获取Kafka分区消费进度失败: {}", e.getMessage(), e);
        }
        
        return progress;
    }
    
    private Map<TopicPartition, Long> getEndOffsets(List<TopicPartition> partitions) {
        Map<TopicPartition, Long> endOffsets = new HashMap<>();
        if (consumer != null && !partitions.isEmpty()) {
            try {
                endOffsets = consumer.endOffsets(partitions);
            } catch (Exception e) {
                log.error("获取主题分区结束偏移量失败: {}", e.getMessage(), e);
            }
        }
        return endOffsets;
    }

    @Override
    public boolean createTopic(String topic, int partitions, int replicationFactor) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            try {
                // 设置主题配置
                NewTopic newTopic = new NewTopic(topic, partitions, (short) replicationFactor);
                
                // 创建主题
                adminClient.createTopics(Collections.singleton(newTopic))
                          .all()
                          .get(10, TimeUnit.SECONDS);
                log.info("创建Kafka主题成功: {}", topic);
                return true;
            } catch (Exception e) {
                log.error("创建Kafka主题失败: {}", e.getMessage(), e);
            }
        } finally {
            closeConnection();
        }
        return false;
    }

    @Override
    public boolean deleteTopic(String topic) {
        try {
            if (!initConnection()) {
                return false;
            }
            
            try {
                adminClient.deleteTopics(Collections.singleton(topic))
                          .all()
                          .get(10, TimeUnit.SECONDS);
                log.info("删除Kafka主题成功: {}", topic);
                return true;
            } catch (Exception e) {
                log.error("删除Kafka主题失败: {}", e.getMessage(), e);
            }
        } finally {
            closeConnection();
        }
        return false;
    }

    @Override
    public Map<String, Object> getTopicMetrics(String topic) {
        Map<String, Object> metrics = new HashMap<>();
        try {
            if (!initConnection()) {
                return metrics;
            }
            
            try {
                // 获取主题描述
                TopicDescription description = 
                    adminClient.describeTopics(Collections.singleton(topic))
                              .all()
                              .get(10, TimeUnit.SECONDS)
                              .get(topic);
                
                if (description != null) {
                    metrics.put("topic", topic);
                    metrics.put("partitionCount", description.partitions().size());
                    
                    // 获取副本因子
                    if (!description.partitions().isEmpty()) {
                        int replicationFactor = description.partitions().get(0).replicas().size();
                        metrics.put("replicationFactor", replicationFactor);
                    } else {
                        metrics.put("replicationFactor", 0);
                    }
                    
                    // 计算总消息数和总存储大小（这需要查询JMX统计数据，模拟假值）
                    metrics.put("messageCount", 1000000);  // 假值
                    metrics.put("storageSize", 1024 * 1024 * 100);  // 假值，100MB
                    
                    // 获取最近的生产和消费速率（假值）
                    metrics.put("messagesInPerSec", 100.0);  // 假值
                    metrics.put("bytesInPerSec", 10240.0);  // 假值
                    metrics.put("bytesOutPerSec", 5120.0);  // 假值
                    
                    // 获取ISR数量
                    int totalIsr = description.partitions().stream()
                        .mapToInt(p -> p.isr().size())
                        .sum();
                    metrics.put("isrCount", totalIsr);
                    
                    // 计算ISR比例
                    int totalReplicas = description.partitions().stream()
                        .mapToInt(p -> p.replicas().size())
                        .sum();
                    double isrRatio = totalReplicas > 0 ? (double) totalIsr / totalReplicas : 1.0;
                    metrics.put("isrRatio", isrRatio);
                }
            } catch (Exception e) {
                log.error("获取Kafka主题监控指标失败: {}", e.getMessage(), e);
            }
        } finally {
            closeConnection();
        }
        return metrics;
    }

    @Override
    public Map<String, Object> getClusterInfo() {
        Map<String, Object> clusterInfo = new HashMap<>();
        try {
            if (!initConnection()) {
                return clusterInfo;
            }
            
            try {
                // 获取集群节点信息
                DescribeClusterResult clusterResult = adminClient.describeCluster();
                Collection<Node> nodes = clusterResult.nodes().get(10, TimeUnit.SECONDS);
                String clusterId = clusterResult.clusterId().get(10, TimeUnit.SECONDS);
                Node controller = clusterResult.controller().get(10, TimeUnit.SECONDS);
                
                clusterInfo.put("clusterId", clusterId);
                clusterInfo.put("controllerNodeId", controller.id());
                
                List<Map<String, Object>> nodeInfos = new ArrayList<>();
                for (Node node : nodes) {
                    Map<String, Object> nodeInfo = new HashMap<>();
                    nodeInfo.put("id", node.id());
                    nodeInfo.put("host", node.host());
                    nodeInfo.put("port", node.port());
                    nodeInfo.put("rack", node.rack());
                    nodeInfo.put("isController", node.id() == controller.id());
                    nodeInfos.add(nodeInfo);
                }
                clusterInfo.put("nodes", nodeInfos);
                clusterInfo.put("nodeCount", nodes.size());
                
                // 获取所有主题
                Collection<TopicListing> topics = adminClient.listTopics().listings().get(10, TimeUnit.SECONDS);
                clusterInfo.put("topicCount", topics.size());
                
                // 获取所有消费者组
                Collection<ConsumerGroupListing> consumerGroups = 
                    adminClient.listConsumerGroups().all().get(10, TimeUnit.SECONDS);
                clusterInfo.put("consumerGroupCount", consumerGroups.size());
            } catch (Exception e) {
                log.error("获取Kafka集群信息失败: {}", e.getMessage(), e);
            }
        } finally {
            closeConnection();
        }
        return clusterInfo;
    }

    @Override
    public boolean topicExists(String topic) {
        try {
            AdminClient adminClient = getAdminClient();
            if (adminClient == null) {
                log.error("获取Kafka AdminClient失败");
                return false;
            }
            
            Map<String, TopicDescription> topicInfo = getTopicDescriptions(adminClient, Collections.singletonList(topic));
            return topicInfo.containsKey(topic);
        } catch (Exception e) {
            log.debug("检查Kafka主题 {} 存在失败: {}", topic, e.getMessage());
            return false;
        }
    }

    @Override
    public String getDefaultPort() {
        return "9092";
    }

    @Override
    public String getDriverClassName() {
        return KAFKA_DRIVER;
    }

    /**
     * 获取Kafka主题列表
     * 实现特定类型的主题获取逻辑
     * @param pattern 主题名称模式（可用于筛选）
     * @return 主题列表
     */
    @Override
    public List<Map<String, Object>> getSpecificTopics(String pattern) {
        return getTopics(pattern);
    }

    /**
     * 获取Kafka消费者组重平衡信息
     * @param groupId 消费者组ID
     * @return 重平衡信息
     */
    public Map<String, Object> getGroupRebalanceInfo(String groupId) {
        Map<String, Object> rebalanceInfo = new HashMap<>();
        
        try {
            AdminClient adminClient = getAdminClient();
            if (adminClient == null) {
                log.error("获取Kafka AdminClient失败");
                return rebalanceInfo;
            }
            
            // 获取消费者组详情
            DescribeConsumerGroupsResult describeResult = adminClient.describeConsumerGroups(
                    Collections.singletonList(groupId), 
                    new DescribeConsumerGroupsOptions().includeAuthorizedOperations(true));
            
            Map<String, ConsumerGroupDescription> groupDetails = describeResult.all().get();
            ConsumerGroupDescription groupDescription = groupDetails.get(groupId);
            
            if (groupDescription != null) {
                rebalanceInfo.put("state", groupDescription.state().toString());
                rebalanceInfo.put("coordinator", groupDescription.coordinator().id());
                rebalanceInfo.put("assignmentStrategy", groupDescription.partitionAssignor());
                
                // 获取消费者组成员
                List<Map<String, Object>> members = new ArrayList<>();
                for (MemberDescription memberDescription : groupDescription.members()) {
                    Map<String, Object> member = new HashMap<>();
                    member.put("id", memberDescription.consumerId());
                    member.put("clientId", memberDescription.clientId());
                    member.put("host", memberDescription.host());
                    
                    // 获取分配的分区
                    MemberAssignment assignment = memberDescription.assignment();
                    if (assignment != null) {
                        Set<TopicPartition> topicPartitions = assignment.topicPartitions();
                        Map<String, List<Integer>> partitionsByTopic = new HashMap<>();
                        
                        for (TopicPartition topicPartition : topicPartitions) {
                            String topicName = topicPartition.topic();
                            if (!partitionsByTopic.containsKey(topicName)) {
                                partitionsByTopic.put(topicName, new ArrayList<>());
                            }
                            partitionsByTopic.get(topicName).add(topicPartition.partition());
                        }
                        
                        member.put("assignment", partitionsByTopic);
                    }
                    
                    members.add(member);
                }
                
                rebalanceInfo.put("members", members);
            }
        } catch (Exception e) {
            log.error("获取Kafka消费者组重平衡信息失败: {}", e.getMessage(), e);
        }
        
        return rebalanceInfo;
    }

    /**
     * 获取Kafka AdminClient
     * @return AdminClient对象
     */
    private AdminClient getAdminClient() {
        if (adminClient == null) {
            initConnection();
        }
        return adminClient;
    }
    
    /**
     * 获取Kafka服务器地址列表
     * @return 服务器地址列表
     */
    private String getKafkaServers() {
        return getBootstrapServers();
    }
    
    /**
     * 获取主题描述信息
     * @param adminClient AdminClient对象
     * @param topics 主题列表
     * @return 主题描述信息映射
     */
    private Map<String, TopicDescription> getTopicDescriptions(AdminClient adminClient, Collection<String> topics) throws Exception {
        DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(topics);
        try {
            // topicNameValues()返回Map<String, KafkaFuture<TopicDescription>>，而不是KafkaFuture<Map<>>
            Map<String, TopicDescription> result = new HashMap<>();
            Map<String, KafkaFuture<TopicDescription>> futures = describeTopicsResult.topicNameValues();
            
            // 获取每个主题的描述
            for (Map.Entry<String, KafkaFuture<TopicDescription>> entry : futures.entrySet()) {
                String topicName = entry.getKey();
                KafkaFuture<TopicDescription> future = entry.getValue();
                // 等待future完成并获取结果
                TopicDescription description = future.get();
                result.put(topicName, description);
            }
            
            return result;
        } catch (Exception e) {
            log.error("获取主题描述失败: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 删除消费者组
     * @param groupId 消费者组ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteConsumerGroup(String groupId) {
        try {
            AdminClient adminClient = getAdminClient();
            if (adminClient == null) {
                log.error("获取Kafka AdminClient失败");
                return false;
            }
            
            DeleteConsumerGroupsResult result = adminClient.deleteConsumerGroups(Collections.singletonList(groupId));
            result.all().get();
            
            log.info("成功删除Kafka消费者组: {}", groupId);
            return true;
        } catch (Exception e) {
            log.error("删除Kafka消费者组失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 创建消费者组
     * @param groupId 消费者组ID
     * @param config 消费者组配置
     * @return 是否创建成功
     */
    @Override
    public boolean createConsumerGroup(String groupId, Map<String, Object> config) {
        try {
            AdminClient adminClient = getAdminClient();
            if (adminClient == null) {
                log.error("获取Kafka AdminClient失败");
                return false;
            }
            
            // Kafka不支持直接创建消费者组，消费者组在消费者首次消费时自动创建
            // 这里我们创建一个临时消费者并订阅一个存在的主题来实现创建消费者组
            Properties props = new Properties();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaServers());
            props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            
            // 应用自定义配置
            if (config != null && !config.isEmpty()) {
                for (Map.Entry<String, Object> entry : config.entrySet()) {
                    props.put(entry.getKey(), entry.getValue());
                }
            }
            
            try (KafkaConsumer<String, String> tempConsumer = new KafkaConsumer<>(props)) {
                // 获取一个存在的主题
                Set<String> topics = adminClient.listTopics().names().get(10, TimeUnit.SECONDS);
                if (!topics.isEmpty()) {
                    // 选择第一个主题订阅
                    String firstTopic = topics.iterator().next();
                    tempConsumer.subscribe(Collections.singletonList(firstTopic));
                    // 轮询一次以触发消费者组创建
                    tempConsumer.poll(Duration.ofMillis(100));
                    log.info("成功创建Kafka消费者组: {}", groupId);
                    return true;
                } else {
                    log.error("创建消费者组失败，集群中没有可用的主题");
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("创建Kafka消费者组失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 检查是否支持消息浏览功能（不消费）
     * @return 是否支持浏览
     */
    @Override
    public boolean supportsBrowsing() {
        // Kafka支持浏览功能
        return true;
    }
    
    /**
     * 浏览消息（不消费）
     * 此方法实现Kafka消息浏览，不会提交偏移量
     * @param topic 主题名称
     * @param partition 分区号（-1表示所有分区）
     * @param offset 起始偏移量（-1表示最新, -2表示最早）
     * @param count 消息数量
     * @return 消息列表
     */
    @Override
    public List<Map<String, Object>> browseMessages(String topic, int partition, long offset, int count) {
        List<Map<String, Object>> messages = new ArrayList<>();
        
        try {
            if (!initConnection()) {
                return messages;
            }
            
            // 获取主题的所有分区
            List<TopicPartition> partitionsToQuery = new ArrayList<>();
            
            try {
                // 如果指定了分区，只查询该分区
                if (partition >= 0) {
                    partitionsToQuery.add(new TopicPartition(topic, partition));
                } else {
                    // 否则获取所有分区
                    TopicDescription topicDescription = 
                        adminClient.describeTopics(Collections.singleton(topic))
                                  .all()
                                  .get(10, TimeUnit.SECONDS)
                                  .get(topic);
                    
                    if (topicDescription != null) {
                        for (TopicPartitionInfo partitionInfo : topicDescription.partitions()) {
                            partitionsToQuery.add(new TopicPartition(topic, partitionInfo.partition()));
                        }
                    }
                }
                
                // 为每个分区设置起始位置
                Map<TopicPartition, Long> partitionOffsets = new HashMap<>();
                
                // 根据请求中的offset参数，设置不同的偏移量策略
                if (offset == -2) {
                    // 最早的偏移量
                    Map<TopicPartition, Long> beginningOffsets = consumer.beginningOffsets(partitionsToQuery);
                    partitionOffsets.putAll(beginningOffsets);
                } else if (offset == -1) {
                    // 最新的偏移量（减去count，以便获取最近的消息）
                    Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitionsToQuery);
                    
                    for (Map.Entry<TopicPartition, Long> entry : endOffsets.entrySet()) {
                        TopicPartition tp = entry.getKey();
                        Long endOffset = entry.getValue();
                        Long beginningOffset = consumer.beginningOffsets(Collections.singleton(tp)).get(tp);
                        
                        // 设置起始偏移量为 max(endOffset - count, beginningOffset)
                        // 确保不会请求超出范围的偏移量
                        if (endOffset > 0) {
                            partitionOffsets.put(tp, Math.max(endOffset - count, beginningOffset));
                        } else {
                            partitionOffsets.put(tp, beginningOffset);
                        }
                    }
                } else {
                    // 使用指定的偏移量
                    for (TopicPartition tp : partitionsToQuery) {
                        partitionOffsets.put(tp, offset);
                    }
                }
                
                // 指定消费者从哪个位置开始消费
                for (Map.Entry<TopicPartition, Long> entry : partitionOffsets.entrySet()) {
                    consumer.assign(Collections.singleton(entry.getKey()));
                    consumer.seek(entry.getKey(), entry.getValue());
                }
                
                // 设置所有要查询的分区
                consumer.assign(partitionsToQuery);
                
                // 获取指定数量的消息
                int remainingCount = count;
                int emptyPolls = 0;
                int maxEmptyPolls = 3; // 最多尝试3次空轮询
                
                while (remainingCount > 0 && emptyPolls < maxEmptyPolls) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                    
                    if (records.isEmpty()) {
                        emptyPolls++;
                        continue;
                    }
                    
                    emptyPolls = 0; // 重置空轮询计数
                    
                    for (ConsumerRecord<String, String> record : records) {
                        Map<String, Object> messageInfo = new HashMap<>();
                        messageInfo.put("topic", record.topic());
                        messageInfo.put("partition", record.partition());
                        messageInfo.put("offset", record.offset());
                        messageInfo.put("key", record.key());
                        messageInfo.put("value", record.value());
                        messageInfo.put("timestamp", record.timestamp());
                        
                        // 添加消息头信息
                        if (record.headers() != null && record.headers().iterator().hasNext()) {
                            Map<String, String> headers = new HashMap<>();
                            record.headers().forEach(header -> {
                                headers.put(header.key(), new String(header.value()));
                            });
                            messageInfo.put("headers", headers);
                        }
                        
                        messages.add(messageInfo);
                        
                        remainingCount--;
                        if (remainingCount <= 0) {
                            break;
                        }
                    }
                }
                
                // 不提交偏移量
                // 取消分配
                consumer.unsubscribe();
                
            } catch (Exception e) {
                log.error("浏览Kafka消息失败: {}", e.getMessage(), e);
            }
        } finally {
            closeConnection();
        }
        
        // 封装响应格式
        Map<String, Object> response = new HashMap<>();
        response.put("messages", messages);
        response.put("total", messages.size()); // 这里可以根据实际情况设置总数
        
        return messages;
    }
} 