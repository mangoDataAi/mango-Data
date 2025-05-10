<template>
  <div 
    class="graph-db-visualizer"
    v-loading.fullscreen.lock="fullscreenLoading"
    :element-loading-text="loadingText"
    element-loading-background="rgba(0, 0, 0, 0.8)"
  >
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button link @click="router.back()">
              <el-icon><ArrowLeft /></el-icon>
              返回
            </el-button>
            <el-divider direction="vertical" />
            <span class="datasource-info">
              {{ route.query.name }} ({{ route.query.type }})
            </span>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="refreshGraph">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
            <el-dropdown>
              <el-button type="primary">
                <el-icon><Plus /></el-icon> 创建
                <el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="createNodeDialog = true">创建节点</el-dropdown-item>
                  <el-dropdown-item @click="openEdgeDialog">创建关系</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </template>

      <div class="content-wrapper">
        <!-- 左侧Schema区域 -->
        <div class="left-area">
          <div class="schema-title">
            <span>数据模型</span>
            <el-tooltip content="刷新数据模型" placement="top">
              <el-button link @click="loadSchema">
                <el-icon><Refresh /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
          <el-tabs v-model="schemaTab">
            <el-tab-pane label="节点类型" name="nodes">
              <div class="schema-list">
                <div 
                  v-for="nodeType in nodeTypes" 
                  :key="nodeType.name"
                  class="schema-item"
                  @click="handleNodeTypeClick(nodeType)"
                >
                  <el-icon><Grid /></el-icon>
                  <span>{{ nodeType.name }}</span>
                  <span class="schema-count">{{ formatCount(nodeType.count) }}</span>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="关系类型" name="edges">
              <div class="schema-list">
                <div 
                  v-for="edgeType in edgeTypes" 
                  :key="edgeType.name"
                  class="schema-item"
                  @click="handleEdgeTypeClick(edgeType)"
                >
                  <el-icon><Connection /></el-icon>
                  <span>{{ edgeType.name }}</span>
                  <span class="schema-count">{{ formatCount(edgeType.count) }}</span>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- 中间可视化区域 -->
        <div class="center-area">
          <div class="visualization-header">
            <div class="visualization-title">图可视化</div>
            <div class="visualization-controls">
              <el-button-group>
                <el-tooltip content="放大" placement="top">
                  <el-button @click="zoomIn"><el-icon><ZoomIn /></el-icon></el-button>
                </el-tooltip>
                <el-tooltip content="缩小" placement="top">
                  <el-button @click="zoomOut"><el-icon><ZoomOut /></el-icon></el-button>
                </el-tooltip>
                <el-tooltip content="重置视图" placement="top">
                  <el-button @click="resetView"><el-icon><RefreshRight /></el-icon></el-button>
                </el-tooltip>
              </el-button-group>
              <el-tooltip content="全屏" placement="top">
                <el-button @click="toggleFullscreen"><el-icon><FullScreen /></el-icon></el-button>
              </el-tooltip>
              <el-select v-model="layoutType" placeholder="布局" style="width: 120px">
                <el-option label="力导向" value="force" />
                <el-option label="环形" value="circular" />
                <el-option label="树形" value="tree" />
                <el-option label="放射状" value="radial" />
              </el-select>
            </div>
          </div>
          <div class="visualization-container" ref="graphContainer"></div>
        </div>

        <!-- 右侧查询区域 -->
        <div class="right-area">
          <el-tabs v-model="queryTab">
            <el-tab-pane label="查询" name="query">
              <div class="query-editor">
                <div class="editor-title">Cypher查询</div>
                <div class="query-textarea">
                  <el-input
                    v-model="cypherQuery"
                    type="textarea"
                    :rows="8"
                    placeholder="输入Cypher查询，例如: MATCH p = (n)-[r]->(m) RETURN n, r, m LIMIT 25"
                  />
                </div>
                <div class="query-actions">
                  <el-button type="primary" @click="executeCypher">执行</el-button>
                  <el-button @click="clearResults">清除结果</el-button>
                </div>
                <div class="query-results">
                  <div v-if="queryResults.length" class="results-info">
                    查询结果: {{ queryResults.length }} 条记录
                  </div>
                  <div v-if="queryResults.length" class="results-display">
                    <el-table :data="queryResults" border style="width: 100%">
                      <el-table-column 
                        v-for="column in resultColumns" 
                        :key="column"
                        :prop="column"
                        :label="column"
                        show-overflow-tooltip
                      />
                    </el-table>
                  </div>
                  <div v-else class="no-results">
                    <el-empty description="暂无查询结果" />
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="探索" name="explore">
              <div class="explorer">
                <div class="explorer-title">数据探索</div>
                <div class="explorer-form">
                  <el-form :model="explorerForm" label-width="80px">
                    <el-form-item label="起始类型">
                      <el-select v-model="explorerForm.startType" placeholder="选择节点类型">
                        <el-option 
                          v-for="type in nodeTypes"
                          :key="type.name"
                          :label="type.name"
                          :value="type.name"
                        />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="属性条件">
                      <el-input v-model="explorerForm.propertyFilter" placeholder="属性:值 (可选)" />
                    </el-form-item>
                    <el-form-item label="深度">
                      <el-input-number v-model="explorerForm.depth" :min="1" :max="3" />
                    </el-form-item>
                    <el-form-item label="限制数量">
                      <el-input-number v-model="explorerForm.limit" :min="1" :max="100" />
                    </el-form-item>
                    <el-form-item>
                      <el-button type="primary" @click="exploreData">开始探索</el-button>
                    </el-form-item>
                  </el-form>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </el-card>

    <!-- 创建节点对话框 -->
    <el-dialog
      v-model="createNodeDialog"
      title="创建节点"
      width="500px"
    >
      <el-form ref="nodeFormRef" :model="nodeForm" label-width="100px">
        <el-form-item label="节点类型" prop="type">
          <el-select v-model="nodeForm.type" placeholder="选择节点类型">
            <el-option 
              v-for="type in nodeTypes"
              :key="type.name"
              :label="type.name"
              :value="type.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="属性">
          <div 
            v-for="(prop, index) in nodeForm.properties" 
            :key="index"
            class="property-item"
          >
            <el-input v-model="prop.key" placeholder="键" style="width: 40%" />
            <el-input v-model="prop.value" placeholder="值" style="width: 40%" />
            <el-button type="danger" circle @click="removeProperty(nodeForm.properties, index)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <el-button type="primary" link @click="addProperty(nodeForm.properties)">
            <el-icon><Plus /></el-icon> 添加属性
          </el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createNodeDialog = false">取消</el-button>
        <el-button type="primary" @click="createNode">创建</el-button>
      </template>
    </el-dialog>

    <!-- 创建关系对话框 -->
    <el-dialog
      v-model="edgeDialog"
      title="创建关系"
      width="500px"
    >
      <el-form ref="edgeFormRef" :model="edgeForm" label-width="100px">
        <el-form-item label="起始节点" prop="fromNode">
          <el-select 
            v-model="edgeForm.fromNode" 
            filterable 
            placeholder="选择起始节点"
            style="width: 100%"
          >
            <el-option 
              v-for="node in nodeList"
              :key="node.id"
              :label="node.displayName"
              :value="node.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关系类型" prop="type">
          <el-select v-model="edgeForm.type" placeholder="选择关系类型">
            <el-option 
              v-for="type in edgeTypes"
              :key="type.name"
              :label="type.name"
              :value="type.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标节点" prop="toNode">
          <el-select 
            v-model="edgeForm.toNode" 
            filterable 
            placeholder="选择目标节点"
            style="width: 100%"
          >
            <el-option 
              v-for="node in nodeList"
              :key="node.id"
              :label="node.displayName"
              :value="node.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="属性">
          <div 
            v-for="(prop, index) in edgeForm.properties" 
            :key="index"
            class="property-item"
          >
            <el-input v-model="prop.key" placeholder="键" style="width: 40%" />
            <el-input v-model="prop.value" placeholder="值" style="width: 40%" />
            <el-button type="danger" circle @click="removeProperty(edgeForm.properties, index)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
          <el-button type="primary" link @click="addProperty(edgeForm.properties)">
            <el-icon><Plus /></el-icon> 添加属性
          </el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="edgeDialog = false">取消</el-button>
        <el-button type="primary" @click="createEdge">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  ArrowLeft, Refresh, Plus, ArrowDown, Grid, Connection,
  ZoomIn, ZoomOut, RefreshRight, FullScreen, Delete
} from '@element-plus/icons-vue'
// 引入ECharts
import * as echarts from 'echarts'
// API导入
import graphService from '@/api/graph-service'
import { 
  getGraphSchema, executeQuery, createNode as apiCreateNode,
  createRelationship, getNeighbors
} from '@/api/graph'

