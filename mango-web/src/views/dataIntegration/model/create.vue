<template>
  <div class="task-form">
    <div class="page-header">
      <div class="header-left">
        <el-button link @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>返回
        </el-button>
        <h2 class="page-title">{{ isEdit ? '编辑任务' : '新建任务' }}</h2>
      </div>
      <div class="header-right">
        <el-button-group>
          <el-button @click="handleCancel">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
        </el-button-group>
      </div>
    </div>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
      class="task-form-content"
    >
      <!-- 基本信息 -->
      <el-card class="form-card">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
          </div>
        </template>
        
        <el-form-item label="任务名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入任务名称" />
        </el-form-item>
        
        <el-form-item label="任务类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择任务类型">
            <el-option label="Delta Lake 同步" value="delta" />
            <el-option label="StarRocks 同步" value="starrocks" />
            <el-option label="实时数据集成" value="realtime" />
            <el-option label="批量数据集成" value="batch" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            rows="3"
            placeholder="请输入任务描述"
          />
        </el-form-item>
      </el-card>

      <!-- 源配置 -->
      <el-card class="form-card">
        <template #header>
          <div class="card-header">
            <span>源配置</span>
            <el-button link type="primary" @click="handleTestSource">
              测试连接
            </el-button>
          </div>
        </template>
        
        <el-form-item label="数据源类型" prop="source.type">
          <el-select v-model="formData.source.type" placeholder="请选择数据源类型">
            <el-option label="Delta Lake" value="delta" />
            <el-option label="StarRocks" value="starrocks" />
            <el-option label="MySQL" value="mysql" />
            <el-option label="Kafka" value="kafka" />
          </el-select>
        </el-form-item>

        <template v-if="formData.source.type === 'delta'">
          <el-form-item label="存储路径" prop="source.path">
            <el-input v-model="formData.source.path" placeholder="请输入Delta Lake存储路径" />
          </el-form-item>
          <el-form-item label="版本控制">
            <el-switch v-model="formData.source.versionControl" />
          </el-form-item>
        </template>

        <template v-else-if="formData.source.type === 'starrocks'">
          <el-form-item label="主机地址" prop="source.host">
            <el-input v-model="formData.source.host" placeholder="请输入主机地址" />
          </el-form-item>
          <el-form-item label="端口" prop="source.port">
            <el-input-number v-model="formData.source.port" :min="1" :max="65535" />
          </el-form-item>
          <el-form-item label="数据库" prop="source.database">
            <el-input v-model="formData.source.database" placeholder="请输入数据库名" />
          </el-form-item>
          <el-form-item label="用户名" prop="source.username">
            <el-input v-model="formData.source.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="source.password">
            <el-input
              v-model="formData.source.password"
              type="password"
              placeholder="请输入密码"
              show-password
            />
          </el-form-item>
        </template>

        <template v-else-if="formData.source.type === 'mysql'">
          <el-form-item label="主机地址" prop="source.host">
            <el-input v-model="formData.source.host" placeholder="请输入主机地址" />
          </el-form-item>
          <el-form-item label="端口" prop="source.port">
            <el-input-number v-model="formData.source.port" :min="1" :max="65535" />
          </el-form-item>
          <el-form-item label="数据库" prop="source.database">
            <el-input v-model="formData.source.database" placeholder="请输入数据库名" />
          </el-form-item>
          <el-form-item label="用户名" prop="source.username">
            <el-input v-model="formData.source.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="source.password">
            <el-input
              v-model="formData.source.password"
              type="password"
              placeholder="请输入密码"
              show-password
            />
          </el-form-item>
        </template>

        <template v-else-if="formData.source.type === 'kafka'">
          <el-form-item label="Broker列表" prop="source.brokers">
            <el-input v-model="formData.source.brokers" placeholder="请输入Kafka Broker列表" />
          </el-form-item>
          <el-form-item label="Topic" prop="source.topic">
            <el-input v-model="formData.source.topic" placeholder="请输入Topic" />
          </el-form-item>
          <el-form-item label="消费组" prop="source.group">
            <el-input v-model="formData.source.group" placeholder="请输入消费组ID" />
          </el-form-item>
        </template>
      </el-card>

      <!-- 目标配置 -->
      <el-card class="form-card">
        <template #header>
          <div class="card-header">
            <span>目标配置</span>
            <el-button link type="primary" @click="handleTestTarget">
              测试连接
            </el-button>
          </div>
        </template>
        
        <el-form-item label="目标类型" prop="target.type">
          <el-select v-model="formData.target.type" placeholder="请选择目标类型">
            <el-option label="Delta Lake" value="delta" />
            <el-option label="StarRocks" value="starrocks" />
          </el-select>
        </el-form-item>

        <template v-if="formData.target.type === 'delta'">
          <el-form-item label="存储路径" prop="target.path">
            <el-input v-model="formData.target.path" placeholder="请输入Delta Lake存储路径" />
          </el-form-item>
          <el-form-item label="写入模式" prop="target.writeMode">
            <el-radio-group v-model="formData.target.writeMode">
              <el-radio value="append">追加</el-radio>
              <el-radio value="overwrite">覆盖</el-radio>
              <el-radio value="upsert">更新插入</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="分区字段" v-if="formData.target.writeMode !== 'append'">
            <el-select
              v-model="formData.target.partitionFields"
              multiple
              placeholder="请选择分区字段"
            >
              <el-option
                v-for="field in availableFields"
                :key="field.name"
                :label="field.label"
                :value="field.name"
              />
            </el-select>
          </el-form-item>
        </template>

        <template v-else-if="formData.target.type === 'starrocks'">
          <el-form-item label="主机地址" prop="target.host">
            <el-input v-model="formData.target.host" placeholder="请输入主机地址" />
          </el-form-item>
          <el-form-item label="端口" prop="target.port">
            <el-input-number v-model="formData.target.port" :min="1" :max="65535" />
          </el-form-item>
          <el-form-item label="数据库" prop="target.database">
            <el-input v-model="formData.target.database" placeholder="请输入数据库名" />
          </el-form-item>
          <el-form-item label="表名" prop="target.table">
            <el-input v-model="formData.target.table" placeholder="请输入表名" />
          </el-form-item>
          <el-form-item label="用户名" prop="target.username">
            <el-input v-model="formData.target.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="target.password">
            <el-input
              v-model="formData.target.password"
              type="password"
              placeholder="请输入密码"
              show-password
            />
          </el-form-item>
          <el-form-item label="导入模式" prop="target.importMode">
            <el-radio-group v-model="formData.target.importMode">
              <el-radio value="append">追加</el-radio>
              <el-radio value="overwrite">覆盖</el-radio>
              <el-radio value="merge">合并</el-radio>
            </el-radio-group>
          </el-form-item>
        </template>
      </el-card>

      <!-- 调度配置 -->
      <el-card class="form-card">
        <template #header>
          <div class="card-header">
            <span>调度配置</span>
          </div>
        </template>
        
        <el-form-item label="调度类型" prop="schedule.type">
          <el-radio-group v-model="formData.schedule.type">
            <el-radio value="manual">手动触发</el-radio>
            <el-radio value="once">单次执行</el-radio>
            <el-radio value="periodic">周期执行</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="formData.schedule.type === 'once'">
          <el-form-item label="执行时间" prop="schedule.executeTime">
            <el-date-picker
              v-model="formData.schedule.executeTime"
              type="datetime"
              placeholder="请选择执行时间"
            />
          </el-form-item>
        </template>

        <template v-else-if="formData.schedule.type === 'periodic'">
          <el-form-item label="调度周期" prop="schedule.period">
            <el-select v-model="formData.schedule.period" placeholder="请选择调度周期">
              <el-option label="分钟" value="minute" />
              <el-option label="小时" value="hour" />
              <el-option label="天" value="day" />
              <el-option label="周" value="week" />
              <el-option label="月" value="month" />
            </el-select>
          </el-form-item>

          <el-form-item label="间隔" prop="schedule.interval">
            <el-input-number v-model="formData.schedule.interval" :min="1" />
          </el-form-item>

          <el-form-item label="开始时间" prop="schedule.startTime">
            <el-date-picker
              v-model="formData.schedule.startTime"
              type="datetime"
              placeholder="请选择开始时间"
            />
          </el-form-item>

          <el-form-item label="结束时间" prop="schedule.endTime">
            <el-date-picker
              v-model="formData.schedule.endTime"
              type="datetime"
              placeholder="请选择结束时间"
            />
          </el-form-item>
        </template>
      </el-card>

      <!-- 高级配置 -->
      <el-card class="form-card">
        <template #header>
          <div class="card-header">
            <span>高级配置</span>
          </div>
        </template>
        
        <el-form-item label="并行度" prop="advanced.parallelism">
          <el-input-number
            v-model="formData.advanced.parallelism"
            :min="1"
            :max="32"
          />
        </el-form-item>

        <el-form-item label="批次大小" prop="advanced.batchSize">
          <el-input-number
            v-model="formData.advanced.batchSize"
            :min="100"
            :max="10000"
            :step="100"
          />
        </el-form-item>

        <el-form-item label="重试次数" prop="advanced.retryTimes">
          <el-input-number
            v-model="formData.advanced.retryTimes"
            :min="0"
            :max="10"
          />
        </el-form-item>

        <el-form-item label="超时时间(秒)" prop="advanced.timeout">
          <el-input-number
            v-model="formData.advanced.timeout"
            :min="0"
            :max="3600"
          />
        </el-form-item>

        <el-form-item label="错误处理">
          <el-radio-group v-model="formData.advanced.errorHandling">
            <el-radio value="continue">继续执行</el-radio>
            <el-radio value="stop">停止执行</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-card>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)

