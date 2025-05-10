<template>
  <div class="dataset-file">
    <!-- 统计卡片区域 -->
    <div class="statistics-area">
      <el-row :gutter="16">
        <el-col :span="6">
          <div class="statistics-card blue-card">
            <div class="statistics-icon blue">
              <el-icon><Document /></el-icon>
            </div>
            <div class="statistics-info">
              <div class="stat-header">
                <div class="stat-title">文件总数</div>
              </div>
              <div class="statistics-value">{{ statsData.totalFiles }}<span class="stat-unit">个</span></div>
              <div class="stat-trend" :class="{ up: statsData.fileGrowthRate >= 0, down: statsData.fileGrowthRate < 0 }">
                <el-icon v-if="statsData.fileGrowthRate >= 0"><ArrowUp /></el-icon>
                <el-icon v-else><ArrowDown /></el-icon>
                <span>{{ Math.abs(statsData.fileGrowthRate) }}%</span>
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
                <div class="stat-title">解析成功率</div>
              </div>
              <div class="statistics-value">{{ statsData.successRate }}<span class="stat-unit">%</span></div>
              <div class="stat-trend" :class="{ up: true, warning: false }">
                <el-icon><ArrowUp /></el-icon>
                <span>{{ Math.abs(1.2) }}%</span>
              </div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="statistics-card orange-card">
            <div class="statistics-icon orange">
              <el-icon><Files /></el-icon>
            </div>
            <div class="statistics-info">
              <div class="stat-header">
                <div class="stat-title">平均文件大小</div>
              </div>
              <div class="statistics-value">{{ statsData.avgFileSize }}<span class="stat-unit">MB</span></div>
              <div class="stat-trend" :class="{ 
                up: statsData.sizeChangeRate >= 0, 
                down: statsData.sizeChangeRate < 0,
                warning: statsData.sizeChangeRate > 0
              }">
                <el-icon v-if="statsData.sizeChangeRate >= 0"><ArrowUp /></el-icon>
                <el-icon v-else><ArrowDown /></el-icon>
                <span>{{ Math.abs(statsData.sizeChangeRate) }}%</span>
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
                <div class="stat-title">解析失败数</div>
              </div>
              <div class="statistics-value">{{ statsData.failedCount }}<span class="stat-unit">个</span></div>
              <div class="stat-trend" :class="{ 
                up: statsData.failedChangeRate >= 0, 
                down: statsData.failedChangeRate < 0,
                good: statsData.failedChangeRate < 0
              }">
                <el-icon v-if="statsData.failedChangeRate >= 0"><ArrowUp /></el-icon>
                <el-icon v-else><ArrowDown /></el-icon>
                <span>{{ Math.abs(statsData.failedChangeRate) }}%</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>

    <el-card>
      <!-- 搜索区域 -->
      <el-form :model="queryParams" ref="queryForm" :inline="true" class="search-form">
        <el-form-item label="文件名" prop="fileName">
          <el-input
              v-model="queryParams.fileName"
              placeholder="请输入文件名"
              clearable
              @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="文件类型" prop="fileType">
          <el-select
              v-model="queryParams.fileType"
              placeholder="请选择文件类型"
              clearable
              style="width: 120px"
          >
            <el-option label="CSV" value="csv" />
            <el-option label="Excel" value="excel" />
            <el-option label="JSON" value="json" />
            <el-option label="XML" value="xml" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select
              v-model="queryParams.status"
              placeholder="请选择状态"
              clearable
              style="width: 180px"
          >
            <el-option label="未解析" value="0" />
            <el-option label="已解析" value="1" />
            <el-option label="解析失败" value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 操作按钮区 -->
      <div class="table-toolbar">
        <div class="left">
          <el-button type="primary" @click="handleCreate">新建数据源</el-button>

          <!-- 将下载模板按钮改为下拉菜单 -->
          <el-dropdown @command="handleTemplateDownload">
            <el-button type="success">
              下载模板<el-icon class="el-icon--right"><arrow-down /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="csv">CSV模板</el-dropdown-item>
                <el-dropdown-item command="xlsx">Excel模板</el-dropdown-item>
                <el-dropdown-item command="json">JSON模板</el-dropdown-item>
                <el-dropdown-item command="xml">XML模板</el-dropdown-item>
                <el-dropdown-item command="txt">TXT模板</el-dropdown-item>
                <el-dropdown-item command="dmp">DMP模板</el-dropdown-item>
                <el-dropdown-item command="sql">SQL模板</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-button type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">
            批量删除
          </el-button>
        </div>
      </div>


      <!-- 数据表格 -->
      <el-table
          v-loading="loading"
          :data="dataList"
          @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="fileName" label="文件名" show-overflow-tooltip />
        <el-table-column prop="fileType" label="文件类型" width="120">
          <template #default="{ row }">
            <el-tag>{{ row.fileType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="文件大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
                link
                type="primary"
                @click="handleView(row)"
                :disabled="row.status !== 1"
            >
              查看数据
            </el-button>
            <el-button
                link
                type="danger"
                @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog
        v-model="uploadDialog.visible"
        title="新建文件数据源"
        width="500px"
        :close-on-click-modal="false"
        @closed="handleDialogClose"
    >
      <el-form :model="uploadDialog.form" label-width="100px">
        <el-form-item label="是否覆盖">
          <el-switch v-model="uploadDialog.form.override" />
          <div class="form-tip">
            选择是否覆盖同名表，若不覆盖则自动添加后缀
          </div>
        </el-form-item>

        <el-upload
            class="upload-area"
            drag
            ref="uploadRef"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :data="uploadDialog.form"
            :before-upload="handleBeforeUpload"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :on-exceed="handleExceed"
            :limit="1"
            accept=".csv,.xlsx,.xls,.json,.xml,.txt,.dmp,.sql"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 CSV、Excel、JSON、XML、DMP、SQL 格式文件，单个文件不超过10MB
            </div>
          </template>
        </el-upload>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  UploadFilled, 
  ArrowDown, 
  Document, 
  CircleCheckFilled, 
  Files, 
  Warning, 
  ArrowUp 
} from '@element-plus/icons-vue'
import {
  getFileSourceList,
  uploadFile,
  deleteFileSource,
  deleteBatchFileSource,
  downloadTemplate,
  getFileSourceStats
} from '@/api/fileSource'

const router = useRouter()
const queryForm = ref(null)

// 查询参数
const queryParams = ref({
  fileName: '',
  fileType: '',
  status: ''
})

// 表格数据
const loading = ref(false)
const dataList = ref([])
const selectedIds = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 统计数据
const statsData = ref({
  totalFiles: 0,
  successRate: 0,
  avgFileSize: 0,
  failedCount: 0,
  fileGrowthRate: 0,
  failedChangeRate: 0,
  sizeChangeRate: 0
})

// 上传对话框
const uploadDialog = reactive({
  visible: false,
  form: {
    override: false  // 是否覆盖同名表
  }
})
const uploadUrl = '/api/file-source/upload'
// 上传请求头
const uploadHeaders = computed(() => {
  return {
    Authorization: `Bearer ${localStorage.getItem('token')}`
  }
})

// 查询方法
const handleQuery = () => {
  currentPage.value = 1
  getList()
}

// 重置查询
const resetQuery = () => {
  queryForm.value?.resetFields()
  handleQuery()
}

// 获取统计数据
const getStats = async () => {
  try {
    const res = await getFileSourceStats()
    if (res.code === 0) {
      statsData.value = res.data
    }
  } catch (error) {
    ElMessage.error('获取统计数据失败')
  }
}

// 获取数据列表
const getList = async () => {
  loading.value = true
  try {
    const res = await getFileSourceList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      ...queryParams.value
    })
    if (res.code === 0) {
      dataList.value = res.data.records
      total.value = res.data.total
    }
  } catch (error) {
    ElMessage.error('获取数据失败')
  }
  loading.value = false
}

