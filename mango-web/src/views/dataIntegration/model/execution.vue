<template>
  <div class="execution-detail">
    <div class="page-header">
      <div class="header-left">
        <el-button link @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>返回
        </el-button>
        <h2 class="page-title">执行详情</h2>
        <el-tag :type="getStatusType(executionInfo.status)">{{ executionInfo.status }}</el-tag>
      </div>
      <div class="header-right">
        <el-button-group>
          <el-button type="primary" @click="handleRerun" :disabled="!canRerun">
            <el-icon><VideoPlay /></el-icon>重新执行
          </el-button>
          <el-button type="warning" @click="handleSkip" :disabled="!canSkip">
            <el-icon><Position /></el-icon>跳过错误
          </el-button>
          <el-button type="danger" @click="handleTerminate" :disabled="!canTerminate">
            <el-icon><CircleClose /></el-icon>终止执行
          </el-button>
        </el-button-group>
      </div>
    </div>

    <el-row :gutter="20" class="detail-content">
      <el-col :span="16">
        <!-- 基本信息 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>基本信息</span>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="执行ID">{{ executionInfo.id }}</el-descriptions-item>
            <el-descriptions-item label="任务名称">{{ executionInfo.taskName }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ executionInfo.startTime }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ executionInfo.endTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="执行类型">{{ executionInfo.type }}</el-descriptions-item>
            <el-descriptions-item label="触发方式">{{ executionInfo.trigger }}</el-descriptions-item>
            <el-descriptions-item label="执行人">{{ executionInfo.executor }}</el-descriptions-item>
            <el-descriptions-item label="重试次数">{{ executionInfo.retryCount }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 执行进度 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>执行进度</span>
              <el-button link type="primary" @click="handleRefreshProgress">
                <el-icon><Refresh /></el-icon>刷新
              </el-button>
            </div>
          </template>
          <div class="progress-view">
            <div class="progress-info">
              <div class="progress-item">
                <span class="label">总进度</span>
                <el-progress
                  :percentage="executionInfo.progress"
                  :status="getProgressStatus(executionInfo.status)"
                />
              </div>
              <div class="metrics">
                <div class="metric-item">
                  <span class="value">{{ formatNumber(executionInfo.processedRecords) }}</span>
                  <span class="label">处理记录数</span>
                </div>
                <div class="metric-item">
                  <span class="value">{{ formatSpeed(executionInfo.speed) }}</span>
                  <span class="label">处理速度</span>
                </div>
                <div class="metric-item">
                  <span class="value">{{ formatNumber(executionInfo.errors) }}</span>
                  <span class="label">错误数</span>
                </div>
                <div class="metric-item">
                  <span class="value">{{ formatDuration(executionInfo.duration) }}</span>
                  <span class="label">执行时长</span>
                </div>
              </div>
            </div>
            <div class="stage-progress">
              <div
                v-for="stage in executionInfo.stages"
                :key="stage.id"
                class="stage-item"
              >
                <div class="stage-header">
                  <span class="stage-name">{{ stage.name }}</span>
                  <el-tag size="small" :type="getStatusType(stage.status)">
                    {{ stage.status }}
                  </el-tag>
                </div>
                <el-progress
                  :percentage="stage.progress"
                  :status="getProgressStatus(stage.status)"
                />
                <div class="stage-metrics">
                  <span>处理: {{ formatNumber(stage.processedRecords) }}</span>
                  <span>耗时: {{ formatDuration(stage.duration) }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 执行日志 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>执行日志</span>
              <div class="header-right">
                <el-radio-group v-model="logLevel" size="small">
                  <el-radio-button value="ALL">全部</el-radio-button>
                  <el-radio-button value="INFO">信息</el-radio-button>
                  <el-radio-button value="WARN">警告</el-radio-button>
                  <el-radio-button value="ERROR">错误</el-radio-button>
                </el-radio-group>
                <el-input
                  v-model="logSearch"
                  placeholder="搜索日志"
                  prefix-icon="Search"
                  style="width: 200px; margin-left: 10px;"
                />
                <el-button-group style="margin-left: 10px;">
                  <el-button @click="handleRefreshLog">
                    <el-icon><Refresh /></el-icon>刷新
                  </el-button>
                  <el-button @click="handleDownloadLog">
                    <el-icon><Download /></el-icon>下载
                  </el-button>
                </el-button-group>
              </div>
            </div>
          </template>
          <div class="log-viewer">
            <div
              v-for="(log, index) in filteredLogs"
              :key="index"
              :class="['log-line', log.level.toLowerCase()]"
            >
              <span class="time">{{ log.time }}</span>
              <span class="level">{{ log.level }}</span>
              <span class="message">{{ log.message }}</span>
            </div>
          </div>
        </el-card>

        <!-- 依赖关系图 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>依赖关系</span>
            </div>
          </template>
          <div class="dependency-graph-container">
            <DependencyGraph :data="dependencyData" />
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <!-- 资源监控 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>资源监控</span>
              <el-radio-group v-model="monitorRange" size="small">
                <el-radio-button value="10m">近10分钟</el-radio-button>
                <el-radio-button value="30m">近30分钟</el-radio-button>
                <el-radio-button value="1h">近1小时</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="monitor-charts">
            <div class="chart-item">
              <div class="chart-title">CPU使用率</div>
              <div class="chart-container" ref="cpuChart"></div>
            </div>
            <div class="chart-item">
              <div class="chart-title">内存使用率</div>
              <div class="chart-container" ref="memoryChart"></div>
            </div>
            <div class="chart-item">
              <div class="chart-title">磁盘I/O</div>
              <div class="chart-container" ref="diskChart"></div>
            </div>
          </div>
        </el-card>

        <!-- 错误统计 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>错误统计</span>
            </div>
          </template>
          <div class="error-stats">
            <el-table :data="errorStats" style="width: 100%">
              <el-table-column prop="type" label="错误类型" />
              <el-table-column prop="count" label="出现次数" width="100" align="right" />
              <el-table-column prop="percentage" label="占比" width="100">
                <template #default="{ row }">
                  <el-progress
                    :percentage="row.percentage"
                    :color="row.color"
                    :show-text="false"
                    :stroke-width="4"
                  />
                  {{ row.percentage }}%
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>

        <!-- 性能分析 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>性能分析</span>
            </div>
          </template>
          <div class="performance-analysis">
            <div class="analysis-item" v-for="(item, index) in performanceAnalysis" :key="index">
              <div class="item-header">
                <span class="title">{{ item.title }}</span>
                <el-tag :type="item.status" size="small">{{ item.statusText }}</el-tag>
              </div>
              <div class="item-content">
                <span class="value">{{ item.value }}</span>
                <span class="unit">{{ item.unit }}</span>
              </div>
              <div class="item-footer">
                <span class="description">{{ item.description }}</span>
                <el-button v-if="item.suggestion" link type="primary" size="small">
                  查看建议
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import {
  ArrowLeft, VideoPlay, Position, CircleClose,
  Refresh, Search, Download
} from '@element-plus/icons-vue'
import DependencyGraph from './components/DependencyGraph.vue'
import './components/DependencyGraphNodes'

const route = useRoute()
const router = useRouter()

// 执行信息
const executionInfo = ref({
  id: route.params.id,
  taskName: '示例数据集成任务',
  status: '运行中',
  startTime: '2024-03-15 10:00:00',
  endTime: null,
  type: '全量同步',
  trigger: '手动触发',
  executor: '管理员',
  retryCount: 0,
  progress: 45,
  processedRecords: 1234567,
  speed: 1000,
  errors: 5,
  duration: 1800000,
  stages: [
    {
      id: 1,
      name: '数据读取',
      status: '已完成',
      progress: 100,
      processedRecords: 500000,
      duration: 600000
    },
    {
      id: 2,
      name: '数据转换',
      status: '运行中',
      progress: 60,
      processedRecords: 300000,
      duration: 900000
    },
    {
      id: 3,
      name: '数据写入',
      status: '等待中',
      progress: 0,
      processedRecords: 0,
      duration: 0
    }
  ]
})

// 是否可以执行操作
const canRerun = computed(() => ['已完成', '已失败'].includes(executionInfo.value.status))
const canSkip = computed(() => executionInfo.value.status === '已失败')
const canTerminate = computed(() => ['运行中', '等待中'].includes(executionInfo.value.status))

// 日志相关
const logLevel = ref('ALL')
const logSearch = ref('')
const logs = ref(generateLogs())

// 监控相关
const monitorRange = ref('10m')
const cpuChart = ref(null)
const memoryChart = ref(null)
const diskChart = ref(null)

// 错误统计
const errorStats = ref([
  { type: '数据格式错误', count: 23, percentage: 46, color: '#f56c6c' },
  { type: '网络超时', count: 15, percentage: 30, color: '#e6a23c' },
  { type: '权限不足', count: 12, percentage: 24, color: '#909399' }
])

// 性能分析
const performanceAnalysis = ref([
  {
    title: '处理速度',
    value: '1,000',
    unit: '条/秒',
    status: 'success',
    statusText: '正常',
    description: '当前处理速度在预期范围内'
  },
  {
    title: '资源利用率',
    value: '75',
    unit: '%',
    status: 'warning',
    statusText: '偏高',
    description: '内存使用率较高，建议优化',
    suggestion: true
  },
  {
    title: '错误率',
    value: '0.05',
    unit: '%',
    status: 'success',
    statusText: '正常',
    description: '错误率在可接受范围内'
  }
])

// 依赖关系数据
const dependencyData = ref({
  nodes: [
    {
      id: '1',
      name: '当前任务',
      type: '数据同步',
      status: 'running',
      createTime: '2024-03-15 10:00:00',
      updateTime: '2024-03-15 10:30:00',
      dependencies: [
        {
          name: '上游任务1',
          type: '强依赖',
          status: 'success'
        },
        {
          name: '上游任务2',
          type: '弱依赖',
          status: 'success'
        }
      ],
      downstream: [
        {
          name: '下游任务1',
          type: '数据同步',
          status: 'pending'
        },
        {
          name: '下游任务2',
          type: '数据开发',
          status: 'pending'
        }
      ]
    },
    {
      id: '2',
      name: '上游任务1',
      type: '数据同步',
      status: 'success'
    },
    {
      id: '3',
      name: '上游任务2',
      type: '数据开发',
      status: 'success'
    },
    {
      id: '4',
      name: '下游任务1',
      type: '数据同步',
      status: 'pending'
    },
    {
      id: '5',
      name: '下游任务2',
      type: '数据开发',
      status: 'pending'
    }
  ],
  edges: [
    {
      source: '2',
      target: '1',
      type: 'strong'
    },
    {
      source: '3',
      target: '1',
      type: 'weak'
    },
    {
      source: '1',
      target: '4',
      type: 'strong'
    },
    {
      source: '1',
      target: '5',
      type: 'strong'
    }
  ]
})

// 生成模拟日志
function generateLogs() {
  const levels = ['INFO', 'INFO', 'INFO', 'WARN', 'ERROR']
  return Array.from({ length: 100 }).map((_, index) => ({
    time: new Date(Date.now() - index * 60000).toLocaleString(),
    level: levels[Math.floor(Math.random() * levels.length)],
    message: `这是一条示例日志消息 ${index + 1}`
  }))
}

// 过滤后的日志
const filteredLogs = computed(() => {
  return logs.value.filter(log => {
    if (logLevel.value !== 'ALL' && log.level !== logLevel.value) return false
    if (logSearch.value && !log.message.toLowerCase().includes(logSearch.value.toLowerCase())) return false
    return true
  })
})

// 获取状态类型
function getStatusType(status) {
  const types = {
    '运行中': 'primary',
    '已完成': 'success',
    '等待中': 'info',
    '已失败': 'danger'
  }
  return types[status] || 'info'
}

// 获取进度状态
function getProgressStatus(status) {
  if (status === '已完成') return 'success'
  if (status === '已失败') return 'exception'
  if (status === '运行中') return ''
  return ''
}

// 格式化数字
function formatNumber(num) {
  return new Intl.NumberFormat().format(num)
}

// 格式化速度
function formatSpeed(records) {
  if (records < 1000) return `${records} 条/秒`
  if (records < 1000000) return `${(records / 1000).toFixed(1)}K 条/秒`
  return `${(records / 1000000).toFixed(1)}M 条/秒`
}

// 格式化持续时间
function formatDuration(ms) {
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  const minutes = Math.floor(ms / 60000)
  const seconds = ((ms % 60000) / 1000).toFixed(0)
  return `${minutes}m ${seconds}s`
}

// 初始化图表
function initCharts() {
  const charts = [
    { ref: cpuChart, title: 'CPU使用率' },
    { ref: memoryChart, title: '内存使用率' },
    { ref: diskChart, title: '磁盘I/O' }
  ]

  charts.forEach(({ ref, title }) => {
    if (!ref.value) return

    const chart = echarts.init(ref.value)
    const times = Array.from({ length: 60 }).map((_, i) => `${59 - i}分钟前`)
    chart.setOption({
      title: {
        text: title,
        left: 'center',
        top: 0,
        textStyle: {
          fontSize: 14
        }
      },
      tooltip: {
        trigger: 'axis'
      },
      grid: {
        top: 30,
        left: 40,
        right: 20,
        bottom: 20
      },
      xAxis: {
        type: 'category',
        data: times.reverse(),
        axisLabel: {
          interval: 10
        }
      },
      yAxis: {
        type: 'value',
        max: 100,
        axisLabel: {
          formatter: '{value}%'
        }
      },
      series: [
        {
          name: title,
          type: 'line',
          smooth: true,
          data: Array.from({ length: 60 }).map(() => Math.floor(Math.random() * 100)),
          areaStyle: {
            opacity: 0.1
          }
        }
      ]
    })
  })
}

// 页面操作处理函数
const handleBack = () => {
  router.back()
}

const handleRerun = async () => {
  try {
    await ElMessageBox.confirm('确认要重新执行该任务吗？', '提示', {
      type: 'warning'
    })
    // TODO: 调用API重新执行任务
    ElMessage.success('任务已重新执行')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('重新执行失败')
    }
  }
}

const handleSkip = async () => {
  try {
    await ElMessageBox.confirm('确认要跳过错误继续执行吗？', '提示', {
      type: 'warning'
    })
    // TODO: 调用API跳过错误
    ElMessage.success('已跳过错误')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleTerminate = async () => {
  try {
    await ElMessageBox.confirm('确认要终止执行吗？', '警告', {
      type: 'warning'
    })
    // TODO: 调用API终止执行
    ElMessage.success('已终止执行')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('终止失败')
    }
  }
}

const handleRefreshProgress = () => {
  // TODO: 刷新进度
  ElMessage.success('进度已更新')
}

const handleRefreshLog = () => {
  // TODO: 刷新日志
  logs.value = generateLogs()
}

const handleDownloadLog = () => {
  // TODO: 下载日志
  ElMessage.success('日志下载中...')
}

// 组件挂载时初始化
onMounted(() => {
  initCharts()
  
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
})

// 组件卸载时清理
onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

// 处理窗口大小变化
const handleResize = () => {
  const charts = [
    echarts.getInstanceByDom(cpuChart.value),
    echarts.getInstanceByDom(memoryChart.value),
    echarts.getInstanceByDom(diskChart.value)
  ]
  charts.forEach(chart => chart?.resize())
}
</script>

<style lang="scss" scoped>
.execution-detail {
  padding: 20px;

  .page-header {
    margin-bottom: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .page-title {
        font-size: 24px;
        color: #2c3e50;
        margin: 0;
      }
    }
  }

  .detail-content {
    .detail-card {
      margin-bottom: 20px;

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .progress-view {
        .progress-info {
          margin-bottom: 20px;

          .progress-item {
            margin-bottom: 10px;

            .label {
              display: block;
              margin-bottom: 5px;
              color: #606266;
            }
          }

          .metrics {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 20px;
            margin-top: 20px;

            .metric-item {
              text-align: center;

              .value {
                display: block;
                font-size: 24px;
                font-weight: bold;
                color: #303133;
                margin-bottom: 5px;
              }

              .label {
                color: #909399;
                font-size: 14px;
              }
            }
          }
        }

        .stage-progress {
          .stage-item {
            margin-bottom: 15px;

            .stage-header {
              display: flex;
              justify-content: space-between;
              align-items: center;
              margin-bottom: 5px;

              .stage-name {
                color: #606266;
              }
            }

            .stage-metrics {
              display: flex;
              justify-content: space-between;
              font-size: 12px;
              color: #909399;
              margin-top: 5px;
            }
          }
        }
      }

      .monitor-charts {
        .chart-item {
          margin-bottom: 20px;

          .chart-title {
            text-align: center;
            margin-bottom: 10px;
            font-weight: 500;
          }

          .chart-container {
            height: 200px;
          }
        }
      }

      .log-viewer {
        height: 500px;
        overflow-y: auto;
        background: #1e1e1e;
        border-radius: 4px;
        padding: 12px;
        font-family: monospace;
        font-size: 14px;

        .log-line {
          margin-bottom: 4px;
          color: #d4d4d4;

          &:last-child {
            margin-bottom: 0;
          }

          .time {
            color: #808080;
            margin-right: 8px;
          }

          .level {
            display: inline-block;
            width: 60px;
            margin-right: 8px;
          }

          &.info .level {
            color: #3794ff;
          }

          &.warn .level {
            color: #cca700;
          }

          &.error .level {
            color: #f14c4c;
          }
        }
      }

      .performance-analysis {
        .analysis-item {
          padding: 15px;
          border-bottom: 1px solid #ebeef5;

          &:last-child {
            border-bottom: none;
          }

          .item-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;

            .title {
              font-weight: 500;
              color: #303133;
            }
          }

          .item-content {
            margin-bottom: 10px;

            .value {
              font-size: 24px;
              font-weight: bold;
              color: #303133;
              margin-right: 5px;
            }

            .unit {
              color: #909399;
            }
          }

          .item-footer {
            display: flex;
            justify-content: space-between;
            align-items: center;

            .description {
              color: #606266;
              font-size: 12px;
            }
          }
        }
      }

      .dependency-graph-container {
        height: 400px;
        border: 1px solid #ebeef5;
        border-radius: 4px;
      }
    }
  }
}
</style> 