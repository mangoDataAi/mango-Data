<template>
  <div 
    class="time-series-browser"
    v-loading.fullscreen.lock="fullscreenLoading"
    :element-loading-text="loadingText"
    element-loading-background="rgba(0, 0, 0, 0.8)"
  >
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button link @click="router.back()">
              <el-icon><ArrowLeft /></el-icon>
              返回
            </el-button>
            <el-divider direction="vertical" />
            <span class="datasource-info">
              {{ route.query.name }} ({{ route.query.type }})
            </span>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="refreshMetrics">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <div class="content-wrapper">
        <!-- 左侧指标列表 -->
        <div class="left-area">
          <div class="search-box">
            <el-input
              v-model="searchText"
              placeholder="搜索指标"
              :prefix-icon="Search"
              clearable
              @clear="filterMetrics"
              @input="filterMetrics"
            />
          </div>
          <div class="metrics-list">
            <el-scrollbar>
              <div 
                v-for="metric in filteredMetrics" 
                :key="metric.name"
                class="metric-item"
                :class="{ 'active': selectedMetric && selectedMetric.name === metric.name }"
                @click="selectMetric(metric)"
              >
                <el-icon><DataLine /></el-icon>
                <span class="metric-name">{{ metric.name }}</span>
              </div>
            </el-scrollbar>
          </div>
        </div>

        <!-- 右侧内容区域 -->
        <div class="right-area">
          <template v-if="selectedMetric">
            <div class="time-range-selector">
              <div class="title">时间范围</div>
              <div class="range-controls">
                <el-radio-group v-model="timeRangeType" size="default" @change="handleTimeRangeTypeChange">
                  <el-radio-button label="quick">快速选择</el-radio-button>
                  <el-radio-button label="custom">自定义</el-radio-button>
                </el-radio-group>
                
                <template v-if="timeRangeType === 'quick'">
                  <el-select v-model="quickRange" placeholder="选择时间范围" @change="handleQuickRangeChange">
                    <el-option label="最近15分钟" value="15m" />
                    <el-option label="最近1小时" value="1h" />
                    <el-option label="最近3小时" value="3h" />
                    <el-option label="最近6小时" value="6h" />
                    <el-option label="最近12小时" value="12h" />
                    <el-option label="最近1天" value="1d" />
                    <el-option label="最近3天" value="3d" />
                    <el-option label="最近7天" value="7d" />
                    <el-option label="最近30天" value="30d" />
                  </el-select>
                </template>
                
                <template v-else>
                  <el-date-picker
                    v-model="dateRange"
                    type="datetimerange"
                    range-separator="至"
                    start-placeholder="开始日期"
                    end-placeholder="结束日期"
                    @change="handleDateRangeChange"
                  />
                </template>
                
                <el-button type="primary" @click="loadTimeSeriesData">查询</el-button>
              </div>
            </div>
            
            <div class="metric-info">
              <div class="title">指标信息</div>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="指标名称">{{ selectedMetric.name }}</el-descriptions-item>
                <el-descriptions-item label="指标类型">{{ selectedMetric.type || '时间序列' }}</el-descriptions-item>
                <el-descriptions-item label="数据单位">{{ selectedMetric.unit || '-' }}</el-descriptions-item>
                <el-descriptions-item label="采样频率">{{ selectedMetric.interval || '-' }}</el-descriptions-item>
                <el-descriptions-item label="描述" :span="2">{{ selectedMetric.description || '-' }}</el-descriptions-item>
              </el-descriptions>
            </div>
            
            <div class="tag-selector" v-if="selectedMetric.tags && selectedMetric.tags.length">
              <div class="title">标签过滤</div>
              <div class="tags-container">
                <div v-for="tag in selectedMetric.tags" :key="tag.key" class="tag-filter">
                  <span class="tag-name">{{ tag.key }}:</span>
                  <el-select 
                    v-model="tagFilters[tag.key]" 
                    placeholder="选择标签值" 
                    multiple 
                    collapse-tags
                    @change="handleTagFilterChange"
                  >
                    <el-option
                      v-for="value in tag.values"
                      :key="value"
                      :label="value"
                      :value="value"
                    />
                  </el-select>
                </div>
              </div>
            </div>
            
            <div class="chart-container">
              <div class="title">
                时序数据
                <div class="chart-controls">
                  <el-select v-model="aggregation" placeholder="聚合函数" style="width: 120px">
                    <el-option label="平均值" value="avg" />
                    <el-option label="最大值" value="max" />
                    <el-option label="最小值" value="min" />
                    <el-option label="求和" value="sum" />
                    <el-option label="计数" value="count" />
                  </el-select>
                  <el-select v-model="resolution" placeholder="分辨率" style="width: 120px">
                    <el-option label="自动" value="auto" />
                    <el-option label="1分钟" value="1m" />
                    <el-option label="5分钟" value="5m" />
                    <el-option label="10分钟" value="10m" />
                    <el-option label="30分钟" value="30m" />
                    <el-option label="1小时" value="1h" />
                    <el-option label="6小时" value="6h" />
                    <el-option label="1天" value="1d" />
                  </el-select>
                </div>
              </div>
              <div class="chart-area" ref="chartContainer"></div>
            </div>
            
            <div class="data-table">
              <div class="title">数据列表</div>
              <el-table :data="timeSeriesData" style="width: 100%" height="250" border>
                <el-table-column prop="timestamp" label="时间" width="180">
                  <template #default="{ row }">
                    {{ formatDateTime(row.timestamp) }}
                  </template>
                </el-table-column>
                <el-table-column prop="value" label="值">
                  <template #default="{ row }">
                    {{ formatNumber(row.value) }}
                  </template>
                </el-table-column>
                <template v-if="selectedMetric.tags && selectedMetric.tags.length">
                  <el-table-column 
                    v-for="tag in selectedMetric.tags" 
                    :key="tag.key"
                    :prop="'tags.' + tag.key"
                    :label="tag.key"
                  />
                </template>
              </el-table>
              <div class="export-actions">
                <el-button @click="exportToCsv">导出为CSV</el-button>
                <el-button @click="exportToExcel">导出为Excel</el-button>
              </div>
            </div>
          </template>
          
          <div v-else class="no-metric-selected">
            <el-empty description="请选择一个指标" />
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  ArrowLeft, Refresh, Search, DataLine
} from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { 
  LineChart,
  BarChart
} from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  DatasetComponent,
  TransformComponent,
  LegendComponent,
  ToolboxComponent
} from 'echarts/components'
import { 
  LabelLayout, 
  UniversalTransition 
} from 'echarts/features'
import { 
  CanvasRenderer 
} from 'echarts/renderers'
// API导入
import { 
  getMetrics, 
  getTimeSeriesData, 
  getMetricInfo
} from '@/api/timeseries'

