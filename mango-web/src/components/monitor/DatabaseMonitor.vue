<template>

  <el-dialog

      :title="`${datasource?.name || ''} - 数据源监控`"

      :model-value="visible"

      @update:model-value="emit('update:visible', $event)"

      width="80%"

      destroy-on-close

      @closed="handleClose"

  >

    <div v-loading="loading" element-loading-text="加载中...">

      <div class="monitor-header">

        <div class="monitor-info">

          <span>数据库类型: {{ datasource?.type }}</span>

          <el-tag

              :type="metrics.status === 'CONNECTED' ? 'success' : 'danger'"

          >

            {{ metrics.status === 'CONNECTED' ? '已连接' : '未连接' }}

          </el-tag>

        </div>

        <el-switch

            v-model="autoRefresh"

            active-text="自动刷新"

            inactive-text="手动刷新"

        />

      </div>


      <el-tabs v-model="activeTab">

        <el-tab-pane label="连接监控" name="connection">

          <div ref="connectionChart" class="monitor-chart"></div>

          <el-descriptions :column="3" border>

            <el-descriptions-item label="活跃连接数">

              {{ metrics.activeConnections || 0 }}

            </el-descriptions-item>

            <el-descriptions-item label="总连接数">

              {{ metrics.totalConnections || 0 }}

            </el-descriptions-item>

            <el-descriptions-item label="空闲连接数">

              {{ metrics.idleConnections || 0 }}

            </el-descriptions-item>

            <el-descriptions-item label="最大连接数">

              {{ metrics.maxConnections || 0 }}

            </el-descriptions-item>

            <el-descriptions-item label="连接使用率">

              {{ (metrics.connectionUsage * 100).toFixed(2) }}%

            </el-descriptions-item>

            <el-descriptions-item label="运行时间">

              {{ formatUptime(metrics.uptime) }}

            </el-descriptions-item>

          </el-descriptions>

        </el-tab-pane>


        <el-tab-pane label="性能监控" name="performance">

          <div class="performance-content">

            <el-row :gutter="20" class="cache-stats">

              <el-col :span="8">

                <el-card shadow="hover">

                  <template #header>

                    <div class="card-header">

                      <span>缓存命中率</span>

                    </div>

                  </template>

                  <div class="metric-value">

                    {{ ((performanceMetrics.cacheStats?.hitRatio || 0) * 100).toFixed(2) }}%

                  </div>

                </el-card>

              </el-col>

              <el-col :span="8">

                <el-card shadow="hover">

                  <template #header>

                    <div class="card-header">

                      <span>缓存命中数</span>

                    </div>

                  </template>

                  <div class="metric-value">

                    {{ performanceMetrics.cacheStats?.hitCount || 0 }}

                  </div>

                </el-card>

              </el-col>

              <el-col :span="8">

                <el-card shadow="hover">

                  <template #header>
                    <div class="card-header">
                      <span>缓存未命中数</span>

                    </div>
                  </template>
                  <div class="metric-value">
                    {{ performanceMetrics.cacheStats?.missCount || 0 }}
                  </div>
                </el-card>

              </el-col>

            </el-row>


            <div class="slow-query-section">

              <div class="table-header">

                <h3>慢查询列表</h3>

              </div>

              <el-table

                  :data="performanceMetrics.slowQueries"

                  border

                  stripe

                  class="slow-query-table"

              >

                <el-table-column prop="sql" label="SQL语句" show-overflow-tooltip/>

                <el-table-column prop="executionTime" label="执行时间(ms)" width="120" sortable/>

                <el-table-column prop="executeTime" label="执行时间点" width="180" sortable/>

                <el-table-column prop="user" label="执行用户" width="120"/>

              </el-table>

            </div>

          </div>

        </el-tab-pane>

      </el-tabs>

    </div>


    <template #footer>

      <div class="dialog-footer">

        <el-button :loading="loading" @click="refresh">

          <el-icon>
            <Refresh/>
          </el-icon>
          刷新

        </el-button>

        <el-button @click="handleClose">关闭</el-button>

      </div>

    </template>

  </el-dialog>

</template>


<script setup>

import {ref, onMounted, watch, onBeforeUnmount, nextTick} from 'vue'

import {ElMessage} from 'element-plus'

