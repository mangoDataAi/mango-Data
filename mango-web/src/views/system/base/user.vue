<!-- 用户管理页面 -->
<template>
  <div class="user-management">
    <!-- 搜索区域 -->
    <el-card class="search-card">
      <el-form :model="searchForm" label-width="80px" :inline="true">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="searchForm.realName" placeholder="请输入姓名" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.mobile" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="searchForm.email" placeholder="请输入邮箱" clearable />
        </el-form-item>
        <el-form-item label="所属机构">
          <el-select v-model="searchForm.organizationId" placeholder="请选择" clearable>
            <el-option v-for="item in orgOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable>
            <el-option label="启用" value="1" />
            <el-option label="禁用" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作和表格区域 -->
    <el-card class="table-card">
      <div class="toolbar">
        <div class="left">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>新增用户
          </el-button>
          <el-button type="danger" :disabled="!selectedRows.length" @click="handleBatchDelete">
            <el-icon><Delete /></el-icon>批量删除
          </el-button>
          <el-button :disabled="!selectedRows.length" @click="handleBatchEnable(true)">
            <el-icon><Check /></el-icon>批量启用
          </el-button>
          <el-button :disabled="!selectedRows.length" @click="handleBatchEnable(false)">
            <el-icon><Close /></el-icon>批量禁用
          </el-button>
        </div>
        <div class="right">
          <el-button @click="refreshTable">
            <el-icon><Refresh /></el-icon>刷新
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        @selection-change="handleSelectionChange"
        style="width: 100%"
        border
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="gender" label="性别" width="80">
          <template #default="scope">
            {{ scope.row.gender === '1' ? '男' : scope.row.gender === '2' ? '女' : '未知' }}
          </template>
        </el-table-column>
        <el-table-column prop="mobile" label="手机号" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="organizationName" label="所属机构" />
        <el-table-column prop="roleNames" label="角色" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="scope">
            <el-tag :type="scope.row.status === '1' ? 'success' : 'danger'">
              {{ scope.row.status === '1' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
            <el-button 
              size="small" 
              :type="scope.row.status === '1' ? 'warning' : 'success'"
              @click="handleStatusChange(scope.row)"
            >
              {{ scope.row.status === '1' ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" @click="handleResetPassword(scope.row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 用户表单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增用户' : '编辑用户'"
      width="550px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="userFormRef"
        :model="userForm"
        :rules="userRules"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input 
            v-model="userForm.username" 
            placeholder="请输入用户名" 
            :disabled="dialogType === 'edit'" 
          />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="userForm.gender">
            <el-radio label="1">男</el-radio>
            <el-radio label="2">女</el-radio>
            <el-radio label="0">未知</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号" prop="mobile">
          <el-input v-model="userForm.mobile" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="所属机构" prop="organizationId">
          <el-cascader
            v-model="userForm.organizationId"
            :options="orgCascaderOptions"
            :props="{ checkStrictly: true, value: 'id', label: 'name' }"
            placeholder="请选择所属机构"
            clearable
          />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="userForm.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="dialogType === 'add'">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword" v-if="dialogType === 'add'">
          <el-input v-model="userForm.confirmPassword" type="password" placeholder="请确认密码" show-password />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="userForm.status" :active-value="'1'" :inactive-value="'0'" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="userForm.remark" type="textarea" rows="3" placeholder="请输入备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog
      v-model="resetPwdDialogVisible"
      title="重置密码"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="resetPwdFormRef"
        :model="resetPwdForm"
        :rules="resetPwdRules"
        label-width="100px"
      >
        <el-form-item label="新密码" prop="newPassword">
          <el-input 
            v-model="resetPwdForm.newPassword" 
            type="password" 
            placeholder="请输入新密码" 
            show-password 
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input 
            v-model="resetPwdForm.confirmPassword" 
            type="password" 
            placeholder="请确认新密码" 
            show-password 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="resetPwdDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitResetPwd">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Plus, Delete, Check, Close, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

// MD5加密函数实现
async function md5(message) {
  // 使用TextEncoder将字符串转换为Uint8Array
  const msgUint8 = new TextEncoder().encode(message);
  // 使用SubtleCrypto API计算哈希
  const hashBuffer = await crypto.subtle.digest('SHA-256', msgUint8);
  // 转换为十六进制字符串
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
  return hashHex;
}

// 搜索表单数据
const searchForm = ref({
  username: '',
  realName: '',
  mobile: '',
  email: '',
  organizationId: '',
  status: ''
})

// 表格数据
const tableData = ref([])
const loading = ref(false)
const selectedRows = ref([])

// 分页数据
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0
})

// 对话框相关数据
const dialogVisible = ref(false)
const dialogType = ref('add') // 'add' 或 'edit'
const userFormRef = ref(null)
const userForm = ref({
  id: '',
  username: '',
  realName: '',
  gender: '1',
  mobile: '',
  email: '',
  organizationId: '',
  roleIds: [],
  password: '',
  confirmPassword: '',
  status: '1',
  remark: ''
})

// 重置密码对话框相关数据
const resetPwdDialogVisible = ref(false)
const resetPwdFormRef = ref(null)
const resetPwdForm = ref({
  userId: '',
  newPassword: '',
  confirmPassword: ''
})
const currentResetUser = ref(null)

// 表单验证规则
const validatePassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请输入密码'))
  } else if (value.length < 6) {
    callback(new Error('密码不能少于6个字符'))
  } else {
    if (userForm.value.confirmPassword !== '') {
      if (!userFormRef.value) return
      userFormRef.value.validateField('confirmPassword', () => null)
    }
    callback()
  }
}

const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== userForm.value.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const validateResetConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== resetPwdForm.value.newPassword) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const userRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  mobile: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  organizationId: [
    { required: true, message: '请选择所属机构', trigger: 'change' }
  ],
  roleIds: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  password: [
    { validator: validatePassword, trigger: 'blur' },
    { required: true, message: '请输入密码', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' },
    { required: true, message: '请确认密码', trigger: 'blur' }
  ]
}

const resetPwdRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码不能少于6个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateResetConfirmPassword, trigger: 'blur' },
    { required: true, message: '请确认新密码', trigger: 'blur' }
  ]
}

// 机构和角色选项
const orgOptions = ref([])
const orgCascaderOptions = ref([])
const roleOptions = ref([])

// 初始化方法
onMounted(() => {
  fetchOrganizations()
  fetchRoles()
  fetchUserList()
})

// 获取机构列表
const fetchOrganizations = async () => {
  try {
    // 调用正确的API路径获取机构列表
    const response = await axios.get('/api/system/organizations/tree')
    if (response.data && response.data.code === 0) {
      const organizations = response.data.data || []
      
      // 处理列表选择器选项
      orgOptions.value = organizations.map(org => ({
        value: org.id,
        label: org.name
      }))
      
      // 处理级联选择器选项
      orgCascaderOptions.value = buildOrgTree(organizations)
    } else {
      ElMessage.error(response.data?.message || '获取机构列表失败')
    }
  } catch (error) {
    console.error('获取机构列表失败:', error)
    ElMessage.error('获取机构列表失败，请稍后重试')
  }
}

// 构建机构树
const buildOrgTree = (organizations) => {
  const map = {}
  const roots = []
  
  // 先创建映射表
  organizations.forEach(org => {
    // 确保每个节点都有children属性，如果原数据中children为null，初始化为空数组
    map[org.id] = { 
      ...org, 
      children: org.children || [] 
    }
  })
  
  // 递归处理children
  const processChildren = (node) => {
    if (node.children && node.children.length > 0) {
      node.children = node.children.map(child => {
        const childNode = { ...child, children: child.children || [] }
        return processChildren(childNode)
      })
    }
    return node
  }
  
  // 构建树结构
  organizations.forEach(org => {
    // 添加到父节点的children中
    if (org.parentId && org.parentId !== '0' && map[org.parentId]) {
      // 已通过映射表处理
    } else {
      // 这是顶级节点
      const rootNode = processChildren(map[org.id])
      roots.push(rootNode)
    }
  })
  
  return roots
}

