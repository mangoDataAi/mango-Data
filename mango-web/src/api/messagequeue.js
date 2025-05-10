import request from '@/utils/request'

/**
 * 获取主题列表
 * @param {string} datasourceId 数据源ID
 * @returns {Promise} 响应Promise
 */
export function getTopics(datasourceId) {
  return request({
    url: `/api/messagequeue/${datasourceId}/topics`,
    method: 'get'
  })
}

/**
 * 获取主题详情
 * @param {string} datasourceId 数据源ID
 * @param {string} topic 主题名称
 * @returns {Promise} 响应Promise
 */
export function getTopicDetails(datasourceId, topic) {
  return request({
    url: `/api/messagequeue/${datasourceId}/topic/${topic}`,
    method: 'get'
  })
}

/**
 * 获取消费者组列表
 * @param {string} datasourceId 数据源ID
 * @param {string} topic 主题名称
 * @returns {Promise} 响应Promise
 */
export function getConsumerGroups(datasourceId, topic) {
  return request({
    url: `/api/messagequeue/${datasourceId}/topic/${topic}/consumer-groups`,
    method: 'get'
  })
}

/**
 * 获取主题分区信息
 * @param {string} datasourceId 数据源ID
 * @param {string} topic 主题名称
 * @returns {Promise} 响应Promise
 */
export function getTopicPartitions(datasourceId, topic) {
  return request({
    url: `/api/messagequeue/${datasourceId}/topic/${topic}/partitions`,
    method: 'get'
  })
}

/**
 * 发送消息到主题
 * @param {string} datasourceId 数据源ID
 * @param {string} topic 主题名称
 * @param {Object} message 消息内容
 * @returns {Promise} 响应Promise
 */
export function sendMessage(datasourceId, topic, message) {
  return request({
    url: `/api/messagequeue/${datasourceId}/topic/${topic}/send`,
    method: 'post',
    data: message
  })
}

/**
 * 消费主题消息
 * @param {string} datasourceId 数据源ID
 * @param {string} topic 主题名称
 * @param {Object} options 消费选项
 * @returns {Promise} 响应Promise
 */
export function consumeMessages(datasourceId, topic, options) {
  return request({
    url: `/api/messagequeue/${datasourceId}/topic/${topic}/consume`,
    method: 'post',
    data: options
  })
}

/**
 * 浏览主题消息（不消费）
 * @param {string} datasourceId 数据源ID
 * @param {string} topic 主题名称
 * @param {Object} options 查询选项
 * @returns {Promise} 响应Promise
 */
export function browseMessages(datasourceId, topic, options) {
  return request({
    url: `/api/messagequeue/${datasourceId}/topic/${topic}/browse`,
    method: 'post',
    data: options
  })
}

/**
 * 创建主题
 * @param {string} datasourceId 数据源ID
 * @param {Object} topicConfig 主题配置
 * @returns {Promise} 响应Promise
 */
export function createTopic(datasourceId, topicConfig) {
  return request({
    url: `/api/messagequeue/${datasourceId}/topic`,
    method: 'post',
    data: topicConfig
  })
}

/**
 * 删除主题
 * @param {string} datasourceId 数据源ID
 * @param {string} topic 主题名称
 * @returns {Promise} 响应Promise
 */
export function deleteTopic(datasourceId, topic) {
  return request({
    url: `/api/messagequeue/${datasourceId}/topic/${topic}`,
    method: 'delete'
  })
}

/**
 * 获取消息队列统计信息
 * @param {string} datasourceId 数据源ID
 * @returns {Promise} 响应Promise
 */
export function getQueueStats(datasourceId) {
  return request({
    url: `/api/messagequeue/${datasourceId}/stats`,
    method: 'get'
  })
} 