<template>
  <div class="task-detail">
    <div class="page-header">
      <div class="header-left">
        <el-button link @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>返回
        </el-button>
        <h2 class="page-title">{{ taskInfo.name }}</h2>
        <el-tag :type="getStatusType(taskInfo.status)">{{ taskInfo.status }}</el-tag>
      </div>
      <div class="header-right">
        <el-button-group>
          <el-button type="primary" @click="handleStart" :disabled="!canStart">
            <el-icon><VideoPlay /></el-icon>启动
          </el-button>
          <el-button type="warning" @click="handleStop" :disabled="!canStop">
            <el-icon><VideoPause /></el-icon>停止
          </el-button>
          <el-button @click="handleEdit">
            <el-icon><Edit /></el-icon>编辑
          </el-button>
          <el-button type="danger" @click="handleDelete">
            <el-icon><Delete /></el-icon>删除
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
            <el-descriptions-item label="任务ID">{{ taskInfo.id }}</el-descriptions-item>
            <el-descriptions-item label="任务类型">
              <el-tag>{{ taskInfo.type }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ taskInfo.createTime }}</el-descriptions-item>
            <el-descriptions-item label="最后修改">{{ taskInfo.updateTime }}</el-descriptions-item>
            <el-descriptions-item label="调度方式">{{ taskInfo.schedule }}</el-descriptions-item>
            <el-descriptions-item label="最近执行">{{ taskInfo.lastRun }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ taskInfo.description }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 执行监控 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>执行监控</span>
              <el-radio-group v-model="timeRange" size="small">
                <el-radio-button value="hour">近1小时</el-radio-button>
                <el-radio-button value="day">近24小时</el-radio-button>
                <el-radio-button value="week">近7天</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="monitor-charts">
            <div class="chart-item">
              <div class="chart-title">处理记录数</div>
              <div class="chart-container" ref="recordChart"></div>
            </div>
            <div class="chart-item">
              <div class="chart-title">处理速度</div>
              <div class="chart-container" ref="speedChart"></div>
            </div>
            <div class="chart-item">
              <div class="chart-title">错误率</div>
              <div class="chart-container" ref="errorChart"></div>
            </div>
          </div>
        </el-card>

        <!-- 执行历史 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>执行历史</span>
            </div>
          </template>
          <el-table :data="historyList" style="width: 100%">
            <el-table-column prop="id" label="执行ID" width="80" />
            <el-table-column prop="startTime" label="开始时间" width="180" />
            <el-table-column prop="endTime" label="结束时间" width="180" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="records" label="处理记录数" width="120" align="right">
              <template #default="{ row }">
                {{ formatNumber(row.records) }}
              </template>
            </el-table-column>
            <el-table-column prop="errors" label="错误数" width="100" align="right">
              <template #default="{ row }">
                {{ formatNumber(row.errors) }}
              </template>
            </el-table-column>
            <el-table-column prop="duration" label="耗时" width="100">
              <template #default="{ row }">
                {{ formatDuration(row.duration) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleViewLog(row)">查看日志</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination">
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
      </el-col>

      <el-col :span="8">
        <!-- 运行状态 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>运行状态</span>
              <el-button link type="primary" @click="handleRefreshStatus">
                <el-icon><Refresh /></el-icon>刷新
              </el-button>
            </div>
          </template>
          <div class="status-info">
            <div class="status-item">
              <span class="label">当前状态</span>
              <el-tag :type="getStatusType(taskInfo.status)">{{ taskInfo.status }}</el-tag>
            </div>
            <div class="status-item">
              <span class="label">运行时长</span>
              <span class="value">{{ formatDuration(taskInfo.runningTime) }}</span>
            </div>
            <div class="status-item">
              <span class="label">处理记录数</span>
              <span class="value">{{ formatNumber(taskInfo.processedRecords) }}</span>
            </div>
            <div class="status-item">
              <span class="label">处理速度</span>
              <span class="value">{{ formatSpeed(taskInfo.speed) }}</span>
            </div>
            <div class="status-item">
              <span class="label">错误数</span>
              <span class="value">{{ formatNumber(taskInfo.errors) }}</span>
            </div>
            <div class="status-item">
              <span class="label">CPU使用率</span>
              <el-progress
                :percentage="taskInfo.cpuUsage"
                :color="getResourceColor(taskInfo.cpuUsage)"
              />
            </div>
            <div class="status-item">
              <span class="label">内存使用率</span>
              <el-progress
                :percentage="taskInfo.memoryUsage"
                :color="getResourceColor(taskInfo.memoryUsage)"
              />
            </div>
          </div>
        </el-card>

        <!-- 配置信息 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>配置信息</span>
            </div>
          </template>
          <div class="config-info">
            <div class="config-section">
              <div class="section-title">源配置</div>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="类型">{{ taskInfo.source.type }}</el-descriptions-item>
                <template v-if="taskInfo.source.type === 'mysql'">
                  <el-descriptions-item label="主机">{{ taskInfo.source.host }}</el-descriptions-item>
                  <el-descriptions-item label="数据库">{{ taskInfo.source.database }}</el-descriptions-item>
                </template>
                <template v-else-if="taskInfo.source.type === 'kafka'">
                  <el-descriptions-item label="Brokers">{{ taskInfo.source.brokers }}</el-descriptions-item>
                  <el-descriptions-item label="Topic">{{ taskInfo.source.topic }}</el-descriptions-item>
                </template>
              </el-descriptions>
            </div>
            <div class="config-section">
              <div class="section-title">目标配置</div>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="类型">{{ taskInfo.target.type }}</el-descriptions-item>
                <template v-if="taskInfo.target.type === 'delta'">
                  <el-descriptions-item label="存储路径">{{ taskInfo.target.path }}</el-descriptions-item>
                  <el-descriptions-item label="写入模式">{{ taskInfo.target.writeMode }}</el-descriptions-item>
                </template>
                <template v-else-if="taskInfo.target.type === 'starrocks'">
                  <el-descriptions-item label="数据库">{{ taskInfo.target.database }}</el-descriptions-item>
                  <el-descriptions-item label="表名">{{ taskInfo.target.table }}</el-descriptions-item>
                </template>
              </el-descriptions>
            </div>
            <div class="config-section">
              <div class="section-title">高级配置</div>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="并行度">{{ taskInfo.advanced.parallelism }}</el-descriptions-item>
                <el-descriptions-item label="批次大小">{{ taskInfo.advanced.batchSize }}</el-descriptions-item>
                <el-descriptions-item label="重试次数">{{ taskInfo.advanced.retryTimes }}</el-descriptions-item>
                <el-descriptions-item label="超时时间">{{ taskInfo.advanced.timeout }}秒</el-descriptions-item>
              </el-descriptions>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 日志对话框 -->
    <el-dialog
      v-model="logDialogVisible"
      title="执行日志"
      width="80%"
      :close-on-click-modal="false"
    >
      <div class="log-content">
        <div class="log-toolbar">
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
            style="width: 200px"
          />
          <el-button-group>
            <el-button @click="handleRefreshLog">
              <el-icon><Refresh /></el-icon>刷新
            </el-button>
            <el-button @click="handleDownloadLog">
              <el-icon><Download /></el-icon>下载
            </el-button>
          </el-button-group>
        </div>
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
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import {
  ArrowLeft, VideoPlay, VideoPause, Edit, Delete,
  Refresh, Search, Download
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

// 任务信息
const taskInfo = ref({
  id: route.params.id,
  name: '示例数据集成任务',
  type: 'Delta Lake 同步',
  status: '运行中',
  createTime: '2024-03-15 10:00:00',
  updateTime: '2024-03-15 15:30:00',
  schedule: '每小时',
  lastRun: '2024-03-15 15:00:00',
  description: '这是一个示例数据集成任务，用于同步数据。',
  runningTime: 1800000, // 30分钟
  processedRecords: 1234567,
  speed: 1000,
  errors: 5,
  cpuUsage: 45,
  memoryUsage: 60,
  source: {
    type: 'mysql',
    host: 'localhost',
    database: 'test_db'
  },
  target: {
    type: 'delta',
    path: '/data/delta/example',
    writeMode: 'append'
  },
  advanced: {
    parallelism: 2,
    batchSize: 1000,
    retryTimes: 3,
    timeout: 3600
  }
})

// 是否可以启动/停止
const canStart = computed(() => ['已停止', '已失败'].includes(taskInfo.value.status))
const canStop = computed(() => ['运行中', '启动中'].includes(taskInfo.value.status))

// 监控图表相关
const timeRange = ref('hour')
const recordChart = ref(null)
const speedChart = ref(null)
const errorChart = ref(null)

// 执行历史相关
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(100)
const historyList = ref(generateHistory())

// 日志相关
const logDialogVisible = ref(false)
const logLevel = ref('ALL')
const logSearch = ref('')
const logs = ref(generateLogs())

// 生成模拟历史数据
function generateHistory() {
  return Array.from({ length: 100 }).map((_, index) => ({
    id: 100 - index,
    startTime: new Date(Date.now() - index * 3600000).toLocaleString(),
    endTime: new Date(Date.now() - index * 3600000 + 1800000).toLocaleString(),
    status: ['已完成', '已完成', '已完成', '已失败'][Math.floor(Math.random() * 4)],
    records: Math.floor(Math.random() * 1000000),
    errors: Math.floor(Math.random() * 100),
    duration: Math.floor(Math.random() * 3600000)
  }))
}

// 生成模拟日志数据
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

// 格式化数字
function formatNumber(num) {
  return new Intl.NumberFormat().format(num)
}

// 格式化持续时间
function formatDuration(ms) {
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  const minutes = Math.floor(ms / 60000)
  const seconds = ((ms % 60000) / 1000).toFixed(0)
  return `${minutes}m ${seconds}s`
}

// 格式化速度
function formatSpeed(records) {
  if (records < 1000) return `${records} 条/秒`
  if (records < 1000000) return `${(records / 1000).toFixed(1)}K 条/秒`
  return `${(records / 1000000).toFixed(1)}M 条/秒`
}

// 获取状态类型
function getStatusType(status) {
  const types = {
    '运行中': 'primary',
    '已完成': 'success',
    '已停止': 'info',
    '已失败': 'danger',
    '启动中': 'warning'
  }
  return types[status] || 'info'
}

// 获取资源使用颜色
function getResourceColor(usage) {
  if (usage >= 80) return '#F56C6C'
  if (usage >= 60) return '#E6A23C'
  return '#67C23A'
}

// 初始化图表
function initCharts() {
  // 处理记录数图表
  if (recordChart.value) {
    const chart = echarts.init(recordChart.value)
    const times = Array.from({ length: 60 }).map((_, i) => `${59 - i}分钟前`)
    chart.setOption({
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: times.reverse()
      },
      yAxis: {
        type: 'value',
        name: '记录数'
      },
      series: [
        {
          name: '处理记录数',
          type: 'line',
          smooth: true,
          data: Array.from({ length: 60 }).map(() => Math.floor(Math.random() * 10000))
        }
      ]
    })
  }

  // 处理速度图表
  if (speedChart.value) {
    const chart = echarts.init(speedChart.value)
    const times = Array.from({ length: 60 }).map((_, i) => `${59 - i}分钟前`)
    chart.setOption({
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: times.reverse()
      },
      yAxis: {
        type: 'value',
        name: '记录/秒'
      },
      series: [
        {
          name: '处理速度',
          type: 'line',
          smooth: true,
          data: Array.from({ length: 60 }).map(() => Math.floor(Math.random() * 1000))
        }
      ]
    })
  }

  // 错误率图表
  if (errorChart.value) {
    const chart = echarts.init(errorChart.value)
    const times = Array.from({ length: 60 }).map((_, i) => `${59 - i}分钟前`)
    chart.setOption({
      tooltip: {
        trigger: 'axis'
      },
      xAxis: {
        type: 'category',
        data: times.reverse()
      },
      yAxis: {
        type: 'value',
        name: '错误率(%)',
        max: 100
      },
      series: [
        {
          name: '错误率',
          type: 'line',
          smooth: true,
          data: Array.from({ length: 60 }).map(() => (Math.random() * 5).toFixed(2))
        }
      ]
    })
  }
}

// 页面操作处理函数
const handleBack = () => {
  router.back()
}

const handleStart = async () => {
  try {
    // TODO: 调用API启动任务
    await new Promise(resolve => setTimeout(resolve, 1000))
    taskInfo.value.status = '启动中'
    ElMessage.success('任务启动成功')
  } catch (error) {
    ElMessage.error('任务启动失败')
  }
}

const handleStop = async () => {
  try {
    await ElMessageBox.confirm('确认要停止任务吗？', '提示', {
      type: 'warning'
    })
    // TODO: 调用API停止任务
    await new Promise(resolve => setTimeout(resolve, 1000))
    taskInfo.value.status = '已停止'
    ElMessage.success('任务已停止')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('停止任务失败')
    }
  }
}

