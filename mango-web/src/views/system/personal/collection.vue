<template>
  <div class="collection-container">
    <!-- 收藏概览 -->
    <el-row :gutter="20" class="overview-section">
      <el-col :span="6" v-for="(item, index) in collectionStats" :key="index">
        <el-card shadow="hover" class="stat-card" :class="item.type">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon :size="32"><component :is="item.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ item.value }}</div>
              <div class="stat-label">{{ item.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 收藏列表卡片 -->
    <el-card class="collection-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>收藏列表</h3>
            <el-tag type="info" effect="plain">共 {{ total }} 条收藏</el-tag>
          </div>
          <div class="header-right">
            <el-button-group>
              <el-tooltip content="批量导出" placement="top">
                <el-button :icon="Download" @click="handleBatchExport" />
              </el-tooltip>
              <el-tooltip content="批量删除" placement="top">
                <el-button :icon="Delete" @click="handleBatchDelete" />
              </el-tooltip>
              <el-tooltip content="刷新" placement="top">
                <el-button :icon="Refresh" @click="refreshList" />
              </el-tooltip>
            </el-button-group>
          </div>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" class="search-form">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="收藏类型">
              <el-select v-model="searchForm.type" placeholder="请选择收藏类型" clearable>
                <el-option label="菜单页面" value="MENU" />
                <el-option label="数据报表" value="REPORT" />
                <el-option label="常用功能" value="FUNCTION" />
                <el-option label="其他内容" value="OTHER" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="收藏标签">
              <el-select
                v-model="searchForm.tags"
                multiple
                collapse-tags
                collapse-tags-tooltip
                placeholder="请选择标签"
                clearable
              >
                <el-option
                  v-for="tag in tagOptions"
                  :key="tag.value"
                  :label="tag.label"
                  :value="tag.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="收藏时间">
              <el-date-picker
                v-model="searchForm.timeRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DD HH:mm:ss"
                :shortcuts="dateShortcuts"
              />
            </el-form-item>
          </el-col>
          <el-col :span="4" class="search-buttons">
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon> 搜索
            </el-button>
            <el-button @click="resetSearch">
              <el-icon><Refresh /></el-icon> 重置
            </el-button>
          </el-col>
        </el-row>
      </el-form>

      <!-- 收藏列表 -->
      <div class="collection-list">
        <el-empty v-if="!collectionList.length" description="暂无收藏" />
        <template v-else>
          <div
            v-for="item in collectionList"
            :key="item.id"
            class="collection-item"
          >
            <el-checkbox
              v-model="item.selected"
              @click.stop
            />
            <div class="item-icon" :class="item.type.toLowerCase()">
              <el-icon :size="24"><component :is="getCollectionIcon(item.type)" /></el-icon>
            </div>
            <div class="item-content" @click="handleViewCollection(item)">
              <div class="item-header">
                <span class="item-title">{{ item.title }}</span>
                <div class="item-tags">
                  <el-tag
                    v-for="tag in item.tags"
                    :key="tag"
                    size="small"
                    effect="light"
                  >
                    {{ tag }}
                  </el-tag>
                </div>
              </div>
              <div class="item-desc">{{ item.description }}</div>
              <div class="item-footer">
                <div class="item-info">
                  <span class="item-time">收藏于：{{ item.createTime }}</span>
                  <span class="item-visits">访问次数：{{ item.visits }}</span>
                </div>
                <div class="item-actions">
                  <el-tooltip content="编辑" placement="top">
                    <el-button type="primary" link @click.stop="handleEdit(item)">
                      <el-icon><Edit /></el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="取消收藏" placement="top">
                    <el-button type="danger" link @click.stop="handleDelete(item)">
                      <el-icon><Star /></el-icon>
                    </el-button>
                  </el-tooltip>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="page.current"
          v-model:page-size="page.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 收藏编辑对话框 -->
    <el-dialog
      v-model="editDialog"
      :title="editForm.id ? '编辑收藏' : '新增收藏'"
      width="600px"
      :destroy-on-close="true"
    >
      <el-form
        ref="editFormRef"
        :model="editForm"
        :rules="editRules"
        label-width="100px"
      >
        <el-form-item label="收藏类型" prop="type">
          <el-select v-model="editForm.type" placeholder="请选择收藏类型">
            <el-option label="菜单页面" value="MENU" />
            <el-option label="数据报表" value="REPORT" />
            <el-option label="常用功能" value="FUNCTION" />
            <el-option label="其他内容" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="收藏标题" prop="title">
          <el-input v-model="editForm.title" placeholder="请输入收藏标题" />
        </el-form-item>
        <el-form-item label="收藏描述" prop="description">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入收藏描述"
          />
        </el-form-item>
        <el-form-item label="收藏标签">
          <el-select
            v-model="editForm.tags"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="请选择或输入标签"
          >
            <el-option
              v-for="tag in tagOptions"
              :key="tag.value"
              :label="tag.label"
              :value="tag.label"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属分类" prop="categoryId">
          <el-cascader
            v-model="editForm.categoryId"
            :options="categoryOptions"
            :props="{
              checkStrictly: true,
              label: 'name',
              value: 'id',
              emitPath: false
            }"
            placeholder="请选择所属分类"
            clearable
          />
        </el-form-item>
        <el-form-item label="收藏备注">
          <el-input
            v-model="editForm.remark"
            type="textarea"
            :rows="2"
            placeholder="请输入收藏备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSaveCollection">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 分类管理抽屉 -->
    <el-drawer
      v-model="categoryDrawer"
      title="分类管理"
      size="500px"
      :destroy-on-close="true"
    >
      <div class="category-container">
        <!-- 分类工具栏 -->
        <div class="category-toolbar">
          <el-input
            v-model="categoryKeyword"
            placeholder="搜索分类"
            clearable
            @input="filterCategories"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="handleAddCategory">
            <el-icon><Plus /></el-icon> 新增分类
          </el-button>
        </div>

        <!-- 分类树 -->
        <div class="category-tree">
          <el-tree
            ref="categoryTreeRef"
            :data="categoryTree"
            :props="{
              label: 'name',
              children: 'children'
            }"
            node-key="id"
            highlight-current
            :filter-node-method="filterNode"
            draggable
            @node-drag-end="handleDragEnd"
          >
            <template #default="{ node, data }">
              <div class="category-node">
                <span class="node-label">
                  <el-icon><Folder /></el-icon>
                  {{ node.label }}
                  <el-tag size="small" type="info" class="node-count">
                    {{ data.count || 0 }}
                  </el-tag>
                </span>
                <span class="node-actions">
                  <el-button-group>
                    <el-tooltip content="添加子分类" placement="top">
                      <el-button
                        link
                        type="primary"
                        :icon="Plus"
                        @click.stop="handleAddSubCategory(data)"
                      />
                    </el-tooltip>
                    <el-tooltip content="编辑分类" placement="top">
                      <el-button
                        link
                        type="primary"
                        :icon="Edit"
                        @click.stop="handleEditCategory(data)"
                      />
                    </el-tooltip>
                    <el-tooltip content="删除分类" placement="top">
                      <el-button
                        link
                        type="danger"
                        :icon="Delete"
                        @click.stop="handleDeleteCategory(data)"
                      />
                    </el-tooltip>
                  </el-button-group>
                </span>
              </div>
            </template>
          </el-tree>
        </div>

        <!-- 分类编辑对话框 -->
        <el-dialog
          v-model="categoryDialog"
          :title="categoryForm.id ? '编辑分类' : '新增分类'"
          width="400px"
          append-to-body
          :destroy-on-close="true"
        >
          <el-form
            ref="categoryFormRef"
            :model="categoryForm"
            :rules="categoryRules"
            label-width="80px"
          >
            <el-form-item label="上级分类">
              <el-cascader
                v-model="categoryForm.parentId"
                :options="categoryOptions"
                :props="{
                  checkStrictly: true,
                  label: 'name',
                  value: 'id',
                  emitPath: false
                }"
                placeholder="顶级分类"
                clearable
                :disabled="!!categoryForm.id"
              />
            </el-form-item>
            <el-form-item label="分类名称" prop="name">
              <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
            </el-form-item>
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="categoryForm.sort" :min="0" :max="999" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input
                v-model="categoryForm.remark"
                type="textarea"
                :rows="2"
                placeholder="请输入备注"
              />
            </el-form-item>
          </el-form>
          <template #footer>
            <div class="dialog-footer">
              <el-button @click="categoryDialog = false">取消</el-button>
              <el-button type="primary" @click="handleSaveCategory">确定</el-button>
            </div>
          </template>
        </el-dialog>
      </div>
    </el-drawer>

    <!-- 收藏分析抽屉 -->
    <el-drawer
      v-model="analysisDrawer"
      title="收藏分析"
      size="800px"
      :destroy-on-close="true"
    >
      <div class="analysis-container">
        <!-- 时间范围选择 -->
        <div class="analysis-header">
          <el-radio-group v-model="analysisTimeRange" @change="handleAnalysisTimeChange">
            <el-radio-button label="week">最近一周</el-radio-button>
            <el-radio-button label="month">最近一月</el-radio-button>
            <el-radio-button label="year">最近一年</el-radio-button>
          </el-radio-group>
          <el-button type="primary" @click="exportAnalysisData">
            <el-icon><Download /></el-icon> 导出分析报告
          </el-button>
        </div>

        <!-- 核心指标 -->
        <el-row :gutter="20" class="analysis-stats">
          <el-col :span="8" v-for="(stat, index) in analysisStats" :key="index">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-content">
                <div class="stat-icon" :class="stat.type">
                  <el-icon :size="24"><component :is="stat.icon" /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-value">{{ stat.value }}</div>
                  <div class="stat-label">{{ stat.label }}</div>
                  <div class="stat-trend" :class="stat.trend > 0 ? 'up' : 'down'">
                    {{ Math.abs(stat.trend) }}% <el-icon><component :is="stat.trend > 0 ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <!-- 趋势图表 -->
        <el-card class="trend-chart">
          <template #header>
            <div class="chart-header">
              <span>收藏趋势</span>
              <div class="chart-actions">
                <el-radio-group v-model="trendType" size="small" @change="renderCollectionTrendChart">
                  <el-radio-button label="count">数量趋势</el-radio-button>
                  <el-radio-button label="type">类型分布</el-radio-button>
                </el-radio-group>
                <el-button 
                  type="text" 
                  :icon="trendChartCollapsed ? 'ArrowDown' : 'ArrowUp'" 
                  @click="trendChartCollapsed = !trendChartCollapsed"
                >
                  {{ trendChartCollapsed ? '展开' : '收起' }}
                </el-button>
              </div>
            </div>
          </template>
          <div class="chart-container" v-show="!trendChartCollapsed">
            <!-- 替换占位符为实际图表 -->
            <div id="collectionTrendChart" style="width: 100%; height: 100%;"></div>
          </div>
          <div v-show="trendChartCollapsed" class="chart-collapsed-hint">
            <el-icon><Hide /></el-icon> 图表已收起，点击"展开"按钮查看
          </div>
        </el-card>

        <!-- 收藏分布 -->
        <el-row :gutter="20" class="distribution-section">
          <el-col :span="12">
            <el-card class="distribution-chart">
              <template #header>
                <div class="chart-header">
                  <span>类型分布</span>
                  <el-button 
                    type="text" 
                    :icon="typeChartCollapsed ? 'ArrowDown' : 'ArrowUp'" 
                    @click="typeChartCollapsed = !typeChartCollapsed"
                  >
                    {{ typeChartCollapsed ? '展开' : '收起' }}
                  </el-button>
                </div>
              </template>
              <div class="chart-container" v-show="!typeChartCollapsed">
                <!-- 替换饼图占位符为实际图表 -->
                <div id="collectionTypeChart" style="width: 100%; height: 100%;"></div>
              </div>
              <div v-show="typeChartCollapsed" class="chart-collapsed-hint">
                <el-icon><Hide /></el-icon> 图表已收起，点击"展开"按钮查看
              </div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card class="distribution-chart">
              <template #header>
                <div class="chart-header">
                  <span>时间分布</span>
                  <el-button 
                    type="text" 
                    :icon="timeChartCollapsed ? 'ArrowDown' : 'ArrowUp'" 
                    @click="timeChartCollapsed = !timeChartCollapsed"
                  >
                    {{ timeChartCollapsed ? '展开' : '收起' }}
                  </el-button>
                </div>
              </template>
              <div class="chart-container" v-show="!timeChartCollapsed">
                <!-- 替换热力图占位符为实际图表 -->
                <div id="collectionTimeChart" style="width: 100%; height: 100%;"></div>
              </div>
              <div v-show="timeChartCollapsed" class="chart-collapsed-hint">
                <el-icon><Hide /></el-icon> 图表已收起，点击"展开"按钮查看
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, nextTick, watch } from 'vue'
import {
  Star, Collection, Document, Grid,
  Download, Delete, Refresh, Search,
  Edit, Link, Plus, Folder, Hide
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

// 收藏统计
const collectionStats = ref([
  {
    label: '全部收藏',
    value: '128',
    icon: 'Collection',
    type: 'primary'
  },
  {
    label: '菜单页面',
    value: '45',
    icon: 'Grid',
    type: 'success'
  },
  {
    label: '数据报表',
    value: '36',
    icon: 'Document',
    type: 'warning'
  },
  {
    label: '常用功能',
    value: '47',
    icon: 'Star',
    type: 'danger'
  }
])

// 标签选项
const tagOptions = ref([
  { label: '常用', value: 'common' },
  { label: '重要', value: 'important' },
  { label: '工作', value: 'work' },
  { label: '学习', value: 'study' },
  { label: '临时', value: 'temp' }
])

// 搜索相关
const searchForm = ref({
  type: '',
  tags: [],
  timeRange: []
})

// 日期快捷选项
const dateShortcuts = [
  {
    text: '最近一周',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7)
      return [start, end]
    }
  },
  {
    text: '最近一月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30)
      return [start, end]
    }
  },
  {
    text: '最近三月',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 90)
      return [start, end]
    }
  }
]

