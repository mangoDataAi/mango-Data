<template>
  <div class="notification-container">
    <!-- 通知概览 -->
    <el-row :gutter="20" class="overview-section">
      <el-col :span="6" v-for="(item, index) in notificationStats" :key="index">
        <el-card shadow="hover" class="stat-card" :class="item.type">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="32"><component :is="item.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-label">{{ item.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 通知列表卡片 -->
    <el-card class="notification-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>通知列表</h3>
            <el-tag type="info" effect="plain">共 {{ total }} 条通知</el-tag>
          </div>
          <div class="header-right">
            <el-button-group>
              <el-tooltip content="全部已读" placement="top">
                <el-button :icon="Check" @click="handleReadAll" />
              </el-tooltip>
              <el-tooltip content="清空通知" placement="top">
                <el-button :icon="Delete" @click="handleClearAll" />
              </el-tooltip>
              <el-tooltip content="刷新" placement="top">
                <el-button :icon="Refresh" @click="refreshList" />
              </el-tooltip>
            </el-button-group>
          </div>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" class="search-form">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="通知类型">
              <el-select v-model="searchForm.type" placeholder="请选择通知类型" clearable>
                <el-option label="系统通知" value="SYSTEM" />
                <el-option label="任务通知" value="TASK" />
                <el-option label="消息通知" value="MESSAGE" />
                <el-option label="预警通知" value="ALERT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="通知状态">
              <el-select v-model="searchForm.status" placeholder="请选择通知状态" clearable>
                <el-option label="未读" value="0" />
                <el-option label="已读" value="1" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="通知时间">
              <el-date-picker
                v-model="searchForm.timeRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                :shortcuts="dateShortcuts"
              />
            </el-form-item>
          </el-col>
          <el-col :span="4" class="search-buttons">
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon> 搜索
            </el-button>
            <el-button @click="resetSearch">
              <el-icon><Refresh /></el-icon> 重置
            </el-button>
          </el-col>
        </el-row>
      </el-form>

      <!-- 通知列表 -->
      <div class="notification-list">
        <el-empty v-if="!notificationList.length" description="暂无通知" />
        <template v-else>
          <div
            v-for="item in notificationList"
            :key="item.id"
            class="notification-item"
            :class="{ unread: !item.read }"
            @click="handleViewNotification(item)"
          >
            <div class="item-icon" :class="item.type.toLowerCase()">
              <el-icon :size="24"><component :is="getNotificationIcon(item.type)" /></el-icon>
            </div>
            <div class="item-content">
              <div class="item-header">
                <span class="item-title">{{ item.title }}</span>
                <el-tag
                  :type="item.read ? 'info' : 'danger'"
                  size="small"
                  effect="light"
                >
                  {{ item.read ? '已读' : '未读' }}
                </el-tag>
              </div>
              <div class="item-body">{{ item.content }}</div>
              <div class="item-footer">
                <div class="item-info">
                  <span class="item-time">{{ item.createTime }}</span>
                  <span class="item-from">来自：{{ item.from }}</span>
                </div>
                <div class="item-actions">
                  <el-button
                    v-if="!item.read"
                    type="primary"
                    link
                    size="small"
                    @click.stop="handleRead(item)"
                  >
                    标记已读
                  </el-button>
                  <el-button
                    type="danger"
                    link
                    size="small"
                    @click.stop="handleDelete(item)"
                  >
                    删除
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="page.current"
          v-model:page-size="page.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 通知详情抽屉 -->
    <el-drawer
      v-model="detailDrawer"
      title="通知详情"
      size="600px"
      :destroy-on-close="true"
    >
      <template #header>
        <div class="drawer-header">
          <div class="header-icon" :class="currentNotification?.type?.toLowerCase()">
            <el-icon :size="24">
              <component :is="getNotificationIcon(currentNotification?.type)" />
            </el-icon>
          </div>
          <div class="header-content">
            <h3>{{ currentNotification?.title }}</h3>
            <div class="header-info">
              <span>{{ currentNotification?.from }}</span>
              <el-divider direction="vertical" />
              <span>{{ currentNotification?.createTime }}</span>
            </div>
          </div>
        </div>
      </template>

      <div class="notification-detail">
        <!-- 通知内容 -->
        <div class="detail-section">
          <div class="section-title">
            <el-icon><Document /></el-icon>
            <span>通知内容</span>
          </div>
          <div class="section-content content-box">
            {{ currentNotification?.content }}
          </div>
        </div>

        <!-- 相关文件 -->
        <div v-if="currentNotification?.files?.length" class="detail-section">
          <div class="section-title">
            <el-icon><Folder /></el-icon>
            <span>相关文件</span>
          </div>
          <div class="section-content">
            <div
              v-for="file in currentNotification?.files"
              :key="file.id"
              class="file-item"
            >
              <el-icon><Document /></el-icon>
              <span class="file-name">{{ file.name }}</span>
              <span class="file-size">{{ formatFileSize(file.size) }}</span>
              <el-button type="primary" link @click="handleDownloadFile(file)">
                下载
              </el-button>
            </div>
          </div>
        </div>

        <!-- 操作记录 -->
        <div class="detail-section">
          <div class="section-title">
            <el-icon><Timer /></el-icon>
            <span>操作记录</span>
          </div>
          <div class="section-content">
            <el-timeline>
              <el-timeline-item
                v-for="(record, index) in currentNotification?.records"
                :key="index"
                :type="record.type"
                :color="getRecordColor(record.type)"
                :timestamp="record.time"
                size="large"
              >
                {{ record.content }}
              </el-timeline-item>
            </el-timeline>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="detailDrawer = false">关闭</el-button>
          <el-button
            v-if="!currentNotification?.read"
            type="primary"
            @click="handleReadInDetail"
          >
            标记已读
          </el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 通知分析抽屉 -->
    <el-drawer
      v-model="analysisDrawer"
      title="通知分析"
      size="800px"
      :destroy-on-close="true"
    >
      <div class="analysis-container">
        <!-- 时间范围选择 -->
        <div class="analysis-header">
          <el-radio-group v-model="analysisRange" @change="handleAnalysisRangeChange">
            <el-radio-button label="week">近一周</el-radio-button>
            <el-radio-button label="month">近一月</el-radio-button>
            <el-radio-button label="year">近一年</el-radio-button>
          </el-radio-group>
        </div>

        <!-- 核心指标 -->
        <el-row :gutter="20" class="analysis-metrics">
          <el-col :span="8" v-for="(metric, index) in analysisMetrics" :key="index">
            <el-card shadow="hover" class="metric-card">
              <div class="metric-header">
                <span class="metric-label">{{ metric.label }}</span>
                <el-tooltip :content="metric.tip" placement="top">
                  <el-icon><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
              <div class="metric-value">
                {{ metric.value }}
                <span class="metric-unit">{{ metric.unit }}</span>
              </div>
              <div class="metric-trend">
                <span :class="metric.trend >= 0 ? 'up' : 'down'">
                  {{ Math.abs(metric.trend) }}%
                </span>
                <span class="trend-label">较上期</span>
              </div>
              <div class="metric-chart" ref="metricChartRefs"></div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 通知趋势图 -->
        <el-card class="trend-card">
          <template #header>
            <div class="card-header">
              <span>通知趋势</span>
              <el-radio-group v-model="trendType" size="small">
                <el-radio-button label="count">数量</el-radio-button>
                <el-radio-button label="type">类型</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="trend-chart" ref="trendChartRef"></div>
        </el-card>

        <!-- 通知分布 -->
        <el-row :gutter="20" class="distribution-section">
          <el-col :span="12">
            <el-card class="distribution-card">
              <template #header>
                <div class="card-header">
                  <span>通知类型分布</span>
                </div>
              </template>
              <div class="distribution-chart" ref="typeChartRef"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card class="distribution-card">
              <template #header>
                <div class="card-header">
                  <span>时段分布</span>
                </div>
              </template>
              <div class="distribution-chart" ref="timeChartRef"></div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-drawer>

    <!-- 通知设置抽屉 -->
    <el-drawer
      v-model="settingsDrawer"
      title="通知设置"
      size="600px"
      :destroy-on-close="true"
    >
      <div class="settings-container">
        <!-- 通知方式设置 -->
        <el-card class="settings-card">
          <template #header>
            <div class="card-header">
              <span>通知方式</span>
              <el-tooltip content="设置接收通知的方式" placement="top">
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
          <el-form :model="notifyMethods" label-width="100px">
            <el-form-item label="站内通知">
              <el-switch v-model="notifyMethods.site" />
            </el-form-item>
            <el-form-item label="邮件通知">
              <el-switch v-model="notifyMethods.email" />
              <div class="method-config" v-if="notifyMethods.email">
                <el-input v-model="notifyMethods.emailAddress" placeholder="请输入接收邮件的地址">
                  <template #append>
                    <el-button @click="handleTestEmail">发送测试</el-button>
                  </template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item label="短信通知">
              <el-switch v-model="notifyMethods.sms" />
              <div class="method-config" v-if="notifyMethods.sms">
                <el-input v-model="notifyMethods.phoneNumber" placeholder="请输入接收短信的手机号">
                  <template #append>
                    <el-button @click="handleTestSms">发送测试</el-button>
                  </template>
                </el-input>
              </div>
            </el-form-item>
            <el-form-item label="微信通知">
              <el-switch v-model="notifyMethods.wechat" />
              <div class="method-config" v-if="notifyMethods.wechat">
                <div class="wechat-bind">
                  <el-image :src="qrCodeUrl" fit="contain" class="qr-code" />
                  <div class="bind-tip">请使用微信扫码绑定</div>
                </div>
              </div>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 订阅管理 -->
        <el-card class="settings-card">
          <template #header>
            <div class="card-header">
              <span>订阅管理</span>
              <el-tooltip content="设置需要接收的通知类型" placement="top">
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
          <div class="subscription-list">
            <div
              v-for="(sub, index) in subscriptions"
              :key="index"
              class="subscription-item"
            >
              <div class="sub-info">
                <div class="sub-icon" :class="sub.type">
                  <el-icon><component :is="sub.icon" /></el-icon>
                </div>
                <div class="sub-content">
                  <div class="sub-title">
                    <span>{{ sub.title }}</span>
                    <el-tag size="small" :type="sub.important ? 'danger' : ''">
                      {{ sub.important ? '重要' : '普通' }}
                    </el-tag>
                  </div>
                  <div class="sub-desc">{{ sub.description }}</div>
                </div>
              </div>
              <div class="sub-actions">
                <el-checkbox-group v-model="sub.methods">
                  <el-tooltip content="站内通知" placement="top">
                    <el-checkbox label="site">
                      <el-icon><Bell /></el-icon>
                    </el-checkbox>
                  </el-tooltip>
                  <el-tooltip content="邮件通知" placement="top">
                    <el-checkbox label="email">
                      <el-icon><Message /></el-icon>
                    </el-checkbox>
                  </el-tooltip>
                  <el-tooltip content="短信通知" placement="top">
                    <el-checkbox label="sms">
                      <el-icon><ChatDotSquare /></el-icon>
                    </el-checkbox>
                  </el-tooltip>
                  <el-tooltip content="微信通知" placement="top">
                    <el-checkbox label="wechat">
                      <el-icon><ChatRound /></el-icon>
                    </el-checkbox>
                  </el-tooltip>
                </el-checkbox-group>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 免打扰时间 -->
        <el-card class="settings-card">
          <template #header>
            <div class="card-header">
              <span>免打扰设置</span>
              <el-tooltip content="设置不接收通知的时间段" placement="top">
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
          <el-form :model="doNotDisturb" label-width="100px">
            <el-form-item label="开启免打扰">
              <el-switch v-model="doNotDisturb.enabled" />
            </el-form-item>
            <template v-if="doNotDisturb.enabled">
              <el-form-item label="时间段">
                <el-time-picker
                  v-model="doNotDisturb.startTime"
                  placeholder="开始时间"
                  format="HH:mm"
                />
                <span class="time-separator">至</span>
                <el-time-picker
                  v-model="doNotDisturb.endTime"
                  placeholder="结束时间"
                  format="HH:mm"
                />
              </el-form-item>
              <el-form-item label="重要通知">
                <el-radio-group v-model="doNotDisturb.importantAllow">
                  <el-radio :label="true">仍然通知</el-radio>
                  <el-radio :label="false">同样屏蔽</el-radio>
                </el-radio-group>
              </el-form-item>
            </template>
          </el-form>
        </el-card>
      </div>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="settingsDrawer = false">取消</el-button>
          <el-button type="primary" @click="handleSaveSettings">保存设置</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import {
  Bell, Message, Warning, Timer,
  Check, Delete, Refresh, Search,
  InfoFilled, ChatDotSquare, CircleCheck,
  Document, Folder, QuestionFilled,
  ArrowUp, ArrowDown, ChatRound, Setting
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

// 通知统计
const notificationStats = ref([
  {
    label: '全部通知',
    value: '128',
    icon: 'Bell',
    type: 'primary'
  },
  {
    label: '未读通知',
    value: '12',
    icon: 'Message',
    type: 'danger'
  },
  {
    label: '今日通知',
    value: '24',
    icon: 'Timer',
    type: 'success'
  },
  {
    label: '预警通知',
    value: '3',
    icon: 'Warning',
    type: 'warning'
  }
])

// 搜索相关
const searchForm = ref({
  type: '',
  status: '',
  timeRange: []
})

// 日期快捷选项
const dateShortcuts = [
  {
    text: '最近一天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24)
      return [start, end]
    }
  },
  {
    text: '最近一周',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      return [start, end]
    }
  },
  {
    text: '最近一月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      return [start, end]
    }
  }
]

