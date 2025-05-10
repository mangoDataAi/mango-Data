<template>
  <div 
    class="model-node" 
    :class="[`node-type-${nodeData.type}`, { 'is-selected': isSelected }]"
  >
    <!-- 节点头部 -->
    <div class="node-header" :style="{ backgroundColor: getNodeColor() }">
      <div class="node-type">{{ getNodeTypeName() }}</div>
      <div class="node-title">{{ nodeData.name || '未命名' }}</div>
      <div class="node-actions">
        <el-tooltip content="编辑" placement="top">
          <el-icon class="action-icon" @click="$emit('edit', nodeData)"><Edit /></el-icon>
        </el-tooltip>
        <el-tooltip content="复制" placement="top">
          <el-icon class="action-icon" @click="$emit('duplicate', nodeData)"><CopyDocument /></el-icon>
        </el-tooltip>
        <el-tooltip content="删除" placement="top">
          <el-icon class="action-icon" @click="$emit('delete', nodeData)"><Delete /></el-icon>
        </el-tooltip>
      </div>
    </div>
    
    <!-- 节点内容 -->
    <div class="node-content">
      <!-- 实体表内容 -->
      <template v-if="nodeData.type === 'entity'">
        <div class="field-count">
          <span>{{ (nodeData.config?.fields || []).length }} 个字段</span>
          <el-button 
            size="small" 
            type="primary" 
            link 
            @click="$emit('update-fields', nodeData)"
          >
            管理字段
          </el-button>
        </div>
        
        <div class="field-list" v-if="(nodeData.config?.fields || []).length > 0">
          <div 
            v-for="(field, index) in nodeData.config?.fields.slice(0, 5)" 
            :key="field.id || index" 
            class="field-item"
          >
            <span class="field-pk" v-if="field.isPrimaryKey">PK</span>
            <span class="field-name">{{ field.name }}</span>
            <span class="field-type">{{ field.type }}</span>
          </div>
          
          <div class="more-fields" v-if="(nodeData.config?.fields || []).length > 5">
            还有 {{ (nodeData.config?.fields || []).length - 5 }} 个字段...
          </div>
        </div>
        
        <div class="empty-fields" v-else>
          <span>暂无字段</span>
          <el-button 
            size="small" 
            type="primary" 
            link 
            @click="$emit('update-fields', nodeData)"
          >
            添加字段
          </el-button>
        </div>
        
        <div class="node-actions-bottom">
          <el-button 
            size="small" 
            type="success" 
            link 
            @click="$emit('create-relation', nodeData)"
          >
            创建关系
          </el-button>
        </div>
      </template>
      
      <!-- 关系表内容 -->
      <template v-else-if="nodeData.type === 'relation'">
        <div class="relation-info">
          <div class="relation-type">
            <span>关系类型:</span>
            <el-tag size="small">{{ getRelationTypeName() }}</el-tag>
          </div>
          
          <div class="relation-entities">
            <div class="relation-entity">
              <span>源实体:</span>
              <span>{{ nodeData.config?.sourceEntityName || '未设置' }}</span>
            </div>
            <div class="relation-entity">
              <span>目标实体:</span>
              <span>{{ nodeData.config?.targetEntityName || '未设置' }}</span>
            </div>
          </div>
        </div>
      </template>
      
      <!-- 维度表内容 -->
      <template v-else-if="nodeData.type === 'dimension'">
        <div class="field-count">
          <span>{{ (nodeData.config?.attributes || []).length }} 个属性</span>
          <el-button 
            size="small" 
            type="primary" 
            link 
            @click="$emit('update-fields', nodeData)"
          >
            管理属性
          </el-button>
        </div>
        
        <div class="field-list" v-if="(nodeData.config?.attributes || []).length > 0">
          <div 
            v-for="(attr, index) in nodeData.config?.attributes.slice(0, 5)" 
            :key="attr.id || index" 
            class="field-item"
          >
            <span class="field-pk" v-if="attr.isPrimary">PK</span>
            <span class="field-name">{{ attr.name }}</span>
            <span class="field-type">{{ attr.type }}</span>
          </div>
          
          <div class="more-fields" v-if="(nodeData.config?.attributes || []).length > 5">
            还有 {{ (nodeData.config?.attributes || []).length - 5 }} 个属性...
          </div>
        </div>
        
        <div class="empty-fields" v-else>
          <span>暂无属性</span>
          <el-button 
            size="small" 
            type="primary" 
            link 
            @click="$emit('update-fields', nodeData)"
          >
            添加属性
          </el-button>
        </div>
      </template>
      
      <!-- 事实表内容 -->
      <template v-else-if="nodeData.type === 'fact'">
        <div class="field-count">
          <span>{{ (nodeData.config?.measures || []).length }} 个度量</span>
          <el-button 
            size="small" 
            type="primary" 
            link 
            @click="$emit('update-fields', nodeData)"
          >
            管理度量
          </el-button>
        </div>
        
        <div class="field-list" v-if="(nodeData.config?.measures || []).length > 0">
          <div 
            v-for="(measure, index) in nodeData.config?.measures.slice(0, 5)" 
            :key="measure.id || index" 
            class="field-item"
          >
            <span class="field-name">{{ measure.name }}</span>
            <span class="field-type">{{ measure.aggregation || measure.type }}</span>
          </div>
          
          <div class="more-fields" v-if="(nodeData.config?.measures || []).length > 5">
            还有 {{ (nodeData.config?.measures || []).length - 5 }} 个度量...
          </div>
        </div>
        
        <div class="empty-fields" v-else>
          <span>暂无度量</span>
          <el-button 
            size="small" 
            type="primary" 
            link 
            @click="$emit('update-fields', nodeData)"
          >
            添加度量
          </el-button>
        </div>
      </template>
    </div>
    
    <!-- 节点底部 -->
    <div class="node-footer">
      <div class="port-indicators">
        <div 
          class="port-indicator" 
          v-for="port in ['top', 'right', 'bottom', 'left']" 
          :key="port"
          @mousedown="handlePortMouseDown(port)"
        >
          <div class="port-dot"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { Edit, Delete, CopyDocument } from '@element-plus/icons-vue'

