<template>
  <div class="monitor-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-title">
        <el-icon><Monitor /></el-icon>
        <span>数据源监控</span>
      </div>
      <div class="header-actions">
        <el-button-group>
          <el-button type="primary" @click="refreshData">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
          <el-button type="success" @click="handleAddAlert">
            <el-icon><Bell /></el-icon>新建告警
          </el-button>
        </el-button-group>
        <el-radio-group v-model="autoRefresh" size="small" class="ml-4">
          <el-radio-button :value="0">手动刷新</el-radio-button>
          <el-radio-button :value="30">30秒</el-radio-button>
          <el-radio-button :value="60">1分钟</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 概览统计卡片 -->
    <el-row :gutter="20" class="overview-section">
      <el-col :span="6" v-for="item in overviewData" :key="item.id">
        <el-card :class="['overview-card', item.type]" shadow="hover">
          <div class="overview-content">
            <div class="overview-icon">
              <el-icon><component :is="item.icon" /></el-icon>
            </div>
            <div class="overview-info">
              <div class="overview-value">{{ item.value }}</div>
              <div class="overview-label">{{ item.label }}</div>
              <div class="overview-trend" :class="item.trend">
                <span>{{ item.trendValue }}</span>
                <el-icon><component :is="item.trend === 'up' ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 监控详情区域 -->
    <el-row :gutter="20" class="monitor-details">
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header>
            <div class="chart-header">
              <span class="title">性能监控</span>
              <div class="chart-controls">
                <el-select v-model="selectedSource" placeholder="选择数据源" class="mr-2">
                  <el-option v-for="source in dataSources" :key="source.id" :label="source.name" :value="source.id" />
                </el-select>
                <el-radio-group v-model="timeRange" size="small">
                  <el-radio-button value="1h">1小时</el-radio-button>
                  <el-radio-button value="6h">6小时</el-radio-button>
                  <el-radio-button value="24h">24小时</el-radio-button>
                  <el-radio-button value="7d">7天</el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </template>
          <div class="charts-container">
            <div ref="connectionChartRef" class="sub-chart"></div>
            <div ref="responseChartRef" class="sub-chart"></div>
            <div ref="throughputChartRef" class="sub-chart"></div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="alert-card">
          <template #header>
            <div class="alert-header">
              <span class="title">告警统计</span>
              <el-tooltip content="展示不同级别的告警分布情况" placement="top">
                <el-icon><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
          <div ref="alertPieChartRef" class="alert-chart"></div>
          <div class="alert-summary">
            <div v-for="item in alertSummary" :key="item.level" class="summary-item">
              <el-tag :type="getAlertType(item.level)">{{ item.label }}</el-tag>
              <span class="count">{{ item.count }}</span>
              <span class="percentage">{{ item.percentage }}%</span>
            </div>
          </div>
        </el-card>

        <el-card class="log-card">
          <template #header>
            <div class="log-header">
              <span class="title">实时日志</span>
              <el-switch v-model="autoScroll" active-text="自动滚动" />
            </div>
          </template>
          <div class="log-container" ref="logContainer">
            <div v-for="log in realtimeLogs" :key="log.id" :class="['log-item', log.level]">
              <span class="log-time">{{ log.time }}</span>
              <span class="log-level">{{ log.level.toUpperCase() }}</span>
              <span class="log-content">{{ log.content }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 告警列表 -->
    <el-card class="alert-list-card">
      <template #header>
        <div class="list-header">
          <span class="title">告警记录</span>
          <div class="header-actions">
            <el-radio-group v-model="alertViewMode" size="small">
              <el-radio-button value="all">全部</el-radio-button>
              <el-radio-button value="unhandled">未处理</el-radio-button>
              <el-radio-button value="handled">已处理</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>
      
      <el-table :data="alertList" style="width: 100%" v-loading="loading">
        <el-table-column type="expand">
          <template #default="props">
            <div class="alert-detail">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="告警规则">{{ props.row.rule }}</el-descriptions-item>
                <el-descriptions-item label="触发条件">{{ props.row.condition }}</el-descriptions-item>
                <el-descriptions-item label="影响范围">{{ props.row.scope }}</el-descriptions-item>
                <el-descriptions-item label="处理建议">{{ props.row.suggestion }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="time" label="告警时间" width="180" />
        <el-table-column prop="source" label="数据源" width="150" />
        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="getAlertType(row.level)">{{ row.level }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="告警内容" min-width="250" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'handled' ? 'success' : 'warning'">
              {{ row.status === 'handled' ? '已处理' : '未处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button type="primary" link @click="handleAlert(row)">
                {{ row.status === 'handled' ? '查看' : '处理' }}
              </el-button>
              <el-button type="info" link @click="showHistory(row)">历史</el-button>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
      
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 新建告警规则对话框 -->
    <el-dialog
      v-model="alertDialogVisible"
      title="新建告警规则"
      width="600px"
    >
      <el-form ref="alertFormRef" :model="alertForm" :rules="alertRules" label-width="100px">
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="alertForm.name" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="数据源" prop="sourceId">
          <el-select v-model="alertForm.sourceId" placeholder="请选择数据源">
            <el-option v-for="source in dataSources" :key="source.id" :label="source.name" :value="source.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="监控指标" prop="metric">
          <el-select v-model="alertForm.metric" placeholder="请选择监控指标">
            <el-option label="连接数" value="connections" />
            <el-option label="响应时间" value="response_time" />
            <el-option label="QPS" value="qps" />
            <el-option label="错误率" value="error_rate" />
          </el-select>
        </el-form-item>
        <el-form-item label="告警级别" prop="level">
          <el-select v-model="alertForm.level" placeholder="请选择告警级别">
            <el-option label="严重" value="critical" />
            <el-option label="警告" value="warning" />
            <el-option label="提示" value="info" />
          </el-select>
        </el-form-item>
        <el-form-item label="触发条件" prop="condition">
          <el-input v-model="alertForm.condition" placeholder="请输入触发条件">
            <template #append>
              <el-select v-model="alertForm.operator" style="width: 100px">
                <el-option label="大于" value=">" />
                <el-option label="小于" value="<" />
                <el-option label="等于" value="=" />
              </el-select>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="持续时间" prop="duration">
          <el-input-number v-model="alertForm.duration" :min="1" :max="60" />
          <span class="ml-2">分钟</span>
        </el-form-item>
        <el-form-item label="通知方式" prop="notifyType">
          <el-checkbox-group v-model="alertForm.notifyType">
            <el-checkbox label="email">邮件</el-checkbox>
            <el-checkbox label="sms">短信</el-checkbox>
            <el-checkbox label="webhook">Webhook</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="alertDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitAlertForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { Monitor, Refresh, Bell, ArrowUp, ArrowDown, QuestionFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

// 自动刷新控制
const autoRefresh = ref(0)
let refreshTimer = null

// 数据源选择
const selectedSource = ref('')
const dataSources = ref([
  { id: 1, name: 'MySQL主库' },
  { id: 2, name: 'StarRocks集群' },
  { id: 3, name: 'Delta Lake存储' }
])

// 时间范围
const timeRange = ref('1h')

// 概览数据
const overviewData = ref([
  {
    id: 1,
    label: '在线数据源',
    value: '12/15',
    icon: 'Connection',
    type: 'success',
    trend: 'up',
    trendValue: '+2'
  },
  {
    id: 2,
    label: '平均响应时间',
    value: '125ms',
    icon: 'Timer',
    type: 'warning',
    trend: 'up',
    trendValue: '+15ms'
  },
  {
    id: 3,
    label: '活跃告警',
    value: '3',
    icon: 'Bell',
    type: 'danger',
    trend: 'down',
    trendValue: '-1'
  },
  {
    id: 4,
    label: '健康评分',
    value: '92',
    icon: 'Check',
    type: 'info',
    trend: 'up',
    trendValue: '+2'
  }
])

// 告警统计
const alertSummary = ref([
  { level: 'critical', label: '严重', count: 2, percentage: 20 },
  { level: 'warning', label: '警告', count: 5, percentage: 50 },
  { level: 'info', label: '提示', count: 3, percentage: 30 }
])

// 实时日志
const autoScroll = ref(true)
const logContainer = ref(null)
const realtimeLogs = ref([
  { id: 1, time: '10:00:00', level: 'info', content: 'MySQL主库连接正常' },
  { id: 2, time: '10:00:30', level: 'warning', content: 'StarRocks响应时间超过阈值' },
  { id: 3, time: '10:01:00', level: 'error', content: 'Delta Lake存储连接异常' }
])

// 告警列表
const loading = ref(false)
const alertViewMode = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(100)

const alertList = ref([
  {
    time: '2024-03-20 10:00:00',
    source: 'MySQL主库',
    level: 'critical',
    title: '连接数超过阈值',
    status: 'unhandled',
    rule: '连接数监控规则',
    condition: '连接数 > 1000',
    scope: '数据库连接池',
    suggestion: '检查连接池配置，适当增加最大连接数'
  },
  // ... 更多告警数据
])

// 新建告警表单
const alertDialogVisible = ref(false)
const alertFormRef = ref(null)
const alertForm = ref({
  name: '',
  sourceId: '',
  metric: '',
  level: '',
  condition: '',
  operator: '>',
  duration: 5,
  notifyType: []
})

const alertRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  sourceId: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  metric: [{ required: true, message: '请选择监控指标', trigger: 'change' }],
  level: [{ required: true, message: '请选择告警级别', trigger: 'change' }],
  condition: [{ required: true, message: '请输入触发条件', trigger: 'blur' }]
}

// 图表容器引用
const connectionChartRef = ref(null)
const responseChartRef = ref(null)
const throughputChartRef = ref(null)
const alertPieChartRef = ref(null)

// 图表实例
let connectionChart = null
let responseChart = null
let throughputChart = null
let alertPieChart = null

// 方法定义
const refreshData = () => {
  loading.value = true
  // 模拟数据刷新
  setTimeout(() => {
    loading.value = false
  }, 1000)
}

const handleAddAlert = () => {
  alertDialogVisible.value = true
}

const submitAlertForm = async () => {
  if (!alertFormRef.value) return
  
  await alertFormRef.value.validate((valid) => {
    if (valid) {
      // TODO: 提交表单
      alertDialogVisible.value = false
    }
  })
}

const getAlertType = (level) => {
  const types = {
    critical: 'danger',
    warning: 'warning',
    info: 'info'
  }
  return types[level] || 'info'
}

const handleAlert = (row) => {
  console.log('处理告警:', row)
}

const showHistory = (row) => {
  console.log('查看历史:', row)
}

const handleSizeChange = (val) => {
  pageSize.value = val
  refreshData()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  refreshData()
}

// 初始化图表
const initCharts = () => {
  // 连接数图表
  if (!connectionChartRef.value) return
  connectionChart = echarts.init(connectionChartRef.value)
  connectionChart.setOption({
    title: { text: '连接数趋势' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'time' },
    yAxis: { type: 'value' },
    series: [{
      name: '连接数',
      type: 'line',
      smooth: true,
      data: generateMockData()
    }]
  })

  // 响应时间图表
  if (!responseChartRef.value) return
  responseChart = echarts.init(responseChartRef.value)
  responseChart.setOption({
    title: { text: '响应时间趋势' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'time' },
    yAxis: { type: 'value' },
    series: [{
      name: '响应时间',
      type: 'line',
      smooth: true,
      data: generateMockData()
    }]
  })

  // 吞吐量图表
  if (!throughputChartRef.value) return
  throughputChart = echarts.init(throughputChartRef.value)
  throughputChart.setOption({
    title: { text: 'QPS趋势' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'time' },
    yAxis: { type: 'value' },
    series: [{
      name: 'QPS',
      type: 'line',
      smooth: true,
      data: generateMockData()
    }]
  })

  // 告警饼图
  if (!alertPieChartRef.value) return
  alertPieChart = echarts.init(alertPieChartRef.value)
  alertPieChart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false
      },
      emphasis: {
        label: {
          show: true,
          fontSize: '20',
          fontWeight: 'bold'
        }
      },
      labelLine: {
        show: false
      },
      data: [
        { value: 2, name: '严重' },
        { value: 5, name: '警告' },
        { value: 3, name: '提示' }
      ]
    }]
  })
}

