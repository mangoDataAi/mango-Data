<template>
  <div 
    class="message-queue-browser"
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
            <el-button-group>
              <el-button type="primary" @click="refreshTopics">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
              <el-button type="primary" @click="showCreateTopicDialog">
                <el-icon><Plus /></el-icon>
                新建主题
              </el-button>
            </el-button-group>
          </div>
        </div>
      </template>

      <div class="content-wrapper">
        <!-- 左侧主题列表 -->
        <div class="left-area">
          <div class="search-area">
            <el-input
              v-model="searchText"
              placeholder="搜索主题"
              :prefix-icon="Search"
              size="default"
              clearable
              @clear="handleSearch"
              @input="handleSearch"
            />
          </div>
          <div class="topics-list">
            <el-scrollbar>
              <el-menu
                :default-active="activeTopic"
                @select="handleTopicSelect"
                class="topics-menu"
              >
                <el-menu-item 
                  v-for="topic in filteredTopics" 
                  :key="topic.name"
                  :index="topic.name"
                >
                  <el-icon><MessageBox /></el-icon>
                  <span>{{ topic.name }}</span>
                  <template #title>
                    <span class="topic-title">{{ topic.name }}</span>
                    <el-tag size="small" :type="getPartitionTagType(topic.partitions)">{{ topic.partitions }}分区</el-tag>
                  </template>
                </el-menu-item>
              </el-menu>
            </el-scrollbar>
          </div>
        </div>

        <!-- 中间内容区域 -->
        <div class="main-content">
          <div v-if="!selectedTopic" class="empty-state">
            <el-empty description="请选择一个主题查看详情" />
          </div>
          <div v-else class="topic-details">
            <div class="topic-header">
              <h2>{{ selectedTopic.name }}</h2>
              <div class="topic-actions">
                <el-button-group>
                  <el-button type="primary" @click="showSendMessageDialog">
                    <el-icon><Upload /></el-icon>
                    发送消息
                  </el-button>
                  <el-button type="success" @click="consumeTopicMessages">
                    <el-icon><Download /></el-icon>
                    消费消息
                  </el-button>
                  <el-button type="danger" @click="confirmDeleteTopic">
                    <el-icon><Delete /></el-icon>
                    删除主题
                  </el-button>
                </el-button-group>
              </div>
            </div>

            <el-tabs v-model="activeTab" class="topic-tabs">
              <el-tab-pane label="概览" name="overview">
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="主题名称">{{ selectedTopic.name }}</el-descriptions-item>
                  <el-descriptions-item label="分区数">{{ selectedTopic.partitions }}</el-descriptions-item>
                  <el-descriptions-item label="副本因子">{{ selectedTopic.replicationFactor }}</el-descriptions-item>
                  <el-descriptions-item label="消息总数">{{ selectedTopic.messageCount }}</el-descriptions-item>
                  <el-descriptions-item label="消费者组数">{{ selectedTopic.consumerGroups?.length || 0 }}</el-descriptions-item>
                  <el-descriptions-item label="创建时间">{{ formatDate(selectedTopic.createTime) }}</el-descriptions-item>
                </el-descriptions>

                <h3>配置信息</h3>
                <el-table :data="topicConfigData" stripe>
                  <el-table-column prop="key" label="配置项" width="250" />
                  <el-table-column prop="value" label="值" />
                  <el-table-column prop="description" label="描述" show-overflow-tooltip />
                </el-table>
              </el-tab-pane>

              <el-tab-pane label="消费者组" name="consumers">
                <el-table :data="consumerGroups" stripe>
                  <el-table-column prop="groupId" label="消费者组ID" width="250" />
                  <el-table-column prop="members" label="成员数" width="100" />
                  <el-table-column prop="lag" label="消息积压量" width="120">
                    <template #default="{ row }">
                      <el-tag :type="getLagTagType(row.lag)">{{ row.lag }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="status" label="状态" width="120">
                    <template #default="{ row }">
                      <el-tag :type="getStatusTagType(row.status)">{{ row.status || '未知' }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="150">
                    <template #default="{ row }">
                      <el-button-group>
                        <el-button type="primary" size="small" @click="viewConsumerDetails(row)">
                          <el-icon><View /></el-icon>
                        </el-button>
                        <el-button type="danger" size="small" @click="deleteConsumerGroup(row)">
                          <el-icon><Delete /></el-icon>
                        </el-button>
                      </el-button-group>
                    </template>
                  </el-table-column>
                </el-table>
              </el-tab-pane>

              <el-tab-pane label="分区信息" name="partitions">
                <el-table :data="partitions" stripe>
                  <el-table-column prop="partition" label="分区ID" width="100" />
                  <el-table-column prop="leader" label="Leader节点" width="120" />
                  <el-table-column prop="replicas" label="副本节点" width="200">
                    <template #default="{ row }">
                      <el-tag v-for="replica in row.replicas" :key="replica" class="replica-tag">
                        {{ replica }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="inSyncReplicas" label="同步副本" width="200">
                    <template #default="{ row }">
                      <el-tag v-for="isr in row.inSyncReplicas" :key="isr" type="success" class="replica-tag">
                        {{ isr }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="beginOffset" label="起始偏移量" width="120" />
                  <el-table-column prop="endOffset" label="结束偏移量" width="120" />
                  <el-table-column prop="messageCount" label="消息数量" width="120" />
                </el-table>
              </el-tab-pane>

              <el-tab-pane label="消息浏览" name="messages">
                <div class="messages-filter">
                  <el-form :inline="true" :model="messagesQuery">
                    <el-form-item label="分区">
                      <el-select v-model="messagesQuery.partition" placeholder="选择分区">
                        <el-option label="全部分区" :value="-1" />
                        <el-option 
                          v-for="partition in partitions" 
                          :key="partition.partition" 
                          :label="`分区 ${partition.partition}`" 
                          :value="partition.partition" 
                        />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="起始偏移量">
                      <el-select v-model="messagesQuery.offset" placeholder="选择起始位置">
                        <el-option label="最新消息" :value="-1" />
                        <el-option label="最早消息" :value="-2" />
                        <el-option label="自定义偏移量" :value="0" />
                      </el-select>
                      <el-input-number 
                        v-if="messagesQuery.offset >= 0" 
                        v-model="messagesQuery.customOffset" 
                        :min="0" 
                        placeholder="偏移量"
                        style="margin-left: 10px;" 
                      />
                    </el-form-item>
                    <el-form-item label="最大消息数">
                      <el-input-number v-model="messagesQuery.maxMessages" :min="1" :max="100" />
                    </el-form-item>
                    <el-form-item>
                      <el-button type="primary" @click="fetchMessages">
                        <el-icon><Search /></el-icon>
                        查询消息
                      </el-button>
                      <el-tooltip content="只浏览消息，不会消费或修改偏移量" placement="top">
                        <el-icon style="margin-left: 5px;"><InfoFilled /></el-icon>
                      </el-tooltip>
                    </el-form-item>
                  </el-form>
                </div>

                <div v-if="messages.length === 0" class="empty-messages">
                  <el-empty description="暂无消息" />
                  <p class="browse-tips">
                    提示：可以选择不同的起始偏移量查询不同时间段的消息，选择"最早消息"可以查看最早的历史记录。
                  </p>
                </div>

                <el-table v-else :data="messages" stripe>
                  <el-table-column prop="partition" label="分区" width="80" />
                  <el-table-column prop="offset" label="偏移量" width="120" />
                  <el-table-column prop="timestamp" label="时间戳" width="180">
                    <template #default="{ row }">
                      {{ formatDate(row.timestamp) }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="key" label="Key" width="150" show-overflow-tooltip />
                  <el-table-column prop="value" label="消息内容" show-overflow-tooltip />
                  <el-table-column label="操作" width="100">
                    <template #default="{ row }">
                      <el-button type="primary" size="small" @click="viewMessageDetails(row)">
                        <el-icon><View /></el-icon>
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>

                <div class="messages-pagination">
                  <el-pagination
                    v-model:current-page="messagesQuery.page"
                    :page-size="messagesQuery.maxMessages"
                    :total="totalMessages"
                    layout="total, prev, pager, next"
                    @current-change="handleMessagesPageChange"
                  />
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 创建主题对话框 -->
    <el-dialog v-model="createTopicDialogVisible" title="创建新主题" width="500px">
      <el-form :model="newTopicForm" :rules="topicFormRules" ref="topicFormRef" label-width="100px">
        <el-form-item label="主题名称" prop="name">
          <el-input v-model="newTopicForm.name" placeholder="请输入主题名称" />
        </el-form-item>
        <el-form-item label="分区数量" prop="partitions">
          <el-input-number v-model="newTopicForm.partitions" :min="1" :max="100" />
        </el-form-item>
        <el-form-item label="副本因子" prop="replicationFactor">
          <el-input-number v-model="newTopicForm.replicationFactor" :min="1" :max="5" />
        </el-form-item>
        <el-form-item label="配置项">
          <div v-for="(config, index) in newTopicForm.configs" :key="index" class="config-item">
            <el-input v-model="config.key" placeholder="配置键" class="config-key" />
            <el-input v-model="config.value" placeholder="配置值" class="config-value" />
            <el-button type="danger" @click="removeConfig(index)" circle plain>
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
          <el-button type="primary" @click="addConfig" plain>添加配置项</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="createTopicDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="createNewTopic">创建</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 发送消息对话框 -->
    <el-dialog v-model="sendMessageDialogVisible" title="发送消息" width="600px">
      <el-form :model="newMessage" :rules="messageFormRules" ref="messageFormRef" label-width="100px">
        <el-form-item label="分区" prop="partition">
          <el-select v-model="newMessage.partition" placeholder="选择分区">
            <el-option label="自动选择" :value="-1" />
            <el-option 
              v-for="partition in partitions" 
              :key="partition.partition" 
              :label="`分区 ${partition.partition}`" 
              :value="partition.partition" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="消息Key" prop="key">
          <el-input v-model="newMessage.key" placeholder="请输入消息Key" />
        </el-form-item>
        <el-form-item label="消息格式" prop="format">
          <el-radio-group v-model="newMessage.format">
            <el-radio label="text">文本</el-radio>
            <el-radio label="json">JSON</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="消息内容" prop="value">
          <el-input 
            v-if="newMessage.format === 'text'"
            v-model="newMessage.value" 
            type="textarea" 
            rows="8"
            placeholder="请输入消息内容" 
          />
          <div v-else style="width: 100%; height: 200px;">
            <json-editor v-model="newMessage.value" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="sendMessageDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="sendNewMessage">发送</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 消息详情对话框 -->
    <el-dialog v-model="messageDetailsDialogVisible" title="消息详情" width="800px">
      <div v-if="selectedMessage" class="message-details">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="主题">{{ selectedTopic?.name }}</el-descriptions-item>
          <el-descriptions-item label="分区">{{ selectedMessage.partition }}</el-descriptions-item>
          <el-descriptions-item label="偏移量">{{ selectedMessage.offset }}</el-descriptions-item>
          <el-descriptions-item label="时间戳">{{ formatDate(selectedMessage.timestamp) }}</el-descriptions-item>
          <el-descriptions-item label="Key">{{ selectedMessage.key || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Headers" :span="2">
            <pre v-if="selectedMessage.headers">{{ JSON.stringify(selectedMessage.headers, null, 2) }}</pre>
            <span v-else>-</span>
          </el-descriptions-item>
        </el-descriptions>
        
        <div class="message-value">
          <h3>消息内容</h3>
          <div v-if="isJsonMessage(selectedMessage.value)" class="json-content">
            <pre>{{ formatJson(selectedMessage.value) }}</pre>
          </div>
          <div v-else class="text-content">
            <pre>{{ selectedMessage.value }}</pre>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 消费者详情对话框 -->
    <el-dialog v-model="consumerDetailsDialogVisible" title="消费者组详情" width="800px">
      <div v-if="selectedConsumerGroup" class="consumer-details">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="消费者组ID">{{ selectedConsumerGroup.groupId }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusTagType(selectedConsumerGroup.status)">{{ selectedConsumerGroup.status || '未知' }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="成员数">{{ selectedConsumerGroup.members }}</el-descriptions-item>
          <el-descriptions-item label="消息积压量">
            <el-tag :type="getLagTagType(selectedConsumerGroup.lag)">{{ selectedConsumerGroup.lag }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="主题">{{ selectedConsumerGroup.offsets?.topic || selectedTopic?.name }}</el-descriptions-item>
        </el-descriptions>
        
        <h3>消费者成员</h3>
        <el-table :data="selectedConsumerGroup.memberDetails || []" stripe>
          <el-table-column prop="memberId" label="成员ID" width="280" show-overflow-tooltip />
          <el-table-column prop="clientId" label="客户端ID" width="200" show-overflow-tooltip />
          <el-table-column prop="host" label="主机" width="180" />
          <el-table-column prop="assignments" label="分区分配">
            <template #default="{ row }">
              <el-tag v-for="assignment in row.assignments" :key="assignment" class="assignment-tag">
                {{ assignment }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        
        <h3>分区偏移量</h3>
        <el-table :data="selectedConsumerGroup.offsetDetails || []" stripe>
          <el-table-column prop="partition" label="分区" width="100" />
          <el-table-column prop="currentOffset" label="当前偏移量" width="150" />
          <el-table-column prop="endOffset" label="结束偏移量" width="150" />
          <el-table-column prop="lag" label="积压量" width="120">
            <template #default="{ row }">
              <el-tag :type="getLagTagType(row.lag)">{{ row.lag }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="lastCommitTime" label="最后提交时间" show-overflow-tooltip>
            <template #default="{ row }">
              {{ formatDate(row.lastCommitTime) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  ArrowLeft, Search, Refresh, Plus, MessageBox, Upload, 
  Download, Delete, View, Close, InfoFilled
} from '@element-plus/icons-vue'
import JsonEditor from '@/components/JsonEditor' // 假设有这个组件

// API 导入
import { 
  getTopics,
  getTopicDetails,
  sendMessage,
  getConsumerGroups,
  consumeMessages,
  deleteTopic,
  createTopic,
  browseMessages,
  getTopicPartitions
} from '@/api/messagequeue'

const router = useRouter()
const route = useRoute()
const fullscreenLoading = ref(false)
const loadingText = ref('加载中...')

// 主题列表
const topics = ref([])
const searchText = ref('')
const filteredTopics = computed(() => {
  if (!searchText.value) return topics.value
  
  const lowercaseSearch = searchText.value.toLowerCase()
  return topics.value.filter(topic => 
    topic.name.toLowerCase().includes(lowercaseSearch)
  )
})

// 选中的主题
const activeTopic = ref('')
const selectedTopic = ref(null)
const activeTab = ref('overview')

// 主题详情数据
const topicConfigData = ref([])
const consumerGroups = ref([])
const partitions = ref([])
const messages = ref([])
const totalMessages = ref(0)

// 消息查询参数
const messagesQuery = ref({
  partition: -1,
  offset: -1, // -1 表示最新, -2 表示最早
  customOffset: 0, // 自定义偏移量
  maxMessages: 20,
  page: 1
})

// 创建主题表单
const topicFormRef = ref(null)
const createTopicDialogVisible = ref(false)
const newTopicForm = ref({
  name: '',
  partitions: 1,
  replicationFactor: 1,
  configs: []
})
const topicFormRules = {
  name: [
    { required: true, message: '请输入主题名称', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9._-]+$/, message: '主题名只能包含字母、数字、下划线、点和连字符', trigger: 'blur' }
  ],
  partitions: [
    { required: true, message: '请输入分区数量', trigger: 'blur' },
    { type: 'number', min: 1, message: '分区数量必须大于0', trigger: 'blur' }
  ],
  replicationFactor: [
    { required: true, message: '请输入副本因子', trigger: 'blur' },
    { type: 'number', min: 1, message: '副本因子必须大于0', trigger: 'blur' }
  ]
}

// 发送消息表单
const messageFormRef = ref(null)
const sendMessageDialogVisible = ref(false)
const newMessage = ref({
  partition: -1,
  key: '',
  value: '',
  format: 'text'
})
const messageFormRules = {
  value: [
    { required: true, message: '请输入消息内容', trigger: 'blur' }
  ]
}

// 消息详情
const messageDetailsDialogVisible = ref(false)
const selectedMessage = ref(null)

// 消费者详情
const consumerDetailsDialogVisible = ref(false)
const selectedConsumerGroup = ref(null)

// 处理搜索
const handleSearch = () => {
  // 实时过滤, 使用computed属性
}

// 加载主题列表
const loadTopics = async () => {
  try {
    fullscreenLoading.value = true
    loadingText.value = '正在加载主题列表...'
    
    const response = await getTopics(route.params.id)
    topics.value = response.data || []
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('加载主题列表失败:', error)
    ElMessage.error('加载主题列表失败')
    fullscreenLoading.value = false
  }
}

// 选择主题
const handleTopicSelect = async (topic) => {
  try {
    if (!topic) {
      selectedTopic.value = null
      messages.value = []
      return
    }
    
    selectedTopic.value = topic
    
    // 获取主题详情
    const detailsResponse = await getTopicDetails(route.params.id, topic)
    const topicData = detailsResponse.data
    
    console.log('主题详情数据:', topicData)
    
    // 更新主题详情数据
    selectedTopic.value = {...selectedTopic.value, ...topicData}
    
    // 提取配置信息并格式化为表格数据
    if (topicData.config) {
      topicConfigData.value = Object.entries(topicData.config).map(([key, value]) => ({
        key,
        value,
        description: getConfigDescription(key)
      }))
    }
    
    // 提取分区信息
    if (topicData.partitions) {
      // 获取分区详情信息
      try {
        const partitionsResponse = await getTopicPartitions(route.params.id, topic)
        partitions.value = partitionsResponse.data || []
      } catch (partitionError) {
        console.error('获取分区信息失败:', partitionError)
        // 如果获取详细分区信息失败，创建基本分区信息
        partitions.value = Array.from({ length: topicData.partitions }, (_, i) => ({ 
          partition: i, 
          leader: '未知', 
          replicas: [], 
          inSyncReplicas: []
        }))
      }
    } else {
      partitions.value = []
    }
    
    // 提取消费者组信息
    if (topicData.consumerGroups && topicData.consumerGroups.length > 0) {
      consumerGroups.value = topicData.consumerGroups.map(group => {
        const offsets = group.offsets || {}
        const totalLag = offsets.totalLag || 0
        
        return {
          groupId: group.groupId,
          members: group.members || '未知',
          lag: totalLag,
          status: group.status || 'Online',
          offsets: offsets
        }
      })
    } else {
      // 如果API返回中没有消费者组信息，可能需要单独请求
      try {
        const groupsResponse = await getConsumerGroups(route.params.id, topic)
        consumerGroups.value = groupsResponse.data || []
      } catch (groupError) {
        console.error('获取消费者组信息失败:', groupError)
        consumerGroups.value = []
      }
    }
    
  } catch (error) {
    console.error('选择主题失败:', error)
    ElMessage.error('选择主题失败')
  }
}

// 刷新主题列表
const refreshTopics = () => {
  loadTopics()
  if (selectedTopic.value) {
    handleTopicSelect(selectedTopic.value.name)
  }
}

// 显示创建主题对话框
const showCreateTopicDialog = () => {
  newTopicForm.value = {
    name: '',
    partitions: 1,
    replicationFactor: 1,
    configs: []
  }
  createTopicDialogVisible.value = true
}

// 添加配置项
const addConfig = () => {
  newTopicForm.value.configs.push({ key: '', value: '' })
}

// 移除配置项
const removeConfig = (index) => {
  newTopicForm.value.configs.splice(index, 1)
}

// 创建新主题
const createNewTopic = async () => {
  if (!topicFormRef.value) return
  
  try {
    await topicFormRef.value.validate()
    
    fullscreenLoading.value = true
    loadingText.value = '正在创建主题...'
    
    // 转换配置为对象
    const configs = {}
    for (const config of newTopicForm.value.configs) {
      if (config.key && config.value) {
        configs[config.key] = config.value
      }
    }
    
    const topicConfig = {
      name: newTopicForm.value.name,
      partitions: newTopicForm.value.partitions,
      replicationFactor: newTopicForm.value.replicationFactor,
      configs
    }
    
    await createTopic(route.params.id, topicConfig)
    
    ElMessage.success('创建主题成功')
    createTopicDialogVisible.value = false
    
    // 刷新主题列表
    await loadTopics()
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('创建主题失败:', error)
    ElMessage.error('创建主题失败')
    fullscreenLoading.value = false
  }
}

// 确认删除主题
const confirmDeleteTopic = async () => {
  if (!selectedTopic.value) return
  
  try {
    await ElMessageBox.confirm(
      `确认删除主题 "${selectedTopic.value.name}"？此操作不可恢复。`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    fullscreenLoading.value = true
    loadingText.value = '正在删除主题...'
    
    await deleteTopic(route.params.id, selectedTopic.value.name)
    
    ElMessage.success('删除主题成功')
    
    // 刷新主题列表
    await loadTopics()
    selectedTopic.value = null
    
    fullscreenLoading.value = false
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除主题失败:', error)
      ElMessage.error('删除主题失败')
    }
    fullscreenLoading.value = false
  }
}

// 显示发送消息对话框
const showSendMessageDialog = () => {
  newMessage.value = {
    partition: -1,
    key: '',
    value: '',
    format: 'text'
  }
  sendMessageDialogVisible.value = true
}

// 发送新消息
const sendNewMessage = async () => {
  if (!messageFormRef.value) return
  
  try {
    await messageFormRef.value.validate()
    
    fullscreenLoading.value = true
    loadingText.value = '正在发送消息...'
    
    const messageData = {
      partition: newMessage.value.partition,
      key: newMessage.value.key,
      value: newMessage.value.value
    }
    
    await sendMessage(route.params.id, selectedTopic.value.name, messageData)
    
    ElMessage.success('发送消息成功')
    sendMessageDialogVisible.value = false
    
    // 如果当前在消息标签页，刷新消息列表
    if (activeTab.value === 'messages') {
      fetchMessages()
    }
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('发送消息失败:', error)
    ElMessage.error('发送消息失败')
    fullscreenLoading.value = false
  }
}

// 消费主题消息
const consumeTopicMessages = async () => {
  if (!selectedTopic.value) return
  
  try {
    fullscreenLoading.value = true
    loadingText.value = '正在消费消息...'
    
    // 切换到消息标签页
    activeTab.value = 'messages'
    
    // 设置查询参数并获取消息
    messagesQuery.value = {
      partition: -1,
      offset: -1,
      maxMessages: 20,
      page: 1
    }
    
    await fetchMessages()
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('消费消息失败:', error)
    ElMessage.error('消费消息失败')
    fullscreenLoading.value = false
  }
}

// 获取消息
const fetchMessages = async () => {
  if (!selectedTopic.value) return
  
  try {
    fullscreenLoading.value = true
    loadingText.value = '正在获取消息...'
    
    // 处理自定义偏移量
    const queryOffset = messagesQuery.value.offset >= 0 ? 
      messagesQuery.value.customOffset : messagesQuery.value.offset
    
    const response = await browseMessages(
      route.params.id, 
      selectedTopic.value.name, 
      {
        partition: messagesQuery.value.partition,
        offset: queryOffset,
        maxMessages: messagesQuery.value.maxMessages,
        page: messagesQuery.value.page
      }
    )
    
    messages.value = response.data.messages || []
    totalMessages.value = response.data.total || 0
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('获取消息失败:', error)
    ElMessage.error('获取消息失败')
    fullscreenLoading.value = false
  }
}

// 消息分页变化
const handleMessagesPageChange = (page) => {
  messagesQuery.value.page = page
  fetchMessages()
}

// 查看消息详情
const viewMessageDetails = (message) => {
  selectedMessage.value = message
  messageDetailsDialogVisible.value = true
}

// 查看消费者详情
const viewConsumerDetails = async (consumerGroup) => {
  selectedConsumerGroup.value = consumerGroup
  
  // 转换偏移量数据为表格格式
  if (consumerGroup.offsets && consumerGroup.offsets.partitions) {
    const offsetDetails = []
    for (const [partition, data] of Object.entries(consumerGroup.offsets.partitions)) {
      offsetDetails.push({
        partition: data.partition,
        currentOffset: data.consumerOffset,
        endOffset: data.endOffset,
        lag: data.lag,
        lastCommitTime: new Date().getTime() // 这里可能需要从API获取
      })
    }
    selectedConsumerGroup.value.offsetDetails = offsetDetails
  } else {
    selectedConsumerGroup.value.offsetDetails = []
  }
  
  // 这里可以加载更详细的消费者组成员信息
  try {
    // 添加一些默认的成员信息，实际中应该从API获取
    selectedConsumerGroup.value.memberDetails = [{
      memberId: `${consumerGroup.groupId}-member-1`,
      clientId: 'consumer-client-1',
      host: '192.168.1.100',
      assignments: ['0']
    }]
  } catch (error) {
    console.error('加载消费者组成员信息失败:', error)
    selectedConsumerGroup.value.memberDetails = []
  }
  
  consumerDetailsDialogVisible.value = true
}

// 删除消费者组
const deleteConsumerGroup = async (consumerGroup) => {
  // 实现删除消费者组的逻辑
}

// 格式化日期
const formatDate = (timestamp) => {
  if (!timestamp) return '-'
  
  const date = new Date(timestamp)
  return date.toLocaleString()
}

// 检查是否为JSON消息
const isJsonMessage = (value) => {
  if (!value) return false
  
  try {
    JSON.parse(value)
    return true
  } catch (error) {
    return false
  }
}

// 格式化JSON
const formatJson = (value) => {
  if (!value) return ''
  
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value
    return JSON.stringify(parsed, null, 2)
  } catch (error) {
    return value
  }
}

// 获取配置项描述
const getConfigDescription = (key) => {
  const descriptions = {
    'cleanup.policy': '数据清理策略',
    'compression.type': '消息压缩类型',
    'delete.retention.ms': '已删除消息的保留时间',
    'file.delete.delay.ms': '文件删除延迟时间',
    'flush.messages': '消息刷盘数量阈值',
    'flush.ms': '消息刷盘时间阈值',
    'min.insync.replicas': '最小同步副本数',
    'retention.bytes': '分区大小保留阈值',
    'retention.ms': '消息保留时间',
    'segment.bytes': '日志分段大小',
    'segment.ms': '日志分段时间'
  }
  
  return descriptions[key] || '配置项'
}

// 获取分区数量标签类型
const getPartitionTagType = (partitions) => {
  if (partitions <= 1) return 'info'
  if (partitions <= 3) return 'success'
  if (partitions <= 10) return 'warning'
  return 'danger'
}

// 获取积压量标签类型
const getLagTagType = (lag) => {
  if (lag === 0) return 'success'
  if (lag < 100) return 'info'
  if (lag < 1000) return 'warning'
  return 'danger'
}

// 获取状态标签类型
const getStatusTagType = (status) => {
  if (status === 'Stable' || status === 'Online') return 'success'
  if (status === 'Preparing' || status === 'Connecting') return 'info'
  if (status === 'Rebalancing') return 'warning'
  return 'danger'
}

// 初始化
onMounted(() => {
  loadTopics()
})

// 监听标签页变化
watch(activeTab, (newTab) => {
  if (newTab === 'messages' && selectedTopic.value && messages.value.length === 0) {
    fetchMessages()
  }
})
</script>

<style scoped>
.message-queue-browser {
  height: calc(100vh - 140px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.datasource-info {
  font-weight: bold;
  margin-right: 20px;
}

.content-wrapper {
  display: flex;
  height: calc(100vh - 200px);
  overflow: hidden;
}

.left-area {
  width: 280px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #ebeef5;
  padding-right: 16px;
}

.search-area {
  margin-bottom: 16px;
}

.topics-list {
  flex: 1;
  overflow: auto;
}

.topics-menu {
  border-right: none;
}

.topic-title {
  margin-right: 8px;
}

.main-content {
  flex: 1;
  padding: 0 16px;
  overflow: auto;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.topic-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.topic-actions {
  display: flex;
  gap: 8px;
}

.topic-tabs {
  margin-top: 20px;
}

.replica-tag {
  margin-right: 4px;
}

.assignment-tag {
  margin-right: 4px;
  margin-bottom: 4px;
}

.messages-filter {
  margin-bottom: 16px;
}

.messages-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.message-details {
  max-height: 70vh;
  overflow: auto;
}

.message-value {
  margin-top: 20px;
}

.json-content, .text-content {
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  max-height: 400px;
  overflow: auto;
}

.json-content pre, .text-content pre {
  font-family: monospace;
  white-space: pre-wrap;
  margin: 0;
}

.consumer-details {
  max-height: 70vh;
  overflow: auto;
}

.config-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.config-key {
  width: 200px;
}

.config-value {
  flex: 1;
}

.empty-messages {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.browse-tips {
  margin-top: 10px;
  color: #909399;
  font-size: 0.8em;
}
</style> 