// 注册ECharts组件
echarts.use([
  TitleComponent,
  TooltipComponent,
  GridComponent,
  DatasetComponent,
  TransformComponent,
  LegendComponent,
  ToolboxComponent,
  LineChart,
  BarChart,
  LabelLayout,
  UniversalTransition,
  CanvasRenderer
])

const router = useRouter()
const route = useRoute()

// 图表实例
let chartInstance = null
const chartContainer = ref(null)

// 加载状态
const fullscreenLoading = ref(false)
const loadingText = ref('加载中...')

// 指标列表
const metrics = ref([])
const searchText = ref('')
const selectedMetric = ref(null)

// 时间序列数据
const timeSeriesData = ref([])

// 时间范围控制
const timeRangeType = ref('quick')
const quickRange = ref('1h')
const dateRange = ref([
  new Date(Date.now() - 3600 * 1000), // 1小时前
  new Date() // 现在
])

// 图表控制
const aggregation = ref('avg')
const resolution = ref('auto')

// 标签过滤器
const tagFilters = ref({})

// 过滤后的指标列表
const filteredMetrics = computed(() => {
  if (!searchText.value) {
    return metrics.value
  }
  const searchLower = searchText.value.toLowerCase()
  return metrics.value.filter(m => 
    m.name.toLowerCase().includes(searchLower) || 
    (m.description && m.description.toLowerCase().includes(searchLower))
  )
})

