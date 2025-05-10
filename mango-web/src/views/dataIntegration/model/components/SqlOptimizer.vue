<template>
  <div class="sql-optimizer">
    <div class="toolbar">
      <el-button-group>
        <el-button
          type="primary"
          :icon="Operation"
          @click="analyze"
          :loading="analyzing"
        >
          分析SQL
        </el-button>
        <el-button
          :icon="Document"
          @click="formatSql"
        >
          格式化
        </el-button>
      </el-button-group>
    </div>

    <el-tabs v-model="activeTab" class="optimizer-tabs">
      <el-tab-pane label="优化建议" name="suggestions">
        <div v-if="suggestions.length" class="suggestions-list">
          <div
            v-for="(suggestion, index) in suggestions"
            :key="index"
            class="suggestion-item"
            :class="{ active: suggestion.selected }"
            @click="selectSuggestion(suggestion)"
          >
            <div class="suggestion-header">
              <div class="left">
                <el-tag :type="getSuggestionType(suggestion.impact)">
                  {{ suggestion.impact }}%
                </el-tag>
                <span class="title">{{ suggestion.title }}</span>
              </div>
              <div class="right">
                <el-button
                  v-if="suggestion.canApply"
                  type="primary"
                  link
                  @click.stop="applySuggestion(suggestion)"
                >
                  应用
                </el-button>
              </div>
            </div>
            
            <div class="suggestion-content">
              <p class="description">{{ suggestion.description }}</p>
              <div v-if="suggestion.example" class="example">
                <div class="example-header">
                  <span>优化示例</span>
                  <el-button
                    type="primary"
                    link
                    size="small"
                    @click.stop="copySql(suggestion.example)"
                  >
                    复制
                  </el-button>
                </div>
                <pre><code>{{ suggestion.example }}</code></pre>
              </div>
              <div v-if="suggestion.explanation" class="explanation">
                <el-collapse>
                  <el-collapse-item title="详细说明">
                    <div v-html="suggestion.explanation"></div>
                  </el-collapse-item>
                </el-collapse>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无优化建议" />
      </el-tab-pane>

      <el-tab-pane label="执行计划" name="plan">
        <div class="plan-view">
          <div class="plan-toolbar">
            <el-radio-group v-model="planView" size="small">
              <el-radio-button label="graph">图形</el-radio-button>
              <el-radio-button label="text">文本</el-radio-button>
            </el-radio-group>
          </div>

          <div v-if="planView === 'graph'" class="plan-graph">
            <div ref="planGraphRef" class="graph-container"></div>
            <div class="node-details" v-if="selectedNode">
              <h4>节点详情</h4>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="操作类型">
                  {{ selectedNode.type }}
                </el-descriptions-item>
                <el-descriptions-item label="预估成本">
                  {{ selectedNode.cost }}
                </el-descriptions-item>
                <el-descriptions-item label="预估行数">
                  {{ formatNumber(selectedNode.rows) }}
                </el-descriptions-item>
                <el-descriptions-item label="过滤条件" v-if="selectedNode.filter">
                  {{ selectedNode.filter }}
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </div>

          <pre v-else class="plan-text">{{ executionPlan?.text }}</pre>
        </div>
      </el-tab-pane>

      <el-tab-pane label="统计信息" name="stats">
        <div class="stats-view">
          <el-table :data="tableStats" border>
            <el-table-column prop="name" label="表名" width="180" />
            <el-table-column prop="rows" label="总行数">
              <template #default="{ row }">
                {{ formatNumber(row.rows) }}
              </template>
            </el-table-column>
            <el-table-column prop="size" label="大小">
              <template #default="{ row }">
                {{ formatBytes(row.size) }}
              </template>
            </el-table-column>
            <el-table-column prop="lastAnalyzed" label="最后分析时间">
              <template #default="{ row }">
                {{ formatTime(row.lastAnalyzed) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  link
                  @click="analyzeTable(row)"
                  :loading="row.analyzing"
                >
                  更新统计
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="column-stats">
            <h4>列统计信息</h4>
            <el-table :data="columnStats" border>
              <el-table-column prop="name" label="列名" width="180" />
              <el-table-column prop="distinctValues" label="不同值数">
                <template #default="{ row }">
                  {{ formatNumber(row.distinctValues) }}
                </template>
              </el-table-column>
              <el-table-column prop="nullCount" label="空值数">
                <template #default="{ row }">
                  {{ formatNumber(row.nullCount) }}
                </template>
              </el-table-column>
              <el-table-column prop="avgLength" label="平均长度">
                <template #default="{ row }">
                  {{ row.avgLength?.toFixed(1) }}
                </template>
              </el-table-column>
              <el-table-column label="数据分布" width="120">
                <template #default="{ row }">
                  <el-button
                    type="primary"
                    link
                    @click="showDistribution(row)"
                  >
                    查看
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 数据分布对话框 -->
    <el-dialog
      v-model="distributionVisible"
      title="数据分布"
      width="600px"
    >
      <div v-if="selectedColumn" class="distribution-view">
        <div class="distribution-header">
          <h4>{{ selectedColumn.name }}</h4>
          <el-radio-group v-model="distributionType" size="small">
            <el-radio-button label="histogram">直方图</el-radio-button>
            <el-radio-button label="topn">TOP-N</el-radio-button>
          </el-radio-group>
        </div>

        <div class="distribution-chart" ref="distributionChartRef"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import G6 from '@antv/g6'
import {
  Operation, Document
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  sql: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['update:sql', 'analyze-complete'])

// 状态变量
const activeTab = ref('suggestions')
const analyzing = ref(false)
const suggestions = ref([])
const planView = ref('graph')
const selectedNode = ref(null)
const planGraphRef = ref(null)
const graph = ref(null)
const distributionVisible = ref(false)
const selectedColumn = ref(null)
const distributionType = ref('histogram')
const distributionChartRef = ref(null)
const distributionChart = ref(null)

// 模拟数据
const executionPlan = ref({
  text: 'Seq Scan on user_behavior  (cost=0.00..1000.00 rows=10000 width=40)\n' +
        '  Filter: (event_type = \'click\'::text)\n',
  nodes: [
    {
      id: 'n1',
      type: 'Seq Scan',
      table: 'user_behavior',
      cost: 1000.00,
      rows: 10000,
      filter: "event_type = 'click'"
    }
  ],
  edges: []
})

const tableStats = ref([
  {
    name: 'user_behavior',
    rows: 1000000,
    size: 1024 * 1024 * 100,
    lastAnalyzed: Date.now() - 86400000,
    analyzing: false
  }
])

const columnStats = ref([
  {
    name: 'event_type',
    distinctValues: 10,
    nullCount: 0,
    avgLength: 12.5,
    distribution: {
      histogram: [
        { value: 'click', count: 5000 },
        { value: 'view', count: 3000 },
        { value: 'purchase', count: 1000 }
      ],
      topN: [
        { value: 'click', percentage: 50 },
        { value: 'view', percentage: 30 },
        { value: 'purchase', percentage: 10 }
      ]
    }
  }
])

// 格式化函数
const formatNumber = (num) => {
  return new Intl.NumberFormat().format(num)
}

const formatBytes = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, i)).toFixed(1)} ${sizes[i]}`
}

const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleString()
}

// 获取建议类型
const getSuggestionType = (impact) => {
  if (impact >= 80) return 'danger'
  if (impact >= 50) return 'warning'
  return 'info'
}

// 分析SQL
const analyze = async () => {
  if (!props.sql) {
    ElMessage.warning('请输入SQL语句')
    return
  }

  analyzing.value = true
  try {
    // TODO: 调用后端API分析SQL
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    suggestions.value = [
      {
        title: '添加索引',
        description: '在user_id列上添加索引可以提升查询性能',
        impact: 85,
        example: 'CREATE INDEX idx_user_id ON user_behavior(user_id)',
        explanation: '当前查询在user_id列上进行了过滤，但该列没有索引，导致需要全表扫描。添加索引后可以显著提升查询性能。',
        canApply: true
      },
      {
        title: '优化JOIN顺序',
        description: '调整JOIN顺序，将小表放在左边可以减少中间结果集',
        impact: 60,
        example: 'SELECT ... FROM small_table a JOIN large_table b ON ...',
        explanation: '在JOIN操作中，将小表放在左边可以减少中间结果集的大小，从而减少内存使用和提升性能。',
        canApply: true
      }
    ]

    emit('analyze-complete', suggestions.value)
  } catch (error) {
    ElMessage.error('分析失败')
  } finally {
    analyzing.value = false
  }
}

// 格式化SQL
const formatSql = () => {
  if (!props.sql) return
  // TODO: 格式化SQL
  const formatted = props.sql.trim()
  emit('update:sql', formatted)
}

// 选择建议
const selectSuggestion = (suggestion) => {
  suggestions.value.forEach(s => {
    s.selected = s === suggestion
  })
}

// 应用建议
const applySuggestion = (suggestion) => {
  if (!suggestion.example) return
  emit('update:sql', suggestion.example)
}

// 复制SQL
const copySql = (sql) => {
  navigator.clipboard.writeText(sql)
  ElMessage.success('已复制到剪贴板')
}

// 分析表统计信息
const analyzeTable = async (table) => {
  table.analyzing = true
  try {
    // TODO: 更新表统计信息
    await new Promise(resolve => setTimeout(resolve, 1000))
    table.lastAnalyzed = Date.now()
    ElMessage.success('统计信息已更新')
  } catch (error) {
    ElMessage.error('更新失败')
  } finally {
    table.analyzing = false
  }
}

// 显示数据分布
const showDistribution = (column) => {
  selectedColumn.value = column
  distributionVisible.value = true
  nextTick(() => {
    initDistributionChart()
  })
}

// 初始化执行计划图
const initPlanGraph = () => {
  if (!planGraphRef.value) return

  graph.value = new G6.Graph({
    container: planGraphRef.value,
    width: planGraphRef.value.offsetWidth,
    height: 400,
    modes: {
      default: ['drag-canvas', 'zoom-canvas', 'drag-node']
    },
    defaultNode: {
      type: 'rect',
      style: {
        radius: 4,
        stroke: '#666',
        lineWidth: 1,
        fill: '#fff'
      }
    },
    defaultEdge: {
      type: 'line',
      style: {
        stroke: '#666',
        lineWidth: 1,
        endArrow: true
      }
    }
  })

  graph.value.data({
    nodes: executionPlan.value.nodes.map(node => ({
      id: node.id,
      label: `${node.type}\n${node.table || ''}`,
      style: {
        fill: '#f0f2f5'
      }
    })),
    edges: executionPlan.value.edges.map(edge => ({
      source: edge.source,
      target: edge.target
    }))
  })

  graph.value.render()

  graph.value.on('node:click', (e) => {
    const nodeId = e.item.get('id')
    selectedNode.value = executionPlan.value.nodes.find(n => n.id === nodeId)
  })
}

// 初始化分布图表
const initDistributionChart = () => {
  if (!distributionChartRef.value || !selectedColumn.value) return

  if (!distributionChart.value) {
    distributionChart.value = echarts.init(distributionChartRef.value)
  }

  const data = selectedColumn.value.distribution[distributionType.value]
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    xAxis: {
      type: 'category',
      data: data.map(d => d.value)
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        type: distributionType.value === 'histogram' ? 'bar' : 'pie',
        data: data.map(d => ({
          name: d.value,
          value: distributionType.value === 'histogram' ? d.count : d.percentage
        }))
      }
    ]
  }

  distributionChart.value.setOption(option)
}

// 监听分布类型变化
watch(distributionType, () => {
  initDistributionChart()
})

// 监听执行计划视图变化
watch(planView, () => {
  if (planView.value === 'graph') {
    nextTick(() => {
      initPlanGraph()
    })
  }
})

onMounted(() => {
  if (planView.value === 'graph') {
    initPlanGraph()
  }
})

onUnmounted(() => {
  graph.value?.destroy()
  distributionChart.value?.dispose()
})
</script>

<style lang="scss" scoped>
.sql-optimizer {
  height: 100%;
  display: flex;
  flex-direction: column;

  .toolbar {
    padding: 16px;
    border-bottom: 1px solid #dcdfe6;
  }

  .optimizer-tabs {
    flex: 1;
    display: flex;
    flex-direction: column;

    :deep(.el-tabs__content) {
      flex: 1;
      padding: 16px;
      overflow: auto;
    }
  }

  .suggestions-list {
    .suggestion-item {
      margin-bottom: 16px;
      padding: 16px;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        border-color: var(--el-color-primary);
      }

      &.active {
        border-color: var(--el-color-primary);
        background: var(--el-color-primary-light-9);
      }

      .suggestion-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        .left {
          display: flex;
          align-items: center;
          gap: 12px;

          .title {
            font-weight: 500;
          }
        }
      }

      .suggestion-content {
        .description {
          color: #606266;
          margin: 0 0 12px;
        }

        .example {
          background: #f5f7fa;
          padding: 12px;
          border-radius: 4px;
          margin-bottom: 12px;

          .example-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 8px;
          }

          pre {
            margin: 0;
            font-family: monospace;
          }
        }
      }
    }
  }

  .plan-view {
    height: 100%;
    display: flex;
    flex-direction: column;
    gap: 16px;

    .plan-toolbar {
      display: flex;
      justify-content: flex-end;
    }

    .plan-graph {
      display: flex;
      gap: 16px;
      height: 100%;

      .graph-container {
        flex: 1;
        border: 1px solid #dcdfe6;
        border-radius: 4px;
      }

      .node-details {
        width: 300px;
        padding: 16px;
        border: 1px solid #dcdfe6;
        border-radius: 4px;

        h4 {
          margin: 0 0 16px;
        }
      }
    }

    .plan-text {
      flex: 1;
      margin: 0;
      padding: 16px;
      background: #f5f7fa;
      border-radius: 4px;
      font-family: monospace;
      white-space: pre-wrap;
    }
  }

  .stats-view {
    .column-stats {
      margin-top: 24px;

      h4 {
        margin: 0 0 16px;
      }
    }
  }
}

.distribution-view {
  .distribution-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h4 {
      margin: 0;
    }
  }

  .distribution-chart {
    height: 400px;
  }
}
</style>