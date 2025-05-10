<template>
  <div
      class="database-tables"
      v-loading.fullscreen.lock="fullscreenLoading"
      :element-loading-text="loadingText"
      element-loading-background="rgba(0, 0, 0, 0.8)"
      element-loading-svg-view-box="-10, -10, 50, 50"
      :element-loading-svg="loadingSvg"
  >
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button link @click="router.back()">
              <el-icon><ArrowLeft /></el-icon>
              返回
            </el-button>
            <el-divider direction="vertical" />
            <span class="database-info">
              {{ route.query.name }} ({{ route.query.type }})
            </span>
            <div class="database-actions">
              <el-button type="primary" @click="importData">导入</el-button>
              <el-dropdown trigger="hover">
                <el-button type="primary">
                  导出<el-icon><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="exportTables(['csv'])">CSV</el-dropdown-item>
                    <el-dropdown-item @click="exportTables(['excel'])">Excel</el-dropdown-item>
                    <el-dropdown-item @click="exportTables(['json'])">JSON</el-dropdown-item>
                    <el-dropdown-item @click="exportTables(['xml'])">XML</el-dropdown-item>
                    <el-dropdown-item @click="exportTables(['txt'])">TXT</el-dropdown-item>
                    <el-dropdown-item @click="exportTables(['dmp'])">DMP</el-dropdown-item>
                    <el-dropdown-item @click="exportTables(['sql'])">SQL</el-dropdown-item>
                    <el-dropdown-item divided @click="exportTables()">导出全部格式</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button type="primary" @click="copyTable">复制</el-button>
              <el-button type="danger" @click="deleteTable">删除</el-button>
            </div>
          </div>
        </div>
      </template>

      <div class="content-wrapper">
        <!-- 左侧表列表 -->
        <div class="left-area">
          <div class="search-area">
            <div class="filter-group">
              <el-checkbox-group
                  v-model="queryParams.types"
                  size="default"
                  @change="handleTypesChange"
              >
                <el-checkbox-button label="table">数据库表</el-checkbox-button>
                <el-checkbox-button label="view">视图</el-checkbox-button>
                <el-checkbox-button label="synonym">同义词</el-checkbox-button>
              </el-checkbox-group>
            </div>
            <!-- 搜索框 -->
            <div class="search-box">
              <el-input
                  v-model="searchText"
                  placeholder="搜索表名"
                  :prefix-icon="Search"
                  size="default"
                  clearable
                  @clear="handleSearch"
                  @input="handleSearch"
              />
            </div>
          </div>
          <div class="table-list-area">
            <el-scrollbar>
              <el-checkbox-group v-model="selectedTableNames">
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
                      <el-checkbox :label="data.name">
                        <el-icon><Grid /></el-icon>
                        <span>{{ data.name }}</span>
                        <span class="table-comment" style="margin-left: 20px;">{{ data.comment }}</span>
                      </el-checkbox>
                    </div>
                  </template>
                </el-tree>
              </el-checkbox-group>
            </el-scrollbar>
          </div>
        </div>

        <!-- 中间表结构 -->
        <div class="structure-area">
          <div class="structure-header">
            <template v-if="selectedTable">
              <h3>{{ selectedTable.name }}</h3>
              <p v-if="selectedTable.comment">{{ selectedTable.comment }}</p>
            </template>
            <template v-else>
              <h3>表结构</h3>
              <p>请选择一个表查看结构</p>
            </template>
          </div>
          <div class="structure-content">
            <template v-if="selectedTable">
              <!-- 关系型数据库表结构 -->
              <el-table
                  v-if="isRelationalDb && dataSourceType !== 'clickhouse'"
                  :data="selectedTable.columns"
                  style="width: 100%"
              >
                <el-table-column prop="name" label="字段名" width="180" />
                <el-table-column prop="type" label="类型" width="120" />
                <el-table-column prop="notNull" label="允许空" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.notNull ? 'info' : 'danger'" size="small">
                      {{ row.notNull ? '是' : '否' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="primaryKey" label="主键" width="80">
                  <template #default="{ row }">
                    <el-tag v-if="row.primaryKey" type="warning" size="small">PK</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="defaultValue" label="默认值" width="120" />
                <el-table-column prop="comment" label="注释" show-overflow-tooltip />
              </el-table>

              <!-- ClickHouse表结构 (特殊处理嵌套字段和数组) -->
              <el-table
                  v-else-if="dataSourceType === 'clickhouse'"
                  :data="selectedTable.columns"
                  style="width: 100%"
                  height="calc(100% - 60px)"
                  :row-class-name="getClickHouseRowClass"
              >
                <el-table-column prop="name" label="字段名" width="180">
                  <template #default="{ row }">
                    <div class="clickhouse-field" :class="{ 'nested-field': isNestedField(row.name) }">
                      {{ formatNestedFieldName(row.name) }}
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="type" label="类型" width="120">
                  <template #default="{ row }">
                    <el-tag v-if="row.type === 'Array'" type="success" size="small">数组</el-tag>
                    <span v-else>{{ row.type }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="notNull" label="允许空" width="80">
                  <template #default="{ row }">
                    <el-tag :type="row.notNull ? 'info' : 'danger'" size="small">
                      {{ row.notNull ? '是' : '否' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="primaryKey" label="主键" width="80">
                  <template #default="{ row }">
                    <el-tag v-if="row.primaryKey" type="warning" size="small">PK</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="defaultValue" label="默认值" width="120" />
                <el-table-column prop="comment" label="注释" show-overflow-tooltip />
              </el-table>

              <!-- NoSQL数据库结构 -->
              <template v-else-if="isNoSqlDb">
                <!-- MongoDB集合 -->
                <el-descriptions v-if="dataSourceType === 'mongodb'" :column="1" border>
                  <el-descriptions-item label="集合名称">{{ selectedTable.name }}</el-descriptions-item>
                  <el-descriptions-item label="文档数量">{{ selectedTable.documentCount }}</el-descriptions-item>
                  <el-descriptions-item label="索引">
                    <el-tag v-for="index in selectedTable.indexes" :key="index.name" class="mx-1">
                      {{ index.name }}
                    </el-tag>
                  </el-descriptions-item>
                </el-descriptions>

                <!-- Redis键值 -->
                <el-descriptions v-else-if="dataSourceType === 'redis'" :column="1" border>
                  <el-descriptions-item label="键名">{{ selectedTable.name }}</el-descriptions-item>
                  <el-descriptions-item label="类型">{{ selectedTable.type }}</el-descriptions-item>
                  <el-descriptions-item label="过期时间">{{ selectedTable.ttl }}</el-descriptions-item>
                </el-descriptions>

                <!-- Elasticsearch索引 -->
                <el-descriptions v-else-if="dataSourceType === 'elasticsearch'" :column="1" border>
                  <el-descriptions-item label="索引名称">{{ selectedTable.name }}</el-descriptions-item>
                  <el-descriptions-item label="文档数量">{{ selectedTable.docCount }}</el-descriptions-item>
                  <el-descriptions-item label="分片数">{{ selectedTable.shardsCount }}</el-descriptions-item>
                  <el-descriptions-item label="映射">
                    <pre>{{ JSON.stringify(selectedTable.mappings, null, 2) }}</pre>
                  </el-descriptions-item>
                </el-descriptions>
              </template>

              <!-- 文件系统结构 -->
              <template v-else-if="isFileSystem">
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="路径">{{ selectedTable.path }}</el-descriptions-item>
                  <el-descriptions-item label="类型">{{ selectedTable.type }}</el-descriptions-item>
                  <el-descriptions-item label="大小">{{ formatFileSize(selectedTable.size) }}</el-descriptions-item>
                  <el-descriptions-item label="修改时间">{{ formatDate(selectedTable.modificationTime) }}</el-descriptions-item>
                  <el-descriptions-item label="权限">{{ selectedTable.permissions }}</el-descriptions-item>
                </el-descriptions>
              </template>
            </template>
            <div v-else class="no-table-selected">
              <el-empty description="请选择要查看的表" />
            </div>
          </div>
        </div>

        <!-- 右侧数据预览 -->
        <div class="table-data">
          <div class="data-header">
            <div class="title">数据预览</div>
            <div class="toolbar">
              <el-button type="primary" @click="handleExecuteQuery">执行SQL</el-button>
              <el-button @click="openCopyDialog">复制查询结果</el-button>
              <el-dropdown trigger="hover">
                <el-button>
                  导出结果<el-icon><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="exportResult(['csv'])">CSV</el-dropdown-item>
                    <el-dropdown-item @click="exportResult(['excel'])">Excel</el-dropdown-item>
                    <el-dropdown-item @click="exportResult(['json'])">JSON</el-dropdown-item>
                    <el-dropdown-item @click="exportResult(['xml'])">XML</el-dropdown-item>
                    <el-dropdown-item @click="exportResult(['txt'])">TXT</el-dropdown-item>
                    <el-dropdown-item @click="exportResult(['dmp'])">DMP</el-dropdown-item>
                    <el-dropdown-item @click="exportResult(['sql'])">SQL</el-dropdown-item>
                    <el-dropdown-item divided @click="exportResult(exportAllFormats)">导出全部</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
            <el-input
                v-model="sqlQuery"
                type="textarea"
                :rows="4"
                placeholder="请输入SQL查询语句"
            />
          </div>
          <div class="data-content">
            <el-table
                v-if="queryResult.columns.length > 0"
                :data="queryResult.data"
                style="width: 100%"
                height="calc(100% - 150px)"
            >
              <el-table-column
                  v-for="col in queryResult.columns.filter(c => c.show)"
                  :key="col.name"
                  :prop="col.name"
                  :label="col.name"
                  show-overflow-tooltip
              />
            </el-table>
            <div v-else class="no-data">
              <el-empty description="请执行查询" />
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 复制表对话框 -->
    <el-dialog
        v-model="copyTableDialogVisible"
        title="复制表"
        width="50%"
    >
      <el-form>
        <!-- 源表信息 -->
        <el-form-item label="选择表名">
          <div class="selected-tables">
            <el-tag
                v-for="table in selectedTables"
                :key="table"
                class="mx-1"
                closable
                @close="removeSelectedTable(table)"
            >
              {{ table }}
            </el-tag>
          </div>
        </el-form-item>

        <!-- 目标数据源 -->
        <el-form-item label="目标数据源">
          <el-select
              v-model="selectedDataSourcesForCopy"
              multiple
              placeholder="请选择目标数据源"
              clearable
              style="width: 100%"
              :popper-class="'datasource-select-dropdown'"
          >
            <el-option
                v-for="item in dataSourceList"
                :key="item.id"
                :label="item.name"
                :value="item.id"
            >
              <span>{{ item.name }}</span>
              <span class="datasource-type">({{ formatDbType(item.type) }})</span>
            </el-option>
          </el-select>
        </el-form-item>

        <!-- 表已存在时的处理策略 -->
        <el-form-item label="表已存在时">
          <el-radio-group v-model="copyTableStrategy">
            <el-radio :value="1">不覆盖</el-radio>
            <el-radio :value="2">覆盖</el-radio>
            <el-radio :value="3">自动增加后缀</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="copyTableDialogVisible = false" :disabled="copyTableLoading">取消</el-button>
          <el-button type="primary" @click="handleCopyTableConfirm" :loading="copyTableLoading">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 复制查询结果对话框 -->
    <el-dialog
        v-model="copyDialogVisible"
        title="复制查询结果"
        width="500px"
    >
      <el-form
          ref="copyFormRef"
          :model="copyForm"
          :rules="copyFormRules"
          label-width="100px"
      >
        <!-- 显示SQL查询语句 -->
        <el-form-item
            label="SQL语句"
            required
        >
          <el-input
              v-model="sqlQuery"
              type="textarea"
              :rows="3"
              readonly
              placeholder="当前SQL查询语句"
          />
        </el-form-item>

        <el-form-item
            label="目标数据源"
            prop="targetDataSources"
            required
        >
          <el-select
              v-model="copyForm.targetDataSources"
              multiple
              placeholder="请选择目标数据源"
          >
            <el-option
                v-for="item in dataSourceList"
                :key="item.id"
                :label="item.name"
                :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item
            label="目标表名"
            prop="tableName"
            required
        >
          <el-input v-model="copyForm.tableName" placeholder="请输入表名" />
        </el-form-item>

        <el-form-item label="表注释" prop="tableComment">
          <el-input v-model="copyForm.tableComment" placeholder="请输入表注释" />
        </el-form-item>

        <el-form-item
            label="表已存在时"
            prop="existsStrategy"
            required
        >
          <el-radio-group v-model="copyForm.existsStrategy">
            <el-radio :value="1">不覆盖</el-radio>
            <el-radio :value="2">覆盖</el-radio>
            <el-radio :value="3">自动增加后缀</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="copyDialogVisible = false" :disabled="copyQueryLoading">取消</el-button>
          <el-button type="primary" @click="handleCopyQuery" :loading="copyQueryLoading">确认</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 导入数据对话框 -->
    <el-dialog
        v-model="importDialogVisible"
        title="导入数据"
        width="500px"
    >
      <div class="import-container">
        <el-upload
            class="upload-area"
            drag
            action="#"
            :auto-upload="false"
            :show-file-list="true"
            :on-change="handleFileChange"
            :before-upload="beforeUpload"
            :file-list="importFiles"
            multiple
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持的文件格式：CSV、Excel、JSON、XML、TXT、DMP、SQL、ZIP
            </div>
          </template>
        </el-upload>

        <!-- 修改表覆盖选项 -->
        <div class="import-options">
          <el-form :model="importOptions" :rules="importRules" ref="importFormRef" label-width="120px">
            <el-form-item
                label="表已存在时"
                prop="existsStrategy"
                required
            >
              <el-radio-group v-model="importOptions.existsStrategy">
                <el-radio :value="1">全部取消</el-radio>
                <el-radio :value="2">跳过</el-radio>
                <el-radio :value="3">覆盖</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="importDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleImportConfirm">开始导入</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Search, Refresh, Grid, ArrowDown, UploadFilled } from '@element-plus/icons-vue'
import { getDbTables, getTableStructure, executeQuery, getDatabaseSources, copyQueryResult, dropTable, dropTables } from '@/api/datasource'
import { debounce } from 'lodash'
import request from '@/utils/request'

const route = useRoute()
const router = useRouter()
const treeRef = ref(null)
const searchText = ref('')
const tableList = ref([])
const selectedTable = ref(null)
const sqlQuery = ref('')
const queryResult = ref({
  columns: [],
  data: [],
  total: 0
})

// 修改查询参数
const queryParams = ref({
  types: ['table'], // 默认选中数据库表
  searchText: ''
})

// 表单ref
const copyFormRef = ref(null)

// 复制查询结果对话框相关
const copyDialogVisible = ref(false)
const dataSourceList = ref([])
const copyForm = ref({
  targetDataSources: [],
  tableName: '',
  tableComment: '',
  existsStrategy: 1
})

// 复制表对话框相关
const copyTableDialogVisible = ref(false)
const selectedTables = ref([])
const selectedDataSourcesForCopy = ref([])
const copyTableStrategy = ref(1) // 默认不覆盖

// 选中的表名数组
const selectedTableNames = ref([])

// 数据源类型判断
const dataSourceType = ref(route.query.type?.toLowerCase())
const isRelationalDb = computed(() => {
  const relationalTypes = ['mysql', 'oracle', 'postgresql', 'sqlserver', 'db2',
    'dm', 'dm7', 'dm8', 'gaussdb', 'kingbase', 'clickhouse']
  return relationalTypes.includes(dataSourceType.value)
})

const isNoSqlDb = computed(() => {
  const noSqlTypes = ['mongodb', 'redis', 'elasticsearch', 'cassandra']
  return noSqlTypes.includes(dataSourceType.value)
})

const isFileSystem = computed(() => {
  const fileSystemTypes = ['hdfs', 's3', 'oss', 'minio', 'ftp', 'sftp']
  return fileSystemTypes.includes(dataSourceType.value)
})

// 文件大小格式化
const formatFileSize = (size) => {
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(2) + ' MB'
  return (size / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

// 日期格式化
const formatDate = (timestamp) => {
  return new Date(timestamp).toLocaleString()
}

// 处理类型变化
const handleTypesChange = (types) => {
  if (types.length === 0) {
    // 至少选择一个类型
    ElMessage.warning('请至少选择一种对象类型')
    queryParams.value.types = ['table']
    return
  }
  fetchTables()
}

// 修改获取表列表方法
const fetchTables = async () => {
  try {
    const res = await getDbTables(
        route.params.id,  // 直接传入 dataSourceId
        {  // 作为第二个参数传入查询参数
          types: queryParams.value.types.join(','),
          searchText: searchText.value
        }
    )

    if (res.code === 0) {
      tableList.value = res.data
    } else {
      ElMessage.error(res.msg || '获取表列表失败')
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
      id: route.params.id,
      tableName
    })
    if (res.code === 0) {
      selectedTable.value = {
        name: tableName,
        columns: res.data
      }
    } else {
      ElMessage.error(res.msg || '获取表结构失败')
    }
  } catch (error) {
    console.error('获取表结构失败:', error)
    ElMessage.error('获取表结构失败')
  }
}

// 导入对话框相关
const importDialogVisible = ref(false)
const importFiles = ref([])

// 允许的文件类型
const allowedTypes = [
  'text/csv',
  'application/vnd.ms-excel',
  'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'application/json',
  'application/xml',
  'text/plain',
  'application/octet-stream',
  'application/sql',
  'application/zip'
]

// 文件扩展名映射
const fileExtensions = {
  'csv': 'text/csv',
  'xls': 'application/vnd.ms-excel',
  'xlsx': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  'json': 'application/json',
  'xml': 'application/xml',
  'txt': 'text/plain',
  'dmp': 'application/octet-stream',
  'sql': 'application/sql',
  'zip': 'application/zip'
}

// 导入数据
const importData = () => {
  importFiles.value = []
  importDialogVisible.value = true
}

// 文件上传前的验证
const beforeUpload = (file) => {
  const extension = file.name.split('.').pop().toLowerCase()
  const isValidType = Object.keys(fileExtensions).includes(extension)

  if (!isValidType) {
    ElMessage.error('不支持的文件类型！')
    return false
  }

  return true
}

// 处理文件变化
const handleFileChange = (file, fileList) => {
  importFiles.value = fileList
}

// 修改导入选项相关的数据
const importOptions = ref({
  existsStrategy: 1 // 默认选择全部取消
})

// 修改导入表单验证规则
const importRules = {
  existsStrategy: [
    { required: true, message: '请选择表已存在时的处理方式', trigger: 'change' }
  ]
}

const importFormRef = ref(null)

// 添加导入表的API调用
const importTablesApi = async ({ dataSourceId, formData }) => {
  return request({
    url: `/api/ds/${dataSourceId}/import`,
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 修改确认导入方法
const handleImportConfirm = async () => {
  if (importFiles.value.length === 0) {
    ElMessage.warning('请选择要导入的文件')
    return
  }

  try {
    // 验证表单
    await importFormRef.value.validate()

    const formData = new FormData()
    importFiles.value.forEach(file => {
      formData.append('files', file.raw)
    })

    // 添加策略参数
    formData.append('strategy', importOptions.value.existsStrategy.toString())

    // 显示加载状态
    fullscreenLoading.value = true
    loadingText.value = '正在导入数据...'

    const res = await importTablesApi({
      dataSourceId: route.params.id,
      formData
    })

    if (res.code === 0) {
      ElMessage.success('导入成功')
      importDialogVisible.value = false
      // 刷新表列表
      fetchTables()
    } else {
      if (res.msg.includes('已存在')) {
        ElMessage.warning(res.msg)
      } else {
        ElMessage.error(res.msg || '导入失败')
      }
    }
  } catch (error) {
    console.error('导入失败:', error)
    ElMessage.error(error.message || '导入失败')
  } finally {
    fullscreenLoading.value = false
  }
}

// 添加加载状态变量
const copyTableLoading = ref(false)

// 添加全屏loading状态和文本
const fullscreenLoading = ref(false)
const loadingText = ref('处理中...')

// 自定义loading图标
const loadingSvg = `
  <path class="path" d="
    M 30 15
    L 28 17
    M 25.61 25.61
    A 15 15, 0, 0, 1, 15 30
    A 15 15, 0, 1, 1, 27.99 7.5
    L 15 15
  " style="stroke-width: 4px; fill: none;"/>
`

// 修改确认复制表操作方法
const handleCopyTableConfirm = async () => {
  if (selectedDataSourcesForCopy.value.length === 0) {
    ElMessage.warning('请选择目标数据源')
    return
  }

  try {
    fullscreenLoading.value = true
    loadingText.value = '正在复制表...'

    // 遍历选中的表进行复制
    for (const tableName of selectedTables.value) {
      loadingText.value = `正在复制表 ${tableName}...`
      const params = {
        sourceId: route.params.id,
        targetIds: selectedDataSourcesForCopy.value,
        tableName: tableName,
        existsStrategy: copyTableStrategy.value,
        sql: `SELECT * FROM ${tableName}`
      }

      const res = await copyQueryResult(params)
      if (res.code !== 0) {
        if (res.msg.includes('已存在')) {
          ElMessage.warning(`表 ${tableName}: ${res.msg}`)
        } else {
          ElMessage.error(`复制表 ${tableName} 失败: ${res.msg}`)
        }
      }
    }

    ElMessage.success('复制表操作完成')
    copyTableDialogVisible.value = false
  } catch (error) {
    if (error.message?.includes('已存在')) {
      ElMessage.warning(error.message)
    } else {
      console.error('复制表失败:', error)
      ElMessage.error(error.message || '复制表失败')
    }
  } finally {
    fullscreenLoading.value = false
  }
}

// 打开复制表对话框
const copyTable = () => {
  const tables = getSelectedTables()
  if (tables.length === 0) {
    ElMessage.warning('请先选择要复制的表')
    return
  }
  openCopyTableDialog(tables)
}

// 打开复制表对话框
const openCopyTableDialog = async (tables) => {
  if (!tables || tables.length === 0) {
    ElMessage.warning('请选择要复制的表')
    return
  }

  // 获取数据源列表
  await fetchDataSourceList()

  selectedTables.value = tables
  selectedDataSourcesForCopy.value = []
  copyTableStrategy.value = 1 // 重置为默认值
  copyTableDialogVisible.value = true
}

// 获取选中的表
const getSelectedTables = () => {
  return selectedTableNames.value
}

// 获取数据源列表
const fetchDataSourceList = async () => {
  try {
    const res = await getDatabaseSources()
    if (res.code === 0) {
      dataSourceList.value = res.data
    } else {
      ElMessage.error(res.msg || '获取数据源列表失败')
    }
  } catch (error) {
    console.error('获取数据源列表失败:', error)
    ElMessage.error('获取数据源列表失败')
  }
}

// 修改删除表方法
const deleteTable = () => {
  if (selectedTableNames.value.length === 0) {
    ElMessage.warning('请选择要删除的表')
    return
  }

  ElMessageBox.confirm(
      `确定要删除选中的 ${selectedTableNames.value.length} 个表吗？此操作将从数据库中永久删除表，且不可恢复！`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      fullscreenLoading.value = true
      loadingText.value = '正在删除表...'

      if (selectedTableNames.value.length === 1) {
        // 单表删除
        await dropTable(route.params.id, selectedTableNames.value[0])
      } else {
        // 批量删除
        await dropTables(route.params.id, selectedTableNames.value)
      }

      ElMessage.success('删除成功')
      // 刷新表列表
      fetchTables()
      // 清空选择
      selectedTableNames.value = []
      // 清空当前选中表
      selectedTable.value = null
      // 清空查询结果
      queryResult.value = {
        columns: [],
        data: [],
        total: 0
      }
    } catch (error) {
      console.error('删除表失败:', error)
      ElMessage.error(error.message || '删除表失败')
    } finally {
      fullscreenLoading.value = false
    }
  }).catch(() => {
    ElMessage.info('已取消删除')
  })
}

// 处理表选择
const handleTableSelect = async (table) => {
  await fetchTableStructure(table.name)
  // 自动生成查询语句
  sqlQuery.value = generateSelectSql(table.name)
  // 自动执行查询
  await handleExecuteQuery()
}

// 执行SQL查询
const handleExecuteQuery = async () => {
  if (!sqlQuery.value) {
    ElMessage.warning('请输入SQL查询语句')
    return
  }

  try {
    const dataSourceId = route.params.id
    if (!dataSourceId) {
      ElMessage.error('请先选择数据源')
      return
    }

    // 处理SQL语句，确保符合当前数据库语法
    const processedSQL = prepareSQL(sqlQuery.value)

    const response = await executeQuery({
      dataSourceId: route.params.id,
      sql: processedSQL
    })

    if (response.code === 0) {
      // 处理返回的数据
      if (response.data && response.data.length > 0) {
        // 从第一行数据中获取字段信息
        const firstRow = response.data[0]
        const columns = Object.keys(firstRow).map(key => ({
          name: key,
          label: key,
          type: typeof firstRow[key],
          width: '',
          show: true
        }))

        // 更新查询结果
        queryResult.value = {
          columns,
          data: response.data,
          total: response.data.length
        }
      } else {
        // 没有数据时重置
        queryResult.value = {
          columns: [],
          data: [],
          total: 0
        }
      }
    } else {
      ElMessage.error(response.msg || '执行查询失败')
    }
  } catch (error) {
    console.error('执行查询失败:', error)
    ElMessage.error(error.message || '执行查询失败')
  }
}

// 根据数据库类型生成合适的SELECT语句
const generateSelectSql = (tableName) => {
  const dbType = dataSourceType.value.toLowerCase()
  const limit = 100
  
  // 根据不同的数据库类型生成不同的SQL
  switch(dbType) {
    // ClickHouse需要使用反引号
    case 'clickhouse':
      return `SELECT * FROM \`${tableName}\` LIMIT ${limit}`
    
    // Hive、Spark等可能需要指定数据库名
    case 'hive':
    case 'spark':
      const dbName = route.query.dbName || 'default'
      return `SELECT * FROM \`${dbName}\`.\`${tableName}\` LIMIT ${limit}`
    
    // Oracle使用双引号，并且使用ROWNUM进行分页
    case 'oracle':
      return `SELECT * FROM "${tableName}" WHERE ROWNUM <= ${limit}`
    
    // SQL Server使用方括号，使用TOP进行分页
    case 'sqlserver':
      return `SELECT TOP ${limit} * FROM [${tableName}]`
    
    // DB2使用特定的FETCH FIRST语法
    case 'db2':
      return `SELECT * FROM "${tableName}" FETCH FIRST ${limit} ROWS ONLY`
    
    // PostgreSQL和类似数据库使用双引号，使用LIMIT
    case 'postgresql':
    case 'gaussdb':
    case 'opengauss':
    case 'kingbase':
    case 'highgo':
      return `SELECT * FROM "${tableName}" LIMIT ${limit}`
    
    // 达梦数据库
    case 'dm':
    case 'dm7':
    case 'dm8':
      return `SELECT * FROM "${tableName}" WHERE ROWNUM <= ${limit}`
    
    // 神通数据库
    case 'shentong':
      return `SELECT * FROM "${tableName}" LIMIT ${limit}`
    
    // Impala
    case 'impala':
      return `SELECT * FROM \`${tableName}\` LIMIT ${limit}`
    
    // StarRocks, Doris使用MySQL兼容语法
    case 'starrocks':
    case 'doris':
    case 'tidb':
    case 'oceanbase':
      return `SELECT * FROM \`${tableName}\` LIMIT ${limit}`
    
    // 默认情况下使用标准SQL语法
    default:
      return `SELECT * FROM ${tableName} LIMIT ${limit}`
  }
}

// 处理SQL查询，确保符合当前数据库语法
const prepareSQL = (sql) => {
  const dbType = dataSourceType.value.toLowerCase()
  
  // 如果SQL已经很复杂，我们不做修改，避免破坏用户自定义的SQL
  if (sql.includes('JOIN') || sql.includes('GROUP BY') || sql.includes('HAVING')) {
    return sql
  }
  
  try {
    // 简单解析SQL，提取表名部分
    // 注意：这是简化处理，复杂SQL需要更健壮的解析器
    if (sql.includes('FROM ')) {
      const fromIndex = sql.toUpperCase().indexOf('FROM ') + 5
      let endIndex = sql.indexOf(' ', fromIndex)
      if (endIndex === -1) endIndex = sql.length
      
      const tableName = sql.substring(fromIndex, endIndex).trim()
      // 移除可能已存在的引号
      const cleanTableName = tableName.replace(/[`"\[\]]/g, '')
      
      // 根据数据库类型替换表名格式
      let newTableRef = ''
      
      switch(dbType) {
        case 'clickhouse':
          newTableRef = `\`${cleanTableName}\``
          break
        case 'oracle':
        case 'postgresql':
        case 'gaussdb':
        case 'opengauss':
        case 'kingbase':
        case 'highgo':
        case 'dm':
        case 'dm7':
        case 'dm8':
          newTableRef = `"${cleanTableName}"`
          break
        case 'sqlserver':
          newTableRef = `[${cleanTableName}]`
          break
        case 'hive':
        case 'spark':
        case 'impala':
          // 如果表名包含数据库名.表名格式
          if (!cleanTableName.includes('.')) {
            const dbName = route.query.dbName || 'default'
            newTableRef = `\`${dbName}\`.\`${cleanTableName}\``
          } else {
            const parts = cleanTableName.split('.')
            newTableRef = `\`${parts[0]}\`.\`${parts[1]}\``
          }
          break
        case 'mysql':
        case 'mariadb':
        case 'starrocks':
        case 'doris':
        case 'tidb':
        case 'oceanbase':
          newTableRef = `\`${cleanTableName}\``
          break
        default:
          newTableRef = cleanTableName
      }
      
      // 替换原SQL中的表引用
      return sql.substring(0, fromIndex) + ' ' + newTableRef + sql.substring(endIndex)
    }
  } catch (error) {
    console.warn('SQL预处理错误，使用原始SQL', error)
  }
  
  // 如果处理失败或不需要处理，返回原SQL
  return sql
}

// 辅助函数：获取列类型
const getColumnType = (value) => {
  if (value === null || value === undefined) return 'STRING'
  if (typeof value === 'number') return value % 1 === 0 ? 'INTEGER' : 'DECIMAL'
  if (value instanceof Date) return 'DATETIME'
  if (typeof value === 'boolean') return 'BOOLEAN'
  return 'STRING'
}

// 处理搜索
const handleSearch = debounce(() => {
  fetchTables()
}, 300)

// ClickHouse嵌套字段和数组处理方法
const isNestedField = (fieldName) => {
  return fieldName.includes('.')
}

const formatNestedFieldName = (fieldName) => {
  if (!fieldName.includes('.')) return fieldName
  
  // 处理嵌套字段名称的显示
  const parts = fieldName.split('.')
  return `${parts[1]} (${parts[0]})`
}

const getClickHouseRowClass = ({ row }) => {
  if (isNestedField(row.name)) {
    return 'nested-field-row'
  }
  return ''
}

// 过滤节点
const filterNode = (value, data) => {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}

// 处理刷新
const handleRefresh = () => {
  fetchTables()
  selectedTable.value = null
}

// 监听搜索文本变化
watch(() => searchText.value, () => {
  handleSearch()
})

// 打开复制结果对话框
const openCopyDialog = async () => {
  if (!queryResult.value.data || !queryResult.value.columns) {
    ElMessage.warning('请先执行查询')
    return
  }

  // 获取数据源列表
  await fetchDataSourceList()

  copyDialogVisible.value = true
  copyForm.value = {
    targetDataSources: [],
    tableName: selectedTable.value?.name + '_copy' || '',
    tableComment: selectedTable.value?.comment || '',
    existsStrategy: 1 // 默认不覆盖
  }

  // 重置表单验证
  if (copyFormRef.value) {
    copyFormRef.value.resetFields()
  }
}

// 表单校验规则
const copyFormRules = {
  targetDataSources: [
    { required: true, message: '请选择目标数据源', trigger: 'change' },
    { type: 'array', min: 1, message: '至少选择一个目标数据源', trigger: 'change' }
  ],
  tableName: [
    { required: true, message: '请输入目标表名', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '表名必须以字母开头，只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  existsStrategy: [
    { required: true, message: '请选择表已存在时的处理策略', trigger: 'change' }
  ]
}

// 添加加载状态变量
const copyQueryLoading = ref(false)

// 修改处理复制查询方法
const handleCopyQuery = async () => {
  try {
    if (!sqlQuery.value) {
      ElMessage.warning('请先执行SQL查询')
      return
    }

    // 表单验证
    await copyFormRef.value.validate()

    fullscreenLoading.value = true
    loadingText.value = '正在复制查询结果...'

    const params = {
      sourceId: route.params.id,
      targetIds: copyForm.value.targetDataSources,
      tableName: copyForm.value.tableName,
      tableComment: copyForm.value.tableComment,
      existsStrategy: copyForm.value.existsStrategy,
      sql: sqlQuery.value
    }

    const res = await copyQueryResult(params)
    if (res.code === 0) {
      ElMessage.success('复制成功')
      copyDialogVisible.value = false
    } else {
      if (res.msg.includes('已存在')) {
        ElMessage.warning(res.msg)
      } else {
        ElMessage.error(res.msg || '复制失败')
      }
    }
  } catch (error) {
    if (error.message?.includes('已存在')) {
      ElMessage.warning(error.message)
    } else {
      console.error('复制查询结果失败:', error)
      ElMessage.error(error.message || '复制查询结果失败')
    }
  } finally {
    fullscreenLoading.value = false
  }
}

// 修改格式映射
const formatMapping = {
  'excel': 'xlsx',  // excel格式实际是xlsx
  'xlsx': 'xlsx',   // 添加这行，确保两种格式都支持
  'csv': 'csv',
  'json': 'json',
  'xml': 'xml',
  'txt': 'txt',
  'dmp': 'dmp',
  'sql': 'sql'
}

// 修改导出全部的处理
const exportAllFormats = ['csv', 'excel', 'json', 'xml', 'txt', 'sql', 'dmp']

// 修改导出查询结果方法
const exportResult = async (types) => {
  if (!Array.isArray(types)) {
    types = [types]
  }

  if (!queryResult.value.data) {
    ElMessage.warning('请先执行查询')
    return
  }

  try {
    fullscreenLoading.value = true
    loadingText.value = '正在导出数据...'

    const url = new URL(`/api/ds/${route.params.id}/export`, window.location.origin)

    types.forEach(type => {
      const mappedType = formatMapping[type] || type
      url.searchParams.append('types', mappedType)
    })

    url.searchParams.append('tableName', selectedTable.value.name)
    url.searchParams.append('sql', sqlQuery.value)

    const link = document.createElement('a')
    link.href = url.toString()
    link.download = types.length > 1
        ? generateZipFileName(true)
        : generateFileName(types[0], true)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  } finally {
    fullscreenLoading.value = false
  }
}

// 修改导出表方法
const exportTables = async (types = ['csv', 'xlsx', 'json', 'xml', 'txt', 'sql', 'dmp']) => {
  if (selectedTableNames.value.length === 0) {
    ElMessage.warning('请选择要导出的表')
    return
  }

  try {
    fullscreenLoading.value = true
    loadingText.value = '正在导出表...'

    const url = new URL(`/api/ds/${route.params.id}/export/tables`, window.location.origin)

    // 确保types是数组
    const exportTypes = Array.isArray(types) ? types : [types]

    exportTypes.forEach(type => {
      const mappedType = formatMapping[type] || type
      url.searchParams.append('types', mappedType)
    })

    selectedTableNames.value.forEach(tableName => {
      url.searchParams.append('tableNames', tableName)
    })

    const link = document.createElement('a')
    link.href = url.toString()
    link.download = exportTypes.length > 1
        ? generateZipFileName(false)
        : generateFileName(exportTypes[0], false)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)

    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  } finally {
    fullscreenLoading.value = false
  }
}

// 添加生成文件名的辅助函数
const generateFileName = (type, isQuery) => {
  const date = new Date().toISOString().slice(0, 10)
  const prefix = isQuery ? 'query_result' : selectedTableNames.value.join('_')
  const extension = type === 'excel' ? 'xlsx' : type
  return `${prefix}_${date}.${extension}`
}

const generateZipFileName = (isQuery) => {
  const date = new Date().toISOString().slice(0, 10)
  const prefix = isQuery ? 'query_result' : 'tables'
  return `${prefix}_${date}.zip`
}

// 格式化数据库类型
const formatDbType = (type) => {
  const dbTypeMap = {
    'mysql': 'MySQL',
    'oracle': 'Oracle',
    'postgresql': 'PostgreSQL',
    'sqlserver': 'SQL Server',
    'dm': '达梦',
    'kingbase': '人大金仓',
    'oscar': '神通',
    'gbase': 'GBase',
    'oceanbase': 'OceanBase',
    'clickhouse': 'ClickHouse',
    'hive': 'Hive',
    'db2': 'DB2'
  }
  return dbTypeMap[type.toLowerCase()] || type
}

// 初始化
onMounted(() => {
  fetchTables()
})
</script>

<style lang="scss" scoped>
.database-tables {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;

      .database-info {
        font-size: 16px;
        font-weight: 500;
      }

      .database-actions {
        display: flex;
        gap: 8px;
        margin-left: 20px;
      }
    }

    .header-right {
      display: flex;
      gap: 12px;

      .el-input {
        width: 200px;
      }
    }
  }

  .content-wrapper {
    display: grid;
    grid-template-columns: 300px 1fr 1fr;
    gap: 20px;
    margin-top: 20px;
    height: calc(100vh - 200px);

    .left-area,
    .structure-area,
    .table-data {
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      overflow: hidden;
    }

    .left-area {
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    .search-area {
      position: sticky;
      top: 0;
      background: #fff;
      z-index: 1;
      padding: 16px;
      border-bottom: 1px solid #eee;
    }

    .table-list-area {
      flex: 1;
      overflow-y: auto;
      padding: 8px;
    }

    .structure-area {
      display: flex;
      flex-direction: column;
      height: 100%;
    }

    .structure-header {
      position: sticky;
      top: 0;
      background: #fff;
      z-index: 1;
      padding: 16px;
      border-bottom: 1px solid #eee;
    }

    .structure-content {
      flex: 1;
      padding: 8px;
      position: relative;
      height: calc(100% - 60px);
    }

    .table-data {
      padding: 20px;

      .data-header {
        margin-bottom: 20px;

        .title {
          font-size: 18px;
          font-weight: 500;
          margin-bottom: 16px;
        }

        .toolbar {
          display: flex;
          gap: 12px;
          margin-bottom: 16px;

          .el-input {
            flex: 1;
          }
        }
      }

      .data-content {
        height: calc(100% - 150px);
      }

      .no-data {
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }
  }
}

.selected-tables {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.table-list {
  .query-options {
    padding: 12px;
    background-color: #f5f7fa;
    border-bottom: 1px solid #e4e7ed;

    .filter-group {
      margin-bottom: 12px;

      .el-checkbox-group {
        width: 100%;
        display: flex;

        .el-checkbox-button {
          flex: 1;

          :deep(.el-checkbox-button__inner) {
            width: 100%;
            display: flex;
            justify-content: center;
          }
        }
      }
    }

    .search-box {
      .el-input {
        :deep(.el-input__wrapper) {
          box-shadow: 0 0 0 1px #dcdfe6 inset;

          &:hover {
            box-shadow: 0 0 0 1px #c0c4cc inset;
          }

          &.is-focus {
            box-shadow: 0 0 0 1px #409eff inset;
          }
        }
      }
    }
  }

  .el-tree {
    padding: 8px;

    .table-node {
      display: flex;
      align-items: center;
      height: 32px;

      .el-checkbox {
        width: 100%;
        margin-right: 0;

        :deep(.el-checkbox__label) {
          display: flex;
          align-items: center;
          gap: 8px;

          .el-icon {
            margin-right: 4px;
            font-size: 16px;
            color: #909399;
          }

          .table-comment {
            color: #909399;
            font-size: 12px;
          }
        }
      }
    }
  }
}

.import-container {
  .upload-area {
    :deep(.el-upload) {
      width: 100%;

      .el-upload-dragger {
        width: 100%;
        height: 200px;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;

        .el-icon--upload {
          font-size: 48px;
          color: #409eff;
          margin-bottom: 16px;
        }

        .el-upload__text {
          font-size: 16px;
          color: #606266;

          em {
            color: #409eff;
            font-style: normal;
          }
        }
      }
    }

    .el-upload__tip {
      text-align: center;
      color: #909399;
      margin-top: 8px;
    }
  }

  .import-options {
    margin-top: 20px;

    :deep(.el-form-item__label) {
      padding-left: 20px;
    }

    :deep(.el-radio) {
      margin-right: 0;
      padding: 6px 0;
    }

    :deep(.el-form-item__content) {
      margin-left: 0 !important;
    }
  }
}

.copy-dialog-content {
  .source-info, .target-selection, .table-settings {
    margin-bottom: 20px;

    h4 {
      margin-bottom: 10px;
      font-weight: 500;
    }
  }

  .target-selection {
    .search-box {
      margin-bottom: 10px;
    }

    .el-tree {
      max-height: 300px;
      overflow-y: auto;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      padding: 10px;

      &::-webkit-scrollbar {
        width: 6px;
      }

      &::-webkit-scrollbar-thumb {
        background-color: #909399;
        border-radius: 3px;
      }

      &::-webkit-scrollbar-track {
        background-color: #f5f7fa;
      }
    }
  }

  .source-info {
    p {
      margin: 5px 0;
      color: #666;
    }
  }
}

/* 表格样式 */
:deep(.el-table) {
  position: relative;
  height: 100%;
}

:deep(.el-table__header-wrapper) {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #fff;
}

:deep(.el-table__fixed-header-wrapper) {
  position: sticky;
  top: 0;
  z-index: 11;
}

:deep(.el-table__header) {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

:deep(.el-table__body-wrapper) {
  height: calc(100% - 40px) !important;
  overflow-y: auto;
}

/* ClickHouse表结构样式 */
.nested-field-row {
  background-color: #f5f7fa;
}

.clickhouse-field {
  &.nested-field {
    padding-left: 12px;
    position: relative;
    
    &:before {
      content: '└';
      position: absolute;
      left: 0;
      color: #909399;
    }
  }
}

.el-radio-group {
  width: 100%;
  display: flex;
  justify-content: space-between;
}

/* 修改单选组的样式 */
.strategy-options {
  display: flex;
  align-items: center;
  gap: 32px;
  justify-content: flex-start;
}

:deep(.el-radio-group) {
  width: 100%;
}

:deep(.el-radio) {
  margin-right: 0;
  padding: 6px 0;
  white-space: nowrap;
}

/* 确保表单项标签对齐 */
:deep(.el-form-item__content) {
  justify-content: flex-start;
}

/* 调整表单项的间距 */
:deep(.el-form-item) {
  margin-bottom: 20px;
}

/* 调整下拉选择框的样式 */
:deep(.el-select) {
  width: 100%;
}

:deep(.el-select .el-select__tags) {
  flex-wrap: wrap;
}

/* 数据源类型样式 */
.datasource-type {
  color: #909399;
  margin-left: 8px;
  font-size: 13px;
}

/* 下拉选项样式 */
:deep(.el-select-dropdown.datasource-select-dropdown) {
  max-height: 300px;

  .el-select-dropdown__wrap {
    max-height: 300px;
  }

  .el-select-dropdown__item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-right: 20px;
  }
}

/* 选中标签的样式 */
:deep(.el-select__tags) {
  flex-wrap: wrap;
  max-height: 120px;
  overflow-y: auto;

  .el-tag {
    display: flex;
    align-items: center;
    max-width: 100%;
    margin: 2px 4px;

    .el-select__tags-text {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      max-width: 200px;
    }
  }
}

// 添加loading动画样式
:deep(.el-loading-spinner) {
  .path {
    stroke: #409eff;
    stroke-linecap: round;
    animation: dash 1.5s ease-in-out infinite;
  }
}

@keyframes dash {
  0% {
    stroke-dasharray: 1, 150;
    stroke-dashoffset: 0;
  }
  50% {
    stroke-dasharray: 90, 150;
    stroke-dashoffset: -35;
  }
  100% {
    stroke-dasharray: 90, 150;
    stroke-dashoffset: -124;
  }
}

// 添加淡入淡出动画
.database-tables {
  transition: all 0.3s ease-in-out;

  &.is-loading {
    filter: blur(1px);
  }
}

// 优化loading文本样式
:deep(.el-loading-text) {
  font-size: 16px;
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.6);
  margin-top: 10px;
}
</style>