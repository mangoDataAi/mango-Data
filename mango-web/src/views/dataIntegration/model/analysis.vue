<template>
  <div class="dataAnalysis">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">数据分析</h2>
        <el-tag>{{ analysisInfo.tableName }}</el-tag>
        <el-tag type="success">{{ analysisInfo.sourceType }}</el-tag>
      </div>
      <div class="header-actions">
        <el-button-group>
          <el-button type="primary" @click="handleRefreshData">
            <el-icon><Refresh /></el-icon>刷新数据
          </el-button>
          <el-button type="success" @click="handleExport">
            <el-icon><Download /></el-icon>导出分析
          </el-button>
        </el-button-group>
      </div>
    </div>

    <el-row :gutter="20" class="analysis-content">
      <!-- 左侧配置面板 -->
      <el-col :span="6">
        <el-card class="config-card">
          <template #header>
            <div class="card-header">
              <span>分析配置</span>
            </div>
          </template>
          
          <el-form :model="analysisConfig" label-position="top">
            <!-- 维度选择 -->
            <el-form-item label="分析维度">
              <el-select
                v-model="analysisConfig.dimensions"
                multiple
                placeholder="选择维度"
                style="width: 100%"
              >
                <el-option
                  v-for="field in dimensionFields"
                  :key="field.name"
                  :label="field.label"
                  :value="field.name"
                />
              </el-select>
            </el-form-item>

            <!-- 指标选择 -->
            <el-form-item label="分析指标">
              <el-select
                v-model="analysisConfig.metrics"
                multiple
                placeholder="选择指标"
                style="width: 100%"
              >
                <el-option
                  v-for="field in metricFields"
                  :key="field.name"
                  :label="field.label"
                  :value="field.name"
                />
              </el-select>
            </el-form-item>

            <!-- 时间范围 -->
            <el-form-item label="时间范围">
              <el-date-picker
                v-model="analysisConfig.timeRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="width: 100%"
              />
            </el-form-item>

            <!-- 聚合方式 -->
            <el-form-item label="聚合方式">
              <el-radio-group v-model="analysisConfig.aggregation">
                <el-radio-button value="count">计数</el-radio-button>
                <el-radio-button value="sum">求和</el-radio-button>
                <el-radio-button value="avg">平均</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <!-- 图表类型 -->
            <el-form-item label="图表类型">
              <el-radio-group v-model="analysisConfig.chartType">
                <el-radio-button value="line">折线图</el-radio-button>
                <el-radio-button value="bar">柱状图</el-radio-button>
                <el-radio-button value="pie">饼图</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="handleAnalyze" style="width: 100%">
                开始分析
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 数据筛选 -->
        <el-card class="filter-card">
          <template #header>
            <div class="card-header">
              <span>数据筛选</span>
              <el-button type="primary" link @click="handleAddFilter">
                添加条件
              </el-button>
            </div>
          </template>

          <div class="filter-list">
            <div
              v-for="(filter, index) in analysisConfig.filters"
              :key="index"
              class="filter-item"
            >
              <el-select
                v-model="filter.field"
                placeholder="选择字段"
                style="width: 120px"
              >
                <el-option
                  v-for="field in allFields"
                  :key="field.name"
                  :label="field.label"
                  :value="field.name"
                />
              </el-select>
              <el-select
                v-model="filter.operator"
                placeholder="运算符"
                style="width: 100px"
              >
                <el-option
                  v-for="op in operators"
                  :key="op.value"
                  :label="op.label"
                  :value="op.value"
                />
              </el-select>
              <el-input
                v-model="filter.value"
                placeholder="输入值"
                style="width: 120px"
              />
              <el-button
                type="danger"
                circle
                @click="handleRemoveFilter(index)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧分析结果 -->
      <el-col :span="18">
        <!-- 图表展示 -->
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>分析图表</span>
              <el-radio-group v-model="displayMode" size="small">
                <el-radio-button value="chart">图表</el-radio-button>
                <el-radio-button value="table">数据表</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          
          <div v-if="displayMode === 'chart'" class="chart-container" ref="chartContainer"></div>
          
          <div v-else class="table-container">
            <el-table :data="analysisData" border style="width: 100%">
              <el-table-column
                v-for="col in tableColumns"
                :key="col.prop"
                :prop="col.prop"
                :label="col.label"
                :width="col.width"
              >
                <template #default="{ row }">
                  <template v-if="col.type === 'number'">
                    {{ formatNumber(row[col.prop]) }}
                  </template>
                  <template v-else>
                    {{ row[col.prop] }}
                  </template>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>

        <!-- 统计摘要 -->
        <el-row :gutter="20" class="summary-row">
          <el-col :span="8" v-for="stat in statistics" :key="stat.label">
            <el-card class="summary-card" shadow="hover">
              <template #header>
                <div class="card-header">
                  <span>{{ stat.label }}</span>
                </div>
              </template>
              <div class="summary-value">
                {{ formatNumber(stat.value) }}
                <span class="trend" :class="stat.trend">
                  {{ stat.trend === 'up' ? '+' : '-' }}{{ stat.change }}%
                </span>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import {
  Refresh, Download, Delete
} from '@element-plus/icons-vue'

const route = useRoute()

// 分析信息
const analysisInfo = reactive({
  tableName: 'user_behavior_detail',
  sourceType: 'Delta Lake'
})