// 加载所有指标
const loadMetrics = async () => {
  try {
    fullscreenLoading.value = true
    loadingText.value = '加载指标列表...'
    
    const response = await getMetrics(route.params.id)
    metrics.value = response.data || []
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('加载指标列表失败:', error)
    ElMessage.error('加载指标列表失败')
    fullscreenLoading.value = false
  }
}

// 刷新指标
const refreshMetrics = () => {
  loadMetrics()
}

// 过滤指标
const filterMetrics = () => {
  // 通过计算属性过滤
}

// 选择指标
const selectMetric = async (metric) => {
  try {
    fullscreenLoading.value = true
    loadingText.value = '加载指标详情...'
    
    const response = await getMetricInfo(route.params.id, metric.name)
    selectedMetric.value = { ...metric, ...response.data }
    
    // 获取标签键
    // const tagKeysResponse = await getTagKeys(route.params.id, metric.name)
    // selectedMetric.value.tagKeys = tagKeysResponse.data || []
    
    // 获取每个标签键可能的值
    selectedMetric.value.tags = []
    // for (const key of selectedMetric.value.tagKeys) {
    //   const tagValuesResponse = await getTagValues(route.params.id, metric.name, key)
    //   selectedMetric.value.tags.push({
    //     key: key,
    //     values: tagValuesResponse.data || []
    //   })
    // }
    
    // 获取支持的聚合函数
    // const aggregationsResponse = await getSupportedAggregations(route.params.id, metric.name)
    // selectedMetric.value.supportedAggregations = aggregationsResponse.data || []
    
    // 重置标签过滤器
    tagFilters.value = {}
    
    // 加载时间序列数据
    await loadTimeSeriesData()
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('加载指标详情失败:', error)
    ElMessage.error('加载指标详情失败')
    fullscreenLoading.value = false
  }
}

// 加载时间序列数据
const loadTimeSeriesData = async () => {
  if (!selectedMetric.value) return
  
  try {
    fullscreenLoading.value = true
    loadingText.value = '加载时间序列数据...'
    
    // 计算开始和结束时间
    const startTime = timeRangeType.value === 'quick' 
      ? getTimeFromQuickRange(quickRange.value)
      : dateRange.value[0]
    const endTime = timeRangeType.value === 'custom' 
      ? dateRange.value[1] 
      : new Date()
    
    const response = await getTimeSeriesData(
      route.params.id, 
      {
        metric: selectedMetric.value.name,
        start: startTime.getTime(),
        end: endTime.getTime(),
        aggregation: aggregation.value,
        resolution: resolution.value,
        tags: tagFilters.value
      }
    )
    
    timeSeriesData.value = response.data || []
    
    // 更新图表
    await nextTick()
    renderChart()
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('加载时间序列数据失败:', error)
    ElMessage.error('加载时间序列数据失败')
    fullscreenLoading.value = false
  }
}

// 根据快速范围获取开始时间
const getTimeFromQuickRange = (range) => {
  const now = new Date()
  const value = parseInt(range.slice(0, -1))
  const unit = range.slice(-1)
  
  switch (unit) {
    case 'm': // 分钟
      return new Date(now.getTime() - value * 60 * 1000)
    case 'h': // 小时
      return new Date(now.getTime() - value * 3600 * 1000)
    case 'd': // 天
      return new Date(now.getTime() - value * 24 * 3600 * 1000)
    default:
      return new Date(now.getTime() - 3600 * 1000) // 默认1小时
  }
}

// 处理时间范围类型变更
const handleTimeRangeTypeChange = () => {
  if (timeRangeType.value === 'quick') {
    quickRange.value = '1h'
  } else {
    dateRange.value = [
      new Date(Date.now() - 3600 * 1000), // 1小时前
      new Date() // 现在
    ]
  }
}

// 处理快速范围变更
const handleQuickRangeChange = () => {
  loadTimeSeriesData()
}

// 处理日期范围变更
const handleDateRangeChange = () => {
  // 日期选择器变更后不自动加载数据，由用户点击查询按钮触发
}

// 处理标签过滤变更
const handleTagFilterChange = () => {
  loadTimeSeriesData()
}