// 列表数据
const loading = ref(false)
const collectionList = ref([
  {
    id: 1,
    type: 'MENU',
    title: '数据分析工作台',
    description: '包含核心指标监控、实时数据分析、趋势图表等功能的数据分析工作台。',
    tags: ['常用', '工作'],
    createTime: '2024-01-15 10:00:00',
    visits: 128,
    selected: false
  },
  {
    id: 2,
    type: 'REPORT',
    title: '月度数据报表',
    description: '系统运营数据月度统计报表，包含用户增长、活跃度、转化率等核心指标。',
    tags: ['重要', '工作'],
    createTime: '2024-01-14 15:30:00',
    visits: 56,
    selected: false
  }
])

// 分页配置
const page = ref({
  current: 1,
  size: 10
})
const total = ref(100)

// 获取收藏图标
const getCollectionIcon = (type) => {
  const icons = {
    'MENU': 'Grid',
    'REPORT': 'Document',
    'FUNCTION': 'Star',
    'OTHER': 'Link'
  }
  return icons[type] || 'Star'
}

// 搜索方法
const handleSearch = () => {
  page.value.current = 1
  fetchList()
}

const resetSearch = () => {
  searchForm.value = {
    type: '',
    tags: [],
    timeRange: []
  }
  handleSearch()
}

