<template>
  <div class="performance-monitor">
    <div class="toolbar">
      <el-radio-group v-model="timeRange" size="small">
        <el-radio-button label="realtime">实时</el-radio-button>
        <el-radio-button label="5min">5分钟</el-radio-button>
        <el-radio-button label="30min">30分钟</el-radio-button>
        <el-radio-button label="1h">1小时</el-radio-button>
      </el-radio-group>

      <el-button-group>
        <el-button
          type="primary"
          :icon="VideoPlay"
          @click="startMonitoring"
          :class="{ active: isMonitoring }"
        >
          开始监控
        </el-button>
        <el-button
          :icon="Download"
          @click="exportMetrics"
        >
          导出数据
        </el-button>
      </el-button-group>
    </div>

    <el-row :gutter="20" class="metrics-grid">
      <el-col :span="6" v-for="metric in metrics" :key="metric.id">
        <el-card class="metric-card" :class="metric.status">
          <template #header>
            <div class="card-header">
              <span>{{ metric.name }}</span>
              <el-tooltip :content="metric.description" placement="top">
                <el-icon><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
          <div class="metric-value">
            {{ formatMetricValue(metric.value, metric.unit) }}
          </div>
          <div class="metric-chart" ref="chartRefs"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="detail-charts">
      <template #header>
        <div class="card-header">
          <span>详细监控</span>
          <el-checkbox-group v-model="selectedMetrics" size="small">
            <el-checkbox-button v-for="metric in metrics" :key="metric.id" :label="metric.id">
              {{ metric.name }}
            </el-checkbox-button>
          </el-checkbox-group>
        </div>
      </template>
      <div class="chart-container" ref="mainChartRef"></div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import {
  VideoPlay, Download, InfoFilled
} from '@element-plus/icons-vue'

const props = defineProps({
  taskId: {
    type: String,
    required: true
  }
})

// 状态变量
const timeRange = ref('realtime')
const isMonitoring = ref(false)
const selectedMetrics = ref([])
const chartRefs = ref([])
const mainChartRef = ref(null)
const charts = ref([])
const mainChart = ref(null)

// 监控指标定义
const metrics = ref([
  {
    id: 'cpu',
    name: 'CPU使用率',
    description: '当前CPU使用百分比',
    value: 0,
    unit: '%',
    status: 'normal',
    history: [],
    threshold: {
      warning: 70,
      critical: 90
    }
  },
  {
    id: 'memory',
    name: '内存使用',
    description: '当前内存使用量',
    value: 0,
    unit: 'MB',
    status: 'normal',
    history: [],
    threshold: {
      warning: 80,
      critical: 95
    }
  },
  {
    id: 'disk_io',
    name: '磁盘IO',
    description: '磁盘读写速率',
    value: 0,
    unit: 'MB/s',
    status: 'normal',
    history: [],
    threshold: {
      warning: 100,
      critical: 200
    }
  },
  {
    id: 'network',
    name: '网络流量',
    description: '网络传输速率',
    value: 0,
    unit: 'MB/s',
    status: 'normal',
    history: [],
    threshold: {
      warning: 50,
      critical: 100
    }
  }
])

// 格式化指标值
const formatMetricValue = (value, unit) => {
  if (unit === '%') {
    return `${value.toFixed(1)}${unit}`
  } else if (unit === 'MB' || unit === 'MB/s') {
    if (value >= 1024) {
      return `${(value / 1024).toFixed(1)} GB${unit.includes('/') ? '/s' : ''}`
    }
    return `${value.toFixed(1)} ${unit}`
  }
  return `${value} ${unit}`
}

// 更新指标状态
const updateMetricStatus = (metric) => {
  if (metric.value >= metric.threshold.critical) {
    metric.status = 'critical'
  } else if (metric.value >= metric.threshold.warning) {
    metric.status = 'warning'
  } else {
    metric.status = 'normal'
  }
}

// 初始化小图表
const initSmallCharts = () => {
  chartRefs.value.forEach((el, index) => {
    const chart = echarts.init(el)
    const metric = metrics.value[index]
    
    chart.setOption({
      grid: {
        top: 5,
        right: 5,
        bottom: 5,
        left: 5
      },
      xAxis: {
        type: 'time',
        show: false
      },
      yAxis: {
        type: 'value',
        show: false
      },
      series: [{
        type: 'line',
        data: metric.history,
        symbol: 'none',
        lineStyle: {
          width: 1
        },
        areaStyle: {
          opacity: 0.2
        }
      }]
    })
    
    charts.value.push(chart)
  })
}

// 初始化主图表
const initMainChart = () => {
  if (!mainChartRef.value) return
  
  mainChart.value = echarts.init(mainChartRef.value)
  updateMainChart()
}

// 更新主图表
const updateMainChart = () => {
  if (!mainChart.value) return
  
  const series = selectedMetrics.value.map(id => {
    const metric = metrics.value.find(m => m.id === id)
    return {
      name: metric.name,
      type: 'line',
      data: metric.history,
      symbol: 'none'
    }
  })
  
  mainChart.value.setOption({
    grid: {
      top: 30,
      right: 20,
      bottom: 30,
      left: 50
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: selectedMetrics.value.map(id => 
        metrics.value.find(m => m.id === id).name
      )
    },
    xAxis: {
      type: 'time'
    },
    yAxis: {
      type: 'value'
    },
    series
  })
}

// 开始监控
const startMonitoring = () => {
  isMonitoring.value = !isMonitoring.value
  if (isMonitoring.value) {
    // TODO: 建立WebSocket连接接收实时数据
  } else {
    // TODO: 关闭WebSocket连接
  }
}

// 导出监控数据
const exportMetrics = () => {
  const data = metrics.value.map(metric => ({
    name: metric.name,
    history: metric.history.map(point => ({
      time: new Date(point[0]).toLocaleString(),
      value: point[1],
      unit: metric.unit
    }))
  }))
  
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `metrics_${props.taskId}_${Date.now()}.json`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

// 监听选中指标变化
watch(selectedMetrics, () => {
  updateMainChart()
})

// 监听时间范围变化
watch(timeRange, () => {
  // TODO: 根据时间范围更新数据
})

onMounted(() => {
  initSmallCharts()
  initMainChart()
})

onUnmounted(() => {
  charts.value.forEach(chart => chart.dispose())
  mainChart.value?.dispose()
})
</script>

<style lang="scss" scoped>
.performance-monitor {
  height: 100%;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;

  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .active {
      background: var(--el-color-primary) !important;
      border-color: var(--el-color-primary) !important;
      color: white !important;
    }
  }

  .metrics-grid {
    .metric-card {
      height: 180px;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-5px);
      }

      &.warning {
        border-color: var(--el-color-warning);
        .metric-value {
          color: var(--el-color-warning);
        }
      }

      &.critical {
        border-color: var(--el-color-danger);
        .metric-value {
          color: var(--el-color-danger);
        }
      }

      .card-header {
        display: flex;
        align-items: center;
        gap: 8px;

        .el-icon {
          font-size: 14px;
          color: #909399;
          cursor: help;
        }
      }

      .metric-value {
        font-size: 24px;
        font-weight: bold;
        margin: 10px 0;
      }

      .metric-chart {
        height: 60px;
      }
    }
  }

  .detail-charts {
    flex: 1;
    display: flex;
    flex-direction: column;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .chart-container {
      flex: 1;
      min-height: 300px;
    }
  }
}
</style> 