// 初始化图表
const initChart = () => {
  if (chartInstance) {
    chartInstance.dispose()
  }
  
  if (chartContainer.value) {
    chartInstance = echarts.init(chartContainer.value)
    
    // 监听窗口大小变化，调整图表大小
    window.addEventListener('resize', () => {
      chartInstance.resize()
    })
  }
}

// 渲染图表
const renderChart = () => {
  if (!chartInstance || !timeSeriesData.value.length) return
  
  const xAxisData = timeSeriesData.value.map(item => formatDateTime(item.timestamp))
  const seriesData = timeSeriesData.value.map(item => item.value)
  
  const option = {
    title: {
      text: selectedMetric.value.name,
      subtext: selectedMetric.value.description || '',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis',
      formatter: function(params) {
        const data = params[0]
        return `${data.axisValue}<br/>${data.marker}${selectedMetric.value.name}: ${formatNumber(data.value)} ${selectedMetric.value.unit || ''}`
      }
    },
    toolbox: {
      feature: {
        saveAsImage: {}
      }
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
      data: xAxisData
    },
    yAxis: {
      type: 'value',
      name: selectedMetric.value.unit || '',
      nameLocation: 'end',
      nameGap: 15
    },
    series: [
      {
        name: selectedMetric.value.name,
        type: 'line',
        smooth: true,
        symbol: 'none',
        sampling: 'lttb',
        data: seriesData
      }
    ]
  }
  
  chartInstance.setOption(option)
}

// 导出数据
const exportToCsv = () => {
  if (!timeSeriesData.value.length) {
    ElMessage.warning('暂无数据可导出')
    return
  }
  
  try {
    // 构建CSV内容
    const header = ['Timestamp', 'Value']
    
    // 添加标签列
    if (selectedMetric.value.tags && selectedMetric.value.tags.length) {
      selectedMetric.value.tags.forEach(tag => {
        header.push(tag.key)
      })
    }
    
    let csvContent = header.join(',') + '\n'
    
    timeSeriesData.value.forEach(item => {
      const row = [
        formatDateTime(item.timestamp),
        item.value
      ]
      
      // 添加标签值
      if (selectedMetric.value.tags && selectedMetric.value.tags.length) {
        selectedMetric.value.tags.forEach(tag => {
          row.push(item.tags ? (item.tags[tag.key] || '') : '')
        })
      }
      
      csvContent += row.join(',') + '\n'
    })
    
    // 创建并下载文件
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const link = document.createElement('a')
    const url = URL.createObjectURL(blob)
    
    link.setAttribute('href', url)
    link.setAttribute('download', `${selectedMetric.value.name}_${formatDateForFilename(new Date())}.csv`)
    link.style.visibility = 'hidden'
    
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
    ElMessage.success('CSV导出成功')
  } catch (error) {
    console.error('导出CSV失败:', error)
    ElMessage.error('导出CSV失败')
  }
}

const exportToExcel = () => {
  ElMessage.info('Excel导出功能开发中')
}

// 格式化函数
const formatDateTime = (timestamp) => {
  if (!timestamp) return '-'
  
  const date = new Date(timestamp)
  return date.toLocaleString()
}

const formatNumber = (value) => {
  if (value === undefined || value === null) return '-'
  
  if (typeof value === 'number') {
    // 如果是整数则不显示小数部分
    if (Number.isInteger(value)) {
      return value.toString()
    }
    // 否则保留2位小数
    return value.toFixed(2)
  }
  
  return value.toString()
}

const formatDateForFilename = (date) => {
  const pad = (num) => (num < 10 ? '0' + num : num)
  
  return `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}_${pad(date.getHours())}${pad(date.getMinutes())}`
}