// 分页方法
const handleSizeChange = (val) => {
  page.value.size = val
  fetchList()
}

const handleCurrentChange = (val) => {
  page.value.current = val
  fetchList()
}

// 获取列表数据
const fetchList = () => {
  loading.value = true
  // 模拟接口请求
  setTimeout(() => {
    loading.value = false
  }, 500)
}

// 查看收藏
const handleViewCollection = (item) => {
  // TODO: 实现查看收藏详情或跳转
  ElMessage.success('正在跳转到：' + item.title)
}

// 编辑收藏
const handleEdit = (item) => {
  editForm.value = {
    id: item.id,
    type: item.type,
    title: item.title,
    description: item.description,
    tags: item.tags,
    categoryId: item.categoryId,
    remark: item.remark
  }
  editDialog.value = true
}

// 删除收藏
const handleDelete = (item) => {
  ElMessageBox.confirm(
    '确定要取消收藏该内容吗？',
    '取消确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      // 调用删除接口
      await new Promise(resolve => setTimeout(resolve, 500))
      collectionList.value = collectionList.value.filter(i => i.id !== item.id)
      ElMessage.success('已取消收藏')
    } catch (error) {
      ElMessage.error('操作失败')
    }
  })
}

// 批量导出
const handleBatchExport = async () => {
  const selected = collectionList.value.filter(item => item.selected)
  if (!selected.length) {
    ElMessage.warning('请先选择要导出的收藏')
    return
  }

  try {
    // 显示导出选项
    const { action } = await ElMessageBox.prompt(
      '请选择导出格式',
      '导出收藏',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputType: 'select',
        inputValue: 'excel',
        inputPlaceholder: '请选择导出格式',
        inputOptions: [
          {
            label: 'Excel 文件',
            value: 'excel'
          },
          {
            label: 'PDF 文件',
            value: 'pdf'
          },
          {
            label: 'JSON 文件',
            value: 'json'
          }
        ]
      }
    )

    // 模拟导出过程
    ElMessage.success('正在导出收藏数据...')
    await new Promise(resolve => setTimeout(resolve, 1500))
    ElMessage.success(`已成功导出 ${selected.length} 条收藏`)
  } catch (error) {
    // 用户取消导出
    return
  }
}

