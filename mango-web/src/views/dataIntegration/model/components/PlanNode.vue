<template>
  <div class="plan-node" :class="[nodeData.type, { selected: isSelected }]">
    <div class="node-header">
      <div class="node-type-icon">
        <el-icon v-if="nodeData.type === 'dimension'"><Histogram /></el-icon>
        <el-icon v-else-if="nodeData.type === 'fact'"><DataAnalysis /></el-icon>
      </div>
      <div class="node-title">{{ nodeData.name }}</div>
      <div class="node-actions">
        <el-tooltip content="编辑" placement="top">
          <el-icon @click.stop="handleEdit"><Edit /></el-icon>
        </el-tooltip>
        <el-tooltip content="复制" placement="top">
          <el-icon @click.stop="handleDuplicate"><CopyDocument /></el-icon>
        </el-tooltip>
        <el-tooltip content="删除" placement="top">
          <el-icon @click.stop="handleDelete"><Delete /></el-icon>
        </el-tooltip>
      </div>
    </div>
    
    <div class="node-content">
      <!-- 维度信息 -->
      <div v-if="nodeData.type === 'dimension'" class="dimension-info">
        <div class="dimension-attributes">
          <div class="attribute-count">
            <el-icon><List /></el-icon>
            <span>{{ getDimensionAttributeCount() }} 个属性</span>
          </div>
          <div class="hierarchy-count" v-if="getDimensionHierarchyCount() > 0">
            <el-icon><Sort /></el-icon>
            <span>{{ getDimensionHierarchyCount() }} 个层级</span>
          </div>
        </div>
        
        <div class="attribute-list" v-if="nodeData.config?.attributes?.length > 0">
          <div v-for="(attr, index) in nodeData.config.attributes.slice(0, 3)" :key="index" class="attribute-item">
            <span class="attribute-name">{{ attr.name }}</span>
            <span class="attribute-type">{{ attr.type }}</span>
          </div>
          <div v-if="nodeData.config.attributes.length > 3" class="more-attributes">
            还有 {{ nodeData.config.attributes.length - 3 }} 个属性...
          </div>
        </div>
      </div>
      
      <!-- 事实信息 -->
      <div v-else-if="nodeData.type === 'fact'" class="fact-info">
        <div class="fact-measures">
          <div class="measure-count">
            <el-icon><DataAnalysis /></el-icon>
            <span>{{ getFactMeasureCount() }} 个度量</span>
          </div>
          <div class="dimension-count" v-if="getFactDimensionCount() > 0">
            <el-icon><Histogram /></el-icon>
            <span>{{ getFactDimensionCount() }} 个维度</span>
          </div>
        </div>
        
        <div class="measure-list" v-if="nodeData.config?.measures?.length > 0">
          <div v-for="(measure, index) in nodeData.config.measures.slice(0, 3)" :key="index" class="measure-item">
            <span class="measure-name">{{ measure.name }}</span>
            <span class="measure-type">{{ measure.aggregation }}</span>
          </div>
          <div v-if="nodeData.config.measures.length > 3" class="more-measures">
            还有 {{ nodeData.config.measures.length - 3 }} 个度量...
          </div>
        </div>
      </div>
    </div>
    
    <div class="node-footer">
      <div class="node-status">
        <el-tag size="small" :type="getStatusType(nodeData.status)">{{ getStatusText(nodeData.status) }}</el-tag>
      </div>
      <div class="node-id">ID: {{ nodeData.id.substring(0, 8) }}</div>
    </div>
    
    <!-- 连接点 -->
    <div class="connection-points">
      <div class="point top" @mousedown.stop="handleStartConnection('top')"></div>
      <div class="point right" @mousedown.stop="handleStartConnection('right')"></div>
      <div class="point bottom" @mousedown.stop="handleStartConnection('bottom')"></div>
      <div class="point left" @mousedown.stop="handleStartConnection('left')"></div>
    </div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'
import { ElMessageBox } from 'element-plus'
import { 
  Histogram, DataAnalysis, Edit, Delete, CopyDocument, 
  List, Sort
} from '@element-plus/icons-vue'