import {getAllMetrics} from '@/api/monitor'

import * as echarts from 'echarts'

import {Refresh} from '@element-plus/icons-vue'


const props = defineProps({

  visible: Boolean,

  datasource: Object

})


const emit = defineEmits(['update:visible'])


// 组件状态

const activeTab = ref('connection')

const autoRefresh = ref(false)

const metrics = ref({})

const performanceMetrics = ref({

  slowQueries: [],

  cacheStats: {}

})

const chartData = ref({

  connections: [],

  performance: []

})


// 图表实例

let connChartInstance = null

const connectionChart = ref(null)

let refreshTimer = null


// 添加 loading 状态

const loading = ref(false)


// 添加一个标记来区分是否是自动刷新

const isAutoRefreshing = ref(false)


// 添加清理数据的方法

const clearMonitorData = () => {

  // 清理指标数据

  metrics.value = {}

  performanceMetrics.value = {

    slowQueries: [],

    cacheStats: {}

  }

  // 清理图表数据

  chartData.value = {

    connections: [],

    performance: []

  }

  // 重置标签页

  activeTab.value = 'connection'

}


// 加载监控数据

const loadMonitorData = async () => {

  try {

    if (!props.datasource?.id) {

      console.warn('数据源未就绪')

      return

    }


    // 只在非自动刷新时显示加载动画

    if (!isAutoRefreshing.value) {

      loading.value = true

    }


    const response = await getAllMetrics(props.datasource.id)


    if (!response) {

      throw new Error('获取监控数据失败：响应为空')

    }


    if (response.code !== 0) {

      throw new Error(response.msg || '获取监控数据失败')

    }


    // 更新连接指标

    if (response.data?.connection) {

      metrics.value = response.data.connection

      chartData.value.connections.push({

        timestamp: new Date(),

        ...response.data.connection

      })

      if (chartData.value.connections.length > 30) {

        chartData.value.connections.shift()

      }

    }


    // 更新性能指标

    if (response.data?.performance) {

      performanceMetrics.value = response.data.performance

      chartData.value.performance.push({

        timestamp: new Date(),

        ...response.data.performance

      })

      if (chartData.value.performance.length > 30) {

        chartData.value.performance.shift()

      }

    }


    // 更新图表

    nextTick(() => {

      updateCharts()

    })


  } catch (error) {

    console.error('获取监控数据失败:', error)

    ElMessage.error(error.message)

    if (autoRefresh.value) {

      autoRefresh.value = false

    }

  } finally {

    loading.value = false

  }

}


// 自动刷新控制

const startAutoRefresh = () => {

  stopAutoRefresh() // 先清除可能存在的定时器

  refreshTimer = setInterval(() => {

    isAutoRefreshing.value = true

    loadMonitorData()

    isAutoRefreshing.value = false

  }, 5000) // 每5秒刷新一次

}


const stopAutoRefresh = () => {

  if (refreshTimer) {

    clearInterval(refreshTimer)

    refreshTimer = null

  }

}


// 格式化运行时间

const formatUptime = (ms) => {

  if (!ms) return '0'

  const seconds = Math.floor(ms / 1000)

  const days = Math.floor(seconds / 86400)

  const hours = Math.floor((seconds % 86400) / 3600)

  const minutes = Math.floor((seconds % 3600) / 60)

  const remainingSeconds = seconds % 60


  const parts = []

  if (days > 0) parts.push(`${days}天`)

  if (hours > 0) parts.push(`${hours}小时`)

  if (minutes > 0) parts.push(`${minutes}分钟`)

  if (remainingSeconds > 0) parts.push(`${remainingSeconds}秒`)


  return parts.join(' ')

}


// 更新图表

const updateCharts = () => {

  if (connChartInstance) {

    const series = [

      {

        name: '活跃连接',

        type: 'line',

        data: chartData.value.connections.map(item => [

          item.timestamp,

          item.activeConnections

        ])

      },

      {

        name: '总连接数',

        type: 'line',

        data: chartData.value.connections.map(item => [

          item.timestamp,

          item.totalConnections

        ])

      },

      {

        name: '空闲连接',

        type: 'line',

        data: chartData.value.connections.map(item => [

          item.timestamp,

          item.idleConnections

        ])

      }

    ]

    connChartInstance.setOption({series})

  }

}


