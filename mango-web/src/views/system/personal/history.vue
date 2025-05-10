<template>
  <div class="history-container">
    <!-- 历史概览 -->
    <el-row :gutter="20" class="overview-section">
      <el-col :span="6" v-for="(item, index) in historyStats" :key="index">
        <el-card shadow="hover" class="stat-card" :class="item.type">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="32"><component :is="item.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-label">{{ item.label }}</div>
              <div class="stat-trend" :class="item.trend > 0 ? 'up' : 'down'">
                {{ Math.abs(item.trend) }}% <el-icon><component :is="item.trend > 0 ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 历史记录列表 -->
    <el-card class="history-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>历史记录</h3>
            <el-tag type="info" effect="plain">共 {{ total }} 条记录</el-tag>
          </div>
          <div class="header-right">
            <el-button-group>
              <el-tooltip content="导出记录" placement="top">
                <el-button :icon="Download" @click="handleExport" />
              </el-tooltip>
              <el-tooltip content="清空记录" placement="top">
                <el-button :icon="Delete" @click="handleClear" />
              </el-tooltip>
              <el-tooltip content="刷新" placement="top">
                <el-button :icon="Refresh" @click="refreshList" />
              </el-tooltip>
            </el-button-group>
          </div>
        </div>
      </template>

      <!-- 高级搜索 -->
      <el-form :model="searchForm" class="search-form" @keyup.enter="handleSearch">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="操作类型">
              <el-select v-model="searchForm.type" placeholder="请选择操作类型" clearable>
                <el-option label="页面访问" value="PAGE_VIEW" />
                <el-option label="数据查询" value="DATA_QUERY" />
                <el-option label="数据导出" value="DATA_EXPORT" />
                <el-option label="系统设置" value="SYSTEM_CONFIG" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="操作状态">
              <el-select v-model="searchForm.status" placeholder="请选择操作状态" clearable>
                <el-option label="成功" value="SUCCESS" />
                <el-option label="失败" value="FAILED" />
                <el-option label="异常" value="ERROR" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="操作时间">
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
        <el-row :gutter="16" v-show="showAdvanced">
          <el-col :span="6">
            <el-form-item label="操作模块">
              <el-cascader
                v-model="searchForm.module"
                :options="moduleOptions"
                :props="{ checkStrictly: true }"
                clearable
                placeholder="请选择操作模块"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="操作IP">
              <el-input v-model="searchForm.ip" placeholder="请输入操作IP" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="操作耗时">
              <el-input-number
                v-model="searchForm.minDuration"
                :min="0"
                placeholder="最小耗时"
                style="width: 120px"
              />
              <span class="duration-separator">至</span>
              <el-input-number
                v-model="searchForm.maxDuration"
                :min="0"
                placeholder="最大耗时"
                style="width: 120px"
              />
              <span class="duration-unit">ms</span>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div class="search-toggle" @click="showAdvanced = !showAdvanced">
        {{ showAdvanced ? '收起' : '展开' }}高级搜索
        <el-icon class="toggle-icon" :class="{ 'is-active': showAdvanced }">
          <ArrowDown />
        </el-icon>
      </div>

      <!-- 历史记录表格 -->
      <el-table
        v-loading="loading"
        :data="historyList"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="type" label="操作类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTag(row.type)">{{ getTypeName(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="操作模块" width="180" />
        <el-table-column prop="description" label="操作描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" label="操作IP" width="140" />
        <el-table-column prop="duration" label="耗时" width="100">
          <template #default="{ row }">
            <span :class="getDurationClass(row.duration)">{{ row.duration }}ms</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusName(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">
              <el-icon><View /></el-icon> 详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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

    <!-- 历史记录详情抽屉 -->
    <el-drawer
      v-model="detailDrawer"
      :title="detailData.description || '历史记录详情'"
      size="800px"
      :destroy-on-close="true"
    >
      <div class="detail-container">
        <!-- 基本信息 -->
        <el-descriptions :column="2" border>
          <el-descriptions-item label="操作类型">
            <el-tag :type="getTypeTag(detailData.type)">
              {{ getTypeName(detailData.type) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作状态">
            <el-tag :type="getStatusType(detailData.status)">
              {{ getStatusName(detailData.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="操作模块">
            {{ detailData.module }}
          </el-descriptions-item>
          <el-descriptions-item label="操作耗时">
            <span :class="getDurationClass(detailData.duration)">
              {{ detailData.duration }}ms
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="操作IP">
            {{ detailData.ip }}
          </el-descriptions-item>
          <el-descriptions-item label="操作时间">
            {{ detailData.createTime }}
          </el-descriptions-item>
          <el-descriptions-item label="操作描述" :span="2">
            {{ detailData.description }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 请求信息 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>请求信息</span>
              <el-button-group>
                <el-button type="primary" link @click="copyRequestInfo">
                  <el-icon><CopyDocument /></el-icon> 复制
                </el-button>
                <el-button type="primary" link @click="formatRequestInfo">
                  <el-icon><Operation /></el-icon> 格式化
                </el-button>
              </el-button-group>
            </div>
          </template>
          <el-tabs v-model="requestActiveTab">
            <el-tab-pane label="请求头" name="headers">
              <el-descriptions :column="1" border>
                <el-descriptions-item
                  v-for="(value, key) in detailData.requestHeaders"
                  :key="key"
                  :label="key"
                >
                  {{ value }}
                </el-descriptions-item>
              </el-descriptions>
            </el-tab-pane>
            <el-tab-pane label="请求参数" name="params">
              <pre class="request-params">{{ formatJson(detailData.requestParams) }}</pre>
            </el-tab-pane>
          </el-tabs>
        </el-card>

        <!-- 响应信息 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>响应信息</span>
              <el-tag :type="detailData.responseCode === 0 ? 'success' : 'danger'">
                {{ detailData.responseCode }}
              </el-tag>
            </div>
          </template>
          <pre class="response-data" :class="{ 'response-error': detailData.responseCode !== 0 }">
            {{ formatJson(detailData.responseData) }}
          </pre>
        </el-card>

        <!-- 异常信息 -->
        <el-card v-if="detailData.error" class="detail-card error-card">
          <template #header>
            <div class="card-header">
              <span>异常信息</span>
              <el-button type="primary" link @click="copyErrorStack">
                <el-icon><CopyDocument /></el-icon> 复制堆栈
              </el-button>
            </div>
          </template>
          <div class="error-message">{{ detailData.error.message }}</div>
          <pre class="error-stack">{{ detailData.error.stack }}</pre>
        </el-card>

        <!-- 操作轨迹 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>操作轨迹</span>
              <el-button type="primary" link @click="replayOperation">
                <el-icon><VideoPlay /></el-icon> 重放操作
              </el-button>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item
              v-for="(activity, index) in detailData.activities"
              :key="index"
              :type="activity.status === 'SUCCESS' ? 'success' : 'danger'"
              :timestamp="activity.timestamp"
            >
              {{ activity.content }}
              <div v-if="activity.detail" class="activity-detail">
                {{ activity.detail }}
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </div>
    </el-drawer>

    <!-- 数据分析抽屉 -->
    <el-drawer
      v-model="analysisDrawer"
      title="历史记录分析"
      size="900px"
      :destroy-on-close="true"
    >
      <div class="analysis-container">
        <!-- 时间范围选择 -->
        <div class="analysis-header">
          <el-radio-group v-model="analysisTimeRange" @change="handleAnalysisTimeChange">
            <el-radio-button label="today">今日</el-radio-button>
            <el-radio-button label="week">本周</el-radio-button>
            <el-radio-button label="month">本月</el-radio-button>
          </el-radio-group>
          <el-button-group>
            <el-tooltip content="导出分析报告" placement="top">
              <el-button type="primary" @click="exportAnalysisReport">
                <el-icon><Download /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="刷新数据" placement="top">
              <el-button @click="refreshAnalysis">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </el-tooltip>
          </el-button-group>
        </div>

        <!-- 核心指标 -->
        <el-row :gutter="20" class="analysis-stats">
          <el-col :span="8" v-for="(stat, index) in analysisStats" :key="index">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-content">
                <div class="stat-info">
                  <div class="stat-label">{{ stat.label }}</div>
                  <div class="stat-value">{{ stat.value }}</div>
                  <div class="stat-trend" :class="stat.trend > 0 ? 'up' : 'down'">
                    较上期{{ stat.trend > 0 ? '上升' : '下降' }} {{ Math.abs(stat.trend) }}%
                  </div>
                </div>
                <div class="stat-chart">
                  <div class="mini-chart" ref="el => miniChartRefs[index] = el"></div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 趋势分析 -->
        <el-card class="analysis-card">
          <template #header>
            <div class="card-header">
              <span>访问趋势分析</span>
              <el-radio-group v-model="trendMetric" size="small">
                <el-radio-button label="count">访问次数</el-radio-button>
                <el-radio-button label="duration">平均耗时</el-radio-button>
                <el-radio-button label="error">异常数量</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="trend-chart">
            <div ref="trendChartRef" style="width: 100%; height: 100%;"></div>
          </div>
        </el-card>

        <!-- 分布分析 -->
        <el-row :gutter="20" class="distribution-section">
          <el-col :span="12">
            <el-card class="analysis-card">
              <template #header>
                <div class="card-header">
                  <span>操作类型分布</span>
                  <el-tooltip content="查看详情" placement="top">
                    <el-button type="primary" link>
                      <el-icon><View /></el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
              </template>
              <div class="distribution-chart">
                <div ref="typeChartRef" style="width: 100%; height: 100%;"></div>
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card class="analysis-card">
              <template #header>
                <div class="card-header">
                  <span>时间段分布</span>
                  <el-tooltip content="查看详情" placement="top">
                    <el-button type="primary" link>
                      <el-icon><View /></el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
              </template>
              <div class="distribution-chart">
                <div ref="timeChartRef" style="width: 100%; height: 100%;"></div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- TOP 分析 -->
        <el-card class="analysis-card">
          <template #header>
            <div class="card-header">
              <span>TOP 分析</span>
              <el-radio-group v-model="topMetric" size="small">
                <el-radio-button label="duration">耗时最长</el-radio-button>
                <el-radio-button label="error">异常最多</el-radio-button>
                <el-radio-button label="module">访问最频繁</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <el-table :data="topList" style="width: 100%">
            <el-table-column type="index" label="排名" width="80" />
            <el-table-column prop="name" label="名称" min-width="200" />
            <el-table-column prop="value" label="数值" width="120">
              <template #default="{ row }">
                <span :class="getValueClass(row)">{{ row.value }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="percent" label="占比" width="180">
              <template #default="{ row }">
                <el-progress
                  :percentage="row.percent"
                  :color="getProgressColor(row)"
                  :stroke-width="8"
                />
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, watch } from 'vue'
import {
  View, Download, Delete, Refresh,
  Search, ArrowDown, ArrowUp, Timer,
  Connection, Warning, DataLine,
  CopyDocument, Operation, VideoPlay
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { debounce } from 'lodash-es'

// 统计数据
const historyStats = ref([
  {
    label: '总访问量',
    value: '12,356',
    icon: 'Connection',
    type: 'primary',
    trend: 15.6
  },
  {
    label: '平均耗时',
    value: '128ms',
    icon: 'Timer',
    type: 'success',
    trend: -8.3
  },
  {
    label: '异常次数',
    value: '12',
    icon: 'Warning',
    type: 'danger',
    trend: -25.8
  },
  {
    label: '数据操作',
    value: '3,256',
    icon: 'DataLine',
    type: 'warning',
    trend: 12.3
  }
])

// 搜索相关
const showAdvanced = ref(false)
const searchForm = reactive({
  type: '',
  status: '',
  timeRange: [],
  module: null,
  ip: '',
  minDuration: null,
  maxDuration: null
})

// 模块选项
const moduleOptions = [
  {
    value: 'system',
    label: '系统管理',
    children: [
      { value: 'user', label: '用户管理' },
      { value: 'role', label: '角色管理' },
      { value: 'menu', label: '菜单管理' }
    ]
  },
  {
    value: 'data',
    label: '数据管理',
    children: [
      { value: 'analysis', label: '数据分析' },
      { value: 'report', label: '数据报表' }
    ]
  }
]

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
const historyList = ref([
  {
    id: 1,
    type: 'PAGE_VIEW',
    module: '系统管理/用户管理',
    description: '访问用户管理页面',
    ip: '192.168.1.100',
    duration: 56,
    status: 'SUCCESS',
    createTime: '2024-01-15 10:00:00'
  },
  {
    id: 2,
    type: 'DATA_QUERY',
    module: '数据管理/数据分析',
    description: '执行数据分析任务：月度销售分析',
    ip: '192.168.1.100',
    duration: 1205,
    status: 'FAILED',
    createTime: '2024-01-15 09:30:00'
  }
])

// 分页配置
const page = ref({
  current: 1,
  size: 10
})
const total = ref(100)

// 获取类型标签样式
const getTypeTag = (type) => {
  const types = {
    'PAGE_VIEW': '',
    'DATA_QUERY': 'success',
    'DATA_EXPORT': 'warning',
    'SYSTEM_CONFIG': 'info'
  }
  return types[type] || ''
}

// 获取类型名称
const getTypeName = (type) => {
  const types = {
    'PAGE_VIEW': '页面访问',
    'DATA_QUERY': '数据查询',
    'DATA_EXPORT': '数据导出',
    'SYSTEM_CONFIG': '系统设置'
  }
  return types[type] || type
}

// 获取耗时样式
const getDurationClass = (duration) => {
  if (duration < 100) return 'duration-normal'
  if (duration < 1000) return 'duration-warning'
  return 'duration-danger'
}

// 获取状态类型
const getStatusType = (status) => {
  const types = {
    'SUCCESS': 'success',
    'FAILED': 'warning',
    'ERROR': 'danger'
  }
  return types[status] || 'info'
}

// 获取状态名称
const getStatusName = (status) => {
  const names = {
    'SUCCESS': '成功',
    'FAILED': '失败',
    'ERROR': '异常'
  }
  return names[status] || status
}

// 搜索方法
const handleSearch = () => {
  page.value.current = 1
  fetchList()
}

const resetSearch = () => {
  searchForm.type = ''
  searchForm.status = ''
  searchForm.timeRange = []
  searchForm.module = null
  searchForm.ip = ''
  searchForm.minDuration = null
  searchForm.maxDuration = null
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

// 查看详情
const handleView = (row) => {
  detailData.value = {
    ...row,
    requestHeaders: {
      'User-Agent': 'Mozilla/5.0',
      'Accept': 'application/json'
    },
    requestParams: {
      pageSize: 10,
      current: 1
    },
    responseCode: 200,
    responseData: {
      code: 200,
      message: '操作成功',
      data: null
    },
    activities: [
      {
        timestamp: row.createTime,
        content: '发起请求',
        status: 'SUCCESS'
      },
      {
        timestamp: row.createTime,
        content: '权限校验',
        status: 'SUCCESS'
      },
      {
        timestamp: row.createTime,
        content: '处理完成',
        status: row.status
      }
    ]
  }
  detailDrawer.value = true
}

// 导出记录
const handleExport = () => {
  ElMessage.success('正在导出历史记录...')
}

// 清空记录
const handleClear = () => {
  ElMessageBox.confirm(
    '确定要清空所有历史记录吗？此操作不可恢复！',
    '清空确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success('历史记录已清空')
  })
}

// 刷新列表
const refreshList = () => {
  fetchList()
}

// 表格选择
const handleSelectionChange = (selection) => {
  console.log('Selected:', selection)
}

// 详情相关
const detailDrawer = ref(false)
const detailData = ref({
  type: 'PAGE_VIEW',
  status: 'SUCCESS',
  module: '系统管理/用户管理',
  description: '访问用户管理页面',
  ip: '192.168.1.100',
  duration: 56,
  createTime: '2024-01-15 10:00:00',
  requestHeaders: {
    'User-Agent': 'Mozilla/5.0',
    'Accept': 'application/json'
  },
  requestParams: {
    pageSize: 10,
    current: 1
  },
  responseCode: 200,
  responseData: {
    code: 200,
    message: '操作成功',
    data: null
  },
  activities: [
    {
      timestamp: '2024-01-15 10:00:00',
      content: '发起请求',
      status: 'SUCCESS'
    },
    {
      timestamp: '2024-01-15 10:00:00',
      content: '权限校验',
      status: 'SUCCESS'
    },
    {
      timestamp: '2024-01-15 10:00:00',
      content: '处理完成',
      status: 'SUCCESS'
    }
  ]
})
const requestActiveTab = ref('headers')

// 分析相关
const analysisDrawer = ref(false)
const analysisTimeRange = ref('week')
const trendMetric = ref('count')
const topMetric = ref('duration')

// 分析统计数据
const analysisStats = ref([
  {
    label: '访问总量',
    value: '1,234',
    trend: 12.5
  },
  {
    label: '平均耗时',
    value: '128ms',
    trend: -8.3
  },
  {
    label: '异常比例',
    value: '0.5%',
    trend: -25.8
  }
])

// TOP 数据
const topList = ref([
  {
    name: '用户管理页面',
    value: '1,234',
    percent: 85
  },
  {
    name: '角色管理页面',
    value: '956',
    percent: 65
  },
  {
    name: '菜单管理页面',
    value: '865',
    percent: 45
  }
])

// 格式化 JSON
const formatJson = (json) => {
  try {
    return JSON.stringify(json, null, 2)
  } catch (e) {
    return json
  }
}

// 复制请求信息
const copyRequestInfo = () => {
  // TODO: 实现复制功能
  ElMessage.success('已复制到剪贴板')
}

// 格式化请求信息
const formatRequestInfo = () => {
  // TODO: 实现格式化功能
  ElMessage.success('格式化完成')
}

// 复制错误堆栈
const copyErrorStack = () => {
  // TODO: 实现复制功能
  ElMessage.success('已复制到剪贴板')
}

// 重放操作
const replayOperation = () => {
  ElMessageBox.confirm(
    '确定要重新执行该操作吗？',
    '重放确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success('操作重放成功')
  })
}

// 处理分析时间范围变化
const handleAnalysisTimeChange = (value) => {
  // TODO: 更新分析数据
  console.log('Analysis time range changed:', value)
}

// 导出分析报告
const exportAnalysisReport = () => {
  ElMessage.success('正在导出分析报告...')
}

// 刷新分析数据
const refreshAnalysis = () => {
  // TODO: 刷新分析数据
  ElMessage.success('数据已更新')
}

// 获取数值样式
const getValueClass = (row) => {
  if (topMetric.value === 'duration' && row.value > 1000) return 'duration-danger'
  if (topMetric.value === 'error' && row.value > 10) return 'duration-danger'
  return ''
}

// 获取进度条颜色
const getProgressColor = (row) => {
  if (row.percent >= 80) return '#f56c6c'
  if (row.percent >= 50) return '#e6a23c'
  return '#67c23a'
}

// 图表实例
const trendChartRef = ref(null)
const typeChartRef = ref(null)
const timeChartRef = ref(null)
const miniChartRefs = ref([])
let trendChart = null
let typeChart = null
let timeChart = null
let miniCharts = []

// 初始化趋势图表
const initTrendChart = () => {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#6a7985'
        }
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
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '访问次数',
        type: 'line',
        smooth: true,
        areaStyle: {
          opacity: 0.1
        },
        emphasis: {
          focus: 'series'
        },
        data: [120, 132, 101, 134, 90, 230, 210]
      },
      {
        name: '异常次数',
        type: 'line',
        smooth: true,
        areaStyle: {
          opacity: 0.1
        },
        emphasis: {
          focus: 'series'
        },
        data: [2, 5, 1, 3, 2, 8, 4]
      }
    ]
  }
  
  trendChart.setOption(option)
}

// 初始化类型分布图
const initTypeChart = () => {
  if (!typeChartRef.value) return
  typeChart = echarts.init(typeChartRef.value)
  
  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '操作类型',
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
          { value: 1048, name: '页面访问' },
          { value: 735, name: '数据查询' },
          { value: 580, name: '数据导出' },
          { value: 484, name: '系统设置' }
        ]
      }
    ]
  }
  
  typeChart.setOption(option)
}

// 初始化时间分布图
const initTimeChart = () => {
  if (!timeChartRef.value) return
  timeChart = echarts.init(timeChartRef.value)
  
  const hours = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '11',
    '12', '13', '14', '15', '16', '17', '18', '19', '20', '21', '22', '23']
  const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  
  const data = []
  for (let i = 0; i < 7; i++) {
    for (let j = 0; j < 24; j++) {
      data.push([j, i, Math.round(Math.random() * 100)])
    }
  }
  
  const option = {
    tooltip: {
      position: 'top',
      formatter: (params) => {
        return `${days[params.value[1]]} ${params.value[0]}:00<br/>访问量：${params.value[2]}`
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
      data: hours,
      splitArea: {
        show: true
      }
    },
    yAxis: {
      type: 'category',
      data: days,
      splitArea: {
        show: true
      }
    },
    visualMap: {
      min: 0,
      max: 100,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      bottom: '0%',
      inRange: {
        color: ['#ebedf0', '#c6e48b', '#7bc96f', '#239a3b', '#196127']
      }
    },
    series: [{
      name: '访问量',
      type: 'heatmap',
      data: data,
      label: {
        show: false
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }]
  }
  
  timeChart.setOption(option)
}

// 初始化迷你图表
const initMiniCharts = () => {
  miniChartRefs.value.forEach((ref, index) => {
    if (!ref) return
    const chart = echarts.init(ref)
    const data = Array.from({ length: 7 }, () => Math.floor(Math.random() * 100))
    
    const option = {
      tooltip: {
        trigger: 'axis'
      },
      grid: {
        left: 0,
        right: 0,
        top: 0,
        bottom: 0
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
        data,
        type: 'line',
        smooth: true,
        symbol: 'none',
        areaStyle: {
          opacity: 0.2
        },
        lineStyle: {
          width: 1
        }
      }]
    }
    
    chart.setOption(option)
    miniCharts.push(chart)
  })
}

// 更新趋势图表数据
const updateTrendChart = () => {
  if (!trendChart) return
  
  const metrics = {
    count: {
      name: '访问次数',
      data: [120, 132, 101, 134, 90, 230, 210]
    },
    duration: {
      name: '平均耗时',
      data: [45, 82, 91, 74, 110, 130, 120]
    },
    error: {
      name: '异常数量',
      data: [2, 5, 1, 3, 2, 8, 4]
    }
  }
  
  const option = {
    series: [{
      name: metrics[trendMetric.value].name,
      data: metrics[trendMetric.value].data
    }]
  }
  
  trendChart.setOption(option)
}

// 监听图表容器大小变化
const handleResize = debounce(() => {
  trendChart?.resize()
  typeChart?.resize()
  timeChart?.resize()
  miniCharts.forEach(chart => chart?.resize())
}, 100)

// 生命周期钩子
onMounted(() => {
  initTrendChart()
  initTypeChart()
  initTimeChart()
  initMiniCharts()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  typeChart?.dispose()
  timeChart?.dispose()
  miniCharts.forEach(chart => chart?.dispose())
})

// 监听趋势指标变化
watch(trendMetric, () => {
  updateTrendChart()
})

// 更新模板部分
const template = `
<!-- 在 stat-chart 部分替换 -->
<div class="stat-chart">
  <div class="mini-chart" ref="el => miniChartRefs[index] = el"></div>
</div>

<!-- 在趋势图表部分替换 -->
<div class="trend-chart">
  <div ref="trendChartRef" style="width: 100%; height: 100%;"></div>
</div>

<!-- 在分布图表部分替换 -->
<div class="distribution-chart">
  <div ref="typeChartRef" style="width: 100%; height: 100%;"></div>
</div>
<div class="distribution-chart">
  <div ref="timeChartRef" style="width: 100%; height: 100%;"></div>
</div>
`

// 更新样式部分
const style = `
/* 图表相关样式 */
.trend-chart,
.distribution-chart {
  background: transparent;
}

.mini-chart {
  background: transparent;
}
`
</script>

<style scoped>
.history-container {
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

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  margin-top: 4px;
}

.stat-trend.up {
  color: var(--el-color-success);
}

.stat-trend.down {
  color: var(--el-color-danger);
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
  margin-bottom: 16px;
  padding: 24px 16px 0;
  background: var(--el-bg-color-page);
  border-radius: 4px;
}

.search-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.search-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  transition: color 0.3s;
  margin-bottom: 16px;
}

.search-toggle:hover {
  color: var(--el-color-primary);
}

.toggle-icon {
  margin-left: 4px;
  transition: transform 0.3s;
}

.toggle-icon.is-active {
  transform: rotate(180deg);
}

.duration-separator {
  margin: 0 8px;
  color: var(--el-text-color-secondary);
}

.duration-unit {
  margin-left: 8px;
  color: var(--el-text-color-secondary);
}

.duration-normal {
  color: var(--el-color-success);
}

.duration-warning {
  color: var(--el-color-warning);
}

.duration-danger {
  color: var(--el-color-danger);
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

/* 详情样式 */
.detail-container {
  padding: 20px;
}

.detail-card {
  margin-top: 20px;
}

.request-params,
.response-data,
.error-stack {
  margin: 0;
  padding: 16px;
  background: var(--el-bg-color-page);
  border-radius: 4px;
  font-family: monospace;
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-all;
}

.response-error {
  color: var(--el-color-danger);
}

.error-card {
  border: 1px solid var(--el-color-danger);
}

.error-message {
  color: var(--el-color-danger);
  margin-bottom: 16px;
  font-weight: bold;
}

.activity-detail {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-top: 4px;
}

/* 分析样式 */
.analysis-container {
  padding: 20px;
}

.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.analysis-stats {
  margin-bottom: 20px;
}

.analysis-card {
  margin-bottom: 20px;
}

.trend-chart,
.distribution-chart {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-bg-color-page);
  border-radius: 4px;
}

.distribution-chart {
  height: 240px;
}

.stat-chart {
  width: 100px;
  height: 40px;
}

.mini-chart {
  width: 100%;
  height: 100%;
  background: var(--el-bg-color-page);
  border-radius: 4px;
}

.chart-placeholder {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
</style> 