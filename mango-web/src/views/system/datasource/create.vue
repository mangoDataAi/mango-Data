<template>
  <div class="datasource-create">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button link @click="router.back()">
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
          <el-divider direction="vertical" />
          <span>新建数据源</span>
        </div>
      </template>

      <el-steps :active="currentStep" finish-status="success" class="steps">
        <el-step title="选择数据源" />
        <el-step title="配置连接" />
        <el-step title="预览数据" />
      </el-steps>

      <!-- 步骤1：选择数据源类型 -->
      <div v-if="currentStep === 0" class="step-content">
        <div class="db-selector">
          <div v-for="(group, groupName) in groupedDatabases" :key="groupName" class="db-group">
            <h3>{{ groupName }}</h3>
            <div class="db-list">
              <div
                v-for="db in group"
                :key="db.value"
                class="db-item"
                :class="{ active: selectedDb === db.value }"
                @click="selectDatabase(db)"
              >
                <img :src="db.icon" :alt="db.label">
                <span>{{ db.label }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 步骤2：配置连接信息 -->
      <div v-if="currentStep === 1" class="step-content">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="120px"
          class="config-form"
        >
          <el-form-item label="数据源名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入数据源名称" />
          </el-form-item>

          <el-form-item label="连接地址" prop="url">
            <el-input v-model="form.url" placeholder="请输入连接地址">
              <template #prepend>
                <span>{{ urlPrefix }}</span>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="数据库名" prop="dbName">
            <el-input v-model="form.dbName" placeholder="请输入数据库名" />
          </el-form-item>

          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" placeholder="请输入用户名" />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleTest">测试连接</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 步骤3：预览数据 -->
      <div v-if="currentStep === 2" class="step-content">
        <div class="preview-wrapper">
          <!-- 左侧表列表 -->
          <div class="table-list">
            <div class="list-header">
              <span>数据表</span>
              <el-input
                v-model="searchText"
                placeholder="搜索表名"
                clearable
                @input="handleSearch"
              />
            </div>
            <el-scrollbar>
              <el-tree
                ref="treeRef"
                :data="tableList"
                :props="{ label: 'name' }"
                :filter-node-method="filterNode"
                @node-click="handleTableSelect"
                highlight-current
              >
                <template #default="{ node, data }">
                  <div class="table-node">
                    <el-icon><Grid /></el-icon>
                    <span>{{ data.name }}</span>
                    <span class="table-comment">{{ data.comment }}</span>
                  </div>
                </template>
              </el-tree>
            </el-scrollbar>
          </div>

          <!-- 右侧表结构 -->
          <div class="table-structure">
            <template v-if="selectedTable">
              <div class="structure-header">
                <h3>{{ selectedTable.name }}</h3>
                <p v-if="selectedTable.comment">{{ selectedTable.comment }}</p>
              </div>
              <el-table :data="selectedTable.columns" style="width: 100%">
                <el-table-column prop="name" label="字段名" width="180" />
                <el-table-column prop="type" label="类型" width="120" />
                <el-table-column prop="nullable" label="允许空" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.nullable ? 'info' : 'danger'" size="small">
                      {{ row.nullable ? '是' : '否' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="key" label="主键" width="80">
                  <template #default="{ row }">
                    <el-tag v-if="row.key" type="warning" size="small">PK</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="defaultValue" label="默认值" width="120" />
                <el-table-column prop="comment" label="注释" show-overflow-tooltip />
              </el-table>
            </template>
            <div v-else class="no-table-selected">
              <el-empty description="请选择要查看的表" />
            </div>
          </div>
        </div>
      </div>

      <!-- 底部按钮 -->
      <div class="step-footer">
        <el-button v-if="currentStep > 0" @click="currentStep--">上一步</el-button>
        <el-button
          v-if="currentStep < 2"
          type="primary"
          @click="handleNext"
        >
          下一步
        </el-button>
        <el-button
          v-else
          type="primary"
          @click="handleSave"
        >
          完成
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Grid } from '@element-plus/icons-vue'
import { createDs, testDs, getDbTables, getTableStructure } from '@/api/datasource'


const router = useRouter()
const currentStep = ref(0)
const selectedDb = ref('')
const formRef = ref(null)
const treeRef = ref(null)
const searchText = ref('')
const tableList = ref([])
const selectedTable = ref(null)

// 表单数据
const form = ref({
  name: '',
  type: '',
  url: '',
  dbName: '',
  username: '',
  password: ''
})

// 表单校验规则
const rules = {
  name: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入连接地址', trigger: 'blur' }],
  dbName: [{ required: true, message: '请输入数据库名', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 数据库分组
const groupedDatabases = computed(() => {
  const groups = {}
  databaseTypes.forEach(db => {
    if (!groups[db.group]) {
      groups[db.group] = []
    }
    groups[db.group].push(db)
  })
  return groups
})

// URL前缀
const urlPrefix = computed(() => {
  const db = databaseTypes.find(d => d.value === selectedDb.value)
  return db ? db.urlPrefix : ''
})

// 选择数据库
const selectDatabase = (db) => {
  selectedDb.value = db.value
  form.value.type = db.value
}

// 测试连接
const handleTest = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    const res = await testDs({
      ...form.value,
      url: urlPrefix.value + form.value.url
    })

    if (res.code === 0) {
      ElMessage.success('连接成功')
    } else {
      ElMessage.error(res.msg || '连接失败')
    }
  } catch (error) {
    console.error('测试连接失败:', error)
    ElMessage.error('连接失败')
  }
}

// 获取表列表
const fetchTables = async () => {
  try {
    const res = await createDs({
      ...form.value,
      url: urlPrefix.value + form.value.url
    })
    if (res.code === 0) {
      const tablesRes = await getDbTables(res.data)
      if (tablesRes.code === 0) {
        tableList.value = tablesRes.data
      }
    }
  } catch (error) {
    console.error('获取表列表失败:', error)
    ElMessage.error('获取表列表失败')
  }
}

// 获取表结构
const fetchTableStructure = async (tableName) => {
  try {
    const res = await getTableStructure({
      name: form.value.name,
      tableName
    })
    if (res.code === 0) {
      selectedTable.value = {
        name: tableName,
        columns: res.data
      }
    }
  } catch (error) {
    console.error('获取表结构失败:', error)
    ElMessage.error('获取表结构失败')
  }
}

// 处理下一步
const handleNext = async () => {
  if (currentStep.value === 0) {
    if (!selectedDb.value) {
      ElMessage.warning('请选择数据源类型')
      return
    }
    currentStep.value++
  } else if (currentStep.value === 1) {
    if (!formRef.value) return

    try {
      await formRef.value.validate()
      await fetchTables()
      currentStep.value++
    } catch (error) {
      console.error('表单验证失败:', error)
    }
  }
}

// 处理保存
const handleSave = async () => {
  try {
    const res = await createDs({
      ...form.value,
      url: urlPrefix.value + form.value.url
    })
    if (res.code === 0) {
      ElMessage.success('保存成功')
      router.push('/dataIntegration/source/database')
    }
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败')
  }
}

// 处理表选择
const handleTableSelect = (data) => {
  fetchTableStructure(data.name)
}

// 处理搜索
const handleSearch = () => {
  treeRef.value?.filter(searchText.value)
}

// 过滤节点
const filterNode = (value, data) => {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}

// 监听搜索文本变化
watch(searchText, (val) => {
  treeRef.value?.filter(val)
})
</script>

<style lang="scss" scoped>
.datasource-create {
  .card-header {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .steps {
    margin: 20px 0 40px;
  }

  .step-content {
    min-height: 400px;
  }

  .db-selector {
    .db-group {
      margin-bottom: 24px;

      h3 {
        margin: 0 0 16px 0;
        color: #606266;
        font-size: 16px;
      }

      .db-list {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
        gap: 16px;
      }

      .db-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        padding: 16px;
        border: 1px solid #dcdfe6;
        border-radius: 4px;
        cursor: pointer;
        transition: all 0.3s;

        &:hover {
          border-color: #409eff;
          box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
        }

        &.active {
          border-color: #409eff;
          background-color: #ecf5ff;
        }

        img {
          width: 40px;
          height: 40px;
          margin-bottom: 8px;
        }

        span {
          font-size: 14px;
          color: #606266;
        }
      }
    }
  }

  .config-form {
    max-width: 600px;
    margin: 0 auto;
  }

  .preview-wrapper {
    display: grid;
    grid-template-columns: 300px 1fr;
    gap: 20px;
    height: 500px;

    .table-list,
    .table-structure {
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      overflow: hidden;
    }

    .table-list {
      .list-header {
        padding: 12px;
        border-bottom: 1px solid #dcdfe6;
        display: flex;
        flex-direction: column;
        gap: 8px;

        span {
          font-weight: 500;
        }
      }

      .table-node {
        display: flex;
        align-items: center;
        gap: 8px;

        .table-comment {
          color: #909399;
          font-size: 12px;
          margin-left: 8px;
        }
      }
    }

    .table-structure {
      padding: 20px;

      .structure-header {
        margin-bottom: 20px;

        h3 {
          margin: 0;
          font-size: 18px;
        }

        p {
          margin: 8px 0 0;
          color: #606266;
        }
      }

      .no-table-selected {
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }
  }

  .step-footer {
    margin-top: 40px;
    display: flex;
    justify-content: center;
    gap: 12px;
  }
}
</style>