const handleEdit = () => {
  router.push(`/dataIntegration/model/edit/${taskInfo.value.id}`)
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm(
      '确认要删除该任务吗？删除后数据将无法恢复。',
      '警告',
      {
        type: 'warning'
      }
    )
    // TODO: 调用API删除任务
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('删除成功')
    router.push('/dataIntegration')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const handleRefreshStatus = async () => {
  try {
    // TODO: 调用API刷新状态
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('状态已更新')
  } catch (error) {
    ElMessage.error('刷新状态失败')
  }
}

const handleSizeChange = (val) => {
  pageSize.value = val
}

const handleCurrentChange = (val) => {
  currentPage.value = val
}

const handleViewLog = (row) => {
  logDialogVisible.value = true
  // TODO: 加载对应执行记录的日志
}

const handleRefreshLog = () => {
  // TODO: 刷新日志
}

const handleDownloadLog = () => {
  // TODO: 下载日志
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
    echarts.getInstanceByDom(recordChart.value),
    echarts.getInstanceByDom(speedChart.value),
    echarts.getInstanceByDom(errorChart.value)
  ]
  charts.forEach(chart => chart?.resize())
}
</script>

<style lang="scss" scoped>
.task-detail {
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

      .monitor-charts {
        display: grid;
        grid-template-columns: repeat(3, 1fr);
        gap: 20px;
        padding: 20px 0;

        .chart-item {
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

      .status-info {
        .status-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 16px;

          .label {
            color: #909399;
          }

          &:last-child {
            margin-bottom: 0;
          }
        }
      }

      .config-info {
        .config-section {
          margin-bottom: 20px;

          .section-title {
            font-weight: 500;
            margin-bottom: 10px;
          }

          &:last-child {
            margin-bottom: 0;
          }
        }
      }
    }
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}

.log-content {
  .log-toolbar {
    margin-bottom: 16px;
    display: flex;
    gap: 16px;
    align-items: center;
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
}
</style> 