// 是否为编辑模式
const isEdit = computed(() => route.params.id !== undefined)

// 表单数据
const formData = reactive({
  name: '',
  type: '',
  description: '',
  source: {
    type: '',
    path: '',
    versionControl: false,
    host: '',
    port: 3306,
    database: '',
    username: '',
    password: '',
    brokers: '',
    topic: '',
    group: ''
  },
  target: {
    type: '',
    path: '',
    writeMode: 'append',
    partitionFields: [],
    host: '',
    port: 9030,
    database: '',
    table: '',
    username: '',
    password: '',
    importMode: 'append'
  },
  schedule: {
    type: 'manual',
    executeTime: null,
    period: 'hour',
    interval: 1,
    startTime: null,
    endTime: null
  },
  advanced: {
    parallelism: 1,
    batchSize: 1000,
    retryTimes: 3,
    timeout: 3600,
    errorHandling: 'stop'
  }
})

// 表单验证规则
const formRules = {
  name: [
    { required: true, message: '请输入任务名称', trigger: 'blur' },
    { min: 3, max: 50, message: '长度在 3 到 50 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择任务类型', trigger: 'change' }
  ],
  'source.type': [
    { required: true, message: '请选择数据源类型', trigger: 'change' }
  ],
  'source.path': [
    { required: true, message: '请输入存储路径', trigger: 'blur' }
  ],
  'source.host': [
    { required: true, message: '请输入主机地址', trigger: 'blur' }
  ],
  'source.port': [
    { required: true, message: '请输入端口号', trigger: 'blur' }
  ],
  'source.database': [
    { required: true, message: '请输入数据库名', trigger: 'blur' }
  ],
  'source.username': [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  'source.password': [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ],
  'target.type': [
    { required: true, message: '请选择目标类型', trigger: 'change' }
  ]
}

// 可用字段列表（模拟数据）
const availableFields = [
  { name: 'id', label: 'ID' },
  { name: 'name', label: '名称' },
  { name: 'type', label: '类型' },
  { name: 'created_at', label: '创建时间' },
  { name: 'updated_at', label: '更新时间' }
]

// 测试源连接
const handleTestSource = async () => {
  try {
    // TODO: 调用API测试源连接
    ElMessage.success('连接测试成功')
  } catch (error) {
    ElMessage.error('连接测试失败')
  }
}

// 测试目标连接
const handleTestTarget = async () => {
  try {
    // TODO: 调用API测试目标连接
    ElMessage.success('连接测试成功')
  } catch (error) {
    ElMessage.error('连接测试失败')
  }
}

// 返回上一页
const handleBack = () => {
  router.back()
}

// 取消
const handleCancel = () => {
  ElMessageBox.confirm(
    '确认要取消编辑吗？未保存的内容将会丢失。',
    '提示',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    router.back()
  }).catch(() => {})
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  try {
    await formRef.value.validate()
    
    submitting.value = true
    // TODO: 调用API保存任务
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    ElMessage.success(isEdit.value ? '保存成功' : '创建成功')
    router.push('/dataIntegration')
  } catch (error) {
    console.error('表单验证失败:', error)
  } finally {
    submitting.value = false
  }
}