// 分析配置
const analysisConfig = reactive({
  dimensions: [],
  metrics: [],
  timeRange: [],
  aggregation: 'count',
  chartType: 'line',
  filters: []
})

// 字段定义
const dimensionFields = [
  { name: 'event_type', label: '事件类型' },
  { name: 'user_type', label: '用户类型' },
  { name: 'platform', label: '平台' },
  { name: 'region', label: '地区' }
]

const metricFields = [
  { name: 'pv', label: '页面浏览量' },
  { name: 'uv', label: '独立访客数' },
  { name: 'duration', label: '访问时长' },
  { name: 'conversion', label: '转化次数' }
]

const allFields = [...dimensionFields, ...metricFields]

// 运算符选项
const operators = [
  { label: '等于', value: '=' },
  { label: '不等于', value: '!=' },
  { label: '大于', value: '>' },
  { label: '小于', value: '<' },
  { label: '包含', value: 'like' }
]

// 展示模式
const displayMode = ref('chart')

// 图表实例
const chartContainer = ref(null)
let chart = null

// 分析数据
const analysisData = ref([])
const tableColumns = ref([])

// 统计数据
const statistics = ref([
  {
    label: '总访问量',
    value: 1234567,
    trend: 'up',
    change: 12.5
  },
  {
    label: '平均时长',
    value: 325,
    trend: 'up',
    change: 5.8
  },
  {
    label: '转化率',
    value: 15.2,
    trend: 'down',
    change: 2.3
  }
])

// 添加筛选条件
const handleAddFilter = () => {
  analysisConfig.filters.push({
    field: '',
    operator: '=',
    value: ''
  })
}

// 删除筛选条件
const handleRemoveFilter = (index) => {
  analysisConfig.filters.splice(index, 1)
}

// 开始分析
const handleAnalyze = () => {
  if (analysisConfig.dimensions.length === 0) {
    ElMessage.warning('请至少选择一个分析维度')
    return
  }
  if (analysisConfig.metrics.length === 0) {
    ElMessage.warning('请至少选择一个分析指标')
    return
  }
  
  // TODO: 调用分析API
  generateMockData()
  updateChart()
}

// 生成模拟数据
const generateMockData = () => {
  const data = []
  const dimensions = analysisConfig.dimensions
  const metrics = analysisConfig.metrics
  
  // 生成表格列
  tableColumns.value = [
    ...dimensions.map(dim => ({
      prop: dim,
      label: dimensionFields.find(f => f.name === dim)?.label || dim,
      width: 120
    })),
    ...metrics.map(metric => ({
      prop: metric,
      label: metricFields.find(f => f.name === metric)?.label || metric,
      width: 120,
      type: 'number'
    }))
  ]
  
  // 生成数据
  for (let i = 0; i < 20; i++) {
    const row = {}
    dimensions.forEach(dim => {
      row[dim] = `${dim}_${Math.floor(Math.random() * 5)}`
    })
    metrics.forEach(metric => {
      row[metric] = Math.floor(Math.random() * 10000)
    })
    data.push(row)
  }
  
  analysisData.value = data
}

// 更新图表
const updateChart = () => {
  if (!chartContainer.value || !chart) return
  
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: analysisConfig.metrics.map(m => 
        metricFields.find(f => f.name === m)?.label || m
      )
    },
    xAxis: {
      type: 'category',
      data: analysisData.value.map(item => 
        analysisConfig.dimensions.map(dim => item[dim]).join('-')
      )
    },
    yAxis: {
      type: 'value'
    },
    series: analysisConfig.metrics.map(metric => ({
      name: metricFields.find(f => f.name === metric)?.label || metric,
      type: analysisConfig.chartType,
      data: analysisData.value.map(item => item[metric])
    }))
  }
  
  chart.setOption(option)
}

// 刷新数据
const handleRefreshData = () => {
  handleAnalyze()
}

// 导出分析
const handleExport = () => {
  ElMessage.success('开始导出分析结果')
  // TODO: 实现导出逻辑
}

// 格式化数字
const formatNumber = (num) => {
  return new Intl.NumberFormat().format(num)
}

// 初始化
onMounted(() => {
  if (chartContainer.value) {
    chart = echarts.init(chartContainer.value)
    generateMockData()
    updateChart()
    
    window.addEventListener('resize', () => {
      chart?.resize()
    })
  }
})

// 清理
onUnmounted(() => {
  chart?.dispose()
  window.removeEventListener('resize', () => {
    chart?.resize()
  })
})
</script>

<style lang="scss" scoped>
.dataAnalysis {
  padding: 20px;

  .page-header {
    margin-bottom: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .page-title {
        font-size: 24px;
        margin: 0;
      }
    }
  }

  .analysis-content {
    .config-card,
    .filter-card {
      margin-bottom: 20px;

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }
    }

    .filter-list {
      .filter-item {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 10px;
      }
    }

    .chart-card {
      margin-bottom: 20px;

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .chart-container {
        height: 400px;
      }

      .table-container {
        max-height: 400px;
        overflow: auto;
      }
    }

    .summary-row {
      .summary-card {
        .summary-value {
          font-size: 24px;
          font-weight: bold;
          color: #303133;

          .trend {
            font-size: 14px;
            margin-left: 8px;

            &.up {
              color: #67c23a;
            }
            &.down {
              color: #f56c6c;
            }
          }
        }
      }
    }
  }
}
</style> 