const router = useRouter()
const route = useRoute()

// 图实例
let graphInstance = null
const graphContainer = ref(null)

// 加载状态
const fullscreenLoading = ref(false)
const loadingText = ref('加载中...')

// 标签页
const schemaTab = ref('nodes')
const queryTab = ref('query')

// 图数据
const nodeTypes = ref([])
const edgeTypes = ref([])
const graphData = ref({
  nodes: [],
  edges: []
})

// 布局设置
const layoutType = ref('force')

// 查询
const cypherQuery = ref('MATCH p = (n)-[r]->(m) RETURN n, r, m LIMIT 25')
const queryResults = ref([])
const resultColumns = ref([])

// 探索表单
const explorerForm = ref({
  startType: '',
  propertyFilter: '',
  depth: 1,
  limit: 20
})

// 创建节点表单
const createNodeDialog = ref(false)
const nodeForm = ref({
  type: '',
  properties: [{ key: '', value: '' }]
})

// 创建边表单
const edgeDialog = ref(false)
const edgeForm = ref({
  fromNode: '',
  type: '',
  toNode: '',
  properties: [{ key: '', value: '' }]
})

// 节点列表
const nodeList = ref([]);

// 获取所有节点
const loadAllNodes = async () => {
  try {
    fullscreenLoading.value = true;
    loadingText.value = '加载节点列表...';
    const res = await graphService.getAllNodes(route.params.id, 200); // 限制获取200个节点
    if (res.code === 0) {
      nodeList.value = res.data;
      console.log('获取节点成功:', nodeList.value);
    } else {
      ElMessage.warning('获取节点列表失败: ' + res.message);
    }
  } catch (error) {
    console.error('获取节点列表出错:', error);
    ElMessage.error('获取节点列表失败: ' + (error?.message || '未知错误'));
  } finally {
    fullscreenLoading.value = false;
  }
};