// 列表数据
const loading = ref(false)
const notificationList = ref([
  {
    id: 1,
    type: 'SYSTEM',
    title: '系统更新通知',
    content: '系统将于今晚 23:00 进行例行维护更新，预计耗时 30 分钟，请提前做好相关准备。',
    from: '系统管理员',
    createTime: '2024-01-15 10:00:00',
    read: false
  },
  {
    id: 2,
    type: 'TASK',
    title: '任务审批提醒',
    content: '您有一个新的任务待审批，请及时处理。',
    from: '工作流系统',
    createTime: '2024-01-15 09:30:00',
    read: true
  }
])

// 分页配置
const page = ref({
  current: 1,
  size: 10
})
const total = ref(100)

// 获取通知图标
const getNotificationIcon = (type) => {
  const icons = {
    'SYSTEM': 'InfoFilled',
    'TASK': 'ChatDotSquare',
    'MESSAGE': 'Message',
    'ALERT': 'Warning'
  }
  return icons[type] || 'Message'
}

// 搜索方法
const handleSearch = () => {
  page.value.current = 1
  fetchList()
}

const resetSearch = () => {
  searchForm.value = {
    type: '',
    status: '',
    timeRange: []
  }
  handleSearch()
}

// 分页方法
const handleSizeChange = (val) => {
  page.value.size = val
  fetchList()
}

