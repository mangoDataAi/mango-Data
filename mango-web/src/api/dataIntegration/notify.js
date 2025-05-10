import request from '@/utils/request'

// 邮件配置相关接口
export function getEmailConfig() {
  return request({
    url: '/api/v1/notify/email/config',
    method: 'get'
  })
}

export function saveEmailConfig(data) {
  return request({
    url: '/api/v1/notify/email/config',
    method: 'post',
    data
  })
}

export function testEmailConfig(data) {
  return request({
    url: '/api/v1/notify/email/test',
    method: 'post',
    data
  })
}

// 短信配置相关接口
export function getSmsConfig() {
  return request({
    url: '/api/v1/notify/sms/config',
    method: 'get'
  })
}

export function saveSmsConfig(data) {
  return request({
    url: '/api/v1/notify/sms/config',
    method: 'post',
    data
  })
}

export function testSmsConfig(data) {
  return request({
    url: '/api/v1/notify/sms/test',
    method: 'post',
    data
  })
}

// Webhook配置相关接口
export function getWebhookConfig() {
  return request({
    url: '/api/v1/notify/webhook/config',
    method: 'get'
  })
}

export function saveWebhookConfig(data) {
  return request({
    url: '/api/v1/notify/webhook/config',
    method: 'post',
    data
  })
}

export function testWebhookConfig(data) {
  return request({
    url: '/api/v1/notify/webhook/test',
    method: 'post',
    data
  })
}

// 钉钉配置相关接口
export function getDingtalkConfig() {
  return request({
    url: '/api/v1/notify/dingtalk/config',
    method: 'get'
  })
}

export function saveDingtalkConfig(data) {
  return request({
    url: '/api/v1/notify/dingtalk/config',
    method: 'post',
    data
  })
}

export function testDingtalkConfig(data) {
  return request({
    url: '/api/v1/notify/dingtalk/test',
    method: 'post',
    data
  })
}

// 通知模板相关接口
export function getTemplateList(params) {
  return request({
    url: '/api/v1/notify/templates',
    method: 'get',
    params
  })
}

export function getTemplateDetail(id) {
  return request({
    url: `/api/v1/notify/templates/${id}`,
    method: 'get'
  })
}

export function createTemplate(data) {
  return request({
    url: '/api/v1/notify/templates',
    method: 'post',
    data
  })
}

export function updateTemplate(id, data) {
  return request({
    url: `/api/v1/notify/templates/${id}`,
    method: 'put',
    data
  })
}

export function deleteTemplate(id) {
  return request({
    url: `/api/v1/notify/templates/${id}`,
    method: 'delete'
  })
}

export function previewTemplate(id, data) {
  return request({
    url: `/api/v1/notify/templates/${id}/preview`,
    method: 'post',
    data
  })
}

// 接收组相关接口
export function getReceiverList(params) {
  return request({
    url: '/api/v1/notify/receivers',
    method: 'get',
    params
  })
}

export function getReceiverDetail(id) {
  return request({
    url: `/api/v1/notify/receivers/${id}`,
    method: 'get'
  })
}

export function createReceiver(data) {
  return request({
    url: '/api/v1/notify/receivers',
    method: 'post',
    data
  })
}

export function updateReceiver(id, data) {
  return request({
    url: `/api/v1/notify/receivers/${id}`,
    method: 'put',
    data
  })
}

export function deleteReceiver(id) {
  return request({
    url: `/api/v1/notify/receivers/${id}`,
    method: 'delete'
  })
}

export function testReceiver(id) {
  return request({
    url: `/api/v1/notify/receivers/${id}/test`,
    method: 'post'
  })
}

// 用户列表接口
export function getUserList(params) {
  return request({
    url: '/api/v1/users',
    method: 'get',
    params
  })
}

// 通知记录相关接口
export function getNotifyLogs(params) {
  // 模拟数据
  return Promise.resolve({
    data: {
      records: [
        {
          id: 1,
          taskName: '数据同步任务',
          type: 'email',
          title: '任务执行成功通知',
          content: '数据同步任务已成功完成',
          status: 'success',
          createTime: '2024-03-20 10:00:00',
          duration: 120
        },
        {
          id: 2,
          taskName: '数据质量检查',
          type: 'sms',
          title: '质量检查告警',
          content: '发现异常数据，请及时处理',
          status: 'warning',
          createTime: '2024-03-20 09:30:00',
          duration: 85
        },
        {
          id: 3,
          taskName: '数据备份任务',
          type: 'webhook',
          title: '备份失败通知',
          content: '数据备份过程中发生错误',
          status: 'error',
          createTime: '2024-03-20 09:00:00',
          duration: 200,
          error: '连接超时'
        }
      ],
      total: 3,
      size: params.size,
      current: params.page
    }
  });

  // 原始 API 调用（已注释）
  /*return request({
    url: '/api/v1/notify/logs',
    method: 'get',
    params
  })*/
}

export function getNotifyLogDetail(id) {
  return request({
    url: `/api/v1/notify/logs/${id}`,
    method: 'get'
  })
}

// 通知统计接口
export function getNotifyStats(params) {
  // Mock data for development
  return Promise.resolve({
    data: {
      // Stats data
      totalCount: 1256,
      totalTrend: 15,
      successRate: 98.5,
      successTrend: 2.3,
      avgDuration: 245,
      durationTrend: -5.2,
      failureCount: 3,
      failureTrend: -12.5,
      // Chart data
      times: ['00:00', '02:00', '04:00', '06:00', '08:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00'],
      total: [120, 132, 101, 134, 90, 230, 210, 182, 191, 234, 290, 330],
      success: [110, 128, 98, 130, 85, 225, 205, 178, 187, 230, 285, 325],
      failure: [10, 4, 3, 4, 5, 5, 5, 4, 4, 4, 5, 5]
    }
  });
  
  // Original API call (commented out)
  /*return request({
    url: '/api/v1/notify/stats',
    method: 'get',
    params
  })*/
} 