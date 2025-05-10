<template>
  <div class="dataset-api">
    <!-- 统计卡片区域 -->
    <div class="statistics-area">
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="statistics-card blue-card">
            <div class="statistics-icon blue">
              <el-icon><Connection /></el-icon>
            </div>
            <div class="statistics-info">
              <div class="stat-header">
                <div class="stat-title">接口总数</div>
              </div>
              <div class="statistics-value">{{ statsData.totalApis }}<span class="stat-unit">个</span></div>
              <div class="stat-trend" :class="{ up: statsData.apiGrowthRate >= 0, down: statsData.apiGrowthRate < 0 }">
                <el-icon v-if="statsData.apiGrowthRate >= 0"><ArrowUp /></el-icon>
                <el-icon v-else><ArrowDown /></el-icon>
                <span>{{ Math.abs(statsData.apiGrowthRate).toFixed(1) }}%</span>
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="statistics-card green-card">
            <div class="statistics-icon green">
              <el-icon><CircleCheckFilled /></el-icon>
            </div>
            <div class="statistics-info">
              <div class="stat-header">
                <div class="stat-title">成功率</div>
              </div>
              <div class="statistics-value">{{ statsData.successRate }}<span class="stat-unit">%</span></div>
              <div class="stat-trend up">
                <el-icon><ArrowUp /></el-icon>
                <span>0.5%</span>
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="statistics-card orange-card">
            <div class="statistics-icon orange">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="statistics-info">
              <div class="stat-header">
                <div class="stat-title">平均延迟</div>
              </div>
              <div class="statistics-value">{{ statsData.avgResponseTime }}<span class="stat-unit">秒</span></div>
              <div class="stat-trend" :class="{ down: statsData.responseTimeChangeRate < 0, up: statsData.responseTimeChangeRate >= 0, good: statsData.responseTimeChangeRate < 0 }">
                <el-icon v-if="statsData.responseTimeChangeRate >= 0"><ArrowUp /></el-icon>
                <el-icon v-else><ArrowDown /></el-icon>
                <span>{{ Math.abs(statsData.responseTimeChangeRate).toFixed(1) }}%</span>
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="statistics-card red-card">
            <div class="statistics-icon red">
              <el-icon><Warning /></el-icon>
            </div>
            <div class="statistics-info">
              <div class="stat-header">
                <div class="stat-title">错误数</div>
              </div>
              <div class="statistics-value">{{ statsData.errorCount }}<span class="stat-unit">个</span></div>
              <div class="stat-trend" :class="{ down: statsData.errorChangeRate < 0, up: statsData.errorChangeRate >= 0, good: statsData.errorChangeRate < 0 }">
                <el-icon v-if="statsData.errorChangeRate >= 0"><ArrowUp /></el-icon>
                <el-icon v-else><ArrowDown /></el-icon>
                <span>{{ Math.abs(statsData.errorChangeRate).toFixed(1) }}%</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>接口数据源</span>
          <el-button type="primary" @click="handleAdd">新建数据源</el-button>
        </div>
      </template>

      <el-table :data="tableData" style="width: 100%">
        <el-table-column prop="name" label="数据源名称" />
        <el-table-column prop="method" label="请求方式">
          <template #default="scope">
            <el-tag :type="getMethodType(scope.row.method)">
              {{ scope.row.method }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="url" label="接口地址" show-overflow-tooltip />
        <el-table-column prop="updateInterval" label="更新周期" />
        <el-table-column prop="lastUpdateTime" label="最后更新时间" />
        <el-table-column prop="status" label="状态">
          <template #default="scope">
            <el-tag :type="scope.row.status === '正常' ? 'success' : 'warning'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="scope">
            <el-button link type="primary" @click="handleTest(scope.row)">测试</el-button>
            <el-button link type="primary" @click="handlePreview(scope.row)">预览</el-button>
            <el-button link type="primary" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 新建/编辑数据源弹窗 -->
      <el-dialog
          v-model="dialogVisible"
          :title="dialogType === 'add' ? '新建数据源' : '编辑数据源'"
          width="800px"
          :close-on-click-modal="false"
      >
        <!-- 步骤条 -->
        <el-steps :active="currentStep" finish-status="success" class="steps">
          <el-step title="配置信息" />
          <el-step title="预览数据" />
        </el-steps>

        <!-- 步骤1：配置信息（合并基础配置和参数设置） -->
        <div v-if="currentStep === 0" class="step-content">
          <el-form
              ref="basicFormRef"
              :model="form"
              :rules="basicRules"
              label-width="120px"
              class="config-form"
          >
            <!-- 基础信息 -->
            <div class="form-section">
              <div class="section-header">
                <h3>基础信息</h3>
              </div>

              <el-form-item label="数据源名称" prop="name">
                <el-input v-model="form.name" placeholder="请输入数据源名称" />
              </el-form-item>

              <el-form-item label="请求方式" prop="method">
                <el-select v-model="form.method" placeholder="请选择请求方式">
                  <el-option label="GET" value="GET" />
                  <el-option label="POST" value="POST" />
                  <el-option label="PUT" value="PUT" />
                  <el-option label="DELETE" value="DELETE" />
                </el-select>
              </el-form-item>

              <el-form-item label="接口地址" prop="url">
                <el-input v-model="form.url" placeholder="请输入接口地址" />
              </el-form-item>

              <el-form-item label="更新周期" prop="updateInterval">
                <el-select v-model="form.updateInterval" placeholder="请选择更新周期">
                  <el-option label="实时" value="realtime" />
                  <el-option label="每分钟" value="1m" />
                  <el-option label="每小时" value="1h" />
                  <el-option label="每天" value="1d" />
                  <el-option label="每周" value="1w" />
                  <el-option label="每月" value="1M" />
                </el-select>
              </el-form-item>

              <el-form-item label="描述" prop="description">
                <el-input
                    v-model="form.description"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入描述信息"
                />
              </el-form-item>
            </div>

            <!-- 参数设置 -->
            <div class="form-section">
              <div class="section-header">
                <h3>参数设置</h3>
              </div>

              <el-form-item label="请求头">
                <el-button type="primary" link @click="addHeader">添加请求头</el-button>
                <div class="params-list">
                  <div v-for="(header, index) in form.headers" :key="index" class="params-item">
                    <el-input v-model="header.key" placeholder="Key" class="param-key" />
                    <el-input v-model="header.value" placeholder="Value" class="param-value" />
                    <el-button type="danger" link @click="removeHeader(index)">删除</el-button>
                  </div>
                </div>
              </el-form-item>

              <el-form-item v-if="form.method !== 'GET'" label="请求体">
                <el-input
                    v-model="form.requestBody"
                    type="textarea"
                    :rows="8"
                    placeholder="请输入请求体 (JSON 格式)"
                />
              </el-form-item>

              <el-form-item label="成功条件">
                <el-input
                    v-model="form.successCondition"
                    placeholder="请输入请求成功的条件表达式，如: response.code === 0"
                />
              </el-form-item>
            </div>

            <!-- 测试连接 -->
            <div class="form-section">
              <div class="section-header">
                <h3>测试连接</h3>
              </div>
              <div class="test-wrapper">
                <el-button type="primary" @click="handleTest">
                  <el-icon><Connection /></el-icon>
                  测试连接
                </el-button>
                <div v-if="testResult" class="test-result">
                  <el-alert
                      :title="testResult.success ? '连接成功' : '连接失败'"
                      :type="testResult.success ? 'success' : 'error'"
                      :description="testResult.message"
                      show-icon
                  />
                </div>
              </div>
            </div>
          </el-form>
        </div>

        <!-- 步骤2：预览数据 -->
        <div v-if="currentStep === 1" class="step-content">
          <div class="preview-tabs">
            <el-tabs v-model="activeTab">
              <el-tab-pane label="数据表格" name="table">
                <div v-if="!previewData.length" class="no-data">
                  <el-empty description="暂无数据" />
                </div>
                <el-table
                    v-else
                    :data="previewData"
                    style="width: 100%"
                    height="400"
                    border
                >
                  <el-table-column
                      v-for="col in previewColumns"
                      :key="col.prop"
                      :prop="col.prop"
                      :label="col.label"
                      show-overflow-tooltip
                  />
                </el-table>
              </el-tab-pane>
              <el-tab-pane label="JSON" name="json">
                <div class="json-preview">
                  <pre v-if="previewJson">{{ previewJson }}</pre>
                  <el-empty v-else description="暂无数据" />
                </div>
              </el-tab-pane>
              <el-tab-pane label="原始响应" name="raw">
                <div class="json-preview">
                  <pre>{{ testResult?.body || '暂无数据' }}</pre>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>

        <!-- 弹窗底部按钮 -->
        <template #footer>
          <div class="dialog-footer">
            <el-button v-if="currentStep > 0" @click="currentStep--">上一步</el-button>
            <el-button
                v-if="currentStep < 1"
                type="primary"
                @click="handleNext"
            >
              下一步
            </el-button>
            <el-button
                v-else
                type="primary"
                @click="handleFinish"
            >
              完成
            </el-button>
            <el-button @click="dialogVisible = false">取消</el-button>
          </div>
        </template>
      </el-dialog>

      <!-- 数据预览对话框 -->
      <el-dialog
          v-model="previewVisible"
          title="数据预览"
          width="800px"
      >
        <el-tabs v-model="previewTab">
          <el-tab-pane label="原始数据" name="raw">
            <pre class="preview-json">{{ previewRawData }}</pre>
          </el-tab-pane>
          <el-tab-pane label="表格视图" name="table">
            <el-table :data="previewData" border style="width: 100%">
              <el-table-column
                  v-for="col in previewColumns"
                  :key="col.prop"
                  :prop="col.prop"
                  :label="col.label"
              />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getApiDsList,
  createApiDs,
  updateApiDs,
  deleteApiDs,
  testApiDs,
  previewApiData,
  getApiSourceStats
} from '@/api/datasource'
import {
  Connection,
  CircleCheckFilled,
  Timer,
  Warning,
  ArrowUp,
  ArrowDown
} from '@element-plus/icons-vue'