// 打开创建关系对话框时加载节点列表
const openEdgeDialog = () => {
  loadAllNodes();
  edgeDialog.value = true;
};

// 加载Schema
const loadSchema = async () => {
  try {
    fullscreenLoading.value = true
    loadingText.value = '加载图数据模型...'
    
    // 1. 首先尝试从服务器获取Schema
    const response = await getGraphSchema(route.params.id)
    console.log('Schema API返回数据:', response)
    
    // 提取节点类型信息
    if (response.data) {
      // 尝试获取节点类型
      if (response.data.nodeTypes && response.data.nodeTypes.length > 0) {
        nodeTypes.value = response.data.nodeTypes
      } else if (response.data.labels && response.data.labels.length > 0) {
        // 如果存在labels而没有nodeTypes，则转换labels为节点类型
        nodeTypes.value = response.data.labels.map(label => ({
          name: label,
          count: 0
        }))
      }
      
      // 尝试获取关系类型
      if (response.data.edgeTypes && response.data.edgeTypes.length > 0) {
        edgeTypes.value = response.data.edgeTypes
      } else if (response.data.relationshipTypes && response.data.relationshipTypes.length > 0) {
        // 如果存在relationshipTypes而没有edgeTypes，则转换关系类型
        edgeTypes.value = response.data.relationshipTypes.map(type => ({
          name: type,
          count: 0
        }))
      }
    }
    
    // 2. 如果从服务器获取的Schema为空，尝试使用查询结果来构建
    if ((nodeTypes.value.length === 0 || edgeTypes.value.length === 0)) {
      console.log('从服务器获取的Schema可能不完整，尝试通过查询构建...')
      
      // 2.1 先尝试获取节点统计信息
      if (nodeTypes.value.length === 0) {
        try {
          // 查询所有节点标签及其数量
          const labelsQuery = `
            MATCH (n)
            RETURN DISTINCT labels(n) AS labels, count(n) AS count
          `
          const labelsResponse = await executeQuery(route.params.id, { query: labelsQuery })
          
          if (labelsResponse.data && labelsResponse.data.data && labelsResponse.data.data.length > 0) {
            const extractedNodeTypes = new Map()
            
            // 处理每个标签组
            labelsResponse.data.data.forEach(item => {
              if (item.labels && Array.isArray(item.labels)) {
                item.labels.forEach(label => {
                  if (!extractedNodeTypes.has(label)) {
                    extractedNodeTypes.set(label, { name: label, count: 0 })
                  }
                  // 累加该标签的节点数量
                  extractedNodeTypes.get(label).count += parseInt(item.count || 0)
                })
              }
            })
            
            nodeTypes.value = Array.from(extractedNodeTypes.values())
            console.log('从查询中提取到', nodeTypes.value.length, '个节点类型')
          }
        } catch (error) {
          console.error('获取节点标签查询失败:', error)
        }
      }
      
      // 2.2 尝试获取关系类型统计信息
      if (edgeTypes.value.length === 0) {
        try {
          // 查询所有关系类型及其数量
          const relationshipsQuery = `
            MATCH ()-[r]->()
            RETURN DISTINCT type(r) AS type, count(r) AS count
          `
          const relationshipsResponse = await executeQuery(route.params.id, { query: relationshipsQuery })
          
          if (relationshipsResponse.data && relationshipsResponse.data.data && relationshipsResponse.data.data.length > 0) {
            // 提取关系类型信息
            edgeTypes.value = relationshipsResponse.data.data.map(item => ({
              name: item.type,
              count: parseInt(item.count || 0)
            }))
            console.log('从查询中提取到', edgeTypes.value.length, '个关系类型')
          }
        } catch (error) {
          console.error('获取关系类型查询失败:', error)
        }
      }
    }
    
    // 3. 如果上述方法都没有获取到数据，从查询结果中直接提取
    if ((nodeTypes.value.length === 0 || edgeTypes.value.length === 0)) {
      console.log('尝试从样本数据中提取模型信息...')
      
      try {
        // 获取一些样本数据
        const sampleQuery = `MATCH p = (n)-[r]->(m) RETURN n, r, m LIMIT 50`
        const sampleResponse = await executeQuery(route.params.id, { query: sampleQuery })
        
        if (sampleResponse.data && sampleResponse.data.data && sampleResponse.data.data.length > 0) {
          // 处理节点类型
          if (nodeTypes.value.length === 0) {
            const nodeLabelsMap = new Map()
            
            // 从样本中提取节点标签
            sampleResponse.data.data.forEach(item => {
              // 处理开始节点
              if (item.n && item.n.labels) {
                item.n.labels.forEach(label => {
                  if (!nodeLabelsMap.has(label)) {
                    nodeLabelsMap.set(label, { name: label, count: 0, nodes: new Set() })
                  }
                  // 避免重复计数
                  nodeLabelsMap.get(label).nodes.add(item.n.id)
                })
              }
              
              // 处理结束节点
              if (item.m && item.m.labels) {
                item.m.labels.forEach(label => {
                  if (!nodeLabelsMap.has(label)) {
                    nodeLabelsMap.set(label, { name: label, count: 0, nodes: new Set() })
                  }
                  // 避免重复计数
                  nodeLabelsMap.get(label).nodes.add(item.m.id)
                })
              }
            })
            
            // 统计每个标签的节点数量
            nodeLabelsMap.forEach(info => {
              info.count = info.nodes.size
              delete info.nodes  // 移除辅助集合
            })
            
            nodeTypes.value = Array.from(nodeLabelsMap.values())
            console.log('从样本中提取到', nodeTypes.value.length, '个节点类型')
          }
          
          // 处理关系类型
          if (edgeTypes.value.length === 0) {
            const edgeTypesMap = new Map()
            
            // 从样本中提取关系类型
            sampleResponse.data.data.forEach(item => {
              if (item.r && item.r.type) {
                if (!edgeTypesMap.has(item.r.type)) {
                  edgeTypesMap.set(item.r.type, { name: item.r.type, count: 0 })
                }
                edgeTypesMap.get(item.r.type).count++
              }
            })
            
            edgeTypes.value = Array.from(edgeTypesMap.values())
            console.log('从样本中提取到', edgeTypes.value.length, '个关系类型')
          }
        }
      } catch (error) {
        console.error('从样本数据提取模型信息失败:', error)
      }
    }
    
    // 4. 如果实在无法获取数据模型，尝试从当前图数据中提取
    if ((nodeTypes.value.length === 0 || edgeTypes.value.length === 0) && graphData.value.nodes && graphData.value.nodes.length > 0) {
      console.log('尝试从当前图数据中提取模型信息...')
      
      // 处理节点类型
      if (nodeTypes.value.length === 0) {
        const nodeCategories = new Map()
        graphData.value.nodes.forEach(node => {
          if (node.category) {
            if (!nodeCategories.has(node.category)) {
              nodeCategories.set(node.category, { name: node.category, count: 0 })
            }
            nodeCategories.get(node.category).count++
          }
        })
        
        nodeTypes.value = Array.from(nodeCategories.values())
        console.log('从图数据中提取到', nodeTypes.value.length, '个节点类型')
      }
      
      // 处理关系类型
      if (edgeTypes.value.length === 0) {
        const edgeLabels = new Map()
        graphData.value.edges.forEach(edge => {
          if (edge.label && edge.label.formatter) {
            const type = edge.label.formatter
            if (!edgeLabels.has(type)) {
              edgeLabels.set(type, { name: type, count: 0 })
            }
            edgeLabels.get(type).count++
          }
        })
        
        edgeTypes.value = Array.from(edgeLabels.values())
        console.log('从图数据中提取到', edgeTypes.value.length, '个关系类型')
      }
    }
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('加载图数据模型失败:', error)
    ElMessage.error('加载图数据模型失败')
    fullscreenLoading.value = false
  }
}

