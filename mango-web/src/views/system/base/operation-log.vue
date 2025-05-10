<template>
  <div class="operation-log-container">
    <!-- 日志概览 -->
    <el-row :gutter="20" class="overview-section">
      <el-col :span="6" v-for="(item, index) in logOverview" :key="index">
        <el-card 
          shadow="hover" 
          class="overview-card" 
          :class="item.type"
        >
          <div class="overview-content">
            <div class="overview-left">
              <div class="overview-title">{{ item.label }}</div>
              <div class="overview-value">{{ item.value }}</div>
              <div class="overview-trend">
                <span>较昨日</span>
                <span :class="item.trend">
                  {{ item.trend === 'up' ? '+' : '-' }}{{ item.rate }}%
                  <el-icon>
                    <component :is="item.trend === 'up' ? 'ArrowUpBold' : 'ArrowDownBold'" />
                  </el-icon>
                </span>
              </div>
            </div>
            <div class="overview-right">
              <el-icon :size="48">
                <component :is="item.icon" />
              </el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 日志统计图表 -->
    <el-row :gutter="20" class="chart-section">
      <el-col :span="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>操作日志趋势</span>
              <el-radio-group v-model="chartTimeRange" size="small">
                <el-radio-button label="week">近一周</el-radio-button>
                <el-radio-button label="month">近一月</el-radio-button>
                <el-radio-button label="year">近一年</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="chart-container" ref="trendChartRef"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>操作类型分布</span>
              <el-tooltip content="刷新数据" placement="top">
                <el-button :icon="Refresh" circle plain @click="refreshTypeChart" />
              </el-tooltip>
            </div>
          </template>
          <div class="chart-container" ref="typeChartRef"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 日志列表 -->
    <el-card class="log-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>操作日志</h3>
            <el-tag type="success" effect="dark">
              共 {{ total }} 条记录
            </el-tag>
          </div>
          <div class="header-right">
            <el-button type="danger" :icon="Delete" @click="handleBatchDelete" :disabled="!selectedLogs.length">
              批量删除
            </el-button>
            <el-button type="warning" :icon="Download" @click="handleExport">
              导出日志
            </el-button>
            <el-button :icon="Refresh" @click="refreshList">
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form
        ref="searchFormRef"
        :model="searchForm"
        class="search-form"
      >
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="操作人员" prop="operator">
              <el-input
                v-model="searchForm.operator"
                placeholder="请输入操作人员"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="操作类型" prop="type">
              <el-select v-model="searchForm.type" placeholder="请选择操作类型" clearable>
                <el-option label="查询" value="SELECT" />
                <el-option label="新增" value="INSERT" />
                <el-option label="修改" value="UPDATE" />
                <el-option label="删除" value="DELETE" />
                <el-option label="导出" value="EXPORT" />
                <el-option label="导入" value="IMPORT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="操作状态" prop="status">
              <el-select v-model="searchForm.status" placeholder="请选择操作状态" clearable>
                <el-option label="成功" value="1" />
                <el-option label="失败" value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="操作时间" prop="timeRange">
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
        </el-row>
        <el-row>
          <el-col :span="24" class="search-buttons">
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon> 搜索
            </el-button>
            <el-button @click="resetSearch">
              <el-icon><Refresh /></el-icon> 重置
            </el-button>
            <el-button type="primary" link @click="toggleAdvanced">
              {{ isAdvanced ? '收起' : '展开' }}高级搜索
              <el-icon>
                <component :is="isAdvanced ? 'ArrowUp' : 'ArrowDown'" />
              </el-icon>
            </el-button>
          </el-col>
        </el-row>

        <!-- 高级搜索 -->
        <el-collapse-transition>
          <div v-show="isAdvanced">
            <el-row :gutter="16" class="mt-4">
              <el-col :span="6">
                <el-form-item label="操作模块" prop="module">
                  <el-input
                    v-model="searchForm.module"
                    placeholder="请输入操作模块"
                    clearable
                  />
                </el-form-item>
              </el-col>
              <el-col :span="6">
                <el-form-item label="请求方式" prop="method">
                  <el-select v-model="searchForm.method" placeholder="请选择请求方式" clearable>
                    <el-option label="GET" value="GET" />
                    <el-option label="POST" value="POST" />
                    <el-option label="PUT" value="PUT" />
                    <el-option label="DELETE" value="DELETE" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="请求地址" prop="url">
                  <el-input
                    v-model="searchForm.url"
                    placeholder="请输入请求地址"
                    clearable
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </el-collapse-transition>
      </el-form>

      <!-- 日志表格 -->
      <el-table
        ref="logTable"
        v-loading="loading"
        :data="logList"
        style="width: 100%"
        :height="tableHeight"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="operator" label="操作人员" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="operator-cell">
              <el-avatar :size="24" :src="row.avatar">
                {{ row.operator.substring(0, 1).toUpperCase() }}
              </el-avatar>
              <span>{{ row.operator }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="操作类型" width="100">
          <template #default="{ row }">
            <el-tag :type="getOperationTypeTag(row.type)">
              {{ getOperationTypeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="module" label="操作模块" width="120" />
        <el-table-column prop="description" label="操作描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" label="操作IP" width="120" show-overflow-tooltip />
        <el-table-column prop="location" label="操作地点" width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="操作状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="duration" label="耗时" width="100" align="right">
          <template #default="{ row }">
            <span :class="getDurationClass(row.duration)">
              {{ row.duration }}ms
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="操作时间" width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-tooltip content="详情" placement="top">
                <el-button type="primary" link :icon="View" @click="handleView(row)" />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button type="danger" link :icon="Delete" @click="handleDelete(row)" />
              </el-tooltip>
            </el-button-group>
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

    <!-- 日志详情抽屉 -->
    <el-drawer
      v-model="detailDrawer"
      :title="'操作日志详情 - ' + currentLog?.id"
      size="800px"
      :destroy-on-close="true"
    >
      <div class="detail-container">
        <!-- 基本信息 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>基本信息</span>
              <el-tag :type="currentLog?.status === 1 ? 'success' : 'danger'">
                {{ currentLog?.status === 1 ? '执行成功' : '执行失败' }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="操作人员">
              <div class="operator-info">
                <el-avatar :size="32" :src="currentLog?.avatar">
                  {{ currentLog?.operator.substring(0, 1).toUpperCase() }}
                </el-avatar>
                <div class="operator-detail">
                  <div>{{ currentLog?.operator }}</div>
                  <div class="operator-role">{{ currentLog?.role || '系统管理员' }}</div>
                </div>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="登录信息">
              <div class="login-info">
                <div>
                  <el-icon><Monitor /></el-icon>
                  {{ currentLog?.browser || 'Chrome' }}
                </div>
                <div>
                  <el-icon><Platform /></el-icon>
                  {{ currentLog?.os || 'Windows 10' }}
                </div>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="操作类型">
              <el-tag :type="getOperationTypeTag(currentLog?.type)">
                {{ getOperationTypeLabel(currentLog?.type) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="操作模块">
              {{ currentLog?.module }}
            </el-descriptions-item>
            <el-descriptions-item label="操作描述">
              {{ currentLog?.description }}
            </el-descriptions-item>
            <el-descriptions-item label="操作时间">
              {{ currentLog?.createTime }}
            </el-descriptions-item>
            <el-descriptions-item label="操作耗时">
              <span :class="getDurationClass(currentLog?.duration)">
                {{ currentLog?.duration }}ms
              </span>
            </el-descriptions-item>
            <el-descriptions-item label="操作地点">
              <el-link type="primary" :underline="false" @click="handleViewMap">
                <el-icon><Location /></el-icon>
                {{ currentLog?.location }}
              </el-link>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 请求信息 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>请求信息</span>
              <el-tag type="info">{{ currentLog?.method || 'GET' }}</el-tag>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="请求地址">
              <div class="url-info">
                <el-tag size="small">{{ currentLog?.method || 'GET' }}</el-tag>
                <el-link type="primary" :underline="false">
                  {{ currentLog?.url || '/api/system/user/list' }}
                </el-link>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="请求参数">
              <div class="code-block">
                <div class="code-header">
                  <span>Request Params</span>
                  <el-button type="primary" link @click="copyCode(currentLog?.params)">
                    复制
                  </el-button>
                </div>
                <pre class="code-content">{{ formatJson(currentLog?.params) }}</pre>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="请求头">
              <div class="code-block">
                <div class="code-header">
                  <span>Request Headers</span>
                  <el-button type="primary" link @click="copyCode(currentLog?.headers)">
                    复制
                  </el-button>
                </div>
                <pre class="code-content">{{ formatJson(currentLog?.headers) }}</pre>
              </div>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 响应信息 -->
        <el-card class="detail-card">
          <template #header>
            <div class="card-header">
              <span>响应信息</span>
              <el-tag :type="currentLog?.status === 1 ? 'success' : 'danger'">
                {{ currentLog?.status === 1 ? '200 OK' : '500 Error' }}
              </el-tag>
            </div>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="响应结果">
              <div class="code-block">
                <div class="code-header">
                  <span>Response Data</span>
                  <el-button type="primary" link @click="copyCode(currentLog?.result)">
                    复制
                  </el-button>
                </div>
                <pre class="code-content" :class="{ 'error-content': currentLog?.status === 0 }">
                  {{ formatJson(currentLog?.result) }}
                </pre>
              </div>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 异常信息 -->
        <el-card v-if="currentLog?.status === 0" class="detail-card">
          <template #header>
            <div class="card-header">
              <span>异常信息</span>
              <el-button type="danger" link @click="handleCopyError">
                复制堆栈信息
              </el-button>
            </div>
          </template>
          <div class="error-stack">
            <pre>{{ currentLog?.errorInfo }}</pre>
          </div>
        </el-card>
      </div>
    </el-drawer>

    <!-- 地图弹窗 -->
    <el-dialog
      v-model="mapDialog"
      title="操作地点"
      width="800px"
      :destroy-on-close="true"
    >
      <div class="map-container" ref="mapRef"></div>
    </el-dialog>

    <!-- 数据分析抽屉 -->
    <el-drawer
      v-model="analysisDrawer"
      title="数据分析"
      size="900px"
      :destroy-on-close="true"
    >
      <div class="analysis-container">
        <!-- 时间范围选择 -->
        <el-card class="analysis-card">
          <template #header>
            <div class="card-header">
              <span>分析范围</span>
              <el-radio-group v-model="analysisTimeRange" size="small" @change="handleAnalysisTimeChange">
                <el-radio-button label="today">今日</el-radio-button>
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
                <el-radio-button label="custom">自定义</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <el-date-picker
            v-if="analysisTimeRange === 'custom'"
            v-model="customTimeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            :shortcuts="dateShortcuts"
            @change="handleCustomTimeChange"
          />
        </el-card>

        <!-- 分析指标 -->
        <el-row :gutter="20" class="analysis-metrics">
          <el-col :span="8" v-for="(metric, index) in analysisMetrics" :key="index">
            <el-card shadow="hover" class="metric-card">
              <div class="metric-content">
                <div class="metric-value">{{ metric.value }}</div>
                <div class="metric-label">{{ metric.label }}</div>
                <div class="metric-chart" ref="metricChartRefs"></div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 详细分析图表 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card class="analysis-chart-card">
              <template #header>
                <div class="card-header">
                  <span>操作类型分布趋势</span>
                  <el-tooltip content="查看详情" placement="top">
                    <el-button :icon="ZoomIn" circle plain @click="handleViewTypeDetail" />
                  </el-tooltip>
                </div>
              </template>
              <div class="analysis-chart" ref="typeAnalysisChartRef"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card class="analysis-chart-card">
              <template #header>
                <div class="card-header">
                  <span>响应时间分布</span>
                  <el-tooltip content="查看详情" placement="top">
                    <el-button :icon="ZoomIn" circle plain @click="handleViewDurationDetail" />
                  </el-tooltip>
                </div>
              </template>
              <div class="analysis-chart" ref="durationAnalysisChartRef"></div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 用户行为分析 -->
        <el-card class="analysis-card">
          <template #header>
            <div class="card-header">
              <span>用户行为分析</span>
              <el-button-group>
                <el-button :type="behaviorView === 'chart' ? 'primary' : ''" @click="behaviorView = 'chart'">
                  图表视图
                </el-button>
                <el-button :type="behaviorView === 'table' ? 'primary' : ''" @click="behaviorView = 'table'">
                  表格视图
                </el-button>
              </el-button-group>
            </div>
          </template>
          <div v-if="behaviorView === 'chart'" class="behavior-chart" ref="behaviorChartRef"></div>
          <el-table
            v-else
            :data="behaviorData"
            style="width: 100%"
            :max-height="400"
          >
            <el-table-column prop="user" label="用户" />
            <el-table-column prop="operations" label="操作次数" sortable />
            <el-table-column prop="avgDuration" label="平均响应时间" sortable>
              <template #default="{ row }">
                <span :class="getDurationClass(row.avgDuration)">{{ row.avgDuration }}ms</span>
              </template>
            </el-table-column>
            <el-table-column prop="successRate" label="成功率" sortable>
              <template #default="{ row }">
                <el-progress
                  :percentage="row.successRate"
                  :status="row.successRate >= 90 ? 'success' : row.successRate >= 70 ? 'warning' : 'exception'"
                />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button type="primary" link @click="handleViewUserDetail(row)">
                  详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </el-drawer>

    <!-- 导出配置抽屉 -->
    <el-drawer
      v-model="exportDrawer"
      title="导出配置"
      size="500px"
      :destroy-on-close="true"
    >
      <div class="export-container">
        <el-form ref="exportFormRef" :model="exportForm" label-width="100px">
          <!-- 导出范围 -->
          <el-form-item label="导出范围">
            <el-radio-group v-model="exportForm.scope">
              <el-radio label="all">全部数据</el-radio>
              <el-radio label="filtered">筛选数据</el-radio>
              <el-radio label="selected">选中数据</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- 时间范围 -->
          <el-form-item label="时间范围" v-if="exportForm.scope === 'all'">
            <el-date-picker
              v-model="exportForm.timeRange"
              type="datetimerange"
              range-separator="至"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              value-format="YYYY-MM-DD HH:mm:ss"
              :shortcuts="dateShortcuts"
            />
          </el-form-item>

          <!-- 导出字段 -->
          <el-form-item label="导出字段">
            <el-checkbox-group v-model="exportForm.fields">
              <el-checkbox label="operator">操作人员</el-checkbox>
              <el-checkbox label="type">操作类型</el-checkbox>
              <el-checkbox label="module">操作模块</el-checkbox>
              <el-checkbox label="description">操作描述</el-checkbox>
              <el-checkbox label="ip">操作IP</el-checkbox>
              <el-checkbox label="location">操作地点</el-checkbox>
              <el-checkbox label="status">操作状态</el-checkbox>
              <el-checkbox label="duration">响应时间</el-checkbox>
              <el-checkbox label="createTime">操作时间</el-checkbox>
            </el-checkbox-group>
          </el-form-item>

          <!-- 文件格式 -->
          <el-form-item label="文件格式">
            <el-radio-group v-model="exportForm.format">
              <el-radio-button label="excel">Excel</el-radio-button>
              <el-radio-button label="csv">CSV</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <!-- 高级选项 -->
          <el-collapse>
            <el-collapse-item title="高级选项" name="advanced">
              <el-form-item label="文件名">
                <el-input v-model="exportForm.filename" placeholder="请输入文件名" />
              </el-form-item>
              <el-form-item label="Sheet名">
                <el-input v-model="exportForm.sheetName" placeholder="请输入Sheet名" />
              </el-form-item>
              <el-form-item label="压缩选项">
                <el-switch
                  v-model="exportForm.compress"
                  active-text="启用压缩"
                  inactive-text="不压缩"
                />
              </el-form-item>
            </el-collapse-item>
          </el-collapse>
        </el-form>

        <!-- 导出按钮 -->
        <div class="export-footer">
          <el-button @click="exportDrawer = false">取消</el-button>
          <el-button type="primary" @click="handleExportConfirm" :loading="exporting">
            {{ exporting ? '导出中...' : '确认导出' }}
          </el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import {
  Delete, Download, Refresh, Search, View,
  ArrowUp, ArrowDown, ArrowUpBold, ArrowDownBold,
  Histogram, PieChart, Timer, Warning,
  Monitor, Platform, Location, ZoomIn
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

// 日志概览数据
const logOverview = ref([
  {
    label: '今日操作总数',
    value: '1,234',
    type: 'primary',
    icon: 'Histogram',
    trend: 'up',
    rate: 15.4
  },
  {
    label: '异常操作数',
    value: '12',
    type: 'danger',
    icon: 'Warning',
    trend: 'down',
    rate: 8.2
  },
  {
    label: '平均响应时间',
    value: '128ms',
    type: 'success',
    icon: 'Timer',
    trend: 'down',
    rate: 12.5
  },
  {
    label: '操作类型数',
    value: '8',
    type: 'warning',
    icon: 'PieChart',
    trend: 'up',
    rate: 5.8
  }
])

// 图表相关
const chartTimeRange = ref('week')
const trendChartRef = ref(null)
const typeChartRef = ref(null)
let trendChart = null
let typeChart = null

// 搜索相关
const isAdvanced = ref(false)
const searchFormRef = ref(null)
const searchForm = ref({
  operator: '',
  type: '',
  status: '',
  timeRange: [],
  module: '',
  method: '',
  url: ''
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

// 表格数据
const loading = ref(false)
const logTable = ref(null)
const logList = ref([
  {
    id: 1,
    operator: 'admin',
    avatar: '',
    type: 'SELECT',
    module: '用户管理',
    description: '查询用户列表',
    ip: '192.168.1.1',
    location: '广东省深圳市',
    status: 1,
    duration: 56,
    createTime: '2024-01-15 10:00:00'
  },
  {
    id: 2,
    operator: 'test',
    avatar: '',
    type: 'UPDATE',
    module: '角色管理',
    description: '修改角色信息',
    ip: '192.168.1.2',
    location: '广东省广州市',
    status: 0,
    duration: 1234,
    createTime: '2024-01-15 09:30:00'
  }
])

// 分页配置
const page = ref({
  current: 1,
  size: 10
})
const total = ref(100)
const tableHeight = ref('calc(100vh - 520px)')

// 批量选择
const selectedLogs = ref([])

// 操作类型样式
const getOperationTypeTag = (type) => {
  const types = {
    'SELECT': 'info',
    'INSERT': 'success',
    'UPDATE': 'warning',
    'DELETE': 'danger',
    'EXPORT': 'warning',
    'IMPORT': 'success'
  }
  return types[type] || 'info'
}

const getOperationTypeLabel = (type) => {
  const labels = {
    'SELECT': '查询',
    'INSERT': '新增',
    'UPDATE': '修改',
    'DELETE': '删除',
    'EXPORT': '导出',
    'IMPORT': '导入'
  }
  return labels[type] || type
}

// 响应时间样式
const getDurationClass = (duration) => {
  if (duration <= 100) return 'duration-normal'
  if (duration <= 500) return 'duration-slow'
  return 'duration-timeout'
}

// 搜索方法
const handleSearch = () => {
  page.value.current = 1
  fetchLogList()
}

const resetSearch = () => {
  searchFormRef.value?.resetFields()
  handleSearch()
}

const toggleAdvanced = () => {
  isAdvanced.value = !isAdvanced.value
}

// 表格操作方法
const handleView = (row) => {
  currentLog.value = {
    ...row,
    params: {
      pageNum: 1,
      pageSize: 10,
      username: 'admin'
    },
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer xxxxxx'
    },
    result: {
      code: row.status === 1 ? 200 : 500,
      msg: row.status === 1 ? '操作成功' : '操作失败',
      data: row.status === 1 ? { total: 100, list: [] } : null
    },
    errorInfo: row.status === 0 ? `
java.lang.RuntimeException: 操作失败
    at com.xxx.service.impl.UserServiceImpl.list(UserServiceImpl.java:42)
    at com.xxx.controller.UserController.list(UserController.java:35)
    at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
    at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
    ` : null
  }
  detailDrawer.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除该条操作日志吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success('删除成功')
  })
}

const handleBatchDelete = () => {
  if (!selectedLogs.value.length) {
    ElMessage.warning('请选择要删除的日志')
    return
  }
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedLogs.value.length} 条日志吗？`,
    '批量删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success('批量删除成功')
  })
}

// 分页方法
const handleSizeChange = (val) => {
  page.value.size = val
  fetchLogList()
}

const handleCurrentChange = (val) => {
  page.value.current = val
  fetchLogList()
}

const handleSelectionChange = (selection) => {
  selectedLogs.value = selection
}

// 导出方法
const handleExport = () => {
  exportDrawer.value = true
}

// 刷新列表
const refreshList = () => {
  fetchLogList()
}

// 获取日志列表
const fetchLogList = () => {
  loading.value = true
  // 模拟接口请求
  setTimeout(() => {
    loading.value = false
  }, 500)
}

// 初始化图表
const initCharts = () => {
  // 趋势图表
  trendChart = echarts.init(trendChartRef.value)
  const trendOption = {
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
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '操作次数',
        type: 'line',
        smooth: true,
        data: [120, 132, 101, 134, 90, 230, 210],
        itemStyle: {
          color: '#409EFF'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            {
              offset: 0,
              color: 'rgba(64,158,255,0.3)'
            },
            {
              offset: 1,
              color: 'rgba(64,158,255,0.1)'
            }
          ])
        }
      },
      {
        name: '异常次数',
        type: 'line',
        smooth: true,
        data: [12, 13, 10, 13, 9, 23, 21],
        itemStyle: {
          color: '#F56C6C'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            {
              offset: 0,
              color: 'rgba(245,108,108,0.3)'
            },
            {
              offset: 1,
              color: 'rgba(245,108,108,0.1)'
            }
          ])
        }
      }
    ]
  }
  trendChart.setOption(trendOption)

  // 类型分布图表
  typeChart = echarts.init(typeChartRef.value)
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
          { value: 1048, name: '查询' },
          { value: 735, name: '新增' },
          { value: 580, name: '修改' },
          { value: 484, name: '删除' },
          { value: 300, name: '导出' }
        ]
      }
    ]
  }
  typeChart.setOption(typeOption)
}

// 刷新图表
const refreshTypeChart = () => {
  typeChart?.setOption({
    series: [
      {
        data: [
          { value: Math.round(Math.random() * 1000), name: '查询' },
          { value: Math.round(Math.random() * 1000), name: '新增' },
          { value: Math.round(Math.random() * 1000), name: '修改' },
          { value: Math.round(Math.random() * 1000), name: '删除' },
          { value: Math.round(Math.random() * 1000), name: '导出' }
        ]
      }
    ]
  })
}

// 监听图表容器大小变化
window.addEventListener('resize', () => {
  trendChart?.resize()
  typeChart?.resize()
})

// 生命周期钩子
onMounted(() => {
  fetchLogList()
  initCharts()
})

// 日志详情相关
const detailDrawer = ref(false)
const currentLog = ref(null)
const mapDialog = ref(false)
const mapRef = ref(null)

// 格式化 JSON
const formatJson = (json) => {
  if (!json) return ''
  try {
    return JSON.stringify(json, null, 2)
  } catch (e) {
    return json
  }
}

// 复制代码
const copyCode = (code) => {
  if (!code) return
  const text = typeof code === 'object' ? JSON.stringify(code, null, 2) : code
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('复制成功')
  })
}

// 复制错误信息
const handleCopyError = () => {
  if (!currentLog.value?.errorInfo) return
  navigator.clipboard.writeText(currentLog.value.errorInfo).then(() => {
    ElMessage.success('复制成功')
  })
}

// 查看地图
const handleViewMap = () => {
  if (!currentLog.value?.location) return
  mapDialog.value = true
  // 这里可以集成地图 SDK
  setTimeout(() => {
    const map = new echarts.init(mapRef.value)
    map.setOption({
      geo: {
        map: 'china',
        roam: true,
        label: {
          show: true
        },
        itemStyle: {
          areaColor: '#e0e0e0',
          borderColor: '#999'
        },
        emphasis: {
          itemStyle: {
            areaColor: '#b8e7fd'
          }
        }
      },
      series: [
        {
          type: 'effectScatter',
          coordinateSystem: 'geo',
          data: [
            {
              name: currentLog.value.location,
              value: [113.23, 23.16, 100] // 示例坐标
            }
          ],
          symbolSize: 15,
          label: {
            show: true,
            position: 'right',
            formatter: '{b}'
          },
          itemStyle: {
            color: '#409EFF'
          }
        }
      ]
    })
  })
}

// 数据分析相关
const analysisDrawer = ref(false)
const analysisTimeRange = ref('week')
const customTimeRange = ref([])
const behaviorView = ref('chart')

// 分析指标数据
const analysisMetrics = ref([
  {
    label: '平均响应时间',
    value: '128ms',
    trend: 'down',
    data: [120, 132, 101, 134, 90, 230, 210]
  },
  {
    label: '异常率',
    value: '1.2%',
    trend: 'down',
    data: [1.5, 1.3, 1.1, 1.4, 0.9, 2.3, 2.1]
  },
  {
    label: '操作频率',
    value: '267/小时',
    trend: 'up',
    data: [220, 232, 201, 234, 190, 330, 310]
  }
])

// 用户行为数据
const behaviorData = ref([
  {
    user: 'admin',
    operations: 1234,
    avgDuration: 128,
    successRate: 99.8
  },
  {
    user: 'test',
    operations: 567,
    avgDuration: 156,
    successRate: 98.5
  }
])

// 导出配置相关
const exportDrawer = ref(false)
const exportFormRef = ref(null)
const exporting = ref(false)
const exportForm = ref({
  scope: 'filtered',
  timeRange: [],
  fields: ['operator', 'type', 'module', 'description', 'status', 'createTime'],
  format: 'excel',
  filename: '操作日志',
  sheetName: '操作日志',
  compress: false
})

// 分析时间范围变更
const handleAnalysisTimeChange = (value) => {
  if (value !== 'custom') {
    // 根据选择的时间范围更新图表数据
    updateAnalysisCharts()
  }
}

const handleCustomTimeChange = () => {
  // 根据自定义时间范围更新图表数据
  updateAnalysisCharts()
}

// 更新分析图表
const updateAnalysisCharts = () => {
  // 更新各个图表的数据
  initTypeAnalysisChart()
  initDurationAnalysisChart()
  initBehaviorChart()
}

// 查看详情
const handleViewTypeDetail = () => {
  // 打开操作类型详情弹窗
}

const handleViewDurationDetail = () => {
  // 打开响应时间详情弹窗
}

const handleViewUserDetail = (row) => {
  // 打开用户详情弹窗
}

// 初始化分析图表
const initTypeAnalysisChart = () => {
  const chart = echarts.init(typeAnalysisChartRef.value)
  // 设置图表配置...
}

const initDurationAnalysisChart = () => {
  const chart = echarts.init(durationAnalysisChartRef.value)
  // 设置图表配置...
}

const initBehaviorChart = () => {
  if (behaviorView.value !== 'chart') return
  const chart = echarts.init(behaviorChartRef.value)
  // 设置图表配置...
}

// 确认导出
const handleExportConfirm = async () => {
  exporting.value = true
  try {
    // 调用导出接口
    await new Promise(resolve => setTimeout(resolve, 2000))
    ElMessage.success('导出成功')
    exportDrawer.value = false
  } catch (error) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.operation-log-container {
  padding: 20px;
}

.overview-section {
  margin-bottom: 20px;
}

.overview-card {
  transition: all 0.3s;
}

.overview-card:hover {
  transform: translateY(-5px);
}

.overview-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.overview-left {
  flex: 1;
}

.overview-title {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.overview-value {
  font-size: 24px;
  font-weight: bold;
  margin: 8px 0;
}

.overview-trend {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.overview-trend .up {
  color: #67c23a;
}

.overview-trend .down {
  color: #f56c6c;
}

.overview-right {
  padding: 16px;
  border-radius: 8px;
  background: rgba(var(--el-color-primary-rgb), 0.1);
}

.chart-section {
  margin-bottom: 20px;
}

.chart-card {
  height: 400px;
}

.chart-container {
  height: 100%;
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

.header-right {
  display: flex;
  gap: 12px;
}

.search-form {
  margin-bottom: 20px;
  padding: 20px;
  background-color: var(--el-bg-color-page);
  border-radius: 4px;
}

.search-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

.operator-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.duration-normal {
  color: #67c23a;
}

.duration-slow {
  color: #e6a23c;
}

.duration-timeout {
  color: #f56c6c;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

:deep(.el-card__body) {
  padding: 20px !important;
}

:deep(.el-form-item) {
  margin-bottom: 16px;
}

:deep(.el-date-editor) {
  width: 100%;
}

.mt-4 {
  margin-top: 16px;
}

.detail-container {
  height: 100%;
  overflow: auto;
  padding: 0 20px;
}

.detail-card {
  margin-bottom: 20px;
}

.detail-card:last-child {
  margin-bottom: 0;
}

.operator-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.operator-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.operator-role {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.login-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.login-info div {
  display: flex;
  align-items: center;
  gap: 8px;
}

.url-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.code-block {
  background-color: var(--el-bg-color-page);
  border-radius: 4px;
  overflow: hidden;
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background-color: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
}

.code-content {
  margin: 0;
  padding: 16px;
  font-family: monospace;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}

.error-content {
  color: var(--el-color-danger);
}

.error-stack {
  background-color: var(--el-color-danger-light-9);
  padding: 16px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--el-color-danger);
}

.map-container {
  height: 500px;
}

.analysis-container {
  padding: 0 20px;
}

.analysis-card {
  margin-bottom: 20px;
}

.analysis-metrics {
  margin: 20px 0;
}

.metric-card {
  height: 180px;
}

.metric-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 8px;
}

.metric-label {
  color: var(--el-text-color-secondary);
  margin-bottom: 16px;
}

.metric-chart {
  flex: 1;
}

.analysis-chart-card {
  height: 400px;
}

.analysis-chart {
  height: 100%;
}

.behavior-chart {
  height: 400px;
}

.export-container {
  padding: 20px;
}

.export-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 20px;
  background: var(--el-bg-color);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  border-top: 1px solid var(--el-border-color-light);
}

:deep(.el-checkbox-group) {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
</style> 