// 加载多个指标的数据（批量查询）
const loadBatchTimeSeriesData = async (metrics) => {
  try {
    fullscreenLoading.value = true
    loadingText.value = '批量加载时间序列数据...'
    
    // 计算开始和结束时间
    const startTime = timeRangeType.value === 'quick' 
      ? getTimeFromQuickRange(quickRange.value)
      : dateRange.value[0]
    const endTime = timeRangeType.value === 'custom' 
      ? dateRange.value[1] 
      : new Date()
    
    // API尚未实现，使用临时方案
    ElMessage.info('批量加载时间序列数据功能尚未实现')
    // const response = await getBatchTimeSeriesData(
    //   route.params.id, 
    //   {
    //     metrics: metrics,
    //     start: startTime.getTime(),
    //     end: endTime.getTime(),
    //     aggregation: aggregation.value,
    //     resolution: resolution.value,
    //     tags: tagFilters.value
    //   }
    // )  // 调用 /api/timeseries/{dataSourceId}/batch-data
    
    // return response.data || {}
    return {}
  } catch (error) {
    console.error('批量加载时间序列数据失败:', error)
    ElMessage.error('批量加载时间序列数据失败')
    return {}
  } finally {
    fullscreenLoading.value = false
  }
}

// 写入时间序列数据
const writeDataPoint = async (dataPoint) => {
  try {
    fullscreenLoading.value = true
    loadingText.value = '写入数据...'
    
    // API尚未实现，使用临时方案
    ElMessage.info('写入时间序列数据功能尚未实现')
    // const response = await writeTimeSeriesData(
    //   route.params.id,
    //   {
    //     metric: selectedMetric.value.name,
    //     timestamp: dataPoint.timestamp,
    //     value: dataPoint.value,
    //     tags: dataPoint.tags || {}
    //   }
    // )  // 调用 /api/timeseries/{dataSourceId}/write
    
    // if (response.data) {
    //   ElMessage.success('数据写入成功')
    //   await loadTimeSeriesData() // 重新加载数据以显示新写入的点
    // }
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('写入时间序列数据失败:', error)
    ElMessage.error('写入时间序列数据失败')
    fullscreenLoading.value = false
  }
}

// 获取数据库统计信息
const loadDatabaseStats = async () => {
  try {
    // API尚未实现，使用临时方案
    ElMessage.info('获取数据库统计信息功能尚未实现')
    // const response = await getDatabaseStats(route.params.id)  // 调用 /api/timeseries/{dataSourceId}/stats
    // return response.data
    return {}
  } catch (error) {
    console.error('获取数据库统计信息失败:', error)
    ElMessage.error('获取数据库统计信息失败')
    return {}
  }
}

// 初始化
onMounted(async () => {
  await loadMetrics()
  initChart()
})

// 监听变化
watch([aggregation, resolution], () => {
  if (selectedMetric.value) {
    loadTimeSeriesData()
  }
})

// 组件销毁前
onMounted(() => {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
})
</script>

<style scoped>
.time-series-browser {
  height: calc(100vh - 140px);
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

.datasource-info {
  font-weight: bold;
}

.header-right {
  display: flex;
  gap: 8px;
}

.content-wrapper {
  display: flex;
  height: calc(100vh - 200px);
  overflow: hidden;
}

.left-area {
  width: 240px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #ebeef5;
  padding-right: 16px;
}

.search-box {
  margin-bottom: 16px;
}

.metrics-list {
  flex: 1;
  overflow: auto;
}

.metric-item {
  display: flex;
  align-items: center;
  padding: 10px;
  cursor: pointer;
  border-radius: 4px;
  margin-bottom: 4px;
}

.metric-item:hover {
  background-color: #f5f7fa;
}

.metric-item.active {
  background-color: #ecf5ff;
  color: #409eff;
}

.metric-name {
  margin-left: 8px;
}

.right-area {
  flex: 1;
  padding-left: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.no-metric-selected {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.title {
  font-weight: bold;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.time-range-selector {
  margin-bottom: 20px;
}

.range-controls {
  display: flex;
  gap: 12px;
  align-items: center;
}

.metric-info {
  margin-bottom: 20px;
}

.tag-selector {
  margin-bottom: 20px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-filter {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tag-name {
  font-weight: bold;
  min-width: 60px;
}

.chart-container {
  margin-bottom: 20px;
}

.chart-controls {
  display: flex;
  gap: 8px;
}

.chart-area {
  height: 300px;
  width: 100%;
}

.data-table {
  margin-top: auto;
}

.export-actions {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style> 