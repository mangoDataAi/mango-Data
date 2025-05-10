<template>
  <div class="query-history">
    <div class="toolbar">
      <el-input
        v-model="searchQuery"
        placeholder="搜索查询"
        prefix-icon="Search"
        clearable
      >
        <template #append>
          <el-button :icon="Filter" @click="showFilterDrawer = true">
            筛选
          </el-button>
        </template>
      </el-input>

      <el-button-group>
        <el-button
          type="primary"
          :icon="Star"
          @click="showSaveDialog = true"
        >
          保存当前查询
        </el-button>
        <el-button
          :icon="Delete"
          @click="handleBatchDelete"
          :disabled="!selectedQueries.length"
        >
          删除
        </el-button>
      </el-button-group>
    </div>

    <el-tabs v-model="activeTab" class="history-tabs">
      <el-tab-pane label="最近查询" name="recent">
        <div class="query-list">
          <el-table
            :data="filteredQueries"
            style="width: 100%"
            @selection-change="handleSelectionChange"
          >
            <el-table-column type="selection" width="50" />
            <el-table-column width="30">
              <template #default="{ row }">
                <el-icon
                  :class="{ 'is-favorite': row.favorite }"
                  @click="toggleFavorite(row)"
                >
                  <Star />
                </el-icon>
              </template>
            </el-table-column>
            <el-table-column prop="name" label="查询名称">
              <template #default="{ row }">
                <div class="query-name">
                  <span>{{ row.name }}</span>
                  <el-tag
                    v-for="tag in row.tags"
                    :key="tag"
                    size="small"
                    class="query-tag"
                  >
                    {{ tag }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="executionTime" label="执行时间" width="180">
              <template #default="{ row }">
                {{ formatTime(row.executionTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="duration" label="耗时" width="120">
              <template #default="{ row }">
                {{ formatDuration(row.duration) }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button-group>
                  <el-button
                    type="primary"
                    link
                    @click="handleUse(row)"
                  >
                    使用
                  </el-button>
                  <el-button
                    type="primary"
                    link
                    @click="handleEdit(row)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    type="danger"
                    link
                    @click="handleDelete(row)"
                  >
                    删除
                  </el-button>
                </el-button-group>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="totalQueries"
              layout="total, sizes, prev, pager, next"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="收藏查询" name="favorites">
        <div class="favorites-list">
          <el-row :gutter="16">
            <el-col
              v-for="query in favoriteQueries"
              :key="query.id"
              :span="8"
            >
              <el-card class="favorite-card" shadow="hover">
                <template #header>
                  <div class="card-header">
                    <span class="name">{{ query.name }}</span>
                    <el-dropdown trigger="click">
                      <el-button type="primary" link>
                        <el-icon><More /></el-icon>
                      </el-button>
                      <template #dropdown>
                        <el-dropdown-menu>
                          <el-dropdown-item @click="handleUse(query)">
                            使用查询
                          </el-dropdown-item>
                          <el-dropdown-item @click="handleEdit(query)">
                            编辑查询
                          </el-dropdown-item>
                          <el-dropdown-item
                            divided
                            @click="toggleFavorite(query)"
                          >
                            取消收藏
                          </el-dropdown-item>
                          <el-dropdown-item
                            divided
                            danger
                            @click="handleDelete(query)"
                          >
                            删除查询
                          </el-dropdown-item>
                        </el-dropdown-menu>
                      </template>
                    </el-dropdown>
                  </div>
                </template>
                <div class="card-content">
                  <div class="tags">
                    <el-tag
                      v-for="tag in query.tags"
                      :key="tag"
                      size="small"
                      class="query-tag"
                    >
                      {{ tag }}
                    </el-tag>
                  </div>
                  <pre class="sql-preview">{{ query.sql }}</pre>
                  <div class="meta">
                    <span>最后执行: {{ formatTime(query.executionTime) }}</span>
                    <span>耗时: {{ formatDuration(query.duration) }}</span>
                  </div>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 保存查询对话框 -->
    <el-dialog
      v-model="showSaveDialog"
      title="保存查询"
      width="500px"
    >
      <el-form :model="saveForm" label-width="100px">
        <el-form-item label="查询名称" required>
          <el-input
            v-model="saveForm.name"
            placeholder="请输入查询名称"
          />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="saveForm.description"
            type="textarea"
            placeholder="请输入查询描述"
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="saveForm.tags"
            multiple
            filterable
            allow-create
            placeholder="请选择或创建标签"
          >
            <el-option
              v-for="tag in availableTags"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="收藏">
          <el-switch v-model="saveForm.favorite" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showSaveDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSave">
            保存
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 筛选抽屉 -->
    <el-drawer
      v-model="showFilterDrawer"
      title="筛选查询"
      direction="rtl"
    >
      <el-form :model="filterForm">
        <el-form-item label="执行时间">
          <el-date-picker
            v-model="filterForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
          />
        </el-form-item>
        <el-form-item label="执行状态">
          <el-select
            v-model="filterForm.status"
            multiple
            placeholder="请选择状态"
          >
            <el-option label="成功" value="success" />
            <el-option label="失败" value="error" />
            <el-option label="取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-select
            v-model="filterForm.tags"
            multiple
            filterable
            placeholder="请选择标签"
          >
            <el-option
              v-for="tag in availableTags"
              :key="tag"
              :label="tag"
              :value="tag"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="执行时长">
          <el-slider
            v-model="filterForm.duration"
            range
            :min="0"
            :max="maxDuration"
            :format-tooltip="formatDurationTooltip"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div style="flex: auto">
          <el-button @click="resetFilter">重置</el-button>
          <el-button type="primary" @click="applyFilter">
            应用筛选
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import {
  Search, Filter, Star, Delete, More
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  currentSql: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['select'])

// 状态变量
const activeTab = ref('recent')
const searchQuery = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const selectedQueries = ref([])
const showSaveDialog = ref(false)
const showFilterDrawer = ref(false)

// 表单数据
const saveForm = ref({
  name: '',
  description: '',
  tags: [],
  favorite: false
})

const filterForm = ref({
  timeRange: null,
  status: [],
  tags: [],
  duration: [0, 100]
})

// 模拟数据
const queries = ref([
  {
    id: 1,
    name: '用户行为分析',
    sql: 'SELECT event_type, COUNT(*) as count FROM user_behavior GROUP BY event_type',
    description: '分析用户行为类型分布',
    tags: ['分析', '统计'],
    executionTime: Date.now() - 3600000,
    duration: 1234,
    status: 'success',
    favorite: true
  },
  {
    id: 2,
    name: '地区分布统计',
    sql: 'SELECT region, COUNT(DISTINCT user_id) as users FROM user_behavior GROUP BY region',
    description: '统计用户地区分布',
    tags: ['分析', '地区'],
    executionTime: Date.now() - 7200000,
    duration: 2345,
    status: 'error',
    favorite: false
  }
])

const availableTags = ref(['分析', '统计', '监控', '报表', '地区'])

// 计算属性
const filteredQueries = computed(() => {
  let result = queries.value

  if (searchQuery.value) {
    const search = searchQuery.value.toLowerCase()
    result = result.filter(q =>
      q.name.toLowerCase().includes(search) ||
      q.sql.toLowerCase().includes(search) ||
      q.tags.some(t => t.toLowerCase().includes(search))
    )
  }

  // 应用筛选
  if (filterForm.value.timeRange) {
    const [start, end] = filterForm.value.timeRange
    result = result.filter(q =>
      q.executionTime >= start && q.executionTime <= end
    )
  }

  if (filterForm.value.status.length) {
    result = result.filter(q =>
      filterForm.value.status.includes(q.status)
    )
  }

  if (filterForm.value.tags.length) {
    result = result.filter(q =>
      filterForm.value.tags.some(t => q.tags.includes(t))
    )
  }

  const [minDuration, maxDuration] = filterForm.value.duration
  result = result.filter(q =>
    q.duration >= minDuration && q.duration <= maxDuration
  )

  return result
})

const favoriteQueries = computed(() =>
  queries.value.filter(q => q.favorite)
)

const totalQueries = computed(() =>
  filteredQueries.value.length
)

const maxDuration = computed(() =>
  Math.max(...queries.value.map(q => q.duration))
)

// 格式化函数
const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleString()
}

const formatDuration = (ms) => {
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  const minutes = Math.floor(ms / 60000)
  const seconds = ((ms % 60000) / 1000).toFixed(1)
  return `${minutes}m ${seconds}s`
}

const formatDurationTooltip = (val) => {
  return formatDuration(val)
}

const getStatusType = (status) => {
  const types = {
    success: 'success',
    error: 'danger',
    cancelled: 'info'
  }
  return types[status] || 'info'
}

// 处理函数
const handleSelectionChange = (selection) => {
  selectedQueries.value = selection
}

const toggleFavorite = (query) => {
  query.favorite = !query.favorite
}

const handleUse = (query) => {
  emit('select', query)
}

const handleEdit = (query) => {
  saveForm.value = {
    name: query.name,
    description: query.description,
    tags: [...query.tags],
    favorite: query.favorite
  }
  showSaveDialog.value = true
}

const handleDelete = async (query) => {
  try {
    await ElMessageBox.confirm(
      '确定要删除该查询吗？',
      '删除确认',
      {
        type: 'warning'
      }
    )
    const index = queries.value.findIndex(q => q.id === query.id)
    if (index > -1) {
      queries.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  } catch {
    // 用户取消删除
  }
}

const handleBatchDelete = async () => {
  if (!selectedQueries.value.length) return

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedQueries.value.length} 条查询吗？`,
      '批量删除确认',
      {
        type: 'warning'
      }
    )
    const ids = selectedQueries.value.map(q => q.id)
    queries.value = queries.value.filter(q => !ids.includes(q.id))
    ElMessage.success('删除成功')
  } catch {
    // 用户取消删除
  }
}

const handleSave = () => {
  if (!saveForm.value.name) {
    ElMessage.warning('请输入查询名称')
    return
  }

  // TODO: 保存查询
  const query = {
    id: Date.now(),
    ...saveForm.value,
    sql: props.currentSql,
    executionTime: Date.now(),
    duration: 0,
    status: 'success'
  }
  queries.value.unshift(query)

  showSaveDialog.value = false
  ElMessage.success('保存成功')
}

const resetFilter = () => {
  filterForm.value = {
    timeRange: null,
    status: [],
    tags: [],
    duration: [0, maxDuration.value]
  }
}

const applyFilter = () => {
  showFilterDrawer.value = false
}
</script>

<style lang="scss" scoped>
.query-history {
  height: 100%;
  display: flex;
  flex-direction: column;

  .toolbar {
    padding: 16px;
    display: flex;
    gap: 16px;
    align-items: center;

    .el-input {
      width: 300px;
    }
  }

  .history-tabs {
    flex: 1;
    display: flex;
    flex-direction: column;

    :deep(.el-tabs__content) {
      flex: 1;
      padding: 16px;
      overflow: auto;
    }
  }

  .query-list {
    .query-name {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .query-tag {
      font-size: 12px;
    }

    .pagination {
      margin-top: 16px;
      display: flex;
      justify-content: flex-end;
    }

    :deep(.el-icon) {
      &.is-favorite {
        color: var(--el-color-warning);
      }
    }
  }

  .favorites-list {
    .favorite-card {
      margin-bottom: 16px;

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .name {
          font-weight: 500;
        }
      }

      .card-content {
        .tags {
          margin-bottom: 12px;

          .query-tag {
            margin-right: 8px;
            margin-bottom: 8px;
          }
        }

        .sql-preview {
          margin: 0;
          padding: 12px;
          background: #f5f7fa;
          border-radius: 4px;
          font-family: monospace;
          font-size: 12px;
          max-height: 100px;
          overflow: auto;
        }

        .meta {
          margin-top: 12px;
          display: flex;
          justify-content: space-between;
          color: #909399;
          font-size: 12px;
        }
      }
    }
  }
}
</style>
</code_block_to_apply_changes_from>