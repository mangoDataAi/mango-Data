<!-- 系统参数管理页面 -->
<template>
  <div class="params-container">
    <!-- 高级搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" label-width="100px" size="default">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-form-item label="参数名称">
              <el-input v-model="searchForm.name" placeholder="请输入参数名称" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="参数键名">
              <el-input v-model="searchForm.key" placeholder="请输入参数键名" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="参数类型">
              <el-select v-model="searchForm.type" placeholder="请选择参数类型" clearable>
                <el-option label="系统参数" value="system" />
                <el-option label="业务参数" value="business" />
                <el-option label="配置参数" value="config" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状态">
              <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
                <el-option label="启用" value="enabled" />
                <el-option label="禁用" value="disabled" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24" style="text-align: right">
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
            <el-button type="success" :icon="Plus" @click="handleAdd">新增参数</el-button>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 数据列表 -->
    <el-card class="list-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="paramsList"
        border
        stripe
        style="width: 100%"
        highlight-current-row
        @row-click="handleRowClick"
      >
        <el-table-column type="index" width="50" align="center" label="序号" />
        <el-table-column prop="name" label="参数名称" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tooltip :content="row.description" placement="top" :show-after="500">
              <span>{{ row.name }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="key" label="参数键名" min-width="150" show-overflow-tooltip />
        <el-table-column prop="value" label="参数值" min-width="150" show-overflow-tooltip />
        <el-table-column prop="type" label="参数类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getParamTypeTag(row.type)">{{ getParamTypeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="'enabled'"
              :inactive-value="'disabled'"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button type="primary" link :icon="Edit" @click.stop="handleEdit(row)">
                编辑
              </el-button>
              <el-button type="success" link :icon="View" @click.stop="handleView(row)">
                查看
              </el-button>
              <el-button type="danger" link :icon="Delete" @click.stop="handleDelete(row)">
                删除
              </el-button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增参数' : '编辑参数'"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="param-form"
      >
        <el-form-item label="参数名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入参数名称" />
        </el-form-item>
        <el-form-item label="参数键名" prop="key">
          <el-input v-model="form.key" placeholder="请输入参数键名" :disabled="dialogType === 'edit'" />
        </el-form-item>
        <el-form-item label="参数值" prop="value">
          <el-input
            v-model="form.value"
            type="textarea"
            :rows="3"
            placeholder="请输入参数值"
          />
        </el-form-item>
        <el-form-item label="参数类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择参数类型" style="width: 100%">
            <el-option label="系统参数" value="system" />
            <el-option label="业务参数" value="business" />
            <el-option label="配置参数" value="config" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio-button label="enabled">启用</el-radio-button>
            <el-radio-button label="disabled">禁用</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入参数描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 查看详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="参数详情"
      size="50%"
      destroy-on-close
    >
      <el-descriptions :column="1" border>
        <el-descriptions-item label="参数名称">{{ currentParam.name }}</el-descriptions-item>
        <el-descriptions-item label="参数键名">{{ currentParam.key }}</el-descriptions-item>
        <el-descriptions-item label="参数值">{{ currentParam.value }}</el-descriptions-item>
        <el-descriptions-item label="参数类型">
          <el-tag :type="getParamTypeTag(currentParam.type)">
            {{ getParamTypeLabel(currentParam.type) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentParam.status === 'enabled' ? 'success' : 'danger'">
            {{ currentParam.status === 'enabled' ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentParam.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentParam.updateTime }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ currentParam.description }}</el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <div style="flex: auto">
          <el-button @click="drawerVisible = false">关闭</el-button>
          <el-button type="primary" @click="handleEdit(currentParam)">编辑</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import {
  Search,
  Refresh,
  Plus,
  Edit,
  View,
  Delete
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 搜索表单
const searchForm = ref({
  name: '',
  key: '',
  type: '',
  status: ''
})

// 列表数据
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const paramsList = ref([
  {
    id: 1,
    name: '系统主题色',
    key: 'system.theme.primary',
    value: '#409EFF',
    type: 'system',
    status: 'enabled',
    description: '系统默认主题色配置',
    createTime: '2024-03-20 10:00:00',
    updateTime: '2024-03-20 10:00:00'
  },
  {
    id: 2,
    name: '数据保留天数',
    key: 'data.retention.days',
    value: '30',
    type: 'business',
    status: 'enabled',
    description: '数据保留天数配置',
    createTime: '2024-03-20 10:00:00',
    updateTime: '2024-03-20 10:00:00'
  }
])

// 表单对话框
const dialogVisible = ref(false)
const dialogType = ref('add')
const formRef = ref(null)
const form = reactive({
  name: '',
  key: '',
  value: '',
  type: '',
  status: 'enabled',
  description: ''
})

// 表单校验规则
const rules = {
  name: [
    { required: true, message: '请输入参数名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  key: [
    { required: true, message: '请输入参数键名', trigger: 'blur' },
    { pattern: /^[a-z][a-z0-9_.]*$/, message: '以小写字母开头，可包含数字、下划线和点', trigger: 'blur' }
  ],
  value: [
    { required: true, message: '请输入参数值', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择参数类型', trigger: 'change' }
  ]
}

// 详情抽屉
const drawerVisible = ref(false)
const currentParam = ref({})

// 方法定义
const handleSearch = () => {
  loading.value = true
  // 模拟搜索
  setTimeout(() => {
    loading.value = false
    ElMessage.success('搜索成功')
  }, 500)
}

const resetSearch = () => {
  searchForm.value = {
    name: '',
    key: '',
    type: '',
    status: ''
  }
}

const handleAdd = () => {
  dialogType.value = 'add'
  dialogVisible.value = true
  // 重置表单
  Object.assign(form, {
    name: '',
    key: '',
    value: '',
    type: '',
    status: 'enabled',
    description: ''
  })
}

const handleEdit = (row) => {
  dialogType.value = 'edit'
  dialogVisible.value = true
  drawerVisible.value = false
  // 填充表单
  Object.assign(form, row)
}

const handleView = (row) => {
  currentParam.value = row
  drawerVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确认删除参数"${row.name}"吗？`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // 模拟删除
    const index = paramsList.value.findIndex(item => item.id === row.id)
    if (index !== -1) {
      paramsList.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  }).catch(() => {})
}

const handleStatusChange = (row) => {
  const action = row.status === 'enabled' ? '启用' : '禁用'
  ElMessage.success(`${action}成功`)
}

const handleRowClick = (row) => {
  handleView(row)
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate((valid, fields) => {
    if (valid) {
      // 模拟提交
      if (dialogType.value === 'add') {
        const newParam = {
          id: paramsList.value.length + 1,
          ...form,
          createTime: new Date().toLocaleString(),
          updateTime: new Date().toLocaleString()
        }
        paramsList.value.unshift(newParam)
        ElMessage.success('添加成功')
      } else {
        const index = paramsList.value.findIndex(item => item.id === form.id)
        if (index !== -1) {
          paramsList.value[index] = {
            ...paramsList.value[index],
            ...form,
            updateTime: new Date().toLocaleString()
          }
          ElMessage.success('更新成功')
        }
      }
      dialogVisible.value = false
    } else {
      console.log('error submit!', fields)
    }
  })
}

const handleSizeChange = (val) => {
  pageSize.value = val
  // 重新加载数据
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  // 重新加载数据
}

const getParamTypeTag = (type) => {
  const types = {
    system: 'danger',
    business: 'warning',
    config: 'info'
  }
  return types[type] || 'info'
}

const getParamTypeLabel = (type) => {
  const types = {
    system: '系统参数',
    business: '业务参数',
    config: '配置参数'
  }
  return types[type] || '未知类型'
}
</script>

<style scoped>
.params-container {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
  
  :deep(.el-card__body) {
    padding-bottom: 0;
  }
}

.list-card {
  :deep(.el-card__body) {
    padding: 0;
  }
}

.pagination-container {
  padding: 15px;
  display: flex;
  justify-content: flex-end;
}

.param-form {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 10px;
}

:deep(.el-drawer__body) {
  padding: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style> 