const handleCurrentChange = (val) => {
  page.value.current = val
  fetchList()
}

// 获取列表数据
const fetchList = () => {
  loading.value = true
  // 模拟接口请求
  setTimeout(() => {
    loading.value = false
  }, 500)
}

// 查看通知
const handleViewNotification = (item) => {
  if (!item.read) {
    handleRead(item)
  }
  // TODO: 实现查看通知详情
}

// 标记已读
const handleRead = async (item) => {
  try {
    // 调用标记已读接口
    await new Promise(resolve => setTimeout(resolve, 500))
    item.read = true
    ElMessage.success('已标记为已读')
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 全部已读
const handleReadAll = () => {
  ElMessageBox.confirm(
    '确定要将所有未读通知标记为已读吗？',
    '操作确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(async () => {
    try {
      // 调用全部已读接口
      await new Promise(resolve => setTimeout(resolve, 500))
      notificationList.value.forEach(item => {
        item.read = true
      })
      ElMessage.success('操作成功')
    } catch (error) {
      ElMessage.error('操作失败')
    }
  })
}

// 删除通知
const handleDelete = (item) => {
  ElMessageBox.confirm(
    '确定要删除该条通知吗？',
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      // 调用删除接口
      await new Promise(resolve => setTimeout(resolve, 500))
      notificationList.value = notificationList.value.filter(i => i.id !== item.id)
      ElMessage.success('删除成功')
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

// 清空通知
const handleClearAll = () => {
  ElMessageBox.confirm(
    '确定要清空所有通知吗？此操作不可恢复！',
    '清空确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      // 调用清空接口
      await new Promise(resolve => setTimeout(resolve, 500))
      notificationList.value = []
      ElMessage.success('清空成功')
    } catch (error) {
      ElMessage.error('清空失败')
    }
  })
}

// 刷新列表
const refreshList = () => {
  fetchList()
}

// 通知详情相关
const detailDrawer = ref(false)
const currentNotification = ref(null)

// 文件大小格式化
const formatFileSize = (size) => {
  if (size < 1024) {
    return size + 'B'
  } else if (size < 1024 * 1024) {
    return (size / 1024).toFixed(2) + 'KB'
  } else if (size < 1024 * 1024 * 1024) {
    return (size / (1024 * 1024)).toFixed(2) + 'MB'
  } else {
    return (size / (1024 * 1024 * 1024)).toFixed(2) + 'GB'
  }
}

// 获取操作记录颜色
const getRecordColor = (type) => {
  const colors = {
    'create': '#409EFF',
    'read': '#67C23A',
    'delete': '#F56C6C'
  }
  return colors[type] || '#909399'
}

// 在详情中标记已读
const handleReadInDetail = async () => {
  await handleRead(currentNotification.value)
  detailDrawer.value = false
}

// 下载文件
const handleDownloadFile = (file) => {
  // TODO: 实现文件下载逻辑
  ElMessage.success('开始下载：' + file.name)
}

// 通知分析相关
const analysisDrawer = ref(false)
const analysisRange = ref('week')
const trendType = ref('count')

// 分析指标数据
const analysisMetrics = ref([
  {
    label: '通知总量',
    value: '1,234',
    unit: '条',
    trend: 12.5,
    tip: '所选时间范围内的通知总数',
    chartData: [30, 40, 35, 50, 45, 55, 40]
  },
  {
    label: '平均处理时间',
    value: '2.5',
    unit: '小时',
    trend: -8.3,
    tip: '从发送到已读的平均时间',
    chartData: [2.8, 2.6, 2.4, 2.5, 2.3, 2.5, 2.7]
  },
  {
    label: '已读率',
    value: '92.5',
    unit: '%',
    trend: 3.6,
    tip: '已读通知占总通知的比例',
    chartData: [88, 90, 89, 91, 92, 93, 92.5]
  }
])

// 图表引用
const metricChartRefs = ref([])
const trendChartRef = ref(null)
const typeChartRef = ref(null)
const timeChartRef = ref(null)

// 初始化指标迷你图表
const initMetricCharts = () => {
  nextTick(() => {
    const chartElements = document.querySelectorAll('.metric-chart')
    chartElements.forEach((el, index) => {
      const chart = echarts.init(el)
      const option = {
        grid: {
          top: 2,
          right: 2,
          bottom: 2,
          left: 2
        },
        xAxis: {
          type: 'category',
          show: false,
          boundaryGap: false
        },
        yAxis: {
          type: 'value',
          show: false
        },
        series: [{
          data: analysisMetrics.value[index].chartData,
          type: 'line',
          smooth: true,
          symbol: 'none',
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
            ])
          },
          lineStyle: {
            color: '#409EFF'
          }
        }]
      }
      chart.setOption(option)
      metricChartRefs.value[index] = chart
    })
  })
}

