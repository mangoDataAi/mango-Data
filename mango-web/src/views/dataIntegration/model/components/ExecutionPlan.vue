<template>
  <div class="execution-plan">
    <div class="toolbar">
      <el-button-group>
        <el-button
          type="primary"
          :icon="VideoPlay"
          @click="handleExecute"
          :loading="executing"
        >
          执行
        </el-button>
        <el-button
          :icon="VideoPause"
          @click="handleStop"
          :disabled="!executing"
        >
          停止
        </el-button>
      </el-button-group>

      <el-button-group>
        <el-button :icon="ZoomIn" @click="handleZoomIn">放大</el-button>
        <el-button :icon="ZoomOut" @click="handleZoomOut">缩小</el-button>
        <el-button :icon="FullScreen" @click="handleFitView">适应画布</el-button>
      </el-button-group>

      <el-radio-group v-model="displayMode" size="small">
        <el-radio-button label="simple">简洁模式</el-radio-button>
        <el-radio-button label="detail">详细模式</el-radio-button>
      </el-radio-group>
    </div>

    <div class="plan-container" ref="planContainer"></div>

    <!-- 节点详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      title="节点详情"
      size="30%"
      destroy-on-close
    >
      <template v-if="selectedNode">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="节点名称">{{ selectedNode.name }}</el-descriptions-item>
          <el-descriptions-item label="节点类型">{{ selectedNode.type }}</el-descriptions-item>
          <el-descriptions-item label="执行状态">
            <el-tag :type="getStatusType(selectedNode.status)">
              {{ selectedNode.status }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 执行统计 -->
        <div class="section-title">执行统计</div>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="label">输入记录数</div>
            <div class="value">{{ formatNumber(selectedNode.stats?.inputRows) }}</div>
          </div>
          <div class="stat-item">
            <div class="label">输出记录数</div>
            <div class="value">{{ formatNumber(selectedNode.stats?.outputRows) }}</div>
          </div>
          <div class="stat-item">
            <div class="label">执行时间</div>
            <div class="value">{{ formatDuration(selectedNode.stats?.duration) }}</div>
          </div>
          <div class="stat-item">
            <div class="label">内存使用</div>
            <div class="value">{{ formatBytes(selectedNode.stats?.memoryUsage) }}</div>
          </div>
        </div>

        <!-- 执行详情 -->
        <div class="section-title">执行详情</div>
        <el-collapse>
          <el-collapse-item title="SQL语句" name="sql">
            <pre><code>{{ selectedNode.details?.sql }}</code></pre>
          </el-collapse-item>
          <el-collapse-item title="执行计划" name="plan">
            <pre><code>{{ selectedNode.details?.plan }}</code></pre>
          </el-collapse-item>
          <el-collapse-item title="优化建议" name="suggestions">
            <div
              v-for="(suggestion, index) in selectedNode.details?.suggestions"
              :key="index"
              class="suggestion-item"
            >
              <el-tag
                size="small"
                :type="suggestion.priority === 'high' ? 'danger' : 'warning'"
              >
                {{ suggestion.priority === 'high' ? '重要' : '建议' }}
              </el-tag>
              <span class="suggestion-text">{{ suggestion.text }}</span>
            </div>
          </el-collapse-item>
        </el-collapse>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Graph } from '@antv/x6'
import { DagreLayout } from '@antv/layout'
import {
  VideoPlay, VideoPause, ZoomIn, ZoomOut,
  FullScreen
} from '@element-plus/icons-vue'