export default {
  name: 'ModelNode',
  components: {
    Edit,
    Delete,
    CopyDocument
  },
  props: {
    nodeData: {
      type: Object,
      required: true
    },
    isSelected: {
      type: Boolean,
      default: false
    }
  },
  methods: {
    getNodeColor() {
      const colors = {
        entity: '#409EFF',
        relation: '#67C23A',
        dimension: '#E6A23C',
        fact: '#F56C6C'
      }
      return colors[this.nodeData.type] || '#909399'
    },
    
    getNodeTypeName() {
      const names = {
        entity: '实体表',
        relation: '关系',
        dimension: '维度表',
        fact: '事实表'
      }
      return names[this.nodeData.type] || this.nodeData.type
    },
    
    getRelationTypeName() {
      const names = {
        'one-to-one': '一对一',
        'one-to-many': '一对多',
        'many-to-many': '多对多'
      }
      return names[this.nodeData.config?.relationType] || '未设置'
    },
    
    handlePortMouseDown(port) {
      this.$emit('start-connection', {
        nodeId: this.nodeData.id,
        position: port
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.model-node {
  width: 100%;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  background-color: #fff;
  overflow: hidden;
  transition: box-shadow 0.3s;
  
  &.is-selected {
    box-shadow: 0 0 0 2px #409EFF;
  }
  
  .node-header {
    padding: 8px 12px;
    color: #fff;
    position: relative;
    
    .node-type {
      font-size: 12px;
      opacity: 0.8;
    }
    
    .node-title {
      font-weight: bold;
      font-size: 14px;
      margin-top: 2px;
      word-break: break-all;
    }
    
    .node-actions {
      position: absolute;
      top: 8px;
      right: 8px;
      display: none;
      
      .action-icon {
        margin-left: 6px;
        cursor: pointer;
        font-size: 16px;
        
        &:hover {
          opacity: 0.8;
        }
      }
    }
    
    &:hover .node-actions {
      display: flex;
    }
  }
  
  .node-content {
    padding: 12px;
    
    .field-count {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      font-size: 12px;
      color: #606266;
    }
    
    .field-list {
      .field-item {
        display: flex;
        align-items: center;
        padding: 4px 0;
        font-size: 12px;
        border-bottom: 1px solid #EBEEF5;
        
        &:last-child {
          border-bottom: none;
        }
        
        .field-pk {
          background-color: #E6A23C;
          color: #fff;
          padding: 1px 4px;
          border-radius: 2px;
          font-size: 10px;
          margin-right: 6px;
        }
        
        .field-name {
          flex: 1;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
        
        .field-type {
          color: #909399;
          font-size: 11px;
          margin-left: 8px;
        }
      }
      
      .more-fields {
        font-size: 12px;
        color: #909399;
        text-align: center;
        padding: 4px 0;
        background-color: #F5F7FA;
        border-radius: 2px;
        margin-top: 4px;
      }
    }
    
    .empty-fields {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px;
      background-color: #F5F7FA;
      border-radius: 4px;
      font-size: 12px;
      color: #909399;
    }
    
    .node-actions-bottom {
      margin-top: 8px;
      display: flex;
      justify-content: flex-end;
    }
    
    .relation-info {
      .relation-type {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
        font-size: 12px;
      }
      
      .relation-entities {
        font-size: 12px;
        
        .relation-entity {
          display: flex;
          justify-content: space-between;
          padding: 4px 0;
          border-bottom: 1px solid #EBEEF5;
          
          &:last-child {
            border-bottom: none;
          }
          
          span:first-child {
            color: #606266;
          }
          
          span:last-child {
            font-weight: 500;
          }
        }
      }
    }
  }
  
  .node-footer {
    padding: 4px;
    
    .port-indicators {
      display: flex;
      justify-content: space-between;
      position: relative;
      height: 16px;
      
      .port-indicator {
        position: absolute;
        width: 16px;
        height: 16px;
        display: flex;
        justify-content: center;
        align-items: center;
        cursor: crosshair;
        
        &:nth-child(1) {
          top: -8px;
          left: 50%;
          transform: translateX(-50%);
        }
        
        &:nth-child(2) {
          right: -8px;
          top: 50%;
          transform: translateY(-50%);
        }
        
        &:nth-child(3) {
          bottom: -8px;
          left: 50%;
          transform: translateX(-50%);
        }
        
        &:nth-child(4) {
          left: -8px;
          top: 50%;
          transform: translateY(-50%);
        }
        
        .port-dot {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background-color: #fff;
          border: 1px solid #409EFF;
          transition: all 0.3s;
          
          &:hover {
            background-color: #409EFF;
            transform: scale(1.2);
          }
        }
      }
    }
  }
  
  // 节点类型特定样式
  &.node-type-entity .node-header {
    background-color: #409EFF;
  }
  
  &.node-type-relation .node-header {
    background-color: #67C23A;
  }
  
  &.node-type-dimension .node-header {
    background-color: #E6A23C;
  }
  
  &.node-type-fact .node-header {
    background-color: #F56C6C;
  }
}
</style> 