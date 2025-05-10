<template>
  <div class="environment-container">
    <!-- 环境概览 -->
    <el-row :gutter="20" class="overview-section">
      <el-col :span="6" v-for="(item, index) in envOverview" :key="index">
        <el-card shadow="hover" class="overview-card" :class="item.type">
          <div class="overview-content">
            <el-icon :size="40" :class="item.type">
              <component :is="item.icon" />
            </el-icon>
            <div class="overview-info">
              <div class="value">{{ item.value }}</div>
              <div class="label">{{ item.label }}</div>
            </div>
            <div class="trend">
              <span :class="item.trend > 0 ? 'up' : 'down'">
                {{ Math.abs(item.trend) }}%
              </span>
              较上周期
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 环境列表 -->
    <el-card class="env-list">
      <template #header>
        <div class="list-header">
          <div class="header-left">
            <h3>环境管理</h3>
            <el-tag 
              :type="activeEnvs > 0 ? 'success' : 'info'"
              effect="dark"
            >
              {{ activeEnvs > 0 ? `${activeEnvs} 个运行中` : '无运行环境' }}
            </el-tag>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="showCreateEnv">
              <el-icon><Plus /></el-icon> 新建环境
            </el-button>
            <el-button type="success" @click="batchDeploy" :disabled="!selectedEnvs.length">
              <el-icon><Upload /></el-icon> 批量部署
            </el-button>
            <el-button @click="refreshList">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 高级搜索 -->
      <el-form :model="searchForm" class="search-form" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="环境类型">
              <el-select v-model="searchForm.type" placeholder="选择环境类型" clearable>
                <el-option label="开发环境" value="dev" />
                <el-option label="测试环境" value="test" />
                <el-option label="预发环境" value="staging" />
                <el-option label="生产环境" value="prod" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="运行状态">
              <el-select v-model="searchForm.status" placeholder="选择运行状态" clearable>
                <el-option label="运行中" value="running" />
                <el-option label="已停止" value="stopped" />
                <el-option label="部署中" value="deploying" />
                <el-option label="异常" value="error" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="资源类型">
              <el-select v-model="searchForm.resource" placeholder="选择资源类型" clearable>
                <el-option label="K8s集群" value="k8s" />
                <el-option label="虚拟机" value="vm" />
                <el-option label="容器" value="container" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="创建人">
              <el-input v-model="searchForm.creator" placeholder="输入创建人" clearable />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="创建时间">
              <el-date-picker
                v-model="searchForm.timeRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关键词">
              <el-input
                v-model="searchForm.keyword"
                placeholder="搜索环境名称、描述或标签"
                clearable
              >
                <template #suffix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24" style="text-align: right;">
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon> 搜索
            </el-button>
            <el-button @click="resetSearch">
              <el-icon><Refresh /></el-icon> 重置
            </el-button>
          </el-col>
        </el-row>
      </el-form>

      <!-- 环境表格 -->
      <el-table
        :data="envList"
        style="width: 100%"
        :loading="loading"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="name" label="环境名称" min-width="180">
          <template #default="{ row }">
            <div class="env-name" @click="showEnvDetail(row)">
              <el-tag :type="getEnvTypeTag(row.type)" size="small" effect="dark">
                {{ row.type }}
              </el-tag>
              <span class="name-text">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="运行状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resource" label="资源类型" width="120">
          <template #default="{ row }">
            <el-tag type="info">{{ row.resource }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="cpu" label="CPU使用率" width="150">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.cpu" 
              :status="getCpuStatus(row.cpu)"
              :stroke-width="10"
            />
          </template>
        </el-table-column>
        <el-table-column prop="memory" label="内存使用率" width="150">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.memory" 
              :status="getMemoryStatus(row.memory)"
              :stroke-width="10"
            />
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column prop="creator" label="创建人" width="120" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-tooltip content="启动/停止" placement="top">
                <el-button 
                  :type="row.status === 'running' ? 'danger' : 'success'" 
                  link
                  :icon="row.status === 'running' ? 'VideoPause' : 'VideoPlay'"
                  @click="toggleEnvStatus(row)"
                />
              </el-tooltip>
              <el-tooltip content="部署" placement="top">
                <el-button 
                  type="primary" 
                  link
                  icon="Upload"
                  @click="deployEnv(row)"
                />
              </el-tooltip>
              <el-tooltip content="监控" placement="top">
                <el-button 
                  type="warning" 
                  link
                  icon="Monitor"
                  @click="monitorEnv(row)"
                />
              </el-tooltip>
              <el-tooltip content="编辑" placement="top">
                <el-button 
                  type="info" 
                  link
                  icon="Edit"
                  @click="editEnv(row)"
                />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button 
                  type="danger" 
                  link
                  icon="Delete"
                  @click="deleteEnv(row)"
                />
              </el-tooltip>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
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

    <!-- 环境详情抽屉 -->
    <el-drawer
      v-model="detailDrawer"
      :title="currentEnv?.name"
      size="80%"
      :destroy-on-close="true"
    >
      <template #header>
        <div class="drawer-header">
          <div class="env-info">
            <el-tag :type="getEnvTypeTag(currentEnv?.type)" size="large" effect="dark">
              {{ currentEnv?.type }}
            </el-tag>
            <h2>{{ currentEnv?.name }}</h2>
          </div>
          <div class="env-actions">
            <el-button-group>
              <el-button 
                :type="currentEnv?.status === 'running' ? 'danger' : 'success'"
                @click="toggleCurrentEnv"
              >
                <el-icon>
                  <component :is="currentEnv?.status === 'running' ? 'VideoPause' : 'VideoPlay'" />
                </el-icon>
                {{ currentEnv?.status === 'running' ? '停止环境' : '启动环境' }}
              </el-button>
              <el-button type="primary" @click="deployCurrentEnv">
                <el-icon><Upload /></el-icon> 部署环境
              </el-button>
              <el-button type="warning" @click="showMonitor">
                <el-icon><Monitor /></el-icon> 查看监控
              </el-button>
            </el-button-group>
          </div>
        </div>
      </template>

      <el-scrollbar>
        <div class="env-detail">
          <!-- 基本信息 -->
          <el-card class="detail-section">
            <template #header>
              <div class="section-header">
                <h3>基本信息</h3>
                <el-tag :type="getStatusType(currentEnv?.status)">
                  {{ currentEnv?.status }}
                </el-tag>
              </div>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="环境ID">{{ currentEnv?.id }}</el-descriptions-item>
              <el-descriptions-item label="资源类型">{{ currentEnv?.resource }}</el-descriptions-item>
              <el-descriptions-item label="创建人">{{ currentEnv?.creator }}</el-descriptions-item>
              <el-descriptions-item label="更新时间">{{ currentEnv?.updateTime }}</el-descriptions-item>
              <el-descriptions-item label="描述" :span="2">{{ currentEnv?.description }}</el-descriptions-item>
            </el-descriptions>
          </el-card>

          <!-- 资源监控 -->
          <el-card class="detail-section">
            <template #header>
              <div class="section-header">
                <h3>资源监控</h3>
                <el-radio-group v-model="monitorTimeRange" size="small">
                  <el-radio-button label="1h">1小时</el-radio-button>
                  <el-radio-button label="6h">6小时</el-radio-button>
                  <el-radio-button label="24h">24小时</el-radio-button>
                  <el-radio-button label="7d">7天</el-radio-button>
                </el-radio-group>
              </div>
            </template>
            <el-row :gutter="20">
              <el-col :span="12">
                <div class="monitor-chart">
                  <div class="chart-header">
                    <span class="chart-title">CPU使用率</span>
                    <el-tag :type="getCpuStatus(currentEnv?.cpu)">
                      {{ currentEnv?.cpu }}%
                    </el-tag>
                  </div>
                  <div ref="cpuChart" style="height: 300px"></div>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="monitor-chart">
                  <div class="chart-header">
                    <span class="chart-title">内存使用率</span>
                    <el-tag :type="getMemoryStatus(currentEnv?.memory)">
                      {{ currentEnv?.memory }}%
                    </el-tag>
                  </div>
                  <div ref="memoryChart" style="height: 300px"></div>
                </div>
              </el-col>
            </el-row>
          </el-card>

          <!-- 部署历史 -->
          <el-card class="detail-section">
            <template #header>
              <h3>部署历史</h3>
            </template>
            <el-timeline>
              <el-timeline-item
                v-for="(activity, index) in deployHistory"
                :key="index"
                :type="getDeployStatusType(activity.status)"
                :timestamp="activity.time"
              >
                {{ activity.content }}
                <el-tag 
                  size="small" 
                  :type="getDeployStatusType(activity.status)"
                  style="margin-left: 10px"
                >
                  {{ activity.status }}
                </el-tag>
              </el-timeline-item>
            </el-timeline>
          </el-card>
        </div>
      </el-scrollbar>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import {
  Plus, Upload, Refresh, Search, Monitor,
  VideoPlay, VideoPause, Edit, Delete
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 环境概览数据
const envOverview = ref([
  {
    label: '运行环境',
    value: 12,
    trend: 20,
    type: 'success',
    icon: 'VideoPlay'
  },
  {
    label: '资源使用率',
    value: '78%',
    trend: 5,
    type: 'warning',
    icon: 'Monitor'
  },
  {
    label: '部署次数',
    value: 156,
    trend: 30,
    type: 'primary',
    icon: 'Upload'
  },
  {
    label: '异常数',
    value: 2,
    trend: -50,
    type: 'danger',
    icon: 'Warning'
  }
])

// 搜索表单
const searchForm = ref({
  type: '',
  status: '',
  resource: '',
  creator: '',
  timeRange: [],
  keyword: ''
})

// 表格数据
const loading = ref(false)
const envList = ref([])
const selectedEnvs = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 计算活跃环境数
const activeEnvs = computed(() => {
  return envList.value.filter(item => item.status === 'running').length
})

// 获取环境列表
const getEnvList = async () => {
  loading.value = true
  try {
    // 模拟接口调用
    await new Promise(resolve => setTimeout(resolve, 1000))
    envList.value = [
      {
        id: 'ENV001',
        name: '开发测试环境',
        type: '开发环境',
        status: 'running',
        resource: 'K8s集群',
        cpu: 65,
        memory: 78,
        updateTime: '2024-03-02 15:30:00',
        creator: '张三',
        description: '用于日常开发和测试的环境'
      },
      {
        id: 'ENV002',
        name: '预发布环境',
        type: '预发环境',
        status: 'stopped',
        resource: '虚拟机',
        cpu: 0,
        memory: 0,
        updateTime: '2024-03-01 14:20:00',
        creator: '李四',
        description: '用于预发布验证的环境'
      }
    ]
    total.value = 2
  } finally {
    loading.value = false
  }
}

// 部署历史数据
const deployHistory = ref([
  {
    time: '2024-03-02 15:30:00',
    content: '部署版本 v1.2.3',
    status: 'success'
  },
  {
    time: '2024-03-01 14:20:00',
    content: '回滚到版本 v1.2.2',
    status: 'warning'
  },
  {
    time: '2024-03-01 10:15:00',
    content: '部署版本 v1.2.2',
    status: 'error'
  }
])

// 环境详情
const detailDrawer = ref(false)
const currentEnv = ref(null)
const monitorTimeRange = ref('6h')

// 状态类型映射
const getEnvTypeTag = (type) => {
  const types = {
    '开发环境': 'info',
    '测试环境': 'success',
    '预发环境': 'warning',
    '生产环境': 'danger'
  }
  return types[type] || 'info'
}

const getStatusType = (status) => {
  const types = {
    'running': 'success',
    'stopped': 'info',
    'deploying': 'warning',
    'error': 'danger'
  }
  return types[status] || 'info'
}

const getCpuStatus = (value) => {
  if (value >= 90) return 'exception'
  if (value >= 70) return 'warning'
  return 'success'
}

const getMemoryStatus = (value) => {
  if (value >= 90) return 'exception'
  if (value >= 70) return 'warning'
  return 'success'
}

const getDeployStatusType = (status) => {
  const types = {
    'success': 'success',
    'warning': 'warning',
    'error': 'danger'
  }
  return types[status] || 'info'
}

// 搜索方法
const handleSearch = () => {
  currentPage.value = 1
  getEnvList()
}

const resetSearch = () => {
  searchForm.value = {
    type: '',
    status: '',
    resource: '',
    creator: '',
    timeRange: [],
    keyword: ''
  }
  handleSearch()
}

// 表格操作方法
const handleSelectionChange = (selection) => {
  selectedEnvs.value = selection
}

// 环境操作方法
const showCreateEnv = () => {
  ElMessage.success('新建环境')
}

const editEnv = (row) => {
  ElMessage.success(`编辑环境: ${row.name}`)
}

const showEnvDetail = (row) => {
  currentEnv.value = row
  detailDrawer.value = true
}

const deleteEnv = (row) => {
  ElMessageBox.confirm(
    `确定要删除环境 "${row.name}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success(`删除环境: ${row.name}`)
  })
}

const toggleEnvStatus = (row) => {
  const action = row.status === 'running' ? '停止' : '启动'
  ElMessage.success(`${action}环境: ${row.name}`)
}

const deployEnv = (row) => {
  ElMessage.success(`部署环境: ${row.name}`)
}

const monitorEnv = (row) => {
  ElMessage.success(`查看环境监控: ${row.name}`)
}

const batchDeploy = () => {
  if (selectedEnvs.value.length === 0) {
    ElMessage.warning('请选择要部署的环境')
    return
  }
  ElMessage.success(`批量部署 ${selectedEnvs.value.length} 个环境`)
}

// 环境详情操作
const toggleCurrentEnv = () => {
  const action = currentEnv.value.status === 'running' ? '停止' : '启动'
  ElMessage.success(`${action}环境: ${currentEnv.value.name}`)
}

const deployCurrentEnv = () => {
  ElMessage.success(`部署环境: ${currentEnv.value.name}`)
}

const showMonitor = () => {
  ElMessage.success(`查看环境监控: ${currentEnv.value.name}`)
}

// 分页方法
const handleSizeChange = (val) => {
  pageSize.value = val
  getEnvList()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  getEnvList()
}

// 刷新列表
const refreshList = () => {
  getEnvList()
}

// 初始化
getEnvList()
</script>

<style scoped>
.environment-container {
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
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.overview-info {
  margin: 16px 0;
}

.overview-info .value {
  font-size: 24px;
  font-weight: bold;
  line-height: 1;
  margin-bottom: 8px;
}

.overview-info .label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.trend {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.up {
  color: #F56C6C;
  margin-right: 4px;
}

.down {
  color: #67C23A;
  margin-right: 4px;
}

.env-list {
  margin-bottom: 20px;
}

.list-header {
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
  margin: 20px 0;
  padding: 20px;
  background-color: var(--el-bg-color-page);
  border-radius: 4px;
}

.env-name {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.name-text {
  color: var(--el-color-primary);
}

.name-text:hover {
  text-decoration: underline;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.drawer-header {
  padding: 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.env-info {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.env-info h2 {
  margin: 0;
}

.env-actions {
  display: flex;
  justify-content: flex-end;
}

.detail-section {
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.section-header h3 {
  margin: 0;
}

.monitor-chart {
  padding: 20px;
  background-color: var(--el-bg-color-page);
  border-radius: 4px;
  margin-bottom: 20px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-title {
  font-weight: bold;
}

.el-timeline-item {
  padding-bottom: 20px;
}
</style> 