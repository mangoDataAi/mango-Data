<template>
  <div class="app-container">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="router.push('/dataIntegration/source/database/create')">
        新建连接
      </el-button>
    </div>

    <!-- 数据源列表 -->
    <el-table :data="dataList" style="width: 100%; margin-top: 20px">
      <el-table-column prop="name" label="连接名称" />
      <el-table-column prop="type" label="数据源类型" />
      <el-table-column prop="url" label="连接地址" show-overflow-tooltip />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'danger'">
            {{ row.status ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250">
        <template #default="{ row }">
          <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
          <el-button type="primary" link @click="handleTest(row)">测试连接</el-button>
          <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item label="数据源类型" prop="dbType">
          <el-select v-model="form.dbType" placeholder="请选择数据源类型" @change="handleDbTypeChange">
            <el-option-group label="关系型数据库">
              <el-option label="达梦数据库" value="dm" />
              <el-option label="MySQL" value="mysql" />
              <el-option label="Oracle" value="oracle" />
            </el-option-group>
            <el-option-group label="文件系统">
              <el-option label="FTP" value="ftp" />
              <el-option label="SFTP" value="sftp" />
              <el-option label="HDFS" value="hdfs" />
              <el-option label="MinIO" value="minio" />
              <el-option label="S3" value="s3" />
              <el-option label="OSS" value="oss" />
              <el-option label="本地文件" value="file" />
            </el-option-group>
          </el-select>
        </el-form-item>

        <!-- 数据库连接表单 -->
        <template v-if="!isFileSystem">
          <el-form-item label="连接地址" prop="url">
            <el-input v-model="form.url" placeholder="请输入数据库连接地址">
              <template #prepend>
                <span>{{ urlPrefix }}</span>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入数据库用户名" />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入数据库密码"
              show-password
            />
          </el-form-item>

          <el-form-item label="初始连接数" prop="initialSize">
            <el-input-number v-model="form.initialSize" :min="1" :max="100" />
          </el-form-item>

          <el-form-item label="最小连接数" prop="minIdle">
            <el-input-number v-model="form.minIdle" :min="1" :max="100" />
          </el-form-item>

          <el-form-item label="最大连接数" prop="maxActive">
            <el-input-number v-model="form.maxActive" :min="1" :max="1000" />
          </el-form-item>
        </template>

        <!-- 文件系统连接表单 -->
        <template v-else>
          <el-form-item label="主机地址" prop="host">
            <el-input v-model="form.host" placeholder="请输入主机地址" />
          </el-form-item>
          <el-form-item label="端口" prop="port">
            <el-input v-model="form.port" placeholder="请输入端口号" />
          </el-form-item>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
          </el-form-item>
          <el-form-item v-if="form.dbType === 'file'" label="文件路径" prop="path">
            <el-input v-model="form.path" placeholder="请输入文件路径" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="handleSubmit">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getDsList, testConnection, saveConfig } from '@/api/datasource'
import { useRouter } from 'vue-router'

const router = useRouter()
const formRef = ref(null)
const dialogVisible = ref(false)
const dialogTitle = ref('新建连接')
const dataList = ref([])

// 数据库驱动映射
const driverMap = {
  dm: 'dm.jdbc.driver.DmDriver',
  mysql: 'com.mysql.cj.jdbc.Driver',
  oracle: 'oracle.jdbc.OracleDriver'
}

// URL前缀映射
const urlPrefixMap = {
  dm: 'jdbc:dm://',
  mysql: 'jdbc:mysql://',
  oracle: 'jdbc:oracle:thin:@'
}

const form = reactive({
  dbType: '',
  url: '',
  host: '',
  port: '',
  username: '',
  password: '',
  path: '',
  initialSize: 5,
  minIdle: 10,
  maxActive: 50,
  maxWait: 60000,
  validationQuery: 'SELECT 1',
  testOnBorrow: true,
  testOnReturn: false,
  testWhileIdle: true
})

const urlPrefix = computed(() => urlPrefixMap[form.dbType] || '')

const isFileSystem = computed(() => {
  const fileTypes = ['ftp', 'sftp', 'hdfs', 'minio', 's3', 'oss', 'file']
  return fileTypes.includes(form.dbType)
})

const rules = {
  dbType: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
  ...(isFileSystem.value ? {
    host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
    port: [{ required: true, message: '请输入端口号', trigger: 'blur' }],
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
    path: [{ required: form.dbType === 'file', message: '请输入文件路径', trigger: 'blur' }]
  } : {
    url: [{ required: true, message: '请输入数据库连接地址', trigger: 'blur' }],
    username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
    password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
  })
}

// 新建连接
const handleAdd = () => {
  dialogTitle.value = '新建连接'
  form.dbType = ''
  form.url = ''
  form.host = ''
  form.port = ''
  form.username = ''
  form.password = ''
  form.path = ''
  form.initialSize = 5
  form.minIdle = 10
  form.maxActive = 50
  dialogVisible.value = true
}

const handleDbTypeChange = (value) => {
  // 切换数据源类型时清空相关字段
  form.url = ''
  form.host = ''
  form.port = ''
  form.username = ''
  form.password = ''
  form.path = ''
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    const config = {
      ...form,
      ...(isFileSystem.value ? {} : {
        driverClassName: driverMap[form.dbType],
        url: urlPrefix.value + form.url
      })
    }

    const res = await saveConfig(config)
    if (res.code === 0) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      loadData()
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error(error.message || '保存失败')
  }
}

const handleTest = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()

    const config = {
      ...form,
      ...(isFileSystem.value ? {} : {
        driverClassName: driverMap[form.dbType],
        url: urlPrefix.value + form.url
      })
    }

    const res = await testConnection(config)
    if (res.code === 0) {
      ElMessage.success('连接成功')
    }
  } catch (error) {
    console.error('测试连接失败:', error)
    ElMessage.error(error.message || '连接失败')
  }
}

// 获取数据源列表
const loadData = async () => {
  try {
    const res = await getDsList()
    if (res.code === 0) {
      dataList.value = res.data
    }
  } catch (error) {
    console.error('获取数据源列表失败:', error)
    ElMessage.error('获取数据源列表失败')
  }
}

// 页面加载时获取数据
loadData()
</script>

<style lang="scss" scoped>
.app-container {
  padding: 20px;

  .toolbar {
    margin-bottom: 20px;
  }

  .el-input-number {
    width: 180px;
  }
}
</style>
