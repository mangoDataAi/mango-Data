<template>
  <div class="log-stream">
    <div class="toolbar">
      <el-select v-model="logLevel" placeholder="日志级别" size="small">
        <el-option label="全部" value="ALL" />
        <el-option label="INFO" value="INFO" />
        <el-option label="WARN" value="WARN" />
        <el-option label="ERROR" value="ERROR" />
        <el-option label="DEBUG" value="DEBUG" />
      </el-select>

      <el-input
        v-model="searchKeyword"
        placeholder="搜索日志"
        prefix-icon="Search"
        size="small"
        clearable
      />

      <el-button-group>
        <el-button
          type="primary"
          :icon="AutoScroll"
          size="small"
          :class="{ active: autoScroll }"
          @click="toggleAutoScroll"
        >
          自动滚动
        </el-button>
        <el-button
          size="small"
          :icon="Download"
          @click="handleDownload"
        >
          下载
        </el-button>
      </el-button-group>

      <el-button
        size="small"
        :icon="Delete"
        @click="handleClear"
      >
        清空
      </el-button>
    </div>

    <div class="log-content" ref="logContainer">
      <div
        v-for="(log, index) in filteredLogs"
        :key="index"
        :class="['log-line', log.level.toLowerCase()]"
      >
        <span class="time">{{ formatTime(log.timestamp) }}</span>
        <span class="level">{{ log.level }}</span>
        <span class="source" v-if="log.source">[{{ log.source }}]</span>
        <span class="message" v-html="highlightKeyword(log.message)"></span>
      </div>

      <div v-if="loading" class="loading-indicator">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载日志中...</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, Download, Delete, Loading,
  ArrowDown as AutoScroll
} from '@element-plus/icons-vue'

const props = defineProps({
  taskId: {
    type: String,
    required: true
  },
  initialLogs: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:logs'])

// 状态变量
const logLevel = ref('ALL')
const searchKeyword = ref('')
const autoScroll = ref(true)
const loading = ref(false)
const logs = ref(props.initialLogs)
const logContainer = ref(null)
let websocket = null

// 过滤后的日志
const filteredLogs = computed(() => {
  return logs.value.filter(log => {
    if (logLevel.value !== 'ALL' && log.level !== logLevel.value) {
      return false
    }
    if (searchKeyword.value) {
      return log.message.toLowerCase().includes(searchKeyword.value.toLowerCase())
    }
    return true
  })
})

// 格式化时间
const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleString()
}

// 高亮关键词
const highlightKeyword = (message) => {
  if (!searchKeyword.value) return message
  const regex = new RegExp(`(${searchKeyword.value})`, 'gi')
  return message.replace(regex, '<span class="highlight">$1</span>')
}

// 切换自动滚动
const toggleAutoScroll = () => {
  autoScroll.value = !autoScroll.value
  if (autoScroll.value) {
    scrollToBottom()
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (!autoScroll.value) return
  nextTick(() => {
    if (logContainer.value) {
      logContainer.value.scrollTop = logContainer.value.scrollHeight
    }
  })
}

// 下载日志
const handleDownload = () => {
  const content = logs.value.map(log => 
    `${formatTime(log.timestamp)} [${log.level}] ${log.source ? `[${log.source}] ` : ''}${log.message}`
  ).join('\n')

  const blob = new Blob([content], { type: 'text/plain' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `task_${props.taskId}_logs_${Date.now()}.txt`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

// 清空日志
const handleClear = () => {
  logs.value = []
  emit('update:logs', [])
}

// 连接WebSocket
const connectWebSocket = () => {
  // TODO: 替换为实际的WebSocket地址
  const wsUrl = `ws://your-api-host/api/tasks/${props.taskId}/logs`
  
  websocket = new WebSocket(wsUrl)
  
  websocket.onopen = () => {
    console.log('WebSocket connected')
  }
  
  websocket.onmessage = (event) => {
    try {
      const log = JSON.parse(event.data)
      logs.value.push(log)
      emit('update:logs', logs.value)
      scrollToBottom()
    } catch (error) {
      console.error('Failed to parse log message:', error)
    }
  }
  
  websocket.onerror = (error) => {
    console.error('WebSocket error:', error)
    ElMessage.error('日志连接失败')
  }
  
  websocket.onclose = () => {
    console.log('WebSocket disconnected')
    // 尝试重新连接
    setTimeout(connectWebSocket, 5000)
  }
}

// 监听日志变化
watch(logs, () => {
  scrollToBottom()
})

onMounted(() => {
  connectWebSocket()
})

onUnmounted(() => {
  if (websocket) {
    websocket.close()
  }
})
</script>

<style lang="scss" scoped>
.log-stream {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #1e1e1e;
  border-radius: 4px;
  overflow: hidden;

  .toolbar {
    padding: 8px;
    background: #2d2d2d;
    border-bottom: 1px solid #3d3d3d;
    display: flex;
    gap: 8px;
    align-items: center;

    .el-select {
      width: 100px;
    }

    .el-input {
      width: 200px;
    }

    .active {
      background: var(--el-color-primary) !important;
      border-color: var(--el-color-primary) !important;
      color: white !important;
    }
  }

  .log-content {
    flex: 1;
    overflow-y: auto;
    padding: 12px;
    font-family: 'Consolas', monospace;
    font-size: 14px;

    .log-line {
      margin-bottom: 4px;
      color: #d4d4d4;
      white-space: pre-wrap;
      word-break: break-all;

      &:last-child {
        margin-bottom: 0;
      }

      .time {
        color: #808080;
        margin-right: 8px;
      }

      .level {
        display: inline-block;
        width: 60px;
        margin-right: 8px;
      }

      .source {
        color: #808080;
        margin-right: 8px;
      }

      &.debug .level {
        color: #808080;
      }

      &.info .level {
        color: #3794ff;
      }

      &.warn .level {
        color: #cca700;
      }

      &.error .level {
        color: #f14c4c;
      }

      .highlight {
        background: #ffd700;
        color: #000;
        border-radius: 2px;
        padding: 0 2px;
      }
    }

    .loading-indicator {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 16px;
      color: #808080;

      .el-icon {
        font-size: 18px;
      }
    }
  }
}
</style> 