// 初始化图实例
const initGraph = () => {
  if (!graphContainer.value) return
  
  // 如果已存在实例，先销毁
  if (graphInstance) {
    graphInstance.dispose()
  }
  
  // 初始化ECharts实例
  graphInstance = echarts.init(graphContainer.value)
  
  // 设置默认配置
  const option = {
    title: {
      text: '图数据可视化',
      show: false
    },
    tooltip: {
      trigger: 'item'
    },
    legend: {
      data: ['节点']
    },
    series: [{
      type: 'graph',
      layout: layoutType.value,
      data: [],
      links: [],
      roam: true,
      draggable: true,
      label: {
        show: true,
        position: 'right'
      },
      lineStyle: {
        color: 'source',
        curveness: 0.3
      }
    }]
  }
  
  graphInstance.setOption(option)
  
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
}

// 处理窗口大小变化
const handleResize = () => {
  if (graphInstance) {
    graphInstance.resize()
  }
}

// 组件销毁前清理
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (graphInstance) {
    graphInstance.dispose()
    graphInstance = null
  }
})

// 缩放控制
const zoomIn = () => {
  if (graphInstance) {
    graphInstance.dispatchAction({
      type: 'graphZoom',
      zoom: 1.2
    })
  }
}

const zoomOut = () => {
  if (graphInstance) {
    graphInstance.dispatchAction({
      type: 'graphZoom',
      zoom: 0.8
    })
  }
}

