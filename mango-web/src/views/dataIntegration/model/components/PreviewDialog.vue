<template>
  <el-dialog
    v-model="dialogVisible"
    title="模型预览"
    width="800px"
    :destroy-on-close="true"
    :close-on-click-modal="false"
  >
    <div class="preview-dialog">
      <div class="preview-tabs">
    <el-tabs v-model="activeTab">
          <el-tab-pane label="实体关系图" name="diagram">
            <div class="diagram-preview">
              <el-empty description="实体关系图预览功能开发中..." />
        </div>
      </el-tab-pane>

          <el-tab-pane label="实体列表" name="entities">
            <div class="entity-list">
              <el-table :data="entityNodes" border>
                <el-table-column prop="name" label="实体名称" min-width="150" />
                <el-table-column label="字段数量" width="100" align="center">
                  <template #default="{ row }">
                    {{ (row.config?.fields || []).length }}
                  </template>
                </el-table-column>
                <el-table-column label="主键" min-width="120">
                  <template #default="{ row }">
                    <el-tag v-for="field in getPrimaryKeys(row)" :key="field.name" size="small" type="warning" class="primary-tag">
                      {{ field.name }}
                    </el-tag>
                    <span v-if="getPrimaryKeys(row).length === 0">-</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center">
                  <template #default="{ row }">
                    <el-button type="primary" link @click="showEntityDetail(row)">
                      查看详情
                </el-button>
                  </template>
                </el-table-column>
              </el-table>
          </div>
          </el-tab-pane>
          
          <el-tab-pane label="关系列表" name="relations">
            <div class="relation-list">
              <el-table :data="relationNodes" border>
                <el-table-column prop="name" label="关系名称" min-width="150" />
                <el-table-column label="关系类型" width="120">
                  <template #default="{ row }">
                    {{ getRelationTypeName(row.config?.relationType) }}
                  </template>
                </el-table-column>
                <el-table-column label="源实体" min-width="120">
                  <template #default="{ row }">
                    {{ row.config?.sourceEntityName || '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="目标实体" min-width="120">
                  <template #default="{ row }">
                    {{ row.config?.targetEntityName || '-' }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                    <el-button type="primary" link @click="showRelationDetail(row)">
                      查看详情
                    </el-button>
              </template>
            </el-table-column>
          </el-table>
            </div>
          </el-tab-pane>
          
          <el-tab-pane label="JSON数据" name="json">
            <div class="json-preview">
              <el-input
                v-model="jsonData"
                type="textarea"
                :rows="15"
                readonly
            />
          </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
    
    <!-- 实体详情对话框 -->
    <el-dialog
      v-model="showEntityDetailDialog"
      :title="currentEntity?.name + ' - 详情'"
      width="700px"
      append-to-body
    >
      <div class="entity-detail">
        <h4>基本信息</h4>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="实体名称">{{ currentEntity?.name }}</el-descriptions-item>
          <el-descriptions-item label="ID">{{ currentEntity?.id }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentEntity?.description || '无' }}</el-descriptions-item>
        </el-descriptions>
        
        <h4>字段列表</h4>
        <el-table :data="currentEntity?.config?.fields || []" border>
          <el-table-column prop="name" label="字段名" min-width="120" />
          <el-table-column prop="type" label="类型" width="100" />
          <el-table-column prop="length" label="长度" width="80" />
          <el-table-column prop="isPrimary" label="主键" width="60" align="center">
            <template #default="{ row }">
              <el-icon v-if="row.isPrimary" color="#E6A23C"><Key /></el-icon>
            </template>
          </el-table-column>
          <el-table-column prop="notNull" label="非空" width="60" align="center">
            <template #default="{ row }">
              <el-icon v-if="row.notNull" color="#67C23A"><Check /></el-icon>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="150" />
        </el-table>
      </div>
    </el-dialog>
    
    <!-- 关系详情对话框 -->
    <el-dialog
      v-model="showRelationDetailDialog"
      :title="currentRelation?.name + ' - 详情'"
      width="700px"
      append-to-body
    >
      <div class="relation-detail">
        <h4>基本信息</h4>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="关系名称">{{ currentRelation?.name }}</el-descriptions-item>
          <el-descriptions-item label="关系类型">{{ getRelationTypeName(currentRelation?.config?.relationType) }}</el-descriptions-item>
          <el-descriptions-item label="源实体">{{ currentRelation?.config?.sourceEntityName }}</el-descriptions-item>
          <el-descriptions-item label="目标实体">{{ currentRelation?.config?.targetEntityName }}</el-descriptions-item>
          <el-descriptions-item label="源字段">{{ currentRelation?.config?.sourceField }}</el-descriptions-item>
          <el-descriptions-item label="目标字段">{{ currentRelation?.config?.targetField }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentRelation?.description || '无' }}</el-descriptions-item>
        </el-descriptions>
        
        <div v-if="currentRelation?.config?.relationType === 'many-to-many'">
          <h4>中间表信息</h4>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="中间表名称">{{ currentRelation?.config?.junctionTable || '未设置' }}</el-descriptions-item>
          </el-descriptions>
        </div>
        
        <div v-if="currentRelation?.config?.cascadeOperations?.length">
          <h4>级联操作</h4>
          <div class="cascade-operations">
            <el-tag 
              v-for="operation in currentRelation?.config?.cascadeOperations" 
              :key="operation"
              class="cascade-tag"
            >
              {{ getCascadeOperationName(operation) }}
            </el-tag>
              </div>
        </div>
      </div>
    </el-dialog>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="dialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleExport">导出</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, defineProps, defineEmits } from 'vue'