// 批量删除
const handleBatchDelete = () => {
  const selected = collectionList.value.filter(item => item.selected)
  if (!selected.length) {
    ElMessage.warning('请先选择要删除的收藏')
    return
  }
  ElMessageBox.confirm(
    `确定要取消收藏选中的 ${selected.length} 条内容吗？`,
    '批量取消确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      // 调用批量删除接口
      await new Promise(resolve => setTimeout(resolve, 500))
      collectionList.value = collectionList.value.filter(item => !item.selected)
      ElMessage.success('批量取消成功')
    } catch (error) {
      ElMessage.error('操作失败')
    }
  })
}

// 刷新列表
const refreshList = () => {
  fetchList()
}

// 编辑相关
const editDialog = ref(false)
const editFormRef = ref(null)
const editForm = reactive({
  id: '',
  type: '',
  title: '',
  description: '',
  tags: [],
  categoryId: null,
  remark: ''
})

// 表单校验规则
const editRules = {
  type: [
    { required: true, message: '请选择收藏类型', trigger: 'change' }
  ],
  title: [
    { required: true, message: '请输入收藏标题', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  description: [
    { required: true, message: '请输入收藏描述', trigger: 'blur' },
    { min: 2, max: 200, message: '长度在 2 到 200 个字符', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择所属分类', trigger: 'change' }
  ]
}

// 保存收藏
const handleSaveCollection = () => {
  editFormRef.value?.validate(async (valid) => {
    if (valid) {
      try {
        // 调用保存接口
        await new Promise(resolve => setTimeout(resolve, 500))
        ElMessage.success(editForm.id ? '编辑成功' : '新增成功')
        editDialog.value = false
        refreshList()
      } catch (error) {
        ElMessage.error('保存失败')
      }
    }
  })
}

// 分类管理相关
const categoryDrawer = ref(false)
const categoryDialog = ref(false)
const categoryTreeRef = ref(null)
const categoryKeyword = ref('')

// 分类表单
const categoryFormRef = ref(null)
const categoryForm = reactive({
  id: '',
  parentId: null,
  name: '',
  sort: 0,
  remark: ''
})

// 分类校验规则
const categoryRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  sort: [
    { required: true, message: '请输入排序号', trigger: 'blur' }
  ]
}

// 分类树数据
const categoryTree = ref([
  {
    id: 1,
    name: '常用收藏',
    count: 25,
    children: [
      {
        id: 11,
        name: '工作相关',
        count: 15
      },
      {
        id: 12,
        name: '学习资料',
        count: 10
      }
    ]
  },
  {
    id: 2,
    name: '数据分析',
    count: 18,
    children: [
      {
        id: 21,
        name: '数据报表',
        count: 8
      },
      {
        id: 22,
        name: '分析工具',
        count: 10
      }
    ]
  }
])

// 分类选项（扁平化处理后的数据）
const categoryOptions = computed(() => {
  const flatten = (nodes) => {
    return nodes.reduce((result, node) => {
      result.push({
        id: node.id,
        name: node.name
      })
      if (node.children) {
        result.push(...flatten(node.children))
      }
      return result
    }, [])
  }
  return flatten(categoryTree.value)
})

// 过滤分类节点
const filterNode = (value, data) => {
  if (!value) return true
  return data.name.includes(value)
}

// 搜索分类
const filterCategories = (val) => {
  categoryTreeRef.value?.filter(val)
}

// 新增分类
const handleAddCategory = () => {
  categoryForm.id = ''
  categoryForm.parentId = null
  categoryForm.name = ''
  categoryForm.sort = 0
  categoryForm.remark = ''
  categoryDialog.value = true
}

// 新增子分类
const handleAddSubCategory = (data) => {
  categoryForm.id = ''
  categoryForm.parentId = data.id
  categoryForm.name = ''
  categoryForm.sort = 0
  categoryForm.remark = ''
  categoryDialog.value = true
}

// 编辑分类
const handleEditCategory = (data) => {
  categoryForm.id = data.id
  categoryForm.parentId = data.parentId
  categoryForm.name = data.name
  categoryForm.sort = data.sort || 0
  categoryForm.remark = data.remark
  categoryDialog.value = true
}

// 删除分类
const handleDeleteCategory = (data) => {
  if (data.children?.length) {
    ElMessage.warning('该分类下存在子分类，无法删除')
    return
  }
  ElMessageBox.confirm(
    '确定要删除该分类吗？删除后不可恢复！',
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      // 调用删除接口
      await new Promise(resolve => setTimeout(resolve, 500))
      ElMessage.success('删除成功')
      // TODO: 更新分类树
    } catch (error) {
      ElMessage.error('删除失败')
    }
  })
}

