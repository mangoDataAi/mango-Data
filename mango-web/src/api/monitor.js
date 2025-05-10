import request from '@/utils/request'

// 获取连接监控指标
export function getConnectionMetrics(datasourceId) {
  return request({
    url: `/api/ds/monitor/connection/${datasourceId}`,
    method: 'get'
  })
}

// 获取性能监控指标
export function getPerformanceMetrics(datasourceId) {
  return request({
    url: `/api/ds/monitor/performance/${datasourceId}`,
    method: 'get'
  })
}

// 获取所有监控指标
export function getAllMetrics(datasourceId) {
  return request({
    url: `/api/ds/monitor/metrics/${datasourceId}`,
    method: 'get'
  })
} 