const resetView = () => {
  if (graphInstance) {
    graphInstance.dispatchAction({
      type: 'graphReset'
    })
  }
}

const toggleFullscreen = () => {
  if (!graphContainer.value) return
  
  const element = graphContainer.value
  
  if (document.fullscreenElement) {
    // 当前处于全屏模式，退出全屏
    if (document.exitFullscreen) {
      document.exitFullscreen()
    } else if (document.webkitExitFullscreen) {
      document.webkitExitFullscreen()
    } else if (document.mozCancelFullScreen) {
      document.mozCancelFullScreen()
    } else if (document.msExitFullscreen) {
      document.msExitFullscreen()
    }
  } else {
    // 当前不是全屏模式，进入全屏
    if (element.requestFullscreen) {
      element.requestFullscreen()
    } else if (element.webkitRequestFullscreen) {
      element.webkitRequestFullscreen()
    } else if (element.mozRequestFullScreen) {
      element.mozRequestFullScreen()
    } else if (element.msRequestFullscreen) {
      element.msRequestFullscreen()
    }
  }
  
  // 调整图表大小以适应新的容器尺寸
  setTimeout(() => {
    if (graphInstance) {
      graphInstance.resize()
    }
  }, 100)
}

// 属性操作
const addProperty = (properties) => {
  properties.push({ key: '', value: '' })
}

const removeProperty = (properties, index) => {
  properties.splice(index, 1)
}

// 创建节点
const createNode = async () => {
  if (!nodeForm.value.type) {
    ElMessage.warning('请选择节点类型')
    return
  }
  
  try {
    const properties = {}
    for (const prop of nodeForm.value.properties) {
      if (prop.key && prop.value !== '') {
        properties[prop.key] = prop.value
      }
    }
    
    await apiCreateNode(route.params.id, {
      labels: [nodeForm.value.type],
      properties
    })
    
    ElMessage.success('节点创建成功')
    createNodeDialog.value = false
    
    // 重置表单
    nodeForm.value = {
      type: '',
      properties: [{ key: '', value: '' }]
    }
    
    // 刷新图数据
    refreshGraph()
  } catch (error) {
    console.error('创建节点失败:', error)
    ElMessage.error('创建节点失败: ' + error.message)
  }
}