// 如果是编辑模式，加载任务数据
onMounted(async () => {
  if (isEdit.value) {
    try {
      // TODO: 调用API获取任务详情
      const taskData = {
        name: '示例任务',
        type: 'delta',
        description: '这是一个示例任务',
        source: {
          type: 'mysql',
          host: 'localhost',
          port: 3306,
          database: 'test',
          username: 'root'
        },
        target: {
          type: 'delta',
          path: '/data/delta/example',
          writeMode: 'append'
        },
        schedule: {
          type: 'periodic',
          period: 'hour',
          interval: 1
        },
        advanced: {
          parallelism: 2,
          batchSize: 1000,
          retryTimes: 3,
          timeout: 3600,
          errorHandling: 'stop'
        }
      }
      
      Object.assign(formData, taskData)
    } catch (error) {
      ElMessage.error('加载任务数据失败')
      console.error('加载任务数据失败:', error)
    }
  }
})
</script>

<style lang="scss" scoped>
.task-form {
  padding: 20px;

  .page-header {
    margin-bottom: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .page-title {
        font-size: 24px;
        color: #2c3e50;
        margin: 0;
      }
    }
  }

  .task-form-content {
    max-width: 1000px;
    margin: 0 auto;

    .form-card {
      margin-bottom: 20px;

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      :deep(.el-form-item:last-child) {
        margin-bottom: 0;
      }
    }
  }
}
</style> 