// 保存分类
const handleSaveCategory = () => {
  categoryFormRef.value?.validate(async (valid) => {
    if (valid) {
      try {
        // 调用保存接口
        await new Promise(resolve => setTimeout(resolve, 500))
        ElMessage.success(categoryForm.id ? '编辑成功' : '新增成功')
        categoryDialog.value = false
        // TODO: 更新分类树
      } catch (error) {
        ElMessage.error('保存失败')
      }
    }
  })
}

// 分类拖拽排序
const handleDragEnd = async (draggingNode, dropNode, dropType) => {
  try {
    // 调用排序接口
    await new Promise(resolve => setTimeout(resolve, 500))
    ElMessage.success('移动成功')
  } catch (error) {
    ElMessage.error('移动失败')
    // 恢复原位置
    await nextTick()
    categoryTreeRef.value?.updateKeyChildren(draggingNode.data.id, draggingNode.data.children)
  }
}

// 分析相关
const analysisDrawer = ref(false)
const analysisTimeRange = ref('week')
const trendType = ref('count')

// 图表折叠状态
const trendChartCollapsed = ref(false)
const typeChartCollapsed = ref(false)
const timeChartCollapsed = ref(false)

// 图表实例
const trendChartInstance = ref(null)
const typeChartInstance = ref(null)
const timeChartInstance = ref(null)