// 创建关系
const createEdge = async () => {
  try {
    // 验证节点ID是否为数字
    const fromNodeId = Number(edgeForm.value.fromNode);
    const toNodeId = Number(edgeForm.value.toNode);
    
    if (isNaN(fromNodeId) || isNaN(toNodeId)) {
      ElMessage.error('节点ID必须为数字');
      return;
    }
    
    const properties = {}
    for (const prop of edgeForm.value.properties) {
      if (prop.key && prop.value !== '') {
        properties[prop.key] = prop.value
      }
    }
    
    await createRelationship(route.params.id, {
      startNodeId: edgeForm.value.fromNode,
      type: edgeForm.value.type,
      endNodeId: edgeForm.value.toNode,
      properties
    })
    
    ElMessage.success('关系创建成功')
    edgeDialog.value = false
    
    // 重置表单
    edgeForm.value = {
      fromNode: '',
      type: '',
      toNode: '',
      properties: [{ key: '', value: '' }]
    }
    
    // 刷新图数据
    refreshGraph()
  } catch (error) {
    console.error('创建关系失败:', error)
    ElMessage.error('创建关系失败: ' + (error.message || '未知错误'))
  }
}

// 格式化函数
const formatCount = (count) => {
  if (count === undefined || count === null) return ''
  return new Intl.NumberFormat().format(count)
}

// 刷新图数据
const refreshGraph = async () => {
  try {
    fullscreenLoading.value = true
    loadingText.value = '加载图数据...'
    
    // 执行默认查询
    await executeCypher()
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('刷新图数据失败:', error)
    ElMessage.error('刷新图数据失败')
    fullscreenLoading.value = false
  }
}

// 执行Cypher查询
const executeCypher = async () => {
  if (!cypherQuery.value.trim()) {
    ElMessage.warning('请输入Cypher查询')
    return
  }
  
  try {
    fullscreenLoading.value = true
    loadingText.value = '执行查询...'
    
    const response = await executeQuery(route.params.id, {
      query: cypherQuery.value
    })
    
    console.log('查询返回数据:', response) // 添加调试信息
    
    // 处理返回的结果格式
    if (response.data && response.data.data) {
      // 检测是否为标准Neo4j响应格式
      const results = response.data.data || []
      
      // 提取表格列
      const columns = []
      if (results.length > 0) {
        // 获取第一条记录中所有可能的键
        const firstResult = results[0]
        const keys = Object.keys(firstResult)
        
        keys.forEach(key => {
          columns.push(key)
        })
      }
      
      // 转换数据为表格格式
      const tableData = results.map(item => {
        const row = {}
        for (const key in item) {
          const value = item[key]
          if (value && typeof value === 'object') {
            // 处理节点或关系对象
            row[key] = JSON.stringify(value.properties || value)
          } else {
            row[key] = value
          }
        }
        return row
      })
      
      queryResults.value = tableData
      resultColumns.value = columns
      
      // 处理图数据
      const nodes = []
      const edges = []
      const nodeMap = new Map() // 用于记录已添加的节点，避免重复
      
      results.forEach(item => {
        // 处理每个返回的记录
        for (const key in item) {
          const value = item[key]
          
          // 处理路径对象
          if (value && value.nodes && value.relationships) {
            // 处理路径中的节点
            if (Array.isArray(value.nodes)) {
              value.nodes.forEach(node => {
                processNode(node, nodes, nodeMap)
              })
            }
            
            // 处理路径中的关系
            if (Array.isArray(value.relationships)) {
              value.relationships.forEach(rel => {
                processRelationship(rel, edges)
              })
            }
          }
          // 处理节点对象
          else if (value && value.id !== undefined && value.properties && value.labels) {
            processNode(value, nodes, nodeMap)
          }
          // 处理关系对象
          else if (value && value.type && value.startNodeId !== undefined && value.endNodeId !== undefined) {
            processRelationship(value, edges)
          }
        }
      })
      
      console.log('构建的图数据:', { nodes, edges })
      
      // 如果查询返回了节点但没有边，尝试执行额外查询获取节点间的关系
      if (nodes.length > 1 && edges.length === 0) {
        try {
          // 构建查询节点间关系的Cypher
          const nodeIds = nodes.map(n => n.id).join(',')
          const relQuery = `MATCH (n)-[r]->(m) WHERE id(n) IN [${nodeIds}] AND id(m) IN [${nodeIds}] RETURN r`
          
          const relResponse = await executeQuery(route.params.id, {
            query: relQuery
          })
          
          if (relResponse.data && relResponse.data.data) {
            relResponse.data.data.forEach(item => {
              for (const key in item) {
                if (item[key] && item[key].type && item[key].startNodeId !== undefined) {
                  processRelationship(item[key], edges)
                }
              }
            })
          }
        } catch (e) {
          console.error('获取节点关系失败:', e)
        }
      }
      
      if (nodes.length > 0) {
        graphData.value = { nodes, edges }
        updateGraph()
      }
    } else {
      queryResults.value = []
      resultColumns.value = []
    }
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('执行查询失败:', error)
    ElMessage.error('执行查询失败: ' + (error.message || '未知错误'))
    fullscreenLoading.value = false
  }
}