// 初始化趋势图表
const initTrendChart = () => {
  if (!trendChartRef.value) return
  const chart = echarts.init(trendChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['系统通知', '任务通知', '消息通知', '预警通知']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '系统通知',
        type: 'line',
        stack: 'Total',
        areaStyle: {},
        emphasis: {
          focus: 'series'
        },
        data: [120, 132, 101, 134, 90, 230, 210]
      },
      {
        name: '任务通知',
        type: 'line',
        stack: 'Total',
        areaStyle: {},
        emphasis: {
          focus: 'series'
        },
        data: [220, 182, 191, 234, 290, 330, 310]
      },
      {
        name: '消息通知',
        type: 'line',
        stack: 'Total',
        areaStyle: {},
        emphasis: {
          focus: 'series'
        },
        data: [150, 232, 201, 154, 190, 330, 410]
      },
      {
        name: '预警通知',
        type: 'line',
        stack: 'Total',
        areaStyle: {},
        emphasis: {
          focus: 'series'
        },
        data: [320, 332, 301, 334, 390, 330, 320]
      }
    ]
  }
  chart.setOption(option)
}

// 初始化分布图表
const initDistributionCharts = () => {
  // 类型分布图
  if (typeChartRef.value) {
    const typeChart = echarts.init(typeChartRef.value)
    const typeOption = {
      tooltip: {
        trigger: 'item'
      },
      legend: {
        orient: 'vertical',
        left: 'left'
      },
      series: [
        {
          name: '通知类型',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: false,
            position: 'center'
          },
          emphasis: {
            label: {
              show: true,
              fontSize: 20,
              fontWeight: 'bold'
            }
          },
          labelLine: {
            show: false
          },
          data: [
            { value: 1048, name: '系统通知' },
            { value: 735, name: '任务通知' },
            { value: 580, name: '消息通知' },
            { value: 484, name: '预警通知' }
          ]
        }
      ]
    }
    typeChart.setOption(typeOption)
  }

  // 时段分布图
  if (timeChartRef.value) {
    const timeChart = echarts.init(timeChartRef.value)
    const timeOption = {
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: [
        {
          type: 'category',
          data: ['00:00', '03:00', '06:00', '09:00', '12:00', '15:00', '18:00', '21:00'],
          axisTick: {
            alignWithLabel: true
          }
        }
      ],
      yAxis: [
        {
          type: 'value'
        }
      ],
      series: [
        {
          name: '通知数量',
          type: 'bar',
          barWidth: '60%',
          data: [10, 5, 8, 120, 80, 90, 70, 20]
        }
      ]
    }
    timeChart.setOption(timeOption)
  }
}