// 分析统计数据
const analysisStats = ref([
  {
    label: '收藏总数',
    value: '256',
    icon: 'Collection',
    type: 'primary',
    trend: 12.5
  },
  {
    label: '本周新增',
    value: '32',
    icon: 'Plus',
    type: 'success',
    trend: 8.3
  },
  {
    label: '访问次数',
    value: '1,024',
    icon: 'View',
    type: 'warning',
    trend: -3.2
  }
])

// 处理分析时间范围变化
const handleAnalysisTimeChange = (value) => {
  // TODO: 根据时间范围更新分析数据
  console.log('Analysis time range changed:', value)
}

// 导出分析报告
const exportAnalysisData = () => {
  ElMessage.success('正在导出分析报告...')
  // TODO: 实现导出功能
}

// 监听抽屉打开状态，初始化图表
watch(analysisDrawer, (newVal) => {
  if (newVal) {
    // 重置折叠状态
    trendChartCollapsed.value = false
    typeChartCollapsed.value = false
    timeChartCollapsed.value = false
    
    nextTick(() => {
      initCollectionCharts()
    })
  }
})

// 监听图表折叠状态变化，在展开时重新渲染图表
watch([trendChartCollapsed, typeChartCollapsed, timeChartCollapsed], ([newTrendCollapsed, newTypeCollapsed, newTimeCollapsed], [oldTrendCollapsed, oldTypeCollapsed, oldTimeCollapsed]) => {
  nextTick(() => {
    // 趋势图从折叠变为展开
    if (oldTrendCollapsed && !newTrendCollapsed) {
      if (trendChartInstance.value) {
        trendChartInstance.value.resize()
        renderCollectionTrendChart()
      } else {
        trendChartInstance.value = echarts.init(document.getElementById('collectionTrendChart'))
        renderCollectionTrendChart()
      }
    }
    
    // 类型图从折叠变为展开
    if (oldTypeCollapsed && !newTypeCollapsed) {
      if (typeChartInstance.value) {
        typeChartInstance.value.resize()
        renderCollectionTypeChart()
      } else {
        typeChartInstance.value = echarts.init(document.getElementById('collectionTypeChart'))
        renderCollectionTypeChart()
      }
    }
    
    // 时间图从折叠变为展开
    if (oldTimeCollapsed && !newTimeCollapsed) {
      if (timeChartInstance.value) {
        timeChartInstance.value.resize()
        renderCollectionTimeChart()
      } else {
        timeChartInstance.value = echarts.init(document.getElementById('collectionTimeChart'))
        renderCollectionTimeChart()
      }
    }
  })
})
</script>