// 获取角色列表
const fetchRoles = async () => {
  try {
    // 调用角色列表API
    const response = await axios.get('/api/system/roles')
    if (response.data && response.data.code === 0) {
      const roles = response.data.data || []
      roleOptions.value = roles.map(role => ({
        value: role.id,
        label: role.name
      }))
    } else {
      ElMessage.error(response.data?.message || '获取角色列表失败')
    }
  } catch (error) {
    console.error('获取角色列表失败:', error)
    ElMessage.error('获取角色列表失败，请稍后重试')
  }
}

// 获取用户列表
const fetchUserList = async () => {
  loading.value = true
  try {
    // 构建查询参数
    const params = {
      ...searchForm.value,
      current: pagination.current,
      size: pagination.size
    }
    
    // 调用用户分页查询API
    const response = await axios.post('/api/system/users/page', params)
    if (response.data && response.data.code === 0) {
      const pageResult = response.data.data
      tableData.value = pageResult.records || []
      pagination.total = pageResult.total || 0
    } else {
      ElMessage.error(response.data?.message || '获取用户列表失败')
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 处理搜索
const handleSearch = () => {
  pagination.current = 1
  fetchUserList()
}

// 重置搜索
const resetSearch = () => {
  searchForm.value = {
    username: '',
    realName: '',
    mobile: '',
    email: '',
    organizationId: '',
    status: ''
  }
  handleSearch()
}

// 刷新表格
const refreshTable = () => {
  fetchUserList()
}

// 处理表格选择变化
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

// 处理分页大小变化
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.current = 1
  fetchUserList()
}

// 处理当前页变化
const handleCurrentChange = (current) => {
  pagination.current = current
  fetchUserList()
}

// 处理新增用户
const handleAdd = () => {
  dialogType.value = 'add'
  userForm.value = {
    id: '',
    username: '',
    realName: '',
    gender: '1',
    mobile: '',
    email: '',
    organizationId: '',
    roleIds: [],
    password: '',
    confirmPassword: '',
    status: '1',
    remark: ''
  }
  dialogVisible.value = true
}

// 处理编辑用户
const handleEdit = (row) => {
  dialogType.value = 'edit'
  userForm.value = {
    id: row.id,
    username: row.username,
    realName: row.realName,
    gender: row.gender,
    mobile: row.mobile,
    email: row.email,
    organizationId: row.organizationId,
    roleIds: Array.isArray(row.roleIds) ? row.roleIds : (row.roleIds ? [row.roleIds] : []),
    status: row.status,
    remark: row.remark
  }
  dialogVisible.value = true
}

// 处理删除用户
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除用户 ${row.username} 吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      // 调用删除用户API
      const response = await axios.delete(`/api/system/users/${row.id}`)
      if (response.data && response.data.code === 0) {
        ElMessage.success('删除成功')
        fetchUserList()
      } else {
        ElMessage.error(response.data?.message || '删除失败')
      }
    } catch (error) {
      console.error('删除用户失败:', error)
      ElMessage.error('删除用户失败，请稍后重试')
    }
  }).catch(() => {
    // 用户取消删除操作
  })
}