// 处理节点数据
const processNode = (node, nodes, nodeMap) => {
  const nodeId = node.id.toString()
  
  // 避免添加重复节点
  if (!nodeMap.has(nodeId)) {
    nodeMap.set(nodeId, true)
    
    // 确定节点标签和颜色
    const label = node.properties.name || (node.labels[0] + '_' + node.id)
    let color = '#5470c6' // 默认颜色
    
    // 为不同类型的节点设置不同颜色
    if (node.labels.includes('User')) color = '#91cc75'
    if (node.labels.includes('Product')) color = '#fac858'
    if (node.labels.includes('Adult')) color = '#ee6666'
    
    nodes.push({
      id: nodeId,
      name: label,
      value: Object.keys(node.properties).length,
      symbolSize: 40,
      itemStyle: { color },
      category: node.labels[0], // 设置节点类别，用于图例
      properties: node.properties,
      label: { show: true }
    })
  }
}

// 处理关系数据
const processRelationship = (rel, edges) => {
  // 检查必要的属性
  if (!rel.type || rel.startNodeId === undefined || rel.endNodeId === undefined) return
  
  // 检查是否已存在相同关系
  const existingEdge = edges.find(e => 
    e.source === rel.startNodeId.toString() && 
    e.target === rel.endNodeId.toString() && 
    e.label.formatter === rel.type
  )
  
  if (!existingEdge) {
    edges.push({
      id: (edges.length + 1).toString(),
      source: rel.startNodeId.toString(),
      target: rel.endNodeId.toString(),
      label: { show: true, formatter: rel.type },
      properties: rel.properties || {}
    })
  }
}

// 清除查询结果
const clearResults = () => {
  queryResults.value = []
  resultColumns.value = []
}

// 数据探索
const exploreData = async () => {
  if (!explorerForm.value.startType) {
    ElMessage.warning('请选择起始节点类型')
    return
  }
  
  try {
    fullscreenLoading.value = true
    loadingText.value = '探索数据...'
    
    const response = await getNeighbors(route.params.id, {
      nodeType: explorerForm.value.startType,
      propertyFilter: explorerForm.value.propertyFilter,
      depth: explorerForm.value.depth,
      limit: explorerForm.value.limit
    })
    
    if (response.data.graphData) {
      graphData.value = response.data.graphData
      updateGraph()
    }
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('数据探索失败:', error)
    ElMessage.error('数据探索失败')
    fullscreenLoading.value = false
  }
}

