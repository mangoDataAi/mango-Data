<template>
  <div class="table-optimize">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">表优化配置</h2>
        <el-tag>{{ tableInfo.name }}</el-tag>
        <el-tag type="success">{{ tableInfo.type }}</el-tag>
      </div>
      <div class="header-actions">
        <el-button-group>
          <el-button type="primary" @click="handleSaveConfig">
            <el-icon><Check /></el-icon>保存配置
          </el-button>
          <el-button type="success" @click="handleApplyOptimize">
            <el-icon><SetUp /></el-icon>应用优化
          </el-button>
        </el-button-group>
      </div>
    </div>

    <el-row :gutter="20" class="optimize-content">
      <!-- 左侧配置面板 -->
      <el-col :span="16">
        <!-- 分区配置 -->
        <el-card class="config-card">
          <template #header>
            <div class="card-header">
              <span>分区配置</span>
              <el-button type="primary" link @click="handleAddPartition">
                添加分区
              </el-button>
            </div>
          </template>

          <div class="partition-list">
            <div
              v-for="(partition, index) in optimizeConfig.partitions"
              :key="index"
              class="partition-item"
            >
              <el-form :model="partition" inline>
                <el-form-item label="分区字段">
                  <el-select v-model="partition.field" placeholder="选择字段">
                    <el-option
                      v-for="field in tableFields"
                      :key="field.name"
                      :label="field.label"
                      :value="field.name"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="分区类型">
                  <el-select v-model="partition.type" placeholder="选择类型">
                    <el-option label="范围分区" value="range" />
                    <el-option label="列表分区" value="list" />
                    <el-option label="哈希分区" value="hash" />
                  </el-select>
                </el-form-item>
                <el-form-item v-if="partition.type === 'hash'" label="分桶数">
                  <el-input-number v-model="partition.buckets" :min="1" :max="128" />
                </el-form-item>
                <el-form-item v-if="partition.type === 'range'">
                  <el-button @click="handleConfigRange(index)">配置范围</el-button>
                </el-form-item>
                <el-form-item v-if="partition.type === 'list'">
                  <el-button @click="handleConfigList(index)">配置列表</el-button>
                </el-form-item>
                <el-form-item>
                  <el-button
                    type="danger"
                    circle
                    @click="handleRemovePartition(index)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-form-item>
              </el-form>
            </div>
          </div>
        </el-card>

        <!-- 索引配置 -->
        <el-card class="config-card">
          <template #header>
            <div class="card-header">
              <span>索引配置</span>
              <el-button type="primary" link @click="handleAddIndex">
                添加索引
              </el-button>
            </div>
          </template>

          <div class="index-list">
            <el-table :data="optimizeConfig.indexes" border style="width: 100%">
              <el-table-column label="索引名称" min-width="150">
                <template #default="{ row }">
                  <el-input v-model="row.name" placeholder="输入索引名称" />
                </template>
              </el-table-column>
              <el-table-column label="索引字段" min-width="200">
                <template #default="{ row }">
                  <el-select
                    v-model="row.fields"
                    multiple
                    placeholder="选择字段"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="field in tableFields"
                      :key="field.name"
                      :label="field.label"
                      :value="field.name"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="索引类型" width="150">
                <template #default="{ row }">
                  <el-select v-model="row.type" placeholder="选择类型">
                    <el-option label="B-tree" value="btree" />
                    <el-option label="Bitmap" value="bitmap" />
                    <el-option label="Hash" value="hash" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="100">
                <template #default="{ $index }">
                  <el-button
                    type="danger"
                    circle
                    @click="handleRemoveIndex($index)"
                  >
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>

        <!-- 压缩配置 -->
        <el-card class="config-card">
          <template #header>
            <div class="card-header">
              <span>压缩配置</span>
            </div>
          </template>

          <el-form :model="optimizeConfig.compression" label-width="120px">
            <el-form-item label="压缩算法">
              <el-select v-model="optimizeConfig.compression.algorithm">
                <el-option label="无压缩" value="none" />
                <el-option label="Snappy" value="snappy" />
                <el-option label="LZ4" value="lz4" />
                <el-option label="ZSTD" value="zstd" />
              </el-select>
            </el-form-item>
            <el-form-item label="压缩级别" v-if="optimizeConfig.compression.algorithm === 'zstd'">
              <el-slider
                v-model="optimizeConfig.compression.level"
                :min="1"
                :max="22"
                :marks="{
                  1: '最快',
                  11: '平衡',
                  22: '最高'
                }"
              />
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧状态面板 -->
      <el-col :span="8">
        <!-- 优化建议 -->
        <el-card class="status-card">
          <template #header>
            <div class="card-header">
              <span>优化建议</span>
              <el-button type="primary" link @click="handleAnalyze">
                开始分析
              </el-button>
            </div>
          </template>

          <div v-loading="analyzing" class="suggestion-list">
            <div
              v-for="(suggestion, index) in optimizeSuggestions"
              :key="index"
              class="suggestion-item"
              :class="suggestion.level"
            >
              <el-icon>
                <Warning v-if="suggestion.level === 'warning'" />
                <InfoFilled v-else-if="suggestion.level === 'info'" />
                <CircleCheck v-else />
              </el-icon>
              <div class="suggestion-content">
                <div class="suggestion-title">{{ suggestion.title }}</div>
                <div class="suggestion-desc">{{ suggestion.description }}</div>
              </div>
              <el-button
                v-if="suggestion.action"
                type="primary"
                link
                @click="handleApplySuggestion(suggestion)"
              >
                应用建议
              </el-button>
            </div>
          </div>
        </el-card>

        <!-- 性能指标 -->
        <el-card class="status-card">
          <template #header>
            <div class="card-header">
              <span>性能指标</span>
              <el-tag type="success">{{ performanceScore }}分</el-tag>
            </div>
          </template>

          <div class="metrics-list">
            <div
              v-for="metric in performanceMetrics"
              :key="metric.name"
              class="metric-item"
            >
              <div class="metric-header">
                <span>{{ metric.label }}</span>
                <el-tag :type="getMetricTagType(metric.score)">
                  {{ metric.score }}分
                </el-tag>
              </div>
              <el-progress
                :percentage="metric.score"
                :color="getMetricColor(metric.score)"
              />
              <div class="metric-detail">{{ metric.detail }}</div>
            </div>
          </div>
        </el-card>

        <!-- 优化历史 -->
        <el-card class="status-card">
          <template #header>
            <div class="card-header">
              <span>优化历史</span>
            </div>
          </template>

          <el-timeline>
            <el-timeline-item
              v-for="(history, index) in optimizeHistory"
              :key="index"
              :type="history.status"
              :timestamp="history.time"
            >
              {{ history.action }}
              <div class="history-detail">{{ history.detail }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <!-- 范围配置对话框 -->
    <el-dialog
      v-model="rangeDialogVisible"
      title="配置范围分区"
      width="500px"
    >
      <el-form :model="rangeConfig" label-width="100px">
        <el-form-item label="起始值">
          <el-input v-model="rangeConfig.start" />
        </el-form-item>
        <el-form-item label="结束值">
          <el-input v-model="rangeConfig.end" />
        </el-form-item>
        <el-form-item label="步长">
          <el-input v-model="rangeConfig.step" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="rangeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveRange">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 列表配置对话框 -->
    <el-dialog
      v-model="listDialogVisible"
      title="配置列表分区"
      width="500px"
    >
      <div class="list-config">
        <div
          v-for="(value, index) in listConfig.values"
          :key="index"
          class="list-item"
        >
          <el-input v-model="listConfig.values[index]" />
          <el-button
            type="danger"
            circle
            @click="listConfig.values.splice(index, 1)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <el-button type="primary" link @click="listConfig.values.push('')">
          添加值
        </el-button>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="listDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSaveList">
            确定
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Check, SetUp, Delete, Warning,
  InfoFilled, CircleCheck
} from '@element-plus/icons-vue'

