<template>
  <div class="data-integration">
    <div class="page-header">
      <h2 class="page-title">数据集成</h2>
      <div class="page-toolbar">
        <el-button-group>
          <el-button type="primary" @click="handleCreateTask">
            <el-icon><Plus /></el-icon>新建任务
          </el-button>
          <el-button @click="handleRefresh">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
        </el-button-group>
      </div>
    </div>

    <!-- 概览统计 -->
    <el-row :gutter="20" class="statistics">
      <el-col :span="6">
        <el-card class="stat-card total-tasks">
          <template #header>
            <div class="card-header">
              <span>总任务数</span>
              <el-icon><Connection /></el-icon>
            </div>
          </template>
          <div class="card-content">
            <div class="number">{{ statistics.totalTasks }}</div>
            <div class="trend">
              <span class="label">较上周</span>
              <span class="value" :class="statistics.tasksTrend >= 0 ? 'up' : 'down'">
                {{ statistics.tasksTrend > 0 ? '+' : '' }}{{ statistics.tasksTrend }}%
              </span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card running-tasks">
          <template #header>
            <div class="card-header">
              <span>运行中任务</span>
              <el-icon><Loading /></el-icon>
            </div>
          </template>
          <div class="card-content">
            <div class="number">{{ statistics.runningTasks }}</div>
            <div class="trend">
              <span class="label">实时</span>
              <span class="value">{{ statistics.runningPercent }}%</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card data-volume">
          <template #header>
            <div class="card-header">
              <span>数据量</span>
              <el-icon><DataLine /></el-icon>
            </div>
          </template>
          <div class="card-content">
            <div class="number">{{ formatStorage(statistics.dataVolume) }}</div>
            <div class="trend">
              <span class="label">日环比</span>
              <span class="value" :class="statistics.volumeTrend >= 0 ? 'up' : 'down'">
                {{ statistics.volumeTrend > 0 ? '+' : '' }}{{ statistics.volumeTrend }}%
              </span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card success-rate">
          <template #header>
            <div class="card-header">
              <span>成功率</span>
              <el-icon><CircleCheck /></el-icon>
            </div>
          </template>
          <div class="card-content">
            <div class="number">{{ statistics.successRate }}%</div>
            <div class="trend">
              <span class="label">较上周</span>
              <span class="value" :class="statistics.successTrend >= 0 ? 'up' : 'down'">
                {{ statistics.successTrend > 0 ? '+' : '' }}{{ statistics.successTrend }}%
              </span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" class="charts-row">
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>任务执行趋势</span>
              <el-radio-group v-model="timeRange" size="small">
                <el-radio-button value="day">今日</el-radio-button>
                <el-radio-button value="week">本周</el-radio-button>
                <el-radio-button value="month">本月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="chart-container" ref="trendChart"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>任务类型分布</span>
            </div>
          </template>
          <div class="chart-container" ref="typeChart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 任务列表 -->
    <el-card class="task-list">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="title">集成任务</span>
            <el-input
              v-model="searchKey"
              placeholder="搜索任务名称"
              prefix-icon="Search"
              style="width: 200px; margin-left: 16px"
            />
          </div>
          <div class="header-right">
            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button value="table">表格</el-radio-button>
              <el-radio-button value="card">卡片</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>

      <!-- 表格视图 -->
      <template v-if="viewMode === 'table'">
        <el-table :data="filteredTasks" style="width: 100%">
          <el-table-column prop="name" label="任务名称" min-width="200">
            <template #default="{ row }">
              <div class="task-name">
                <el-icon><component :is="getTaskIcon(row.type)" /></el-icon>
                <span>{{ row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="类型" width="120">
            <template #default="{ row }">
              <el-tag>{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="schedule" label="调度" width="120" />
          <el-table-column prop="lastRun" label="最近执行" width="180" />
          <el-table-column prop="duration" label="耗时" width="100">
            <template #default="{ row }">
              {{ formatDuration(row.duration) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button-group>
                <el-button link type="primary" @click="handleViewTask(row)">查看</el-button>
                <el-button link type="primary" @click="handleEditTask(row)">编辑</el-button>
                <el-button link type="danger" @click="handleDeleteTask(row)">删除</el-button>
              </el-button-group>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 卡片视图 -->
      <template v-else>
        <div class="task-cards">
          <el-row :gutter="20">
            <el-col v-for="task in filteredTasks" :key="task.id" :span="8">
              <el-card class="task-card" :body-style="{ padding: '0' }">
                <div class="task-card-header">
                  <div class="task-info">
                    <el-icon><component :is="getTaskIcon(task.type)" /></el-icon>
                    <span class="task-name">{{ task.name }}</span>
                  </div>
                  <el-tag :type="getStatusType(task.status)">{{ task.status }}</el-tag>
                </div>
                <div class="task-card-content">
                  <div class="info-item">
                    <span class="label">类型:</span>
                    <span class="value">{{ task.type }}</span>
                  </div>
                  <div class="info-item">
                    <span class="label">调度:</span>
                    <span class="value">{{ task.schedule }}</span>
                  </div>
                  <div class="info-item">
                    <span class="label">最近执行:</span>
                    <span class="value">{{ task.lastRun }}</span>
                  </div>
                  <div class="info-item">
                    <span class="label">耗时:</span>
                    <span class="value">{{ formatDuration(task.duration) }}</span>
                  </div>
                </div>
                <div class="task-card-footer">
                  <el-button-group>
                    <el-button link type="primary" @click="handleViewTask(task)">查看</el-button>
                    <el-button link type="primary" @click="handleEditTask(task)">编辑</el-button>
                    <el-button link type="danger" @click="handleDeleteTask(task)">删除</el-button>
                  </el-button-group>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </template>

      <div class="pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[12, 24, 36, 48]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import {
  Plus, Refresh, Connection, Loading, DataLine,
  CircleCheck, Search, Share, DataBoard
} from '@element-plus/icons-vue'

const router = useRouter()

// 统计数据
const statistics = ref({
  totalTasks: 156,
  tasksTrend: 12.5,
  runningTasks: 23,
  runningPercent: 14.7,
  dataVolume: 1024 * 1024 * 1024 * 1024 * 2.5, // 2.5TB
  volumeTrend: 8.3,
  successRate: 99.5,
  successTrend: 0.2
})

// 图表相关
const timeRange = ref('day')
const trendChart = ref(null)
const typeChart = ref(null)

// 任务列表相关
const viewMode = ref('table')
const searchKey = ref('')
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(156)

// 模拟任务数据
const tasks = ref(generateTasks())

// 过滤后的任务列表
const filteredTasks = computed(() => {
  const filtered = tasks.value.filter(task =>
    task.name.toLowerCase().includes(searchKey.value.toLowerCase())
  )
  total.value = filtered.length
  const start = (currentPage.value - 1) * pageSize.value
  return filtered.slice(start, start + pageSize.value)
})

// 生成模拟任务数据
function generateTasks() {
  const types = ['Delta Lake 同步', 'StarRocks 同步', '实时数据集成', '批量数据集成']
  const statuses = ['运行中', '已完成', '已暂停', '已失败']
  const schedules = ['每小时', '每天', '每周', '手动触发']
  
  return Array.from({ length: 156 }).map((_, index) => ({
    id: index + 1,
    name: `数据集成任务 ${String(index + 1).padStart(3, '0')}`,
    type: types[Math.floor(Math.random() * types.length)],
    status: statuses[Math.floor(Math.random() * statuses.length)],
    schedule: schedules[Math.floor(Math.random() * schedules.length)],
    lastRun: generateRandomTime(),
    duration: Math.floor(Math.random() * 3600000) // 0-1小时
  }))
}

// 生成随机时间
function generateRandomTime() {
  const now = new Date()
  const hours = Math.floor(Math.random() * 24)
  now.setHours(now.getHours() - hours)
  return now.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 格式化存储大小
function formatStorage(bytes) {
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = bytes
  let unitIndex = 0
  
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }
  
  return `${size.toFixed(2)} ${units[unitIndex]}`
}

// 格式化持续时间
function formatDuration(ms) {
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  const minutes = Math.floor(ms / 60000)
  const seconds = ((ms % 60000) / 1000).toFixed(0)
  return `${minutes}m ${seconds}s`
}

// 获取任务图标
function getTaskIcon(type) {
  const icons = {
    'Delta Lake 同步': DataBoard,
    'StarRocks 同步': Share,
    '实时数据集成': Connection,
    '批量数据集成': DataLine
  }
  return icons[type] || Connection
}

// 获取状态类型
function getStatusType(status) {
  const types = {
    '运行中': 'primary',
    '已完成': 'success',
    '已暂停': 'warning',
    '已失败': 'danger'
  }
  return types[status] || 'info'
}

// 初始化趋势图表
function initTrendChart() {
  if (!trendChart.value) return
  
  const chart = echarts.init(trendChart.value)
  const hours = Array.from({ length: 24 }).map((_, i) => `${i}:00`)
  
  chart.setOption({
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['运行任务数', '成功数', '失败数']
    },
    xAxis: {
      type: 'category',
      data: hours
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '运行任务数',
        type: 'line',
        smooth: true,
        data: Array.from({ length: 24 }).map(() => Math.floor(Math.random() * 30 + 10))
      },
      {
        name: '成功数',
        type: 'line',
        smooth: true,
        data: Array.from({ length: 24 }).map(() => Math.floor(Math.random() * 20 + 5))
      },
      {
        name: '失败数',
        type: 'line',
        smooth: true,
        data: Array.from({ length: 24 }).map(() => Math.floor(Math.random() * 5))
      }
    ]
  })
}

// 初始化类型分布图表
function initTypeChart() {
  if (!typeChart.value) return
  
  const chart = echarts.init(typeChart.value)
  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
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
            fontSize: '14',
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: 45, name: 'Delta Lake 同步' },
          { value: 38, name: 'StarRocks 同步' },
          { value: 42, name: '实时数据集成' },
          { value: 31, name: '批量数据集成' }
        ]
      }
    ]
  })
}