const dialogVisible = ref(false)
const previewVisible = ref(false)
const dialogType = ref('add')
const currentStep = ref(0)
const basicFormRef = ref(null)
const previewTab = ref('raw')
const testResult = ref(null)
const loading = ref(false)

// 表格数据
const tableData = ref([])

// 统计数据
const statsData = ref({
  totalApis: 0,
  successRate: 0,
  avgResponseTime: 0,
  errorCount: 0,
  apiGrowthRate: 0,
  errorChangeRate: 0,
  responseTimeChangeRate: 0
})

// 表单数据
const form = ref({
  name: '',
  method: 'GET',
  url: '',
  headers: [],
  requestBody: '',
  updateInterval: '',
  successCondition: '',
  description: ''
})

// 预览数据
const previewData = ref([])
const previewRawData = ref('')

// 预览相关的响应式变量
const activeTab = ref('table')
const previewJson = ref('')

// 基础配置校验规则
const basicRules = {
  name: [{required: true, message: '请输入数据源名称', trigger: 'blur'}],
  method: [{required: true, message: '请选择请求方式', trigger: 'change'}],
  url: [{required: true, message: '请输入接口地址', trigger: 'blur'}],
  updateInterval: [{required: true, message: '请选择更新周期', trigger: 'change'}]
}

