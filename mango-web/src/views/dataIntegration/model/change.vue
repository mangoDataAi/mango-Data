<template>
  <div class="change-history">
    <div class="page-header">
      <h2 class="page-title">变更历史</h2>
      <div class="header-actions">
        <el-input
          v-model="searchQuery"
          placeholder="搜索变更记录"
          prefix-icon="Search"
          style="width: 250px"
        />
        <el-button-group>
          <el-button @click="handleExport">
            <el-icon><Download /></el-icon>导出记录
          </el-button>
          <el-button @click="handleRefresh">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
        </el-button-group>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 变更统计 -->
      <el-col :span="24">
        <div class="stats-cards">
          <el-card class="stats-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>今日变更</span>
                <el-tag type="success">{{ todayChanges }}</el-tag>
              </div>
            </template>
            <div class="trend-chart" ref="todayTrendChart"></div>
          </el-card>

          <el-card class="stats-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>本周变更</span>
                <el-tag type="warning">{{ weekChanges }}</el-tag>
              </div>
            </template>
            <div class="trend-chart" ref="weekTrendChart"></div>
          </el-card>

          <el-card class="stats-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>变更类型分布</span>
              </div>
            </template>
            <div class="pie-chart" ref="typeDistChart"></div>
          </el-card>

          <el-card class="stats-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>变更状态分布</span>
              </div>
            </template>
            <div class="pie-chart" ref="statusDistChart"></div>
          </el-card>
        </div>
      </el-col>

      <!-- 变更记录表格 -->
      <el-col :span="24">
        <el-card class="table-card">
          <template #header>
            <div class="card-header">
              <div class="header-left">
                <span>变更记录</span>
                <el-radio-group v-model="timeFilter" size="small">
                  <el-radio-button label="today">今日</el-radio-button>
                  <el-radio-button label="week">本周</el-radio-button>
                  <el-radio-button label="month">本月</el-radio-button>
                  <el-radio-button label="all">全部</el-radio-button>
                </el-radio-group>
              </div>
              <div class="header-right">
                <el-button type="primary" @click="handleShowAdvancedSearch">
                  高级筛选<el-icon class="el-icon--right"><Filter /></el-icon>
                </el-button>
              </div>
            </div>
          </template>

          <el-table
            :data="filteredChangeRecords"
            style="width: 100%"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="55" />
            <el-table-column prop="id" label="变更ID" width="100" />
            <el-table-column prop="type" label="变更类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getChangeTypeTag(row.type)" size="small">
                  {{ row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="target" label="变更对象" min-width="150" />
            <el-table-column prop="content" label="变更内容" min-width="200" show-overflow-tooltip />
            <el-table-column prop="author" label="操作人" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTag(row.status)" size="small">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="变更时间" width="180" />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button-group>
                  <el-button link type="primary" @click="handleViewDetail(row)">
                    详情
                  </el-button>
                  <el-button link type="primary" @click="handleCompare(row)">
                    对比
                  </el-button>
                  <el-button 
                    link 
                    type="primary" 
                    @click="handleRollback(row)"
                    :disabled="!canRollback(row)"
                  >
                    回滚
                  </el-button>
                </el-button-group>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="totalRecords"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 高级搜索抽屉 -->
    <el-drawer
      v-model="advancedSearchVisible"
      title="高级筛选"
      size="400px"
    >
      <el-form :model="advancedSearchForm" label-width="100px">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="advancedSearchForm.timeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="变更类型">
          <el-select
            v-model="advancedSearchForm.types"
            multiple
            placeholder="请选择变更类型"
            style="width: 100%"
          >
            <el-option label="新增" value="add" />
            <el-option label="修改" value="modify" />
            <el-option label="删除" value="delete" />
            <el-option label="重命名" value="rename" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更状态">
          <el-select
            v-model="advancedSearchForm.status"
            multiple
            placeholder="请选择变更状态"
            style="width: 100%"
          >
            <el-option label="成功" value="success" />
            <el-option label="失败" value="failed" />
            <el-option label="回滚" value="rollback" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作人">
          <el-select
            v-model="advancedSearchForm.authors"
            multiple
            filterable
            placeholder="请选择操作人"
            style="width: 100%"
          >
            <el-option
              v-for="author in authorOptions"
              :key="author.value"
              :label="author.label"
              :value="author.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="变更对象">
          <el-input
            v-model="advancedSearchForm.target"
            placeholder="请输入变更对象"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div style="flex: auto">
          <el-button @click="handleResetSearch">重置</el-button>
          <el-button type="primary" @click="handleApplySearch">应用</el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 变更详情抽屉 -->
    <el-drawer
      v-model="detailDrawerVisible"
      title="变更详情"
      size="60%"
      destroy-on-close
    >
      <template v-if="selectedChange">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="变更ID">{{ selectedChange.id }}</el-descriptions-item>
          <el-descriptions-item label="变更类型">
            <el-tag :type="getChangeTypeTag(selectedChange.type)">
              {{ selectedChange.type }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="变更对象">{{ selectedChange.target }}</el-descriptions-item>
          <el-descriptions-item label="操作人">{{ selectedChange.author }}</el-descriptions-item>
          <el-descriptions-item label="变更时间">{{ selectedChange.createTime }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTag(selectedChange.status)">
              {{ selectedChange.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="变更内容" :span="2">
            {{ selectedChange.content }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-title">SQL语句</div>
        <pre class="code-block">{{ selectedChange.sql }}</pre>

        <div class="section-title">执行计划</div>
        <pre class="code-block">{{ selectedChange.executionPlan }}</pre>

        <div class="section-title">影响分析</div>
        <el-collapse>
          <el-collapse-item title="上游依赖" name="upstream">
            <el-table :data="selectedChange.impacts?.upstream" style="width: 100%">
              <el-table-column prop="name" label="对象名称" />
              <el-table-column prop="type" label="对象类型" width="120" />
              <el-table-column prop="impact" label="影响程度" width="120">
                <template #default="{ row }">
                  <el-tag :type="getImpactTag(row.impact)">{{ row.impact }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
          <el-collapse-item title="下游影响" name="downstream">
            <el-table :data="selectedChange.impacts?.downstream" style="width: 100%">
              <el-table-column prop="name" label="对象名称" />
              <el-table-column prop="type" label="对象类型" width="120" />
              <el-table-column prop="impact" label="影响程度" width="120">
                <template #default="{ row }">
                  <el-tag :type="getImpactTag(row.impact)">{{ row.impact }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search, Download, Refresh, Filter,
  Plus, Delete, Edit, View
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'

// 状态变量
const searchQuery = ref('')
const timeFilter = ref('week')
const currentPage = ref(1)
const pageSize = ref(20)
const totalRecords = ref(100)
const advancedSearchVisible = ref(false)
const detailDrawerVisible = ref(false)
const selectedChange = ref(null)
const todayTrendChart = ref(null)
const weekTrendChart = ref(null)
const typeDistChart = ref(null)
const statusDistChart = ref(null)

// 统计数据
const todayChanges = ref(25)
const weekChanges = ref(156)

// 高级搜索表单
const advancedSearchForm = ref({
  timeRange: [],
  types: [],
  status: [],
  authors: [],
  target: ''
})

// 作者选项
const authorOptions = [
  { label: '张三', value: 'zhangsan' },
  { label: '李四', value: 'lisi' },
  { label: '王五', value: 'wangwu' }
]

// 模拟变更记录数据
const changeRecords = ref([
  {
    id: 'CHG001',
    type: 'add',
    target: '用户行为分析表',
    content: '新增用户行为分析表，包含用户ID、行为类型、时间等字段',
    author: '张三',
    status: 'success',
    createTime: '2024-03-18 10:30:00',
    sql: 'CREATE TABLE user_behavior (\n  user_id STRING,\n  behavior_type STRING,\n  create_time TIMESTAMP\n)',
    executionPlan: '1. Create Table\n2. Add Columns\n3. Set Properties',
    impacts: {
      upstream: [
        { name: '用户表', type: '表', impact: 'low' }
      ],
      downstream: [
        { name: '用户画像视图', type: '视图', impact: 'high' },
        { name: '行为分析任务', type: '任务', impact: 'medium' }
      ]
    }
  },
  {
    id: 'CHG002',
    type: 'modify',
    target: '订单表',
    content: '修改订单状态字段类型，从VARCHAR改为ENUM',
    author: '李四',
    status: 'success',
    createTime: '2024-03-18 11:15:00'
  }
])

// 过滤后的记录
const filteredChangeRecords = computed(() => {
  return changeRecords.value.filter(record => {
    if (searchQuery.value) {
      const query = searchQuery.value.toLowerCase()
      return record.target.toLowerCase().includes(query) ||
             record.content.toLowerCase().includes(query) ||
             record.author.toLowerCase().includes(query)
    }
    return true
  })
})

// 获取变更类型标签
const getChangeTypeTag = (type) => {
  const types = {
    add: 'success',
    modify: 'warning',
    delete: 'danger',
    rename: 'info'
  }
  return types[type] || ''
}

// 获取状态标签
const getStatusTag = (status) => {
  const types = {
    success: 'success',
    failed: 'danger',
    rollback: 'info'
  }
  return types[status] || ''
}

// 获取影响程度标签
const getImpactTag = (impact) => {
  const types = {
    high: 'danger',
    medium: 'warning',
    low: 'info'
  }
  return types[impact] || ''
}

// 判断是否可以回滚
const canRollback = (row) => {
  return row.status === 'success' && row.type !== 'rollback'
}

// 表格选择变化
const handleSelectionChange = (selection) => {
  console.log('selected:', selection)
}

// 分页处理
const handleSizeChange = (size) => {
  pageSize.value = size
  // TODO: 重新加载数据
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  // TODO: 重新加载数据
}

// 显示高级搜索
const handleShowAdvancedSearch = () => {
  advancedSearchVisible.value = true
}

// 重置搜索
const handleResetSearch = () => {
  advancedSearchForm.value = {
    timeRange: [],
    types: [],
    status: [],
    authors: [],
    target: ''
  }
}

// 应用搜索
const handleApplySearch = () => {
  // TODO: 应用筛选条件
  advancedSearchVisible.value = false
}

// 查看详情
const handleViewDetail = (row) => {
  selectedChange.value = row
  detailDrawerVisible.value = true
}

// 版本对比
const handleCompare = (row) => {
  // TODO: 实现版本对比功能
}

// 回滚操作
const handleRollback = (row) => {
  ElMessageBox.confirm(
    `确定要回滚该变更吗？这可能会影响依赖的对象。`,
    '回滚确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // TODO: 调用回滚API
    ElMessage.success('回滚成功')
  })
}

// 导出记录
const handleExport = () => {
  // TODO: 实现导出功能
  ElMessage.success('导出成功')
}

// 刷新
const handleRefresh = () => {
  // TODO: 重新加载数据
  ElMessage.success('刷新成功')
}

// 初始化图表
const initCharts = () => {
  // 今日趋势图
  const today = echarts.init(todayTrendChart.value)
  today.setOption({
    title: { text: '今日变更趋势', left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['00:00', '06:00', '12:00', '18:00', '24:00'] },
    yAxis: { type: 'value' },
    series: [{
      data: [3, 7, 10, 5, 0],
      type: 'line',
      smooth: true,
      areaStyle: {}
    }]
  })

  // 本周趋势图
  const week = echarts.init(weekTrendChart.value)
  week.setOption({
    title: { text: '本周变更趋势', left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
    yAxis: { type: 'value' },
    series: [{
      data: [20, 32, 25, 34, 45, 0, 0],
      type: 'bar'
    }]
  })

  // 类型分布图
  const typeDist = echarts.init(typeDistChart.value)
  typeDist.setOption({
    title: { text: '变更类型分布', left: 'center' },
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '50%',
      data: [
        { value: 45, name: '新增' },
        { value: 35, name: '修改' },
        { value: 15, name: '删除' },
        { value: 5, name: '重命名' }
      ]
    }]
  })

  // 状态分布图
  const statusDist = echarts.init(statusDistChart.value)
  statusDist.setOption({
    title: { text: '变更状态分布', left: 'center' },
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{
      type: 'pie',
      radius: '50%',
      data: [
        { value: 85, name: '成功' },
        { value: 10, name: '失败' },
        { value: 5, name: '回滚' }
      ]
    }]
  })
}

onMounted(() => {
  initCharts()
})
</script>

<style lang="scss" scoped>
.change-history {
  padding: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .page-title {
      font-size: 24px;
      color: #2c3e50;
      margin: 0;
    }

    .header-actions {
      display: flex;
      gap: 10px;
    }
  }

  .stats-cards {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    margin-bottom: 20px;

    .stats-card {
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .trend-chart,
      .pie-chart {
        height: 200px;
      }
    }
  }

  .table-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .header-left {
        display: flex;
        align-items: center;
        gap: 16px;
      }
    }
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .section-title {
    font-size: 16px;
    font-weight: 500;
    margin: 20px 0 12px;
    color: #303133;
  }

  .code-block {
    background: #f5f7fa;
    padding: 12px;
    border-radius: 4px;
    font-family: 'Consolas', monospace;
    font-size: 12px;
    margin: 0;
    overflow-x: auto;
  }
}
</style> 