import { ElMessage } from 'element-plus'
import { Key, Check } from '@element-plus/icons-vue'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  previewData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:visible'])

// 对话框可见性
const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

// 当前激活的标签页
const activeTab = ref('entities')

// 实体节点
const entityNodes = computed(() => {
  if (!props.previewData || !props.previewData.nodes) return []
  
  return props.previewData.nodes.filter(node => node.type === 'entity')
})

// 关系节点
const relationNodes = computed(() => {
  if (!props.previewData || !props.previewData.nodes) return []
  
  return props.previewData.nodes.filter(node => node.type === 'relation')
})

// JSON数据
const jsonData = computed(() => {
  return JSON.stringify(props.previewData, null, 2)
})

// 获取主键字段
const getPrimaryKeys = (entity) => {
  if (!entity.config || !entity.config.fields) return []
  
  return entity.config.fields.filter(field => field.isPrimary)
}

// 获取关系类型名称
const getRelationTypeName = (type) => {
  const typeNames = {
    'one-to-one': '一对一',
    'one-to-many': '一对多',
    'many-to-many': '多对多'
  }
  return typeNames[type] || type
}

// 获取级联操作名称
const getCascadeOperationName = (operation) => {
  const operationNames = {
    'create': '级联创建',
    'update': '级联更新',
    'delete': '级联删除'
  }
  return operationNames[operation] || operation
}

// 实体详情
const showEntityDetailDialog = ref(false)
const currentEntity = ref(null)

// 显示实体详情
const showEntityDetail = (entity) => {
  currentEntity.value = entity
  showEntityDetailDialog.value = true
}

// 关系详情
const showRelationDetailDialog = ref(false)
const currentRelation = ref(null)

// 显示关系详情
const showRelationDetail = (relation) => {
  currentRelation.value = relation
  showRelationDetailDialog.value = true
}

// 导出
const handleExport = () => {
  // 创建下载链接
  const dataStr = JSON.stringify(props.previewData, null, 2)
  const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr)
  
  const exportName = 'model-export-' + new Date().toISOString().slice(0, 10) + '.json'
  
  const linkElement = document.createElement('a')
  linkElement.setAttribute('href', dataUri)
  linkElement.setAttribute('download', exportName)
  linkElement.click()
  
  ElMessage.success('导出成功')
}
</script>

<style lang="scss" scoped>
.preview-dialog {
  .preview-tabs {
    .diagram-preview {
      height: 400px;
      display: flex;
      justify-content: center;
      align-items: center;
    }
    
    .entity-list, .relation-list {
      margin-bottom: 16px;
      
      .primary-tag {
        margin-right: 4px;
      }
    }
    
    .json-preview {
      margin-bottom: 16px;
    }
  }
}

.entity-detail, .relation-detail {
  h4 {
    margin-top: 20px;
    margin-bottom: 12px;
    font-size: 16px;
  }
  
  .cascade-operations {
    margin-top: 8px;
    
    .cascade-tag {
      margin-right: 8px;
      margin-bottom: 8px;
    }
  }
}
</style> 