// 更新分析数据
const handleAnalysisRangeChange = () => {
  // TODO: 根据时间范围更新数据
  initTrendChart()
  initDistributionCharts()
}

// 监听窗口大小变化
window.addEventListener('resize', () => {
  metricChartRefs.value.forEach(chart => chart?.resize())
  trendChartRef.value && echarts.getInstanceByDom(trendChartRef.value)?.resize()
  typeChartRef.value && echarts.getInstanceByDom(typeChartRef.value)?.resize()
  timeChartRef.value && echarts.getInstanceByDom(timeChartRef.value)?.resize()
})

// 在 onMounted 中添加
onMounted(() => {

  initMetricCharts()
  initTrendChart()
  initDistributionCharts()
})

// 通知设置相关
const settingsDrawer = ref(false)

// 通知方式设置
const notifyMethods = reactive({
  site: true,
  email: false,
  emailAddress: '',
  sms: false,
  phoneNumber: '',
  wechat: false
})

// 测试邮件通知
const handleTestEmail = async () => {
  if (!notifyMethods.emailAddress) {
    ElMessage.warning('请先输入邮箱地址')
    return
  }
  try {
    // 调用发送测试邮件接口
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('测试邮件已发送')
  } catch (error) {
    ElMessage.error('发送失败')
  }
}