// 更新图可视化
const updateGraph = () => {
  console.log('更新图可视化', graphData.value)
  
  if (!graphInstance) {
    // 如果图实例不存在，先初始化
    initGraph()
    return
  }
  
  if (!graphData.value.nodes || graphData.value.nodes.length === 0) {
    // 如果没有节点数据，清空图表
    graphInstance.setOption({
      series: [{
        type: 'graph',
        data: [],
        links: []
      }]
    })
    return
  }
  
  // 从节点数据中提取唯一的类别并创建图例数据
  const categoriesMap = new Map()
  
  graphData.value.nodes.forEach(node => {
    if (node.category && !categoriesMap.has(node.category)) {
      categoriesMap.set(node.category, {
        name: node.category
      })
    }
  })
  
  // 转换为数组
  const categories = Array.from(categoriesMap.values())
  
  // 如果没有类别，添加一个默认类别
  if (categories.length === 0) {
    categories.push({ name: 'Default' })
  }
  
  // 确保每个节点有一个类别名称，与categories对应
  const nodes = graphData.value.nodes.map(node => {
    return {
      ...node,
      // 如果节点没有类别，则使用默认类别
      category: node.category || 'Default',
      // 设置默认大小，或根据属性数量调整大小
      symbolSize: node.symbolSize || Math.max(30, Math.min(50, (node.value || 0) * 5 + 30))
    }
  })
  
  // 处理边数据 - ECharts使用links而不是edges
  const links = graphData.value.edges.map(edge => {
    return {
      source: edge.source,
      target: edge.target,
      value: edge.label?.formatter || '',
      lineStyle: {
        width: 2,
        curveness: 0.2
      },
      label: {
        show: true,
        formatter: edge.label?.formatter || ''
      },
      // 保留原始属性
      properties: edge.properties
    }
  })
  
  // 设置图表选项
  const option = {
    tooltip: {
      trigger: 'item',
      formatter: function(params) {
        if (params.dataType === 'node') {
          // 节点提示框
          let result = `<div style="font-weight:bold;margin-bottom:5px;">${params.name || '节点'}</div>`;
          if (params.data.properties) {
            for (let key in params.data.properties) {
              const value = params.data.properties[key];
              result += `<div>${key}: ${value}</div>`;
            }
          }
          return result;
        } else if (params.dataType === 'edge') {
          // 边提示框
          let result = `<div style="font-weight:bold;margin-bottom:5px;">${params.value || '关系'}</div>`;
          if (params.data && params.data.properties) {
            for (let key in params.data.properties) {
              const value = params.data.properties[key];
              result += `<div>${key}: ${value}</div>`;
            }
          }
          return result;
        }
      }
    },
    legend: {
      // 确保legend数据与categories一致
      data: categories.map(c => c.name),
      orient: 'vertical',
      right: 10,
      top: 20,
      bottom: 20,
      formatter: function(name) {
        return name || 'Default';
      }
    },
    animationDurationUpdate: 1500,
    animationEasingUpdate: 'quinticInOut',
    series: [{
      type: 'graph',
      layout: layoutType.value,
      data: nodes,
      links: links,
      categories: categories,
      roam: true,
      draggable: true,
      focus: 'adjacency',
      label: {
        show: true,
        position: 'right',
        formatter: '{b}'
      },
      labelLayout: {
        hideOverlap: true
      },
      scaleLimit: {
        min: 0.1,
        max: 5
      },
      lineStyle: {
        color: 'source',
        curveness: 0.3
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: {
          width: 4
        }
      },
      force: {
        repulsion: 150,
        edgeLength: 100,
        friction: 0.2
      }
    }]
  }
  
  graphInstance.setOption(option, true)
}

// 处理节点类型点击
const handleNodeTypeClick = async (nodeType) => {
  cypherQuery.value = `MATCH p = (n:${nodeType.name})-[r]-(m) RETURN n, r, m LIMIT 25`
  queryTab.value = 'query'
  await executeCypher()
}

// 处理边类型点击
const handleEdgeTypeClick = async (edgeType) => {
  cypherQuery.value = `MATCH p = ()-[r:${edgeType.name}]->() RETURN p LIMIT 25`
  queryTab.value = 'query'
  await executeCypher()
}

// 初始化
onMounted(async () => {
  await loadSchema()
  await nextTick()
  initGraph()
  refreshGraph()
})
</script>

<style scoped>
.graph-db-visualizer {
  height: calc(100vh - 140px);
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

.header-right {
  display: flex;
  gap: 8px;
}

.content-wrapper {
  display: flex;
  height: calc(100vh - 200px);
  overflow: hidden;
}

.left-area {
  width: 240px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #ebeef5;
  padding-right: 8px;
}

.schema-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-weight: bold;
}

.schema-list {
  overflow-y: auto;
  max-height: calc(100vh - 280px);
}

.schema-item {
  display: flex;
  align-items: center;
  padding: 8px;
  border-radius: 4px;
  cursor: pointer;
  margin-bottom: 4px;
}

.schema-item:hover {
  background-color: #f5f7fa;
}

.schema-item .el-icon {
  margin-right: 8px;
}

.schema-count {
  margin-left: auto;
  color: #909399;
  font-size: 12px;
}

.center-area {
  flex: 2;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #ebeef5;
  padding: 0 16px;
}

.visualization-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.visualization-title {
  font-weight: bold;
}

.visualization-controls {
  display: flex;
  gap: 8px;
}

.visualization-container {
  flex: 1;
  background-color: #f8f9fa;
  border-radius: 4px;
  overflow: hidden;
}

.right-area {
  width: 360px;
  display: flex;
  flex-direction: column;
  padding-left: 8px;
}

.editor-title {
  font-weight: bold;
  margin-bottom: 8px;
}

.query-textarea {
  margin-bottom: 8px;
}

.query-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.query-results {
  margin-top: 16px;
}

.results-info {
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.results-display {
  max-height: 300px;
  overflow: auto;
}

.property-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}

.explorer-title {
  font-weight: bold;
  margin-bottom: 16px;
}

.explorer-form {
  padding: 8px;
}
</style> 