// 表信息
const tableInfo = reactive({
  name: 'user_behavior_detail',
  type: 'Delta Lake'
})

// 表字段
const tableFields = [
  { name: 'id', label: 'ID', type: 'bigint' },
  { name: 'user_id', label: '用户ID', type: 'string' },
  { name: 'event_type', label: '事件类型', type: 'string' },
  { name: 'event_time', label: '事件时间', type: 'timestamp' },
  { name: 'region', label: '地区', type: 'string' },
  { name: 'platform', label: '平台', type: 'string' },
  { name: 'properties', label: '属性', type: 'json' }
]

// 优化配置
const optimizeConfig = reactive({
  partitions: [],
  indexes: [],
  compression: {
    algorithm: 'none',
    level: 11
  }
})

// 范围配置
const rangeDialogVisible = ref(false)
const currentPartitionIndex = ref(0)
const rangeConfig = reactive({
  start: '',
  end: '',
  step: ''
})

// 列表配置
const listDialogVisible = ref(false)
const listConfig = reactive({
  values: []
})

// 优化建议
const analyzing = ref(false)
const optimizeSuggestions = ref([
  {
    level: 'warning',
    title: '建议添加时间分区',
    description: '根据查询模式分析，建议对event_time字段添加范围分区以提升查询性能。',
    action: true
  },
  {
    level: 'info',
    title: '建议创建索引',
    description: '频繁查询的user_id字段建议创建B-tree索引。',
    action: true
  },
  {
    level: 'success',
    title: '压缩配置合理',
    description: '当前压缩配置适合数据特征。',
    action: false
  }
])

