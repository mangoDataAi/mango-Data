<template>
  <div class="model-verify">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">模型验证</h2>
      <div class="header-actions">
        <el-button type="primary" @click="handleStartValidation">
          <el-icon><VideoPlay /></el-icon>开始验证
        </el-button>
        <el-button @click="handleRefresh">
          <el-icon><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </div>

    <!-- 高级搜索 -->
    <el-card class="search-card">
      <el-form :model="searchForm" label-width="100px" inline>
        <el-form-item label="模型名称">
          <el-input v-model="searchForm.modelName" placeholder="请输入模型名称" clearable />
        </el-form-item>
        <el-form-item label="验证状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="待验证" value="pending" />
            <el-option label="验证中" value="validating" />
            <el-option label="验证成功" value="success" />
            <el-option label="验证失败" value="failed" />
          </el-select>
        </el-form-item>
        <el-form-item label="验证时间">
          <el-date-picker
            v-model="searchForm.timeRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 验证规则列表 -->
    <el-card class="rules-card">
      <template #header>
        <div class="card-header">
          <span>验证规则</span>
          <el-button type="primary" link @click="handleAddRule">
            <el-icon><Plus /></el-icon>添加规则
          </el-button>
        </div>
      </template>
      <el-table :data="rulesList" border stripe>
        <el-table-column type="selection" width="55" />
        <el-table-column prop="name" label="规则名称" min-width="150" />
        <el-table-column prop="type" label="规则类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getRuleTypeTag(row.type)">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="规则描述" min-width="200" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEditRule(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDeleteRule(row)">删除</el-button>
            <el-button type="success" link @click="handleRunRule(row)">执行</el-button>
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

    <!-- 验证结果 -->
    <el-row :gutter="20" class="result-row">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>验证结果统计</span>
              <el-radio-group v-model="timeRange" size="small">
                <el-radio-button value="day">今日</el-radio-button>
                <el-radio-button value="week">本周</el-radio-button>
                <el-radio-button value="month">本月</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="chart-container" ref="resultChart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>规则执行趋势</span>
            </div>
          </template>
          <div class="chart-container" ref="trendChart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 规则编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '添加规则' : '编辑规则'"
      width="600px"
    >
      <el-form
        ref="ruleFormRef"
        :model="ruleForm"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="ruleForm.name" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="规则类型" prop="type">
          <el-select v-model="ruleForm.type" placeholder="请选择规则类型">
            <el-option label="完整性检查" value="completeness" />
            <el-option label="准确性检查" value="accuracy" />
            <el-option label="一致性检查" value="consistency" />
            <el-option label="有效性检查" value="validity" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则描述" prop="description">
          <el-input
            v-model="ruleForm.description"
            type="textarea"
            rows="3"
            placeholder="请输入规则描述"
          />
        </el-form-item>
        <el-form-item label="验证条件" prop="condition">
          <el-input
            v-model="ruleForm.condition"
            type="textarea"
            rows="3"
            placeholder="请输入验证条件"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveRule">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  VideoPlay, Refresh, Plus
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'

// 搜索表单
const searchForm = ref({
  modelName: '',
  status: '',
  timeRange: []
})

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 时间范围
const timeRange = ref('week')

// 图表实例
const resultChart = ref(null)
const trendChart = ref(null)

// 规则列表数据
const rulesList = ref([
  {
    id: 1,
    name: '字段完整性检查',
    type: 'completeness',
    description: '检查必填字段是否存在空值',
    status: 'success'
  },
  {
    id: 2,
    name: '数值范围检查',
    type: 'validity',
    description: '检查数值字段是否在有效范围内',
    status: 'failed'
  }
])

// 对话框控制
const dialogVisible = ref(false)
const dialogType = ref('add')
const ruleFormRef = ref(null)
const ruleForm = ref({
  name: '',
  type: '',
  description: '',
  condition: ''
})

// 表单验证规则
const rules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  description: [{ required: true, message: '请输入规则描述', trigger: 'blur' }],
  condition: [{ required: true, message: '请输入验证条件', trigger: 'blur' }]
}