// 生成模拟数据
const generateMockData = () => {
  const data = []
  const now = new Date()
  for (let i = 0; i < 100; i++) {
    data.push({
      name: new Date(now - (100 - i) * 1000),
      value: [
        new Date(now - (100 - i) * 1000),
        Math.random() * 100
      ]
    })
  }
  return data
}

// 生命周期钩子
onMounted(() => {
  nextTick(() => {
    initCharts()
    
    // 监听窗口大小变化
    window.addEventListener('resize', () => {
      connectionChart?.resize()
      responseChart?.resize()
      throughputChart?.resize()
      alertPieChart?.resize()
    })
  })

  // 设置自动刷新
  watch(autoRefresh, (newVal) => {
    if (refreshTimer) {
      clearInterval(refreshTimer)
    }
    if (newVal > 0) {
      refreshTimer = setInterval(refreshData, newVal * 1000)
    }
  })
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  // 销毁图表实例
  connectionChart?.dispose()
  responseChart?.dispose()
  throughputChart?.dispose()
  alertPieChart?.dispose()
})
</script>

<style lang="scss" scoped>
.monitor-container {
  padding: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .header-title {
      font-size: 20px;
      display: flex;
      align-items: center;
      gap: 8px;
    }
  }

  .overview-section {
    margin-bottom: 20px;

    .overview-card {
      height: 120px;
      transition: all 0.3s;
      
      &:hover {
        transform: translateY(-5px);
      }

      &.success {
        background: linear-gradient(135deg, rgba(103, 194, 58, 0.1), rgba(103, 194, 58, 0.05));
      }
      
      &.warning {
        background: linear-gradient(135deg, rgba(230, 162, 60, 0.1), rgba(230, 162, 60, 0.05));
      }
      
      &.danger {
        background: linear-gradient(135deg, rgba(245, 108, 108, 0.1), rgba(245, 108, 108, 0.05));
      }
      
      &.info {
        background: linear-gradient(135deg, rgba(144, 147, 153, 0.1), rgba(144, 147, 153, 0.05));
      }

      .overview-content {
        display: flex;
        align-items: center;
        height: 100%;
        padding: 20px;

        .overview-icon {
          font-size: 48px;
          margin-right: 20px;
          opacity: 0.7;
        }

        .overview-info {
          flex: 1;

          .overview-value {
            font-size: 24px;
            font-weight: bold;
            margin-bottom: 5px;
          }

          .overview-label {
            color: #909399;
            margin-bottom: 5px;
          }

          .overview-trend {
            font-size: 12px;
            display: flex;
            align-items: center;
            gap: 4px;

            &.up {
              color: #67C23A;
            }

            &.down {
              color: #F56C6C;
            }
          }
        }
      }
    }
  }

  .monitor-details {
    margin-bottom: 20px;

    .chart-card {
      margin-bottom: 20px;

      .chart-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .charts-container {
        .sub-chart {
          height: 200px;
          margin-bottom: 20px;

          &:last-child {
            margin-bottom: 0;
          }
        }
      }
    }

    .alert-card {
      margin-bottom: 20px;

      .alert-chart {
        height: 300px;
      }

      .alert-summary {
        padding: 20px;

        .summary-item {
          display: flex;
          align-items: center;
          margin-bottom: 10px;
          
          .count {
            margin: 0 10px;
            font-weight: bold;
          }
          
          .percentage {
            color: #909399;
          }
        }
      }
    }

    .log-card {
      .log-container {
        height: 300px;
        overflow-y: auto;
        padding: 10px;
        background: #1e1e1e;
        border-radius: 4px;
        font-family: monospace;

        .log-item {
          margin-bottom: 8px;
          color: #fff;
          font-size: 12px;
          
          &.info {
            color: #909399;
          }
          
          &.warning {
            color: #E6A23C;
          }
          
          &.error {
            color: #F56C6C;
          }

          .log-time {
            margin-right: 10px;
            color: #606266;
          }

          .log-level {
            margin-right: 10px;
            font-weight: bold;
          }
        }
      }
    }
  }

  .alert-list-card {
    .list-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .alert-detail {
      padding: 20px;
    }
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style> 