const props = defineProps({
  planData: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['execute', 'stop'])

// 状态变量
const planContainer = ref(null)
const graph = ref(null)
const displayMode = ref('simple')
const executing = ref(false)
const drawerVisible = ref(false)
const selectedNode = ref(null)

// 初始化图表
const initGraph = () => {
  if (!planContainer.value) return

  graph.value = new Graph({
    container: planContainer.value,
    grid: {
      type: 'mesh',
      size: 10,
      visible: true,
      color: '#ddd'
    },
    mousewheel: {
      enabled: true,
      modifiers: ['ctrl', 'meta'],
      minScale: 0.5,
      maxScale: 2
    },
    connecting: {
      enabled: false
    },
    interacting: {
      nodeMovable: false,
      edgeMovable: false,
      edgeLabelMovable: false,
      magnetConnectable: false,
      toolsAddable: false,
      vertexAddable: false,
      vertexDeletable: false,
      vertexsMovable: false
    }
  })

  // 注册节点点击事件
  graph.value.on('node:click', ({ node }) => {
    selectedNode.value = props.planData.nodes.find(n => n.id === node.id)
    drawerVisible.value = true
  })
}

// 更新执行计划
const updatePlan = () => {
  if (!graph.value) return

  // 清空画布
  graph.value.clearCells()

  // 构建节点
  const nodes = props.planData.nodes.map(node => ({
    id: node.id,
    shape: 'plan-node',
    data: {
      ...node,
      displayMode: displayMode.value
    }
  }))

  // 构建边
  const edges = props.planData.edges.map(edge => ({
    shape: 'plan-edge',
    source: edge.source,
    target: edge.target,
    data: edge
  }))

  // 应用布局
  const layout = new DagreLayout({
    type: 'dagre',
    rankdir: 'TB',
    align: 'UL',
    ranksep: 50,
    nodesep: 50
  })

  const layoutData = layout.layout({
    nodes,
    edges
  })

  // 添加节点和边
  graph.value.addNodes(layoutData.nodes)
  graph.value.addEdges(layoutData.edges)

  // 适应画布
  graph.value.zoomToFit({ padding: 20 })
}

// 格式化数字
const formatNumber = (num) => {
  if (num === undefined || num === null) return '-'
  return new Intl.NumberFormat().format(num)
}

// 格式化持续时间
const formatDuration = (ms) => {
  if (ms === undefined || ms === null) return '-'
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  const minutes = Math.floor(ms / 60000)
  const seconds = ((ms % 60000) / 1000).toFixed(1)
  return `${minutes}m ${seconds}s`
}

// 格式化字节数
const formatBytes = (bytes) => {
  if (bytes === undefined || bytes === null) return '-'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = bytes
  let unitIndex = 0
  while (size >= 1024 && unitIndex < units.length - 1) {
    size /= 1024
    unitIndex++
  }
  return `${size.toFixed(2)} ${units[unitIndex]}`
}

// 获取状态类型
const getStatusType = (status) => {
  const types = {
    'running': 'primary',
    'success': 'success',
    'error': 'danger',
    'pending': 'info'
  }
  return types[status] || 'info'
}

// 画布操作
const handleZoomIn = () => graph.value?.zoom(0.1)
const handleZoomOut = () => graph.value?.zoom(-0.1)
const handleFitView = () => graph.value?.zoomToFit({ padding: 20 })

// 执行操作
const handleExecute = () => {
  executing.value = true
  emit('execute')
}

const handleStop = () => {
  executing.value = false
  emit('stop')
}

// 监听显示模式变化
watch(displayMode, () => {
  updatePlan()
})

// 监听计划数据变化
watch(() => props.planData, () => {
  updatePlan()
}, { deep: true })

onMounted(() => {
  initGraph()
  updatePlan()
})

onUnmounted(() => {
  graph.value?.dispose()
})
</script>

<style lang="scss" scoped>
.execution-plan {
  height: 100%;
  display: flex;
  flex-direction: column;

  .toolbar {
    padding: 10px;
    border-bottom: 1px solid #dcdfe6;
    display: flex;
    gap: 10px;
    align-items: center;
  }

  .plan-container {
    flex: 1;
    overflow: hidden;
  }

  .section-title {
    margin: 16px 0 8px;
    font-weight: 500;
    color: #303133;
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
    margin: 16px 0;

    .stat-item {
      padding: 12px;
      background: #f5f7fa;
      border-radius: 4px;

      .label {
        font-size: 12px;
        color: #909399;
        margin-bottom: 4px;
      }

      .value {
        font-size: 16px;
        font-weight: 500;
        color: #303133;
      }
    }
  }

  .suggestion-item {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    margin-bottom: 8px;

    &:last-child {
      margin-bottom: 0;
    }

    .suggestion-text {
      flex: 1;
      font-size: 14px;
      color: #606266;
    }
  }

  :deep(pre) {
    margin: 0;
    padding: 12px;
    background: #f5f7fa;
    border-radius: 4px;
    font-family: 'Consolas', monospace;
    font-size: 12px;
    overflow-x: auto;
  }
}
</style> 