// 获取规则类型标签样式
const getRuleTypeTag = (type) => {
  const types = {
    completeness: 'primary',
    accuracy: 'success',
    consistency: 'warning',
    validity: 'info'
  }
  return types[type] || ''
}

// 获取状态标签样式
const getStatusType = (status) => {
  const types = {
    success: 'success',
    failed: 'danger',
    pending: 'info',
    validating: 'warning'
  }
  return types[status] || ''
}

// 初始化图表
const initCharts = () => {
  // 结果统计图表
  const result = echarts.init(resultChart.value)
  result.setOption({
    title: {
      text: '验证结果分布',
      left: 'center'
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        type: 'pie',
        radius: '50%',
        data: [
          { value: 235, name: '验证成功' },
          { value: 45, name: '验证失败' },
          { value: 20, name: '待验证' }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  })

  // 趋势图表
  const trend = echarts.init(trendChart.value)
  trend.setOption({
    title: {
      text: '规则执行趋势',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['执行次数', '成功率'],
      bottom: 0
    },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: [
      {
        type: 'value',
        name: '执行次数',
        min: 0,
        max: 100
      },
      {
        type: 'value',
        name: '成功率',
        min: 0,
        max: 100,
        axisLabel: {
          formatter: '{value}%'
        }
      }
    ],
    series: [
      {
        name: '执行次数',
        type: 'bar',
        data: [45, 52, 38, 64, 58, 32, 42]
      },
      {
        name: '成功率',
        type: 'line',
        yAxisIndex: 1,
        data: [92, 88, 95, 89, 94, 96, 90]
      }
    ]
  })

  // 监听窗口大小变化
  window.addEventListener('resize', () => {
    result.resize()
    trend.resize()
  })
}

// 搜索
const handleSearch = () => {
  // TODO: 实现搜索逻辑
  console.log('搜索条件:', searchForm.value)
}

// 重置
const handleReset = () => {
  searchForm.value = {
    modelName: '',
    status: '',
    timeRange: []
  }
}

// 开始验证
const handleStartValidation = () => {
  ElMessage.success('开始验证')
}

// 刷新
const handleRefresh = () => {
  // TODO: 实现刷新逻辑
}

// 添加规则
const handleAddRule = () => {
  dialogType.value = 'add'
  ruleForm.value = {
    name: '',
    type: '',
    description: '',
    condition: ''
  }
  dialogVisible.value = true
}

// 编辑规则
const handleEditRule = (row) => {
  dialogType.value = 'edit'
  ruleForm.value = { ...row }
  dialogVisible.value = true
}

// 删除规则
const handleDeleteRule = (row) => {
  ElMessageBox.confirm(
    '确认删除该规则吗？',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success('删除成功')
  })
}

// 执行规则
const handleRunRule = (row) => {
  ElMessage.success(`开始执行规则: ${row.name}`)
}

// 保存规则
const handleSaveRule = () => {
  ruleFormRef.value?.validate((valid) => {
    if (valid) {
      ElMessage.success(dialogType.value === 'add' ? '添加成功' : '更新成功')
      dialogVisible.value = false
    }
  })
}

// 分页处理
const handleSizeChange = (val) => {
  pageSize.value = val
  // TODO: 重新加载数据
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  // TODO: 重新加载数据
}

// 组件挂载时初始化
onMounted(() => {
  nextTick(() => {
    initCharts()
  })
})
</script>

<style lang="scss" scoped>
.model-verify {
  padding: 20px;

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .page-title {
      font-size: 24px;
      margin: 0;
    }

    .header-actions {
      display: flex;
      gap: 10px;
    }
  }

  .search-card {
    margin-bottom: 20px;
  }

  .rules-card {
    margin-bottom: 20px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }

  .result-row {
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

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