// 初始化图表

const initCharts = () => {

  try {

    loading.value = true


    // 销毁旧的图表实例

    connChartInstance?.dispose()


    if (!connectionChart.value) {

      console.warn('图表容器未就绪')

      return

    }


    // 初始化连接监控图表

    connChartInstance = echarts.init(connectionChart.value)

    connChartInstance.setOption({

      title: {text: '连接数变化趋势'},

      tooltip: {

        trigger: 'axis',

        formatter: function (params) {

          const time = new Date(params[0].value[0]).toLocaleString()

          let result = `${time}<br/>`

          params.forEach(param => {

            result += `${param.seriesName}: ${param.value[1]}<br/>`

          })

          return result

        }

      },

      legend: {

        data: ['活跃连接', '总连接数', '空闲连接']

      },

      xAxis: {type: 'time'},

      yAxis: {type: 'value'},

      series: [

        {

          name: '活跃连接',

          type: 'line',

          data: []

        },

        {

          name: '总连接数',

          type: 'line',

          data: []

        },

        {

          name: '空闲连接',

          type: 'line',

          data: []

        }

      ]

    })


  } catch (error) {

    console.error('初始化图表失败:', error)

    ElMessage.error('初始化图表失败')

  } finally {

    loading.value = false

  }

}


// 手动刷新

const refresh = () => {

  isAutoRefreshing.value = false

  loadMonitorData()

}


// 关闭对话框

const handleClose = () => {

  stopAutoRefresh()

  clearMonitorData() // 关闭时也清理数据

  emit('update:visible', false)

}


// 生命周期钩子

onMounted(() => {

  // 只在有数据源时初始化

  if (props.datasource?.id) {

    nextTick(() => {

      initCharts()

      loadMonitorData()

    })

  }

})


onBeforeUnmount(() => {

  stopAutoRefresh()

  connChartInstance?.dispose()

})


// 监听器

watch(() => props.visible, (newVal) => {

  if (newVal && props.datasource?.id) {

    clearMonitorData()

    nextTick(() => {

      isAutoRefreshing.value = false // 确保首次加载显示加载动画

      initCharts()

      loadMonitorData()

      if (autoRefresh.value) {

        startAutoRefresh()

      }

    })

  } else {

    stopAutoRefresh()

  }

})


watch(autoRefresh, (newVal) => {

  if (newVal) {

    startAutoRefresh()

  } else {

    stopAutoRefresh()

  }

})


// 修改 datasource 的监听器

watch(() => props.datasource, (newVal) => {

  if (newVal?.id && props.visible) {

    clearMonitorData()

    nextTick(() => {

      isAutoRefreshing.value = false // 确保切换数据源时显示加载动画

      initCharts()

      loadMonitorData()

    })

  }

}, {deep: true})

</script>


<style scoped>

.monitor-header {

  display: flex;

  justify-content: space-between;

  align-items: center;

  margin-bottom: 20px;

  padding: 0 20px;

}


.monitor-info {

  display: flex;

  align-items: center;

  gap: 16px;

}


.monitor-chart {

  height: 300px;

  margin: 20px 0;

}


.el-descriptions {

  margin: 20px 0;

}


.dialog-footer {

  display: flex;

  justify-content: flex-end;

  gap: 12px;

}


/* 添加加载遮罩样式 */

:deep(.el-loading-mask) {

  background-color: rgba(255, 255, 255, 0.7);

}


.performance-content {

  padding: 24px;

}


.cache-stats {

  margin-bottom: 24px;

}


.cache-stats :deep(.el-card) {

  transition: all 0.3s;


  &:hover {

    transform: translateY(-2px);

    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

  }

}


.cache-stats .metric-value {

  font-size: 32px;

  font-weight: bold;

  color: #409EFF;

  text-align: center;

  padding: 20px 0;

}


.slow-query-section {

  margin-top: 24px;

  background: #fff;

  padding: 20px;

  border-radius: 4px;

  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);

}


.table-header {

  margin: 16px 0;


  h3 {

    font-size: 16px;

    font-weight: 500;

    color: #303133;

    margin: 0;

  }

}


.slow-query-table {

  margin-top: 20px;

}

</style>
