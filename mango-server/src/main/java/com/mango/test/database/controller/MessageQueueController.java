package com.mango.test.database.controller;

import com.mango.test.vo.R;
import com.mango.test.database.service.MessageQueueService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Api(tags = "消息队列")
@RestController
@RequestMapping("/api/messagequeue")
public class MessageQueueController {

    @Autowired
    private MessageQueueService messageQueueService;

    @GetMapping("/{datasourceId}/topics")
    @ApiOperation("获取主题列表")
    public R<List<Map<String, Object>>> getTopics(
            @PathVariable String datasourceId,
            @RequestParam(required = false) String searchText) {
        try {
            List<Map<String, Object>> topics = messageQueueService.getTopics(datasourceId, searchText);
            return R.ok(topics);
        } catch (Exception e) {
            log.error("获取主题列表失败", e);
            return R.fail("获取主题列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{datasourceId}/topic/{topic}")
    @ApiOperation("获取主题详情")
    public R<Map<String, Object>> getTopicDetails(
            @PathVariable String datasourceId,
            @PathVariable String topic) {
        try {
            Map<String, Object> details = messageQueueService.getTopicDetails(datasourceId, topic);
            return R.ok(details);
        } catch (Exception e) {
            log.error("获取主题详情失败", e);
            return R.fail("获取主题详情失败: " + e.getMessage());
        }
    }

    @GetMapping("/{datasourceId}/topic/{topic}/consumer-groups")
    @ApiOperation("获取消费者组列表")
    public R<List<Map<String, Object>>> getConsumerGroups(
            @PathVariable String datasourceId,
            @PathVariable String topic) {
        try {
            List<Map<String, Object>> groups = messageQueueService.getConsumerGroups(datasourceId, topic);
            return R.ok(groups);
        } catch (Exception e) {
            log.error("获取消费者组列表失败", e);
            return R.fail("获取消费者组列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{datasourceId}/topic/{topic}/partitions")
    @ApiOperation("获取主题分区信息")
    public R<List<Map<String, Object>>> getTopicPartitions(
            @PathVariable String datasourceId,
            @PathVariable String topic) {
        try {
            List<Map<String, Object>> partitions = messageQueueService.getTopicPartitions(datasourceId, topic);
            return R.ok(partitions);
        } catch (Exception e) {
            log.error("获取主题分区信息失败", e);
            return R.fail("获取主题分区信息失败: " + e.getMessage());
        }
    }

    @PostMapping("/{datasourceId}/topic/{topic}/send")
    @ApiOperation("发送消息")
    public R<Boolean> sendMessage(
            @PathVariable String datasourceId,
            @PathVariable String topic,
            @RequestBody Map<String, Object> message) {
        try {
            boolean success = messageQueueService.sendMessage(datasourceId, topic, message);
            return R.ok("消息发送成功", success);
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return R.fail("发送消息失败: " + e.getMessage());
        }
    }

    @PostMapping("/{datasourceId}/topic/{topic}/consume")
    @ApiOperation("消费消息")
    public R<List<Map<String, Object>>> consumeMessages(
            @PathVariable String datasourceId,
            @PathVariable String topic,
            @RequestBody Map<String, Object> options) {
        try {
            List<Map<String, Object>> messages = messageQueueService.consumeMessages(datasourceId, topic, options);
            return R.ok(messages);
        } catch (Exception e) {
            log.error("消费消息失败", e);
            return R.fail("消费消息失败: " + e.getMessage());
        }
    }

    @PostMapping("/{datasourceId}/topic/{topic}/browse")
    @ApiOperation("浏览消息（不消费）")
    public R<Map<String, Object>> browseMessages(
            @PathVariable String datasourceId,
            @PathVariable String topic,
            @RequestBody Map<String, Object> options) {
        try {
            List<Map<String, Object>> messages = messageQueueService.browseMessages(datasourceId, topic, options);
            
            // 构建响应结构
            Map<String, Object> result = new HashMap<>();
            result.put("messages", messages);
            result.put("total", messages.size());
            result.put("topic", topic);
            
            return R.ok(result);
        } catch (Exception e) {
            log.error("浏览消息失败", e);
            return R.fail("浏览消息失败: " + e.getMessage());
        }
    }

    @PostMapping("/{datasourceId}/topic")
    @ApiOperation("创建主题")
    public R<Boolean> createTopic(
            @PathVariable String datasourceId,
            @RequestBody Map<String, Object> topicConfig) {
        try {
            boolean success = messageQueueService.createTopic(datasourceId, topicConfig);
            return R.ok("主题创建成功", success);
        } catch (Exception e) {
            log.error("创建主题失败", e);
            return R.fail("创建主题失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{datasourceId}/topic/{topic}")
    @ApiOperation("删除主题")
    public R<Boolean> deleteTopic(
            @PathVariable String datasourceId,
            @PathVariable String topic) {
        try {
            boolean success = messageQueueService.deleteTopic(datasourceId, topic);
            return R.ok("主题删除成功", success);
        } catch (Exception e) {
            log.error("删除主题失败", e);
            return R.fail("删除主题失败: " + e.getMessage());
        }
    }

    @GetMapping("/{datasourceId}/consumer-group/{groupId}")
    @ApiOperation("获取消费者组详情")
    public R<Map<String, Object>> getConsumerGroupDetails(
            @PathVariable String datasourceId,
            @PathVariable String groupId) {
        try {
            Map<String, Object> details = messageQueueService.getConsumerGroupDetails(datasourceId, groupId);
            return R.ok(details);
        } catch (Exception e) {
            log.error("获取消费者组详情失败", e);
            return R.fail("获取消费者组详情失败: " + e.getMessage());
        }
    }

    @PostMapping("/{datasourceId}/consumer-group")
    @ApiOperation("创建消费者组")
    public R<Boolean> createConsumerGroup(
            @PathVariable String datasourceId,
            @RequestBody Map<String, Object> groupConfig) {
        try {
            boolean success = messageQueueService.createConsumerGroup(datasourceId, groupConfig);
            return R.ok("消费者组创建成功", success);
        } catch (Exception e) {
            log.error("创建消费者组失败", e);
            return R.fail("创建消费者组失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{datasourceId}/consumer-group/{groupId}")
    @ApiOperation("删除消费者组")
    public R<Boolean> deleteConsumerGroup(
            @PathVariable String datasourceId,
            @PathVariable String groupId) {
        try {
            boolean success = messageQueueService.deleteConsumerGroup(datasourceId, groupId);
            return R.ok("消费者组删除成功", success);
        } catch (Exception e) {
            log.error("删除消费者组失败", e);
            return R.fail("删除消费者组失败: " + e.getMessage());
        }
    }

    @GetMapping("/{datasourceId}/stats")
    @ApiOperation("获取消息队列统计信息")
    public R<Map<String, Object>> getQueueStats(@PathVariable String datasourceId) {
        try {
            Map<String, Object> stats = messageQueueService.getQueueStats(datasourceId);
            return R.ok(stats);
        } catch (Exception e) {
            log.error("获取消息队列统计信息失败", e);
            return R.fail("获取消息队列统计信息失败: " + e.getMessage());
        }
    }

    @PostMapping("/{datasourceId}/reset-offsets")
    @ApiOperation("重置消费者组偏移量")
    public R<Boolean> resetConsumerGroupOffsets(
            @PathVariable String datasourceId,
            @RequestBody Map<String, Object> request) {
        try {
            boolean success = messageQueueService.resetConsumerGroupOffsets(datasourceId, request);
            return R.ok("偏移量重置成功", success);
        } catch (Exception e) {
            log.error("重置偏移量失败", e);
            return R.fail("重置偏移量失败: " + e.getMessage());
        }
    }
} 