// 测试短信通知
const handleTestSms = async () => {
  if (!notifyMethods.phoneNumber) {
    ElMessage.warning('请先输入手机号')
    return
  }
  try {
    // 调用发送测试短信接口
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('测试短信已发送')
  } catch (error) {
    ElMessage.error('发送失败')
  }
}

// 微信二维码
const qrCodeUrl = ref('https://example.com/qr-code.png')

// 订阅设置
const subscriptions = ref([
  {
    type: 'system',
    icon: 'InfoFilled',
    title: '系统通知',
    description: '系统更新、维护等重要通知',
    important: true,
    methods: ['site', 'email']
  },
  {
    type: 'task',
    icon: 'ChatDotSquare',
    title: '任务通知',
    description: '任务分配、审批等工作相关通知',
    important: false,
    methods: ['site']
  },
  {
    type: 'message',
    icon: 'Message',
    title: '消息通知',
    description: '评论、回复等互动消息',
    important: false,
    methods: ['site']
  },
  {
    type: 'alert',
    icon: 'Warning',
    title: '预警通知',
    description: '系统异常、安全预警等紧急通知',
    important: true,
    methods: ['site', 'email', 'sms']
  }
])

// 免打扰设置
const doNotDisturb = reactive({
  enabled: false,
  startTime: null,
  endTime: null,
  importantAllow: true
})