// 页面操作处理函数
const handleCreateTask = () => {
  router.push('/dataIntegration/model/create')
}

const handleRefresh = () => {
  // TODO: 刷新数据
}

const handleViewTask = (task) => {
  router.push(`/dataIntegration/model/detail/${task.id}`)
}

const handleEditTask = (task) => {
  router.push(`/dataIntegration/model/edit/${task.id}`)
}

const handleDeleteTask = (task) => {
  // TODO: 删除任务
}

const handleSizeChange = (val) => {
  pageSize.value = val
}

const handleCurrentChange = (val) => {
  currentPage.value = val
}

// 组件挂载时初始化
onMounted(() => {
  nextTick(() => {
    initTrendChart()
    initTypeChart()
    
    // 监听窗口大小变化
    window.addEventListener('resize', () => {
      const trend = echarts.getInstanceByDom(trendChart.value)
      const type = echarts.getInstanceByDom(typeChart.value)
      trend?.resize()
      type?.resize()
    })
  })
})
</script>

<style lang="scss" scoped>
.data-integration {
  padding: 20px;

  .page-header {
    margin-bottom: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .page-title {
      font-size: 24px;
      color: #2c3e50;
      margin: 0;
    }
  }

  .statistics {
    margin-bottom: 20px;

    .stat-card {
      height: 180px;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-5px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      }

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-size: 16px;

        .el-icon {
          font-size: 20px;
          color: #409EFF;
        }
      }

      .card-content {
        padding: 20px 0;

        .number {
          font-size: 36px;
          font-weight: bold;
          color: #2c3e50;
          margin-bottom: 10px;
        }

        .trend {
          display: flex;
          align-items: center;
          gap: 10px;

          .label {
            color: #909399;
          }

          .value {
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

    .total-tasks {
      background: linear-gradient(135deg, rgba(64, 158, 255, 0.1), rgba(64, 158, 255, 0.05));
    }

    .running-tasks {
      background: linear-gradient(135deg, rgba(103, 194, 58, 0.1), rgba(103, 194, 58, 0.05));
    }

    .data-volume {
      background: linear-gradient(135deg, rgba(230, 162, 60, 0.1), rgba(230, 162, 60, 0.05));
    }

    .success-rate {
      background: linear-gradient(135deg, rgba(144, 147, 153, 0.1), rgba(144, 147, 153, 0.05));
    }
  }

  .charts-row {
    margin-bottom: 20px;

    .chart-card {
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .chart-container {
        height: 300px;
      }
    }
  }

  .task-list {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .header-left {
        display: flex;
        align-items: center;

        .title {
          font-size: 16px;
          font-weight: 500;
        }
      }
    }

    .task-name {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .task-cards {
      .task-card {
        margin-bottom: 20px;

        .task-card-header {
          padding: 12px;
          border-bottom: 1px solid #ebeef5;
          display: flex;
          justify-content: space-between;
          align-items: center;

          .task-info {
            display: flex;
            align-items: center;
            gap: 8px;

            .task-name {
              font-weight: 500;
            }
          }
        }

        .task-card-content {
          padding: 12px;

          .info-item {
            display: flex;
            justify-content: space-between;
            margin-bottom: 8px;

            .label {
              color: #909399;
            }
          }
        }

        .task-card-footer {
          padding: 12px;
          border-top: 1px solid #ebeef5;
          text-align: right;
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
</style> 