// 处理批量删除
const handleBatchDelete = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请至少选择一条记录')
    return
  }
  
  ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 条记录吗?`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      const ids = selectedRows.value.map(row => row.id)
      // 调用批量删除API
      const response = await axios.post('/api/system/users/batch-delete', ids)
      if (response.data && response.data.code === 0) {
        ElMessage.success('批量删除成功')
        fetchUserList()
      } else {
        ElMessage.error(response.data?.message || '批量删除失败')
      }
    } catch (error) {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败，请稍后重试')
    }
  }).catch(() => {
    // 用户取消删除操作
  })
}

// 处理用户状态变更
const handleStatusChange = async (row) => {
  const newStatus = row.status === '1' ? '0' : '1'
  const statusText = newStatus === '1' ? '启用' : '禁用'
  
  try {
    // 调用状态变更API
    const response = await axios.put(`/api/system/users/status/${row.id}?status=${newStatus}`)
    if (response.data && response.data.code === 0) {
      ElMessage.success(`${statusText}成功`)
      row.status = newStatus
    } else {
      ElMessage.error(response.data?.message || `${statusText}失败`)
    }
  } catch (error) {
    console.error(`${statusText}用户失败:`, error)
    ElMessage.error(`${statusText}用户失败，请稍后重试`)
  }
}

// 处理批量启用/禁用
const handleBatchEnable = async (enable) => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请至少选择一条记录')
    return
  }
  
  const status = enable ? '1' : '0'
  const statusText = enable ? '启用' : '禁用'
  
  try {
    const ids = selectedRows.value.map(row => row.id)
    // 调用批量状态变更API
    const response = await axios.post(`/api/system/users/batch-status?status=${status}`, ids)
    if (response.data && response.data.code === 0) {
      ElMessage.success(`批量${statusText}成功`)
      fetchUserList()
    } else {
      ElMessage.error(response.data?.message || `批量${statusText}失败`)
    }
  } catch (error) {
    console.error(`批量${statusText}失败:`, error)
    ElMessage.error(`批量${statusText}失败，请稍后重试`)
  }
}

// 提交表单
const submitForm = async () => {
  if (!userFormRef.value) return
  
  await userFormRef.value.validate(async (valid, fields) => {
    if (valid) {
      try {
        const formData = { ...userForm.value }
        if (dialogType.value === 'edit') {
          delete formData.password
          delete formData.confirmPassword
        } else if (dialogType.value === 'add' && formData.password) {
          // 对新用户的密码进行加密
          formData.password = await md5(formData.password)
          delete formData.confirmPassword
        }
        
        // 调用新增或编辑用户API
        const url = '/api/system/users'
        const method = dialogType.value === 'add' ? 'post' : 'put'
        
        const response = await axios({ url, method, data: formData })
        
        if (response.data && response.data.code === 0) {
          ElMessage.success(dialogType.value === 'add' ? '添加成功' : '更新成功')
          dialogVisible.value = false
          fetchUserList()
        } else {
          ElMessage.error(response.data?.message || (dialogType.value === 'add' ? '添加失败' : '更新失败'))
        }
      } catch (error) {
        console.error(dialogType.value === 'add' ? '添加用户失败:' : '更新用户失败:', error)
        ElMessage.error(dialogType.value === 'add' ? '添加用户失败，请稍后重试' : '更新用户失败，请稍后重试')
      }
    } else {
      console.log('表单验证失败:', fields)
    }
  })
}

// 处理重置密码
const handleResetPassword = (row) => {
  currentResetUser.value = row
  resetPwdForm.value = {
    userId: row.id,
    newPassword: '',
    confirmPassword: ''
  }
  resetPwdDialogVisible.value = true
}

// 提交重置密码
const submitResetPwd = async () => {
  if (!resetPwdFormRef.value) return
  
  await resetPwdFormRef.value.validate(async (valid, fields) => {
    if (valid) {
      try {
        // 加密新密码
        const encryptedPassword = await md5(resetPwdForm.value.newPassword)
        
        // 准备发送的数据
        const resetData = {
          userId: resetPwdForm.value.userId,
          newPassword: encryptedPassword
        }
        
        // 调用重置密码API
        const response = await axios.post('/api/system/users/reset-password', resetData)
        
        if (response.data && response.data.code === 0) {
          ElMessage.success('密码重置成功')
          resetPwdDialogVisible.value = false
        } else {
          ElMessage.error(response.data?.message || '密码重置失败')
        }
      } catch (error) {
        console.error('重置密码失败:', error)
        ElMessage.error('重置密码失败，请稍后重试')
      }
    } else {
      console.log('表单验证失败:', fields)
    }
  })
}
</script>

<style scoped>
.user-management {
  padding: 16px;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  margin-bottom: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}
</style> 