// 根据响应数据生成表格列
const previewColumns = computed(() => {
  if (!previewData.value.length) return []
  const firstRow = previewData.value[0]
  return Object.keys(firstRow).map(key => ({
    prop: key,
    label: key
  }))
})

// 获取请求方式对应的标签类型
const getMethodType = (method) => {
  const types = {
    GET: 'success',
    POST: 'warning',
    PUT: 'primary',
    DELETE: 'danger'
  }
  return types[method] || 'info'
}

// 获取数据源列表
const loadData = async () => {
  try {
    loading.value = true
    const res = await getApiDsList()
    if (res.code === 0) {
      tableData.value = res.data
    }
  } catch (error) {
    console.error('获取数据源列表失败：', error)
    ElMessage.error('获取数据源列表失败')
  } finally {
    loading.value = false
  }
}

// 获取统计数据
const loadStats = async () => {
  try {
    const res = await getApiSourceStats()
    if (res.code === 0) {
      statsData.value = res.data
    }
  } catch (error) {
    console.error('获取统计数据失败：', error)
  }
}

// 新建数据源
const handleAdd = () => {
  dialogType.value = 'add'
  currentStep.value = 0
  form.value = {
    name: '',
    method: 'GET',
    url: '',
    headers: [],
    requestBody: '',
    updateInterval: '',
    successCondition: '',
    description: ''
  }
  testResult.value = null
  dialogVisible.value = true
}