// 性能指标
const performanceScore = ref(85)
const performanceMetrics = ref([
  {
    label: '查询性能',
    score: 75,
    detail: '平均查询延迟: 200ms'
  },
  {
    label: '存储效率',
    score: 90,
    detail: '压缩比: 5.2:1'
  },
  {
    label: '写入性能',
    score: 85,
    detail: '平均写入延迟: 50ms'
  }
])

// 优化历史
const optimizeHistory = ref([
  {
    time: '2024-03-15 15:30:00',
    action: '添加时间分区',
    detail: '对event_time字段添加按天分区',
    status: 'success'
  },
  {
    time: '2024-03-15 15:00:00',
    action: '创建索引',
    detail: '对user_id字段创建B-tree索引',
    status: 'success'
  },
  {
    time: '2024-03-15 14:30:00',
    action: '修改压缩配置',
    detail: '将压缩算法修改为ZSTD',
    status: 'warning'
  }
])

// 添加分区
const handleAddPartition = () => {
  optimizeConfig.partitions.push({
    field: '',
    type: 'range',
    buckets: 32
  })
}

// 删除分区
const handleRemovePartition = (index) => {
  optimizeConfig.partitions.splice(index, 1)
}

// 配置范围分区
const handleConfigRange = (index) => {
  currentPartitionIndex.value = index
  const partition = optimizeConfig.partitions[index]
  if (partition.range) {
    rangeConfig.start = partition.range.start
    rangeConfig.end = partition.range.end
    rangeConfig.step = partition.range.step
  }
  rangeDialogVisible.value = true
}

// 保存范围配置
const handleSaveRange = () => {
  const partition = optimizeConfig.partitions[currentPartitionIndex.value]
  partition.range = {
    start: rangeConfig.start,
    end: rangeConfig.end,
    step: rangeConfig.step
  }
  rangeDialogVisible.value = false
}

// 配置列表分区
const handleConfigList = (index) => {
  currentPartitionIndex.value = index
  const partition = optimizeConfig.partitions[index]
  if (partition.list) {
    listConfig.values = [...partition.list.values]
  } else {
    listConfig.values = ['']
  }
  listDialogVisible.value = true
}