// 保存设置
const handleSaveSettings = async () => {
  try {
    // 调用保存设置接口
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('设置已保存')
    settingsDrawer.value = false
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

// 添加到工具栏按钮
const toolbarButtons = ref([
  {
    icon: 'Setting',
    tooltip: '通知设置',
    handler: () => settingsDrawer.value = true
  }
])
</script>

<style scoped>
.notification-container {
  padding: 20px;
}

.overview-section {
  margin-bottom: 20px;
}

.stat-card {
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 8px;
}

.stat-card.primary .stat-icon {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.stat-card.success .stat-icon {
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
}

.stat-card.warning .stat-icon {
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
}

.stat-card.danger .stat-icon {
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  line-height: 1.5;
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h3 {
  margin: 0;
}

.search-form {
  margin-bottom: 20px;
  padding: 20px;
  background: var(--el-bg-color-page);
  border-radius: 4px;
}

.search-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.notification-list {
  min-height: 300px;
}

.notification-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  transition: all 0.3s;
}

.notification-item:last-child {
  border-bottom: none;
}

.notification-item:hover {
  background: var(--el-bg-color-page);
}

.notification-item.unread {
  background: var(--el-color-primary-light-9);
}

.item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  flex-shrink: 0;
}

.item-icon.system {
  background: var(--el-color-info-light-9);
  color: var(--el-color-info);
}

.item-icon.task {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.item-icon.message {
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
}

.item-icon.alert {
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.item-title {
  font-size: 16px;
  font-weight: bold;
}

.item-body {
  margin-bottom: 8px;
  color: var(--el-text-color-regular);
}

.item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 16px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.item-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
}

.notification-item:hover .item-actions {
  opacity: 1;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

/* 通知详情样式 */
.drawer-header {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.header-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  flex-shrink: 0;
}

.header-content {
  flex: 1;
  min-width: 0;
}

.header-content h3 {
  margin: 0 0 8px;
  font-size: 18px;
}

.header-info {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.notification-detail {
  padding: 0 20px;
}

.detail-section {
  margin-bottom: 24px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: bold;
}

.content-box {
  padding: 16px;
  background: var(--el-bg-color-page);
  border-radius: 4px;
  line-height: 1.6;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--el-bg-color-page);
  border-radius: 4px;
  margin-bottom: 8px;
}

.file-item:last-child {
  margin-bottom: 0;
}

.file-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-size {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

/* 通知分析样式 */
.analysis-container {
  padding: 0 20px;
}

.analysis-header {
  margin-bottom: 24px;
  text-align: center;
}

.analysis-metrics {
  margin-bottom: 24px;
}

.metric-card {
  height: 180px;
  padding: 20px;
}

.metric-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.metric-label {
  font-size: 14px;
  color: var(--el-text-color-regular);
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  line-height: 1.5;
}

.metric-unit {
  font-size: 14px;
  font-weight: normal;
  margin-left: 4px;
  color: var(--el-text-color-secondary);
}

.metric-trend {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  font-size: 13px;
}

.metric-trend .up {
  color: #f56c6c;
}

.metric-trend .down {
  color: #67c23a;
}

.trend-label {
  color: var(--el-text-color-secondary);
}

.metric-chart {
  height: 50px;
  margin-top: 12px;
}

.trend-card {
  margin-bottom: 24px;
}

.trend-chart {
  height: 300px;
}

.distribution-section {
  margin-bottom: 24px;
}

.distribution-chart {
  height: 300px;
}

/* 通知设置样式 */
.settings-container {
  padding: 0 20px;
}

.settings-card {
  margin-bottom: 20px;
}

.settings-card:last-child {
  margin-bottom: 0;
}

.method-config {
  margin-top: 12px;
}

.wechat-bind {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 20px;
}

.qr-code {
  width: 200px;
  height: 200px;
  background: var(--el-bg-color-page);
}

.bind-tip {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

.subscription-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.subscription-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: var(--el-bg-color-page);
  border-radius: 4px;
  transition: all 0.3s;
}

.subscription-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.sub-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.sub-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  flex-shrink: 0;
}

.sub-content {
  flex: 1;
  min-width: 0;
}

.sub-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-weight: bold;
}

.sub-desc {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.sub-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.time-separator {
  margin: 0 12px;
  color: var(--el-text-color-secondary);
}

/* 工具栏按钮样式 */
.toolbar-buttons {
  display: flex;
  gap: 8px;
}
</style> 