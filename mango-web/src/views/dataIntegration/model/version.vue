<template>
  <div class="version-management">
    <div class="page-header">
      <h2 class="page-title">版本管理</h2>
      <div class="header-actions">
        <el-button-group>
          <el-button type="primary" @click="handleCreateVersion">
            <el-icon><Plus /></el-icon>创建版本
          </el-button>
          <el-button @click="handleRollback" :disabled="!selectedVersion">
            <el-icon><RefreshLeft /></el-icon>回滚版本
          </el-button>
        </el-button-group>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 版本历史列表 -->
      <el-col :span="8">
        <el-card class="version-list">
          <template #header>
            <div class="card-header">
              <span>版本历史</span>
              <el-radio-group v-model="timeRange" size="small">
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
                <el-radio-button label="all">全部</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          
          <el-timeline>
            <el-timeline-item
              v-for="version in versionList"
              :key="version.id"
              :type="getVersionType(version.status)"
              :timestamp="version.createTime"
              :hollow="version.id !== selectedVersion?.id"
              @click="handleSelectVersion(version)"
            >
              <div class="version-item" :class="{ active: version.id === selectedVersion?.id }">
                <div class="version-info">
                  <span class="version-number">v{{ version.version }}</span>
                  <el-tag size="small" :type="getVersionTagType(version.status)">
                    {{ version.status }}
                  </el-tag>
                </div>
                <div class="version-desc">{{ version.description }}</div>
                <div class="version-meta">
                  <span>作者: {{ version.author }}</span>
                  <span>变更: {{ version.changesCount }}项</span>
                </div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>

      <!-- 版本详情 -->
      <el-col :span="16">
        <el-card v-if="selectedVersion" class="version-detail">
          <template #header>
            <div class="card-header">
              <span>版本详情 - v{{ selectedVersion.version }}</span>
              <el-button-group>
                <el-button link type="primary" @click="handleCompare">
                  <el-icon><DocumentCopy /></el-icon>对比
                </el-button>
                <el-button link type="primary" @click="handleViewDependencies">
                  <el-icon><Connection /></el-icon>依赖关系
                </el-button>
              </el-button-group>
            </div>
          </template>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="版本号">v{{ selectedVersion.version }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getVersionTagType(selectedVersion.status)">
                {{ selectedVersion.status }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ selectedVersion.createTime }}</el-descriptions-item>
            <el-descriptions-item label="创建人">{{ selectedVersion.author }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ selectedVersion.description }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-title">变更记录</div>
          <el-table :data="selectedVersion.changes" style="width: 100%">
            <el-table-column prop="type" label="变更类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getChangeTagType(row.type)" size="small">
                  {{ row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="target" label="变更对象" />
            <el-table-column prop="detail" label="变更内容" show-overflow-tooltip />
            <el-table-column prop="time" label="变更时间" width="180" />
          </el-table>

          <div class="section-title">性能对比</div>
          <div class="performance-comparison">
            <div class="chart-container">
              <div ref="performanceChart" style="height: 300px"></div>
            </div>
          </div>
        </el-card>

        <el-empty v-else description="请选择版本查看详情" />
      </el-col>
    </el-row>

    <!-- 创建版本对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="创建新版本"
      width="500px"
    >
      <el-form ref="createFormRef" :model="createForm" label-width="100px">
        <el-form-item label="版本描述" prop="description" required>
          <el-input
            v-model="createForm.description"
            type="textarea"
            rows="3"
            placeholder="请输入版本描述"
          />
        </el-form-item>
        <el-form-item label="发布环境" prop="environment" required>
          <el-select v-model="createForm.environment" placeholder="请选择发布环境">
            <el-option label="开发环境" value="dev" />
            <el-option label="测试环境" value="test" />
            <el-option label="预发环境" value="staging" />
            <el-option label="生产环境" value="prod" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleConfirmCreate">确认</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, RefreshLeft, DocumentCopy, Connection
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'

// 状态变量
const timeRange = ref('week')
const selectedVersion = ref(null)
const createDialogVisible = ref(false)
const createForm = ref({
  description: '',
  environment: ''
})
const performanceChart = ref(null)

// 模拟数据
const versionList = ref([
  {
    id: '1',
    version: '1.0.0',
    status: 'stable',
    createTime: '2024-03-15 10:00',
    author: '张三',
    description: '初始版本发布',
    changesCount: 5,
    changes: [
      {
        type: 'add',
        target: '用户表',
        detail: '新增用户基础信息表',
        time: '2024-03-15 09:30'
      },
      {
        type: 'modify',
        target: '订单表',
        detail: '修改订单状态字段类型',
        time: '2024-03-15 09:45'
      }
    ]
  },
  {
    id: '2',
    version: '1.1.0',
    status: 'testing',
    createTime: '2024-03-16 14:30',
    author: '李四',
    description: '新增数据分析功能',
    changesCount: 3,
    changes: [
      {
        type: 'add',
        target: '分析视图',
        detail: '新增用户行为分析视图',
        time: '2024-03-16 14:00'
      }
    ]
  }
])

// 获取版本类型
const getVersionType = (status) => {
  const types = {
    stable: 'success',
    testing: 'warning',
    failed: 'danger'
  }
  return types[status] || 'info'
}

// 获取版本标签类型
const getVersionTagType = (status) => {
  const types = {
    stable: 'success',
    testing: 'warning',
    failed: 'danger'
  }
  return types[status] || ''
}

// 获取变更标签类型
const getChangeTagType = (type) => {
  const types = {
    add: 'success',
    modify: 'warning',
    delete: 'danger'
  }
  return types[type] || 'info'
}

// 选择版本
const handleSelectVersion = (version) => {
  selectedVersion.value = version
  initPerformanceChart()
}

// 创建版本
const handleCreateVersion = () => {
  createDialogVisible.value = true
}

// 确认创建版本
const handleConfirmCreate = () => {
  if (!createForm.value.description || !createForm.value.environment) {
    ElMessage.warning('请填写完整信息')
    return
  }
  
  // TODO: 调用创建版本API
  ElMessage.success('版本创建成功')
  createDialogVisible.value = false
}

// 回滚版本
const handleRollback = () => {
  ElMessageBox.confirm(
    `确定要回滚到版本 v${selectedVersion.value.version} 吗？`,
    '回滚确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // TODO: 调用回滚API
    ElMessage.success('版本回滚成功')
  })
}

// 版本对比
const handleCompare = () => {
  // TODO: 实现版本对比功能
}

// 查看依赖关系
const handleViewDependencies = () => {
  // TODO: 实现依赖关系查看功能
}

// 初始化性能对比图表
const initPerformanceChart = () => {
  if (!performanceChart.value) return

  const chart = echarts.init(performanceChart.value)
  
  const option = {
    title: {
      text: '性能指标对比',
      left: 'center'
    },
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['当前版本', '上一版本'],
      bottom: 0
    },
    radar: {
      indicator: [
        { name: '查询性能', max: 100 },
        { name: '写入性能', max: 100 },
        { name: '存储效率', max: 100 },
        { name: '资源占用', max: 100 },
        { name: '并发能力', max: 100 }
      ]
    },
    series: [{
      type: 'radar',
      data: [
        {
          value: [80, 85, 90, 75, 88],
          name: '当前版本'
        },
        {
          value: [70, 80, 85, 70, 82],
          name: '上一版本'
        }
      ]
    }]
  }
  
  chart.setOption(option)
}

onMounted(() => {
  if (versionList.value.length > 0) {
    handleSelectVersion(versionList.value[0])
  }
})
</script>

<style lang="scss" scoped>
.version-management {
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
  }

  .version-list {
    .version-item {
      cursor: pointer;
      padding: 8px;
      border-radius: 4px;
      transition: all 0.3s;

      &:hover {
        background: #f5f7fa;
      }

      &.active {
        background: #ecf5ff;
      }

      .version-info {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 4px;

        .version-number {
          font-weight: 500;
        }
      }

      .version-desc {
        color: #606266;
        font-size: 14px;
        margin-bottom: 4px;
      }

      .version-meta {
        display: flex;
        gap: 16px;
        color: #909399;
        font-size: 12px;
      }
    }
  }

  .version-detail {
    .section-title {
      font-size: 16px;
      font-weight: 500;
      margin: 20px 0 12px;
      color: #303133;
    }

    .performance-comparison {
      margin-top: 16px;
    }
  }
}
</style> 