<style scoped>
.collection-container {
  padding: 20px;
}

.overview-section {
  margin-bottom: 20px;
}

.stat-card {
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 8px;
}

.stat-card.primary .stat-icon {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.stat-card.success .stat-icon {
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
}

.stat-card.warning .stat-icon {
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
}

.stat-card.danger .stat-icon {
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  line-height: 1.5;
}

.stat-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.card-header {
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

.search-form {
  margin-bottom: 20px;
  padding: 20px;
  background: var(--el-bg-color-page);
  border-radius: 4px;
}

.search-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.collection-list {
  min-height: 300px;
}

.collection-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  transition: all 0.3s;
}

.collection-item:last-child {
  border-bottom: none;
}

.collection-item:hover {
  background: var(--el-bg-color-page);
}

.item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  flex-shrink: 0;
}

.item-icon.menu {
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
}

.item-icon.report {
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
}

.item-icon.function {
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
}

.item-icon.other {
  background: var(--el-color-info-light-9);
  color: var(--el-color-info);
}

.item-content {
  flex: 1;
  min-width: 0;
  cursor: pointer;
}

.item-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.item-title {
  font-size: 16px;
  font-weight: bold;
}

.item-tags {
  display: flex;
  gap: 8px;
}

.item-desc {
  margin-bottom: 8px;
  color: var(--el-text-color-regular);
}

.item-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.item-info {
  display: flex;
  align-items: center;
  gap: 16px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.item-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s;
}

.collection-item:hover .item-actions {
  opacity: 1;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}

/* 分类管理样式 */
.category-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0 20px;
}

.category-toolbar {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.category-tree {
  flex: 1;
  overflow: auto;
  background: var(--el-bg-color-page);
  border-radius: 4px;
  padding: 16px;
}

.category-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.node-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.node-count {
  margin-left: 8px;
}

.node-actions {
  opacity: 0;
  transition: opacity 0.3s;
}

.category-node:hover .node-actions {
  opacity: 1;
}

/* 分析相关样式 */
.analysis-container {
  padding: 20px;
}

.analysis-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.analysis-stats {
  margin-bottom: 20px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  margin-top: 4px;
}

.stat-trend.up {
  color: var(--el-color-success);
}

.stat-trend.down {
  color: var(--el-color-danger);
}

.trend-chart {
  margin-bottom: 20px;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chart-container {
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-bg-color-page);
  border-radius: 4px;
  transition: height 0.3s ease;
}

.chart-collapsed-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 60px;
  color: var(--el-text-color-secondary);
  background: var(--el-bg-color-page);
  border-radius: 4px;
  font-size: 14px;
}

.chart-collapsed-hint .el-icon {
  margin-right: 8px;
}

.distribution-section {
  margin-bottom: 20px;
}

.distribution-chart .chart-container {
  height: 240px;
}

.chart-placeholder {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
</style> 