// 保存列表配置
const handleSaveList = () => {
  const partition = optimizeConfig.partitions[currentPartitionIndex.value]
  partition.list = {
    values: listConfig.values.filter(v => v)
  }
  listDialogVisible.value = false
}

// 添加索引
const handleAddIndex = () => {
  optimizeConfig.indexes.push({
    name: '',
    fields: [],
    type: 'btree'
  })
}

// 删除索引
const handleRemoveIndex = (index) => {
  optimizeConfig.indexes.splice(index, 1)
}

// 开始分析
const handleAnalyze = () => {
  analyzing.value = true
  setTimeout(() => {
    analyzing.value = false
    ElMessage.success('分析完成')
  }, 2000)
}

// 应用优化建议
const handleApplySuggestion = (suggestion) => {
  if (suggestion.title.includes('时间分区')) {
    optimizeConfig.partitions.push({
      field: 'event_time',
      type: 'range',
      range: {
        start: 'now() - interval 1 year',
        end: 'now()',
        step: '1 day'
      }
    })
  } else if (suggestion.title.includes('索引')) {
    optimizeConfig.indexes.push({
      name: 'idx_user_id',
      fields: ['user_id'],
      type: 'btree'
    })
  }
  ElMessage.success('已应用优化建议')
}

// 保存配置
const handleSaveConfig = () => {
  // TODO: 调用API保存配置
  ElMessage.success('配置已保存')
}

// 应用优化
const handleApplyOptimize = () => {
  // TODO: 调用API应用优化
  ElMessage.success('开始应用优化')
}

// 获取指标标签类型
const getMetricTagType = (score) => {
  if (score >= 90) return 'success'
  if (score >= 70) return 'warning'
  return 'danger'
}

// 获取指标颜色
const getMetricColor = (score) => {
  if (score >= 90) return '#67C23A'
  if (score >= 70) return '#E6A23C'
  return '#F56C6C'
}
</script>

<style lang="scss" scoped>
.table-optimize {
  padding: 20px;

  .page-header {
    margin-bottom: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .page-title {
        font-size: 24px;
        margin: 0;
      }
    }
  }

  .optimize-content {
    .config-card {
      margin-bottom: 20px;

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .partition-list {
        .partition-item {
          margin-bottom: 15px;
          padding: 15px;
          background: #f5f7fa;
          border-radius: 4px;

          &:last-child {
            margin-bottom: 0;
          }
        }
      }
    }

    .status-card {
      margin-bottom: 20px;

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .suggestion-list {
        .suggestion-item {
          display: flex;
          align-items: flex-start;
          gap: 10px;
          padding: 10px;
          margin-bottom: 10px;
          border-radius: 4px;

          &.warning {
            background: #fdf6ec;
            .el-icon {
              color: #e6a23c;
            }
          }

          &.info {
            background: #f4f4f5;
            .el-icon {
              color: #909399;
            }
          }

          &.success {
            background: #f0f9eb;
            .el-icon {
              color: #67c23a;
            }
          }

          .suggestion-content {
            flex: 1;

            .suggestion-title {
              font-weight: bold;
              margin-bottom: 4px;
            }

            .suggestion-desc {
              font-size: 12px;
              color: #606266;
            }
          }
        }
      }

      .metrics-list {
        .metric-item {
          margin-bottom: 20px;

          &:last-child {
            margin-bottom: 0;
          }

          .metric-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
          }

          .metric-detail {
            font-size: 12px;
            color: #909399;
            margin-top: 4px;
          }
        }
      }
    }
  }

  .list-config {
    .list-item {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 10px;

      &:last-child {
        margin-bottom: 0;
      }
    }
  }

  :deep(.el-timeline-item__content) {
    .history-detail {
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
    }
  }
}
</style> 