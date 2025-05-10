<!-- 机构管理页面 -->
<template>
  <div class="organization-management">
    <el-row :gutter="20">
      <!-- 左侧机构树 -->
      <el-col :span="6">
        <el-card class="tree-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>机构列表</span>
              <div class="header-operations">
                <el-button type="primary" size="small" @click="handleAddRoot">
                  <el-icon><Plus /></el-icon>添加一级机构
                </el-button>
                <el-button size="small" @click="refreshTree">
                  <el-icon><Refresh /></el-icon>刷新
                </el-button>
              </div>
            </div>
          </template>
          <div class="filter-container">
            <el-input
              v-model="filterText"
              placeholder="输入关键字进行过滤"
              clearable
              prefix-icon="Search"
            />
          </div>
          <div class="tree-container">
            <el-tree
              ref="orgTree"
              :data="treeData"
              node-key="id"
              :props="defaultProps"
              :filter-node-method="filterNode"
              :highlight-current="true"
              :expand-on-click-node="false"
              default-expand-all
              @node-click="handleNodeClick"
            >
              <template #default="{ node, data }">
                <div class="custom-tree-node">
                  <span class="label">{{ node.label }}</span>
                  <div class="operations">
                    <el-button type="primary" link @click.stop="handleAddChild(data)">
                      <el-icon><Plus /></el-icon>
                    </el-button>
                    <el-button type="primary" link @click.stop="handleEdit(data)">
                      <el-icon><Edit /></el-icon>
                    </el-button>
                    <el-button type="danger" link @click.stop="handleDelete(data)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
              </template>
            </el-tree>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧详情 -->
      <el-col :span="18">
        <template v-if="currentNode">
          <el-card class="detail-card" shadow="hover">
            <template #header>
              <div class="card-header">
                <span>{{ currentNode.name }} - 详细信息</span>
              </div>
            </template>

            <el-descriptions :column="2" border>
              <el-descriptions-item label="机构编码">{{ currentNode.code }}</el-descriptions-item>
              <el-descriptions-item label="机构名称">{{ currentNode.name }}</el-descriptions-item>
              <el-descriptions-item label="机构类型">{{ getOrgTypeName(currentNode.type) }}</el-descriptions-item>
              <el-descriptions-item label="排序">{{ currentNode.sort }}</el-descriptions-item>
              <el-descriptions-item label="状态">
                <el-tag :type="currentNode.status === '1' ? 'success' : 'danger'">
                  {{ currentNode.status === '1' ? '启用' : '禁用' }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ currentNode.createTime }}</el-descriptions-item>
              <el-descriptions-item label="负责人">{{ currentNode.leader }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ currentNode.phone }}</el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ currentNode.remark }}</el-descriptions-item>
            </el-descriptions>
            
            <div class="detail-operations">
              <el-divider content-position="left">操作</el-divider>
              <el-button type="primary" @click="handleEdit(currentNode)">
                <el-icon><Edit /></el-icon>编辑机构
              </el-button>
              <el-button @click="handleAddChild(currentNode)">
                <el-icon><Plus /></el-icon>添加下级机构
              </el-button>
              <el-button 
                :type="currentNode.status === '1' ? 'warning' : 'success'"
                @click="handleChangeStatus(currentNode)"
              >
                <template v-if="currentNode.status !== '1'">
                  <el-icon><CircleCheck /></el-icon>
                </template>
                <template v-else>
                  <el-icon><CircleClose /></el-icon>
                </template>
                {{ currentNode.status === '1' ? '禁用' : '启用' }}
              </el-button>
              <el-button type="danger" @click="handleDelete(currentNode)">
                <el-icon><Delete /></el-icon>删除机构
              </el-button>
              <el-button @click="handleMoveUp(currentNode)" :disabled="!canMoveUp">
                <el-icon><Top /></el-icon>上移
              </el-button>
              <el-button @click="handleMoveDown(currentNode)" :disabled="!canMoveDown">
                <el-icon><Bottom /></el-icon>下移
              </el-button>
            </div>
            
            <!-- 子机构列表 -->
            <div class="children-list" v-if="currentNode.children && currentNode.children.length > 0">
              <el-divider content-position="left">下级机构</el-divider>
              <el-table :data="currentNode.children" border stripe style="width: 100%">
                <el-table-column prop="code" label="机构编码" width="120" />
                <el-table-column prop="name" label="机构名称" />
                <el-table-column prop="type" label="机构类型" width="120">
                  <template #default="scope">
                    {{ getOrgTypeName(scope.row.type) }}
                  </template>
                </el-table-column>
                <el-table-column prop="leader" label="负责人" width="100" />
                <el-table-column prop="phone" label="联系电话" width="120" />
                <el-table-column prop="status" label="状态" width="80">
                  <template #default="scope">
                    <el-tag :type="scope.row.status === '1' ? 'success' : 'danger'">
                      {{ scope.row.status === '1' ? '启用' : '禁用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="200" fixed="right">
                  <template #default="scope">
                    <el-button size="small" @click="handleNodeClick(scope.row)">查看</el-button>
                    <el-button size="small" type="primary" @click="handleEdit(scope.row)">编辑</el-button>
                    <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-card>
        </template>
        <el-empty v-else description="请选择机构查看详情" />
      </el-col>
    </el-row>

    <!-- 机构表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="(dialogType === 'add' ? '新增' : '编辑') + (parentNode ? '下级' : '') + '机构'"
      width="550px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="orgFormRef"
        :model="orgForm"
        :rules="orgRules"
        label-width="100px"
      >
        <el-form-item label="上级机构">
          <el-input v-model="parentNodeName" disabled placeholder="无" />
        </el-form-item>
        <el-form-item label="机构名称" prop="name">
          <el-input v-model="orgForm.name" placeholder="请输入机构名称" />
        </el-form-item>
        <el-form-item label="机构编码" prop="code">
          <el-input v-model="orgForm.code" placeholder="请输入机构编码" />
        </el-form-item>
        <el-form-item label="机构类型" prop="type">
          <el-select v-model="orgForm.type" placeholder="请选择机构类型" style="width: 100%">
            <el-option v-for="item in orgTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="机构负责人">
          <el-select v-model="orgForm.leader" filterable placeholder="请选择负责人" style="width: 100%">
            <el-option
              v-for="item in leaderOptions"
              :key="item.id"
              :label="item.realName || item.username"
              :value="item.realName"
            >
              <div style="display: flex; align-items: center;">
                <span>{{ item.realName || item.username }}</span>
                <span style="font-size: 12px; color: #999; margin-left: 10px;">{{ item.email || item.mobile }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="orgForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="orgForm.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="orgForm.status" :active-value="'1'" :inactive-value="'0'" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="orgForm.remark" type="textarea" rows="3" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, nextTick } from 'vue'
import { 
  Plus, Edit, Delete, Refresh, Search, CircleCheck, CircleClose, Top, Bottom
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { getUserList } from '@/api/system/user'

// 机构类型选项
const orgTypeOptions = [
  { value: '1', label: '公司' },
  { value: '2', label: '部门' },
  { value: '3', label: '小组' },
  { value: '4', label: '其他' }
]

// 获取机构类型名称
const getOrgTypeName = (type) => {
  const option = orgTypeOptions.find(item => item.value === type)
  return option ? option.label : '未知类型'
}

// 树形数据
const treeData = ref([])
const currentNode = ref(null)
const defaultProps = {
  children: 'children',
  label: 'name'
}

// 树节点过滤
const filterText = ref('')
const orgTree = ref(null)

watch(filterText, (val) => {
  orgTree.value?.filter(val)
})

const filterNode = (value, data) => {
  if (!value) return true
  return data.name.includes(value) || data.code.includes(value)
}

// 表单相关
const dialogVisible = ref(false)
const dialogType = ref('add') // 'add' 或 'edit'
const parentNode = ref(null)
const parentNodeName = computed(() => parentNode.value ? parentNode.value.name : '无')
const orgFormRef = ref(null)
const orgForm = ref({
  id: '',
  parentId: '',
  name: '',
  code: '',
  type: '2',
  leader: '',
  phone: '',
  sort: 0,
  status: '1',
  remark: ''
})

// 监听负责人变化，自动填充电话
watch(() => orgForm.value.leader, (newValue) => {
  if (newValue && leaderOptions.value.length > 0) {
    const selectedUser = leaderOptions.value.find(user => user.realName === newValue)
    if (selectedUser) {
      orgForm.value.phone = selectedUser.mobile || ''
    }
  }
})

// 表单验证规则
const orgRules = {
  name: [
    { required: true, message: '请输入机构名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入机构编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择机构类型', trigger: 'change' }
  ],
  sort: [
    { required: true, message: '请输入排序值', trigger: 'blur' }
  ]
}

// 机构上下移动
const canMoveUp = computed(() => {
  if (!currentNode.value || !currentNode.value.parentId) return false
  const parent = findNodeById(treeData.value, currentNode.value.parentId)
  if (!parent || !parent.children) return false
  const index = parent.children.findIndex(item => item.id === currentNode.value.id)
  return index > 0
})

const canMoveDown = computed(() => {
  if (!currentNode.value || !currentNode.value.parentId) return false
  const parent = findNodeById(treeData.value, currentNode.value.parentId)
  if (!parent || !parent.children) return false
  const index = parent.children.findIndex(item => item.id === currentNode.value.id)
  return index < parent.children.length - 1
})

// 用户列表选项
const leaderOptions = ref([])

// 获取用户列表
const fetchUserList = async () => {
  try {
    const params = {
      pageNum: 1,
      pageSize: 100,
      status: '1' // 只获取启用状态的用户
    }
    const res = await getUserList(params)
    if (res && res.data) {
      leaderOptions.value = res.data.records || []
      console.log('用户列表数据:', leaderOptions.value)
    } else {
      console.error('获取用户列表失败')
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

// 初始化方法
onMounted(() => {
  fetchOrganizationTree()
  fetchUserList() // 获取用户列表
})

// 获取机构树数据
const fetchOrganizationTree = async () => {
  try {
    // 调用真实的API接口
    const response = await axios.get('/api/system/organizations/tree')
    if (response.data && response.data.code === 0) {
      treeData.value = response.data.data || []
      // 默认选择第一级组织
      if (treeData.value.length > 0) {
        handleNodeClick(treeData.value[0])
        // 高亮第一个节点
        nextTick(() => {
          if (orgTree.value) {
            orgTree.value.setCurrentKey(treeData.value[0].id)
          }
        })
      }
    } else {
      ElMessage.error(response.data?.message || '获取机构树失败')
    }
  } catch (error) {
    console.error('获取机构树失败:', error)
    ElMessage.error('获取机构树失败，请稍后重试')
  }
}

// 刷新树
const refreshTree = () => {
  fetchOrganizationTree()
}

// 处理树节点点击
const handleNodeClick = (data) => {
  currentNode.value = data
}

// 通过ID查找节点
const findNodeById = (nodes, id) => {
  for (const node of nodes) {
    if (node.id === id) {
      return node
    }
    if (node.children && node.children.length > 0) {
      const found = findNodeById(node.children, id)
      if (found) return found
    }
  }
  return null
}

// 处理添加根节点
const handleAddRoot = () => {
  dialogType.value = 'add'
  parentNode.value = null
  orgForm.value = {
    id: '',
    parentId: '0',
    name: '',
    code: '',
    type: '1', // 默认为公司类型
    leader: '',
    phone: '',
    sort: 0,
    status: '1',
    remark: ''
  }
  dialogVisible.value = true
  // 确保用户列表已加载
  if (leaderOptions.value.length === 0) {
    fetchUserList()
  }
}

// 处理添加子节点
const handleAddChild = (data) => {
  dialogType.value = 'add'
  parentNode.value = data
  orgForm.value = {
    id: '',
    parentId: data.id,
    name: '',
    code: '',
    type: '2', // 默认为部门类型
    leader: '',
    phone: '',
    sort: 0,
    status: '1',
    remark: ''
  }
  dialogVisible.value = true
  // 确保用户列表已加载
  if (leaderOptions.value.length === 0) {
    fetchUserList()
  }
}

// 处理编辑节点
const handleEdit = (data) => {
  dialogType.value = 'edit'
  
  // 设置父节点
  if (data.parentId === '0') {
    parentNode.value = null
  } else {
    parentNode.value = findNodeById(treeData.value, data.parentId)
  }
  
  // 设置表单数据
  orgForm.value = {
    id: data.id,
    parentId: data.parentId,
    name: data.name,
    code: data.code,
    type: data.type,
    leader: data.leader,
    phone: data.phone,
    sort: data.sort,
    status: data.status,
    remark: data.remark
  }
  
  dialogVisible.value = true
  // 确保用户列表已加载
  if (leaderOptions.value.length === 0) {
    fetchUserList()
  }
}

// 处理删除节点
const handleDelete = (data) => {
  if (data.children && data.children.length > 0) {
    ElMessage.warning('该机构下存在子机构，无法删除')
    return
  }
  
  ElMessageBox.confirm(`确定要删除机构 ${data.name} 吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      // 调用真实的删除API
      const response = await axios.delete(`/api/system/organizations/${data.id}`)
      if (response.data && response.data.code === 0) {
        ElMessage.success('删除成功')
        // 如果是当前选中的节点，则清空选中
        if (currentNode.value && currentNode.value.id === data.id) {
          currentNode.value = null
        }
        fetchOrganizationTree()
      } else {
        ElMessage.error(response.data?.message || '删除失败')
      }
    } catch (error) {
      console.error('删除机构失败:', error)
      ElMessage.error('删除机构失败，请稍后重试')
    }
  }).catch(() => {
    // 用户取消删除操作
  })
}

// 处理更改状态
const handleChangeStatus = async (data) => {
  const newStatus = data.status === '1' ? '0' : '1'
  const statusText = newStatus === '1' ? '启用' : '禁用'
  
  try {
    // 调用真实的状态更新API
    const response = await axios.put(`/api/system/organizations/status/${data.id}?status=${newStatus}`)
    if (response.data && response.data.code === 0) {
      ElMessage.success(`${statusText}成功`)
      data.status = newStatus
    } else {
      ElMessage.error(response.data?.message || `${statusText}失败`)
    }
  } catch (error) {
    console.error(`${statusText}机构失败:`, error)
    ElMessage.error(`${statusText}机构失败，请稍后重试`)
  }
}

// 处理上移
const handleMoveUp = async (data) => {
  if (!canMoveUp.value) return
  
  try {
    // 调用真实的上移API
    const response = await axios.put(`/api/system/organizations/move-up/${data.id}`)
    if (response.data && response.data.code === 0) {
      ElMessage.success('上移成功')
      fetchOrganizationTree()
    } else {
      ElMessage.error(response.data?.message || '上移失败')
    }
  } catch (error) {
    console.error('上移机构失败:', error)
    ElMessage.error('上移机构失败，请稍后重试')
  }
}

// 处理下移
const handleMoveDown = async (data) => {
  if (!canMoveDown.value) return
  
  try {
    // 调用真实的下移API
    const response = await axios.put(`/api/system/organizations/move-down/${data.id}`)
    if (response.data && response.data.code === 0) {
      ElMessage.success('下移成功')
      fetchOrganizationTree()
    } else {
      ElMessage.error(response.data?.message || '下移失败')
    }
  } catch (error) {
    console.error('下移机构失败:', error)
    ElMessage.error('下移机构失败，请稍后重试')
  }
}

// 提交表单
const submitForm = async () => {
  if (!orgFormRef.value) return
  
  await orgFormRef.value.validate(async (valid, fields) => {
    if (valid) {
      try {
        const formData = { ...orgForm.value }
        
        // 调用真实的API
        const method = dialogType.value === 'add' ? 'post' : 'put'
        const url = '/api/system/organizations'
        
        const response = await axios({ method, url, data: formData })
        
        if (response.data && response.data.code === 0) {
          ElMessage.success(dialogType.value === 'add' ? '添加成功' : '更新成功')
          dialogVisible.value = false
          
          // 刷新树形数据
          await fetchOrganizationTree()
          
          // 如果是编辑，则更新当前选中节点
          if (dialogType.value === 'edit' && currentNode.value && currentNode.value.id === formData.id) {
            nextTick(() => {
              currentNode.value = findNodeById(treeData.value, formData.id)
            })
          }
        } else {
          ElMessage.error(response.data?.message || (dialogType.value === 'add' ? '添加失败' : '更新失败'))
        }
      } catch (error) {
        console.error(dialogType.value === 'add' ? '添加机构失败:' : '更新机构失败:', error)
        ElMessage.error(dialogType.value === 'add' ? '添加机构失败，请稍后重试' : '更新机构失败，请稍后重试')
      }
    } else {
      console.log('表单验证失败:', fields)
    }
  })
}
</script>

<style scoped>
.organization-management {
  padding: 16px;
}

.tree-card {
  height: calc(100vh - 120px);
  overflow: auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-operations {
  display: flex;
  gap: 8px;
}

.filter-container {
  margin-bottom: 16px;
}

.tree-container {
  overflow: auto;
  max-height: calc(100vh - 240px);
}

.detail-card {
  min-height: 300px;
}

.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}

.operations {
  display: none;
}

.custom-tree-node:hover .operations {
  display: flex;
}

.detail-operations {
  margin-top: 20px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.children-list {
  margin-top: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style> 