// 编辑数据源
const handleEdit = (row) => {
  dialogType.value = 'edit'
  currentStep.value = 0
  form.value = JSON.parse(JSON.stringify(row))
  // 解析headers字符串为数组
  try {
    form.value.headers = row.headers ? JSON.parse(row.headers) : []
  } catch (e) {
    form.value.headers = []
    console.error('解析headers失败:', e)
  }
  testResult.value = null
  dialogVisible.value = true
}

// 测试接口
const handleTest = async () => {
  try {
    const testData = {
      ...form.value,
      headers: JSON.stringify(form.value.headers || [])
    }
    const res = await testApiDs(testData)
    if (res.code === 0) {
      const data = res.data
      testResult.value = {
        success: data.success,
        status: data.status,
        headers: data.headers,
        body: data.body,
        message: data.success ? '连接成功' : (data.error || '连接失败')
      }
      if (data.success) {
        ElMessage.success('测试成功')
      } else {
        ElMessage.warning(data.error || '测试失败')
      }
    } else {
      testResult.value = {
        success: false,
        message: res.msg || '测试失败'
      }
      ElMessage.error(res.msg || '测试失败')
    }
  } catch (error) {
    console.error('测试失败：', error)
    testResult.value = {
      success: false,
      message: error.message || '测试失败'
    }
    ElMessage.error('测试失败')
  }
}

// 预览数据
const handlePreview = async (row) => {
  try {
    loading.value = true
    const res = await previewApiData(row.id)
    if (res.code === 0) {
      previewRawData.value = JSON.stringify(res.data, null, 2)
      if (res.data.body) {
        try {
          const bodyData = JSON.parse(res.data.body)
          if (Array.isArray(bodyData)) {
            previewData.value = bodyData
          } else if (bodyData.data && Array.isArray(bodyData.data)) {
            previewData.value = bodyData.data
          } else {
            previewData.value = [bodyData]
          }
        } catch (e) {
          previewData.value = []
        }
      }
      previewVisible.value = true
    }
  } catch (error) {
    console.error('预览数据失败：', error)
    ElMessage.error('预览数据失败')
  } finally {
    loading.value = false
  }
}

// 删除数据源
const handleDelete = (row) => {
  ElMessageBox.confirm(
      '确定要删除该数据源吗？',
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      const res = await deleteApiDs(row.id)
      if (res.code === 0) {
        ElMessage.success('删除成功')
        loadData()
      }
    } catch (error) {
      console.error('删除失败：', error)
      ElMessage.error('删除失败')
    }
  }).catch(() => {
  })
}

// 添加请求头
const addHeader = () => {
  form.value.headers.push({key: '', value: ''})
}

// 删除请求头
const removeHeader = (index) => {
  form.value.headers.splice(index, 1)
}

// 完成配置
const handleFinish = async () => {
  if (!testResult.value?.success) {
    ElMessage.warning('请先进行接口测试并确保测试成功')
    return
  }
  try {
    const data = {
      ...form.value,
      headers: JSON.stringify(form.value.headers || [])
    }
    const api = dialogType.value === 'add' ? createApiDs : updateApiDs
    const res = await api(data)
    if (res.code === 0) {
      ElMessage.success(dialogType.value === 'add' ? '创建成功' : '更新成功')
      dialogVisible.value = false
      loadData()
    }
  } catch (error) {
    console.error('保存失败：', error)
    ElMessage.error('保存失败')
  }
}