// 表格选择变化
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(item => item.id)
}

// 批量删除
const handleBatchDelete = () => {
  if (!selectedIds.value.length) {
    ElMessage.warning('请选择要删除的数据')
    return
  }

  ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 条数据吗？`, '提示', {
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteBatchFileSource(selectedIds.value)
      if (res.code === 0) {
        ElMessage.success('删除成功')
        getList()
      }
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

// 新增模板下载处理方法
const handleTemplateDownload = async (fileType) => {
  try {
    await downloadTemplate(fileType)
  } catch (error) {
    ElMessage.error('下载失败')
  }
}

// 格式化文件大小
const formatFileSize = (size) => {
  if (size < 1024) {
    return size + ' B'
  } else if (size < 1024 * 1024) {
    return (size / 1024).toFixed(2) + ' KB'
  } else {
    return (size / 1024 / 1024).toFixed(2) + ' MB'
  }
}

// 获取状态类型
const getStatusType = (status) => {
  switch (status) {
    case 0:
      return 'info'
    case 1:
      return 'success'
    case 2:
      return 'danger'
    default:
      return 'info'
  }
}

// 获取状态文本
const getStatusText = (status) => {
  switch (status) {
    case 0:
      return '未解析'
    case 1:
      return '已解析'
    case 2:
      return '解析失败'
    default:
      return '未知'
  }
}

// 查看数据
const handleView = (row) => {
  router.push(`/dataset/file/data/${row.id}`)
}

// 删除数据源
const handleDelete = (row) => {
  ElMessageBox.confirm('确定要删除该数据源吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const res = await deleteFileSource(row.id)
      if (res.code === 0) {
        ElMessage.success('删除成功')
        getList()
      }
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

// 分页相关方法
const handleSizeChange = (val) => {
  pageSize.value = val
  getList()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  getList()
}

// 打开新建对话框
const handleCreate = () => {
  // 清除上传记录缓存
  localStorage.removeItem('uploadFiles')
  
  // 重置上传组件
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
  
  // 重置表单
  uploadDialog.form = {
    override: false
  }
  
  // 打开对话框
  uploadDialog.visible = true
}

// 上传前校验
const handleBeforeUpload = (file) => {
  // 检查文件大小
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB!')
    return false
  }

  // 检查文件类型
  const fileName = file.name.toLowerCase()
  const extension = fileName.substring(fileName.lastIndexOf('.') + 1)
  const allowTypes = ['csv', 'xlsx', 'json', 'xml', 'dmp', 'sql']

  if (!allowTypes.includes(extension)) {
    ElMessage.error('不支持的文件类型!')
    return false
  }

  return true
}

// 上传成功回调
const handleUploadSuccess = (response) => {
  if (response.code === 0) {
    ElMessage.success('上传成功')
    // 清除上传记录缓存
    localStorage.removeItem('uploadFiles')
    
    // 重置上传组件，先检查uploadRef是否存在
    if (uploadRef.value) {
      uploadRef.value.clearFiles()
    }
    
    // 刷新文件列表
    getList()
    
    // 确保对话框关闭
    uploadDialog.visible = false
    
    // 使用setTimeout确保DOM更新
    setTimeout(() => {
      if (uploadDialog.visible) {
        uploadDialog.visible = false
      }
    }, 100)
  } else {
    ElMessage.error(response.msg || '上传失败')
  }
}

// 关闭对话框时清理
const handleDialogClose = () => {
  // 重置上传组件
  uploadRef.value.clearFiles()
  // 清除上传记录缓存
  localStorage.removeItem('uploadFiles')
  // 重置表单
  uploadDialog.form = {
    override: false
  }
}

// 上传失败回调
const handleUploadError = () => {
  ElMessage.error('文件上传失败')
}

// 超出文件数量限制
const handleExceed = () => {
  ElMessage.warning('只能上传一个文件')
}

const uploadRef = ref(null)

// 提交上传
const submitUpload = () => {
  uploadRef.value.submit()
}

onMounted(() => {
  getList()
  getStats()
})
</script>

<style lang="scss" scoped>
.dataset-file {
  padding: 15px;
  
  .search-wrapper {
    background-color: #fff;
    padding: 16px; /* 减小内边距 */
    border-radius: 4px;
    margin-bottom: 16px;
  }
  
  .statistics-area {
    margin-bottom: 16px;
    
    .el-row {
      margin-bottom: 0;
    }
  }
  
  .statistics-card {
    display: flex;
    align-items: center;
    padding: 14px; /* 减小内边距 */
    transition: all 0.3s;
    border-left-width: 4px;
    border-left-style: solid;
    background-color: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
    height: 90px; /* 减小高度 */
    
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
    
    .statistics-icon {
      width: 42px; /* 减小尺寸 */
      height: 42px; /* 减小尺寸 */
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 14px; /* 减小间距 */
      
      .el-icon {
        font-size: 22px; /* 减小图标 */
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
    }
    
    .stat-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px; /* 减小边距 */
    }
    
    .stat-title {
      font-size: 13px; /* 减小字体 */
      color: var(--el-text-color-secondary);
      font-weight: 500;
    }
    
    .statistics-value {
      font-size: 22px; /* 减小字体 */
      font-weight: bold;
      color: var(--el-text-color-primary);
      
      .stat-unit {
        font-size: 13px; /* 减小字体 */
        font-weight: normal;
        color: var(--el-text-color-secondary);
        margin-left: 2px;
      }
    }
    
    .stat-trend {
      display: flex;
      align-items: center;
      font-size: 11px; /* 减小字体 */
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

  .search-form {
    margin-bottom: 16px;

    .el-select {
      width: 160px; /* 减小选择框宽度 */
    }
  }

  .table-toolbar {
    margin-bottom: 14px; /* 减小间距 */

    .left {
      display: flex;
      gap: 8px; /* 减小按钮间距 */
      align-items: center;
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .pagination {
    margin-top: 16px; /* 减小分页上边距 */
    display: flex;
    justify-content: flex-end;
  }

  .upload-area {
    :deep(.el-upload) {
      width: 100%;
    }

    :deep(.el-upload-dragger) {
      width: 100%;
      height: 200px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      .el-icon--upload {
        font-size: 48px;
        color: #409EFF;
        margin-bottom: 16px;
      }

      .el-upload__text {
        font-size: 16px;
        color: #606266;

        em {
          color: #409EFF;
          font-style: normal;
        }
      }
    }
  }

  .el-upload__tip {
    color: #909399;
    font-size: 14px;
    line-height: 1.5;
    text-align: center;
    margin-top: 12px;
  }

  /* 表格区域样式调整 */
  :deep(.el-table) {
    font-size: 13px; /* 减小表格字体 */
    
    .el-table__header th {
      padding: 8px 0; /* 减小表头内边距 */
      font-size: 13px; /* 减小表头字体 */
    }
    
    .el-table__body td {
      padding: 6px 0; /* 减小单元格内边距 */
    }
    
    .el-button.el-button--text {
      padding: 4px 5px; /* 减小文本按钮内边距 */
    }
  }

  /* 减小卡片内容区域内边距 */
  :deep(.el-card__body) {
    padding: 15px; /* 减小卡片内边距 */
  }
}
</style>