const props = defineProps({
  nodeData: {
    type: Object,
    required: true
  },
  isSelected: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['edit', 'delete', 'duplicate', 'start-connection'])

// 获取维度属性数量
const getDimensionAttributeCount = () => {
  if (!props.nodeData.config || !props.nodeData.config.attributes) return 0
  return props.nodeData.config.attributes.length
}

// 获取维度层级数量
const getDimensionHierarchyCount = () => {
  if (!props.nodeData.config || !props.nodeData.config.hierarchies) return 0
  return props.nodeData.config.hierarchies.length
}

// 获取事实度量数量
const getFactMeasureCount = () => {
  if (!props.nodeData.config || !props.nodeData.config.measures) return 0
  return props.nodeData.config.measures.length
}

// 获取事实关联维度数量
const getFactDimensionCount = () => {
  if (!props.nodeData.config || !props.nodeData.config.dimensions) return 0
  return props.nodeData.config.dimensions.length
}

// 获取状态类型
const getStatusType = (status) => {
  const statusMap = {
    'draft': 'info',
    'published': 'success',
    'deprecated': 'warning',
    'error': 'danger'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'draft': '草稿',
    'published': '已发布',
    'deprecated': '已弃用',
    'error': '错误'
  }
  return statusMap[status] || status || '草稿'
}

// 处理编辑
const handleEdit = () => {
  emit('edit', props.nodeData)
}

// 处理删除
const handleDelete = () => {
  ElMessageBox.confirm(
    `确定要删除 ${props.nodeData.name} 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    emit('delete', props.nodeData)
  }).catch(() => {})
}

// 处理复制
const handleDuplicate = () => {
  emit('duplicate', props.nodeData)
}

// 处理开始连接
const handleStartConnection = (position) => {
  emit('start-connection', {
    nodeId: props.nodeData.id,
    position
  })
}
</script>

<style lang="scss" scoped>
.plan-node {
  width: 220px;
  background: white;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  position: relative;
  user-select: none;
  
  &.selected {
    box-shadow: 0 0 0 2px #409EFF;
  }
  
  &.dimension {
    border-top: 4px solid #67C23A;
  }
  
  &.fact {
    border-top: 4px solid #F56C6C;
  }
  
  .node-header {
    padding: 8px 12px;
    display: flex;
    align-items: center;
    border-bottom: 1px solid #EBEEF5;
    
    .node-type-icon {
      margin-right: 8px;
      
      .el-icon {
        font-size: 16px;
      }
    }
    
    .node-title {
      flex: 1;
      font-weight: bold;
      font-size: 14px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    
    .node-actions {
      display: flex;
      gap: 8px;
      opacity: 0;
      transition: opacity 0.2s;
      
      .el-icon {
        cursor: pointer;
        font-size: 14px;
        color: #909399;
        
        &:hover {
          color: #409EFF;
        }
      }
    }
  }
  
  &:hover .node-actions {
    opacity: 1;
  }
  
  .node-content {
    padding: 8px 12px;
    min-height: 100px;
    max-height: 200px;
    overflow-y: auto;
    
    .dimension-info, .fact-info {
      .dimension-attributes, .fact-measures {
        margin-bottom: 12px;
        
        .attribute-count, .hierarchy-count,
        .measure-count, .dimension-count {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 4px;
          
          .el-icon {
            color: #909399;
          }
        }
      }
      
      .attribute-list, .measure-list {
        .attribute-item, .measure-item {
          display: flex;
          justify-content: space-between;
          padding: 4px 0;
          border-bottom: 1px solid #F2F6FC;
          
          .attribute-name, .measure-name {
            font-weight: 500;
          }
          
          .attribute-type, .measure-type {
            color: #909399;
            font-size: 12px;
          }
        }
        
        .more-attributes, .more-measures {
          padding: 4px 0;
          text-align: center;
          color: #909399;
          font-size: 12px;
        }
      }
    }
  }
  
  .node-footer {
    padding: 8px 12px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-top: 1px solid #EBEEF5;
    
    .node-id {
      font-size: 12px;
      color: #909399;
    }
  }
  
  .connection-points {
    .point {
      position: absolute;
      width: 10px;
      height: 10px;
      background: #409EFF;
      border-radius: 50%;
      cursor: crosshair;
      z-index: 10;
      
      &.top {
        top: -5px;
        left: 50%;
        transform: translateX(-50%);
      }
      
      &.right {
        top: 50%;
        right: -5px;
        transform: translateY(-50%);
      }
      
      &.bottom {
        bottom: -5px;
        left: 50%;
        transform: translateX(-50%);
      }
      
      &.left {
        top: 50%;
        left: -5px;
        transform: translateY(-50%);
      }
    }
  }
}
</style> 