// 下一步
const handleNext = async () => {
  if (currentStep.value === 0) {
    try {
      await basicFormRef.value.validate()
      if (!testResult.value?.success) {
        ElMessage.warning('请先测试接口连接')
        return
      }
      // 解析测试结果中的数据
      if (testResult.value.body) {
        try {
          const bodyData = JSON.parse(testResult.value.body)
          if (Array.isArray(bodyData)) {
            previewData.value = bodyData
          } else if (bodyData.data && Array.isArray(bodyData.data)) {
            previewData.value = bodyData.data
          } else {
            previewData.value = [bodyData]
          }
          // 设置JSON预览
          previewJson.value = JSON.stringify(bodyData, null, 2)
        } catch (e) {
          console.error('解析响应数据失败：', e)
          previewData.value = []
          previewJson.value = testResult.value.body
        }
      } else {
        previewData.value = []
        previewJson.value = ''
      }
      currentStep.value++
    } catch (error) {
      console.error('表单验证失败：', error)
    }
  }
}

// 页面加载时获取数据
loadData()
loadStats()
</script>

<style scoped>
.search-wrapper {
  background-color: #fff;
  padding: 20px;
  border-radius: 4px;
  margin-bottom: 16px;
}

.statistics-area {
  margin-bottom: 20px;
  
  .statistics-card {
    display: flex;
    align-items: center;
    padding: 16px;
    transition: all 0.3s;
    border-left-width: 4px;
    border-left-style: solid;
    background-color: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    }
    
    &.blue-card {
      border-left-color: #409EFF;
    }
    
    &.green-card {
      border-left-color: #67C23A;
    }
    
    &.orange-card {
      border-left-color: #E6A23C;
    }
    
    &.red-card {
      border-left-color: #F56C6C;
    }
  }

  .statistics-icon {
    width: 48px;
    height: 48px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-right: 16px;

    .el-icon {
      font-size: 24px;
      color: #fff;
    }
    
    &.blue {
      background-color: #409EFF;
    }
    
    &.green {
      background-color: #67C23A;
    }
    
    &.orange {
      background-color: #E6A23C;
    }
    
    &.red {
      background-color: #F56C6C;
    }
  }

  .statistics-info {
    flex: 1;

    .stat-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;

      .stat-title {
        font-size: 14px;
        color: var(--el-text-color-secondary);
        font-weight: 500;
      }
    }

    .statistics-value {
      font-size: 24px;
      font-weight: bold;
      color: var(--el-text-color-primary);
      
      .stat-unit {
        font-size: 14px;
        font-weight: normal;
        color: var(--el-text-color-secondary);
        margin-left: 2px;
      }
    }
    
    .stat-trend {
      display: flex;
      align-items: center;
      font-size: 12px;
      margin-top: 4px;
      
      &.up {
        color: #67C23A;
        
        &.warning {
          color: #F56C6C;
        }
      }
      
      &.down {
        color: #F56C6C;
        
        &.good {
          color: #67C23A;
        }
      }
      
      :deep(svg) {
        margin-right: 4px;
      }
    }
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.params-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.params-item {
  display: flex;
  gap: 10px;
  align-items: center;
}

.param-key,
.param-value {
  flex: 1;
}

.preview-json {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  font-family: monospace;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.steps {
  margin: 20px 0;
}

.step-content {
  min-height: 300px;
  margin: 20px 0;
}

.config-form {
  /* Add your styles here */
}

.preview-tabs {
  /* Add your styles here */
}

.json-preview {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  font-family: monospace;
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 400px;
  overflow-y: auto;
}

.no-data {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 400px;
}

.test-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  align-items: center;
}

.test-result {
  width: 100%;
  margin-top: 16px;

  .el-alert {
    margin-bottom: 0;
  }
}
</style>

