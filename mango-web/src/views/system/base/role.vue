<template>
  <div class="role-container">
    <!-- 角色概览 -->
    <el-row :gutter="20" class="overview-section">
      <el-col :span="6" v-for="(item, index) in roleOverview" :key="index">
        <el-card 
          shadow="hover" 
          class="overview-card" 
          :class="item.type"
          :body-style="{ background: item.background }"
        >
          <div class="overview-content">
            <div class="overview-icon">
              <el-icon :size="40">
                <component :is="item.icon" />
              </el-icon>
            </div>
            <div class="overview-info">
              <div class="value">{{ item.value }}</div>
              <div class="label">{{ item.label }}</div>
            </div>
            <div class="trend" :class="item.trend">
              <span>{{ item.rate }}%</span>
              <el-icon>
                <component :is="item.trend === 'up' ? 'ArrowUpBold' : 'ArrowDownBold'" />
              </el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 角色列表卡片 -->
    <el-card class="role-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>角色管理</h3>
            <el-tag type="success" effect="dark">
              {{ total }} 个角色
            </el-tag>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="handleCreate">
              <el-icon><Plus /></el-icon> 新建角色
            </el-button>
            <el-button type="success" @click="handleImport">
              <el-icon><Upload /></el-icon> 导入
            </el-button>
            <el-button type="warning" @click="handleExport">
              <el-icon><Download /></el-icon> 导出
            </el-button>
            <el-button @click="refreshList">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 高级搜索 -->
      <el-form
        ref="searchFormRef"
        :model="searchForm"
        class="search-form"
      >
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="角色名称" prop="name">
              <el-input
                v-model="searchForm.name"
                placeholder="请输入角色名称"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="角色编码" prop="code">
              <el-input
                v-model="searchForm.code"
                placeholder="请输入角色编码"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状态" prop="status">
              <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
                <el-option label="启用" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="创建时间" prop="createTime">
              <el-date-picker
                v-model="searchForm.createTime"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24" class="search-buttons">
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon> 搜索
            </el-button>
            <el-button @click="resetSearch">
              <el-icon><Refresh /></el-icon> 重置
            </el-button>
            <el-button type="primary" link @click="toggleAdvanced">
              {{ isAdvanced ? '收起' : '展开' }}
              <el-icon>
                <component :is="isAdvanced ? 'ArrowUp' : 'ArrowDown'" />
              </el-icon>
            </el-button>
          </el-col>
        </el-row>
      </el-form>

      <!-- 角色列表 -->
      <el-table
        ref="roleTable"
        v-loading="loading"
        :data="roleList"
        style="width: 100%"
        :height="tableHeight"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column type="index" label="序号" width="80" />
        <el-table-column prop="name" label="角色名称" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="role-name">
              <el-icon :size="16" :style="{ color: row.color }">
                <UserFilled />
              </el-icon>
              <span>{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="角色编码" min-width="150" show-overflow-tooltip />
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status"
              :active-value="1"
              :inactive-value="0"
              @update:model-value="(val) => handleSwitchChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-tooltip content="编辑" placement="top">
                <el-button type="primary" link :icon="Edit" @click="handleEdit(row)" />
              </el-tooltip>
              <el-tooltip content="分配权限" placement="top">
                <el-button type="success" link :icon="Key" @click="handleAssignAuth(row)" />
              </el-tooltip>
              <el-tooltip content="分配用户" placement="top">
                <el-button type="warning" link :icon="User" @click="handleAssignUser(row)" />
              </el-tooltip>
              <el-tooltip content="数据权限" placement="top">
                <el-button type="info" link :icon="Operation" @click="handleDataScope(row)" />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button type="danger" link :icon="Delete" @click="handleDelete(row)" />
              </el-tooltip>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="page.current"
          v-model:page-size="page.size"
          :page-sizes="[10, 20, 30, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 角色编辑对话框 -->
    <el-dialog
      v-model="editDialog"
      :title="isEdit ? '编辑角色' : '新建角色'"
      width="600px"
      :destroy-on-close="true"
    >
      <el-form
        ref="roleFormRef"
        :model="roleForm"
        :rules="roleRules"
        label-width="100px"
      >
        <el-form-item label="角色名称" prop="name">
          <el-input
            v-model="roleForm.name"
            placeholder="请输入角色名称"
            :maxlength="50"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input
            v-model="roleForm.code"
            placeholder="请输入角色编码"
            :maxlength="30"
            show-word-limit
            :disabled="isEdit"
          >
            <template #prefix>
              <span class="code-prefix">ROLE_</span>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="角色颜色" prop="color">
          <el-color-picker v-model="roleForm.color" show-alpha />
        </el-form-item>
        <el-form-item label="显示排序" prop="sort">
          <el-input-number
            v-model="roleForm.sort"
            :min="0"
            :max="999"
            controls-position="right"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="roleForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
            :maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="roleForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editDialog = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 权限分配抽屉 -->
    <el-drawer
      v-model="authDrawer"
      :title="'分配权限 - ' + currentRole?.name"
      size="500px"
      :destroy-on-close="true"
    >
      <div class="auth-container">
        <div class="auth-header">
          <el-alert
            title="温馨提示：勾选模块即可分配该模块的所有操作权限"
            type="info"
            :closable="false"
            show-icon
          />
          <div class="auth-toolbar">
            <el-checkbox
              v-model="checkAll"
              :indeterminate="isIndeterminate"
              @change="handleCheckAllChange"
            >
              全选/全不选
            </el-checkbox>
            <el-button type="primary" link @click="expandAll">
              展开/折叠
            </el-button>
          </div>
        </div>
        <div class="auth-tree">
          <el-tree
            ref="authTreeRef"
            :data="menuTree"
            :props="{ label: 'name' }"
            show-checkbox
            node-key="id"
            :default-checked-keys="checkedKeys"
            :default-expanded-keys="expandedKeys"
            highlight-current
          >
            <template #default="{ node, data }">
              <div class="auth-node">
                <el-icon v-if="data.icon" :size="16">
                  <component :is="data.icon" />
                </el-icon>
                <span>{{ node.label }}</span>
                <el-tag 
                  v-if="data.type" 
                  size="small" 
                  :type="getMenuTypeTag(data.type)"
                >
                  {{ getMenuTypeLabel(data.type) }}
                </el-tag>
              </div>
            </template>
          </el-tree>
        </div>
      </div>
      <template #footer>
        <div style="flex: auto">
          <el-button @click="authDrawer = false">取消</el-button>
          <el-button type="primary" @click="submitAuth">
            确定
          </el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 用户分配抽屉 -->
    <el-drawer
      v-model="userDrawer"
      :title="'分配用户 - ' + currentRole?.name"
      size="800px"
      :destroy-on-close="true"
    >
      <div class="user-container">
        <div class="user-header">
          <el-form :inline="true" :model="userSearchForm">
            <el-form-item label="用户名称">
              <el-input
                v-model="userSearchForm.username"
                placeholder="请输入用户名称"
                clearable
                @keyup.enter="handleUserSearch"
              />
            </el-form-item>
            <el-form-item label="手机号码">
              <el-input
                v-model="userSearchForm.mobile"
                placeholder="请输入手机号码"
                clearable
                @keyup.enter="handleUserSearch"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUserSearch">
                <el-icon><Search /></el-icon> 搜索
              </el-button>
              <el-button @click="resetUserSearch">
                <el-icon><Refresh /></el-icon> 重置
              </el-button>
            </el-form-item>
          </el-form>
        </div>
        <div class="user-content">
          <el-transfer
            v-model="selectedUsers"
            :data="userList"
            :props="{
              key: 'id',
              label: 'username'
            }"
            :titles="['待选用户', '已选用户']"
            :button-texts="['移除', '添加']"
            filterable
            filter-placeholder="请输入用户名称"
          >
            <template #default="{ option }">
              <div class="transfer-item">
                <el-avatar :size="24" :src="option.avatar">
                  {{ option.username.substring(0, 1).toUpperCase() }}
                </el-avatar>
                <span>{{ option.username }}</span>
                <span class="transfer-extra">{{ option.mobile }}</span>
              </div>
            </template>
          </el-transfer>
        </div>
      </div>
      <template #footer>
        <div style="flex: auto">
          <el-button @click="userDrawer = false">取消</el-button>
          <el-button type="primary" @click="submitUsers">
            确定
          </el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 数据权限配置抽屉 -->
    <el-drawer
      v-model="dataScopeDrawer"
      :title="'数据权限配置 - ' + currentRole?.name"
      size="600px"
      :destroy-on-close="true"
    >
      <div class="data-scope-container">
        <el-alert
          title="特别提醒：被分配数据权限的角色，仅能看到被授权的数据范围！"
          type="warning"
          :closable="false"
          show-icon
          class="mb-4"
        />
        
        <el-form
          ref="dataScopeFormRef"
          :model="dataScopeForm"
          :rules="dataScopeRules"
          label-width="100px"
        >
          <el-form-item label="权限范围" prop="dataScope">
            <el-radio-group v-model="dataScopeForm.dataScope" @change="handleDataScopeChange">
              <el-space direction="vertical" size="small" style="width: 100%">
                <el-radio label="ALL">
                  <div class="radio-label">
                    <span>全部数据权限</span>
                    <el-tag size="small" type="success">不限制数据范围</el-tag>
                  </div>
                </el-radio>
                <el-radio label="CUSTOM">
                  <div class="radio-label">
                    <span>自定义数据权限</span>
                    <el-tag size="small" type="warning">仅限选择的部门及以下数据</el-tag>
                  </div>
                </el-radio>
                <el-radio label="DEPT">
                  <div class="radio-label">
                    <span>本部门数据权限</span>
                    <el-tag size="small" type="info">仅限所在部门数据</el-tag>
                  </div>
                </el-radio>
                <el-radio label="DEPT_AND_CHILD">
                  <div class="radio-label">
                    <span>本部门及以下数据权限</span>
                    <el-tag size="small" type="info">包含所在部门及下级数据</el-tag>
                  </div>
                </el-radio>
                <el-radio label="SELF">
                  <div class="radio-label">
                    <span>仅本人数据权限</span>
                    <el-tag size="small" type="danger">仅限个人数据</el-tag>
                  </div>
                </el-radio>
              </el-space>
            </el-radio-group>
          </el-form-item>

          <el-form-item
            v-if="dataScopeForm.dataScope === 'CUSTOM'"
            label="数据范围"
            prop="depts"
          >
            <div class="custom-scope">
              <div class="scope-tree">
                <el-input
                  v-model="deptFilterText"
                  placeholder="请输入部门名称"
                  clearable
                  prefix-icon="Search"
                />
                <div class="tree-container">
                  <el-tree
                    ref="deptTreeRef"
                    :data="deptTree"
                    :props="{ label: 'name' }"
                    show-checkbox
                    node-key="id"
                    highlight-current
                    :filter-node-method="filterDeptNode"
                    @check="handleDeptCheck"
                  >
                    <template #default="{ node, data }">
                      <div class="dept-node">
                        <el-icon><OfficeBuilding /></el-icon>
                        <span>{{ node.label }}</span>
                        <el-tag 
                          size="small" 
                          type="info" 
                          v-if="data.users"
                        >
                          {{ data.users }}人
                        </el-tag>
                      </div>
                    </template>
                  </el-tree>
                </div>
              </div>
              <div class="scope-preview">
                <div class="preview-header">
                  <span>已选择 {{ selectedDepts.length }} 个部门</span>
                  <el-button type="primary" link @click="handleClearDepts">
                    清空
                  </el-button>
                </div>
                <div class="preview-list">
                  <el-empty
                    v-if="!selectedDepts.length"
                    description="暂未选择任何部门"
                  />
                  <el-scrollbar v-else>
                    <div
                      v-for="dept in selectedDepts"
                      :key="dept.id"
                      class="preview-item"
                    >
                      <el-icon><OfficeBuilding /></el-icon>
                      <span>{{ dept.name }}</span>
                      <el-button
                        type="danger"
                        link
                        :icon="Delete"
                        @click="handleRemoveDept(dept)"
                      />
                    </div>
                  </el-scrollbar>
                </div>
              </div>
            </div>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div style="flex: auto">
          <el-button @click="dataScopeDrawer = false">取消</el-button>
          <el-button type="primary" @click="submitDataScope">
            确定
          </el-button>
        </div>
      </template>
    </el-drawer>

    <!-- 导入对话框 -->
    <el-dialog
      v-model="importDialog"
      title="导入角色"
      width="500px"
    >
      <div class="import-container">
        <el-steps :active="importStep" finish-status="success" simple>
          <el-step title="上传文件" />
          <el-step title="数据预览" />
          <el-step title="导入完成" />
        </el-steps>

        <!-- 步骤一：上传文件 -->
        <div v-if="importStep === 0" class="step-content">
          <el-upload
            class="upload-demo"
            drag
            action="#"
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            accept=".xlsx,.xls"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                <div>只能上传 xlsx/xls 文件，且文件大小不超过 2MB</div>
                <div class="mt-2">
                  <el-button type="primary" link @click="handleDownloadTemplate">
                    <el-icon><Download /></el-icon>
                    下载模板
                  </el-button>
                </div>
              </div>
            </template>
          </el-upload>
        </div>

        <!-- 步骤二：数据预览 -->
        <div v-if="importStep === 1" class="step-content">
          <el-alert
            v-if="importData.length"
            :title="'共读取到 ' + importData.length + ' 条数据'"
            type="success"
            :closable="false"
            show-icon
            class="mb-4"
          />
          <el-table
            :data="importData"
            style="width: 100%"
            max-height="300"
            border
          >
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="name" label="角色名称" />
            <el-table-column prop="code" label="角色编码" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                  {{ row.status === 1 ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 步骤三：导入完成 -->
        <div v-if="importStep === 2" class="step-content">
          <el-result
            icon="success"
            title="导入成功"
            :sub-title="'成功导入 ' + importData.length + ' 条数据'"
          >
            <template #extra>
              <el-button type="primary" @click="closeImport">完成</el-button>
            </template>
          </el-result>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelImport">取消</el-button>
          <el-button
            v-if="importStep === 0"
            type="primary"
            :disabled="!importFile"
            @click="previewImportData"
          >
            下一步
          </el-button>
          <el-button
            v-if="importStep === 1"
            type="primary"
            @click="submitImport"
          >
            开始导入
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import {
  Plus, Edit, Delete, Search, Refresh, Upload, Download,
  Key, User, Operation, ArrowUp, ArrowDown,
  ArrowUpBold, ArrowDownBold, UserFilled, UploadFilled,
  OfficeBuilding
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
// 导入API接口
import {
  getRoleOverview,
  getRoleList,
  getRoleInfo,
  addRole,
  updateRole,
  deleteRole,
  updateRoleStatus,
  assignRoleMenus,
  assignRoleDataScope,
  getRoleOptions,
  getUserRoles,
  assignUserRoles,
  exportRole,
  importRole,
  downloadTemplate,
  previewImport
} from '@/api/system/role'
// 导入其他需要的API
import { getMenuTree } from '@/api/system/menu'
import { getDeptTree, getDeptsByIds } from '@/api/system/dept'
import { getUserList as fetchSystemUserList } from '@/api/system/user'

// 角色概览数据
const roleOverview = ref([
  {
    label: '角色总数',
    value: 0,
    type: 'primary',
    icon: 'User',
    trend: 'up',
    rate: 0,
    background: 'linear-gradient(135deg, #1890FF 0%, #36CBCB 100%)'
  },
  {
    label: '系统角色',
    value: 0,
    type: 'warning',
    icon: 'Setting',
    trend: 'up',
    rate: 0,
    background: 'linear-gradient(135deg, #FF9F43 0%, #FFB976 100%)'
  },
  {
    label: '自定义角色',
    value: 0,
    type: 'success',
    icon: 'Edit',
    trend: 'up',
    rate: 0,
    background: 'linear-gradient(135deg, #36D1DC 0%, #5B86E5 100%)'
  },
  {
    label: '停用角色',
    value: 0,
    type: 'danger',
    icon: 'CircleClose',
    trend: 'down',
    rate: 0,
    background: 'linear-gradient(135deg, #FF6B6B 0%, #FF8E8E 100%)'
  }
])

// 获取角色概览数据
const fetchRoleOverview = async () => {
  try {
    const res = await getRoleOverview()
    if (res.success) {
      const data = res.data
      roleOverview.value[0].value = data.total
      roleOverview.value[1].value = data.systemRoles
      roleOverview.value[2].value = data.customRoles
      roleOverview.value[3].value = data.disabledRoles
    }
  } catch (error) {
    console.error('获取角色概览失败:', error)
  }
}

// 搜索表单
const isAdvanced = ref(false)
const searchFormRef = ref(null)
const searchForm = ref({
  name: '',
  code: '',
  status: '',
  createTime: []
})

// 表格数据
const loading = ref(false)
const roleTable = ref(null)
const roleList = ref([])

// 分页配置
const page = ref({
  current: 1,
  size: 10
})
const total = ref(0)
const tableHeight = ref('calc(100vh - 520px)')

// 记录每个角色的原始状态
const originalStatus = ref(new Map());

// 增加初始化标志
const isLoading = ref(true);

// 搜索方法
const handleSearch = () => {
  page.value.current = 1
  isLoading.value = true
  fetchRoleList()
}

const resetSearch = () => {
  searchFormRef.value?.resetFields()
  isLoading.value = true
  handleSearch()
}

const toggleAdvanced = () => {
  isAdvanced.value = !isAdvanced.value
}

// 获取角色列表
const fetchRoleList = async () => {
  loading.value = true
  try {
    const params = {
      current: page.value.current,
      size: page.value.size,
      name: searchForm.value.name,
      code: searchForm.value.code,
      status: searchForm.value.status
    }
    
    // 添加时间范围参数
    if (searchForm.value.createTime && searchForm.value.createTime.length === 2) {
      params.beginTime = searchForm.value.createTime[0]
      params.endTime = searchForm.value.createTime[1]
    }
    
    const res = await getRoleList(params)
    if (res.success) {
      const pageData = res.data
      roleList.value = pageData.records || []
      total.value = pageData.total
      
      // 记录每个角色的原始状态
      originalStatus.value.clear();
      roleList.value.forEach(role => {
        originalStatus.value.set(role.id, role.status);
      });
    }
  } catch (error) {
    console.error('获取角色列表失败:', error)
    ElMessage.error('获取角色列表失败')
  } finally {
    loading.value = false
  }
}

// 表格操作方法
const handleCreate = () => {
  isEdit.value = false
  roleForm.value = {
    name: '',
    code: '',
    color: '#1890FF',
    sort: 0,
    remark: '',
    status: 1
  }
  editDialog.value = true
}

const handleEdit = async (row) => {
  try {
    loading.value = true
    console.log('开始编辑角色:', row);
    
    // 如果row中已包含所有需要的数据，直接使用，避免额外请求
    if (row.name && row.code && row.sort !== undefined && row.status !== undefined) {
      console.log('使用现有数据编辑角色');
  isEdit.value = true
  roleForm.value = { ...row }
  editDialog.value = true
      return;
    }
    
    // 否则从后端获取完整数据
    const res = await getRoleInfo(row.id)
    console.log('获取角色详情结果:', res);
    
    if (res && res.success) {
      isEdit.value = true
      roleForm.value = res.data || { ...row }
      editDialog.value = true
    } else {
      const errorMsg = (res && res.msg) || '获取角色详情失败'
      console.error('获取角色详情失败:', errorMsg)
      ElMessage.error(errorMsg)
      
      // 如果远程获取失败但有基本数据，仍可编辑
      if (row.name && row.code) {
        isEdit.value = true
        roleForm.value = { ...row }
        editDialog.value = true
        ElMessage.warning('使用本地数据编辑，部分信息可能不完整')
      }
    }
  } catch (error) {
    console.error('获取角色详情失败:', error)
    ElMessage.error('获取角色详情失败: ' + (error.message || '未知错误'))
    
    // 尝试使用表格行数据
    if (row.name && row.code) {
      isEdit.value = true
      roleForm.value = { ...row }
      editDialog.value = true
      ElMessage.warning('使用本地数据编辑，部分信息可能不完整')
    }
  } finally {
    loading.value = false
  }
}

const handleAssignAuth = async (row) => {
  try {
  currentRole.value = row
    loading.value = true
    
    // 获取角色菜单权限
    const res = await getRoleInfo(row.id)
    if (res.success) {
      const roleData = res.data
      checkedKeys.value = roleData.menuIds || []
      expandedKeys.value = [...checkedKeys.value] // 展开已选节点
  authDrawer.value = true
    }
  } catch (error) {
    console.error('获取角色权限失败:', error)
    ElMessage.error('获取角色权限失败')
  } finally {
    loading.value = false
  }
}

const handleAssignUser = async (row) => {
  try {
  currentRole.value = row
    loading.value = true
    
    // 获取角色用户分配情况
    const res = await getUserRoles(row.id)
    console.log('获取角色用户结果:', res)
    
    if (res.success) {
      // 确保res.data是一个数组
      if (Array.isArray(res.data)) {
        selectedUsers.value = res.data
      } else {
        console.error('用户角色数据结构异常:', res.data)
        selectedUsers.value = []
      }
      
      // 获取完用户角色后再获取用户列表
      await fetchUserList()
  userDrawer.value = true
    } else {
      ElMessage.error(res.message || '获取角色用户分配失败')
    }
  } catch (error) {
    console.error('获取角色用户分配失败:', error)
    ElMessage.error('获取角色用户分配失败')
  } finally {
    loading.value = false
  }
}

const handleDataScope = async (row) => {
  try {
  currentRole.value = row
    loading.value = true
    
    // 获取角色数据权限
    const res = await getRoleInfo(row.id)
    if (res.success) {
      const roleData = res.data
  dataScopeForm.value = {
        dataScope: roleData.dataScope || 'ALL',
        depts: []
      }
      
      // 如果是自定义权限，获取部门列表
      if (roleData.dataScope === 'CUSTOM' && roleData.deptIds && roleData.deptIds.length > 0) {
        const deptRes = await getDeptsByIds(roleData.deptIds)
        if (deptRes.success) {
          selectedDepts.value = deptRes.data || []
        } else {
          selectedDepts.value = []
        }
      } else {
        selectedDepts.value = []
      }
      
  dataScopeDrawer.value = true
    }
  } catch (error) {
    console.error('获取角色数据权限失败:', error)
    ElMessage.error('获取角色数据权限失败')
  } finally {
    loading.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除角色 "${row.name}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const res = await deleteRole(row.id)
      if (res.success) {
        ElMessage.success('删除角色成功')
        fetchRoleList()
      } else {
        ElMessage.error(res.message || '删除角色失败')
      }
    } catch (error) {
      console.error('删除角色失败:', error)
      ElMessage.error('删除角色失败')
    }
  }).catch(() => {})
}

// 处理开关状态变更
const handleSwitchChange = async (row, newValue) => {
  // 如果是正在加载中，忽略所有状态变更
  if (isLoading.value) {
    console.log('数据加载中，忽略状态变更操作', row.id);
    return;
  }
  
  try {
    console.log('手动操作状态变更:', row.id, '原状态:', row.status, '新状态:', newValue);
    
    // 更新本地状态
    row.status = newValue;
    
    // 调用API更新服务器状态
    const res = await updateRoleStatus(row.id, newValue);
    if (res.success) {
      const action = newValue === 1 ? '启用' : '停用';
      ElMessage.success(`${action}角色成功`);
    } else {
      // 恢复原状态
      row.status = originalStatus.value.get(row.id) || (newValue === 1 ? 0 : 1);
      ElMessage.error(res.message || '更新角色状态失败');
    }
  } catch (error) {
    // 恢复原状态
    row.status = originalStatus.value.get(row.id) || (newValue === 1 ? 0 : 1);
    console.error('更新角色状态失败:', error);
    ElMessage.error('更新角色状态失败');
  }
}

// 分页方法
const handleSizeChange = (val) => {
  page.value.size = val
  isLoading.value = true
  fetchRoleList()
}

const handleCurrentChange = (val) => {
  page.value.current = val
  isLoading.value = true
  fetchRoleList()
}

// 批量选择
const selectedRoles = ref([])
const handleSelectionChange = (selection) => {
  selectedRoles.value = selection
}

// 导入导出
const handleImport = () => {
  importDialog.value = true
}

const handleExport = () => {
  ElMessageBox.confirm(
    '确定要导出当前筛选条件下的角色数据吗？',
    '导出确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(async () => {
    try {
      const params = {
        name: searchForm.value.name,
        code: searchForm.value.code,
        status: searchForm.value.status
      }
      
      if (searchForm.value.createTime && searchForm.value.createTime.length === 2) {
        params.beginTime = searchForm.value.createTime[0]
        params.endTime = searchForm.value.createTime[1]
      }
      
      const response = await exportRole(params)
      
      // 处理文件下载
      const blob = new Blob([response.data])
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', `角色数据_${new Date().getTime()}.xlsx`)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      
    ElMessage.success('导出成功')
    } catch (error) {
      console.error('导出失败:', error)
      ElMessage.error('导出失败')
    }
  }).catch(() => {})
}

// 刷新列表
const refreshList = () => {
  isLoading.value = true
  fetchRoleList()
}

// 角色表单相关
const editDialog = ref(false)
const isEdit = ref(false)
const roleFormRef = ref(null)
const roleForm = ref({
  name: '',
  code: '',
  color: '#1890FF',
  sort: 0,
  remark: '',
  status: 1
})

const roleRules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]*$/, message: '角色编码只能包含大写字母、数字和下划线，且必须以字母开头', trigger: 'blur' }
  ]
}

const submitForm = async () => {
  if (!roleFormRef.value) return
  
  await roleFormRef.value.validate(async (valid, fields) => {
    if (valid) {
      try {
        loading.value = true
        console.log('提交角色表单:', roleForm.value)
        
        // 确保表单数据完整
        const formData = {
          ...roleForm.value,
          status: roleForm.value.status !== undefined ? roleForm.value.status : 1,
          sort: roleForm.value.sort !== undefined ? roleForm.value.sort : 0,
          menuIds: roleForm.value.menuIds || [],
          deptIds: roleForm.value.deptIds || []
        }
        
        console.log('处理后的提交数据:', formData)
        
        let res
        
        if (isEdit.value) {
          // 确保编辑时有ID
          if (!formData.id) {
            ElMessage.error('角色ID不能为空')
            return
          }
          // 更新角色
          res = await updateRole(formData)
        } else {
          // 新增角色
          res = await addRole(formData)
        }
        
        console.log('角色操作结果:', res)
        
        if (res && res.success) {
      ElMessage.success(`${isEdit.value ? '更新' : '创建'}角色成功`)
      editDialog.value = false
          fetchRoleList()
        } else {
          const errorMsg = (res && res.message) || `${isEdit.value ? '更新' : '创建'}角色失败`
          console.error('角色操作失败:', errorMsg)
          ElMessage.error(errorMsg)
        }
      } catch (error) {
        const errorMsg = (error.response && error.response.data && error.response.data.message) || 
                        error.message || 
                        `${isEdit.value ? '更新' : '创建'}角色失败`
        console.error(`${isEdit.value ? '更新' : '创建'}角色失败:`, error)
        ElMessage.error(errorMsg)
      } finally {
        loading.value = false
      }
    } else {
      console.log('验证失败:', fields)
    }
  })
}

// 权限分配相关
const authDrawer = ref(false)
const currentRole = ref(null)
const authTreeRef = ref(null)
const checkAll = ref(false)
const isIndeterminate = ref(false)
const checkedKeys = ref([])
const expandedKeys = ref([])
const menuTree = ref([])

// 获取菜单树
const fetchMenuTree = async () => {
  try {
    const res = await getMenuTree()
    if (res.success) {
      menuTree.value = res.data || []
    }
  } catch (error) {
    console.error('获取菜单树失败:', error)
    ElMessage.error('获取菜单树失败')
  }
}

const handleCheckAllChange = (val) => {
  const allKeys = getAllKeys(menuTree.value)
  checkedKeys.value = val ? allKeys : []
  isIndeterminate.value = false
  
  // 更新树选中状态
  if (authTreeRef.value) {
    authTreeRef.value.setCheckedKeys(checkedKeys.value)
  }
}

const getAllKeys = (menus) => {
  const keys = []
  const getKeys = (items) => {
    items.forEach(item => {
      keys.push(item.id)
      if (item.children) {
        getKeys(item.children)
      }
    })
  }
  getKeys(menus)
  return keys
}

const expandAll = () => {
  // 获取所有节点ID
  const allKeys = getAllKeys(menuTree.value)
  
  // 判断当前是否已经全部展开
  const isAllExpanded = expandedKeys.value.length === allKeys.length
  
  // 切换展开/折叠状态
  expandedKeys.value = isAllExpanded ? [] : allKeys
}

const submitAuth = async () => {
  const checkedNodes = authTreeRef.value.getCheckedKeys()
  
  try {
    loading.value = true
    const res = await assignRoleMenus(currentRole.value.id, checkedNodes)
    if (res.success) {
  ElMessage.success('权限分配成功')
  authDrawer.value = false
    } else {
      ElMessage.error(res.message || '权限分配失败')
    }
  } catch (error) {
    console.error('权限分配失败:', error)
    ElMessage.error('权限分配失败')
  } finally {
    loading.value = false
  }
}

// 用户分配相关
const userDrawer = ref(false)
const userSearchForm = ref({
  username: '',
  mobile: ''
})
const selectedUsers = ref([])
const userList = ref([])

// 获取用户列表
const fetchUserList = async () => {
  try {
    const params = { ...userSearchForm.value }
    const res = await fetchSystemUserList(params)
    if (res.success) {
      // 检查返回的数据结构
      console.log('用户列表数据:', res.data)
      
      // 判断数据结构类型
      if (res.data && res.data.records && Array.isArray(res.data.records)) {
        // 如果是分页结构 {records: [...], total: number}
        userList.value = res.data.records.map(user => ({
          id: user.id,
          username: user.username || user.realName || '未命名',
          mobile: user.mobile || '未设置',
          avatar: user.avatar || ''
        }))
      } else if (Array.isArray(res.data)) {
        // 如果直接返回数组
        userList.value = res.data.map(user => ({
          id: user.id,
          username: user.username || user.realName || '未命名',
          mobile: user.mobile || '未设置',
          avatar: user.avatar || ''
        }))
      } else if (res.data && res.data.records) {
        // 可能是返回对象，但records不是数组
        userList.value = []
        console.error('用户列表数据结构异常:', res.data)
      } else {
        // 其他情况
        userList.value = []
        console.error('用户列表数据结构不符合预期:', res.data)
      }
    } else {
      userList.value = []
      ElMessage.error(res.message || '获取用户列表失败')
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
    userList.value = []
  }
}

const handleUserSearch = () => {
  fetchUserList()
}

const resetUserSearch = () => {
  userSearchForm.value = {
    username: '',
    mobile: ''
  }
  handleUserSearch()
}

const submitUsers = async () => {
  try {
    loading.value = true
    const res = await assignUserRoles(currentRole.value.id, selectedUsers.value)
    if (res.success) {
  ElMessage.success('用户分配成功')
  userDrawer.value = false
    } else {
      ElMessage.error(res.message || '用户分配失败')
    }
  } catch (error) {
    console.error('用户分配失败:', error)
    ElMessage.error('用户分配失败')
  } finally {
    loading.value = false
  }
}

// 数据权限相关
const dataScopeDrawer = ref(false)
const dataScopeFormRef = ref(null)
const dataScopeForm = ref({
  dataScope: 'ALL',
  depts: []
})

const dataScopeRules = {
  dataScope: [
    { required: true, message: '请选择权限范围', trigger: 'change' }
  ]
}

const deptFilterText = ref('')
const deptTreeRef = ref(null)
const selectedDepts = ref([])

// 获取部门树
const fetchDeptTree = async () => {
  try {
    const res = await getDeptTree()
    if (res.success) {
      deptTree.value = res.data || []
    }
  } catch (error) {
    console.error('获取部门树失败:', error)
    ElMessage.error('获取部门树失败')
  }
}

const deptTree = ref([])

watch(deptFilterText, (val) => {
  deptTreeRef.value?.filter(val)
})

const filterDeptNode = (value, data) => {
  if (!value) return true
  return data.name.includes(value)
}

const handleDataScopeChange = (value) => {
  if (value !== 'CUSTOM') {
    selectedDepts.value = []
    if (deptTreeRef.value) {
      deptTreeRef.value.setCheckedKeys([])
    }
  }
}

const handleDeptCheck = (data, { checkedNodes }) => {
  selectedDepts.value = checkedNodes.filter(node => !node.children || node.children.length === 0)
}

const handleClearDepts = () => {
  deptTreeRef.value?.setCheckedKeys([])
  selectedDepts.value = []
}

const handleRemoveDept = (dept) => {
  const index = selectedDepts.value.findIndex(item => item.id === dept.id)
  if (index > -1) {
    selectedDepts.value.splice(index, 1)
    deptTreeRef.value?.setChecked(dept.id, false)
  }
}

const submitDataScope = async () => {
  if (!dataScopeFormRef.value) return
  
  await dataScopeFormRef.value.validate(async (valid, fields) => {
    if (valid) {
      try {
        loading.value = true
        
        // 构建请求数据
        const requestData = {
          id: currentRole.value.id,
          dataScope: dataScopeForm.value.dataScope,
          deptIds: dataScopeForm.value.dataScope === 'CUSTOM' 
            ? selectedDepts.value.map(dept => dept.id) 
            : []
        }
        
        const res = await assignRoleDataScope(currentRole.value.id, requestData)
        if (res.success) {
      ElMessage.success('数据权限配置成功')
      dataScopeDrawer.value = false
        } else {
          ElMessage.error(res.message || '数据权限配置失败')
        }
      } catch (error) {
        console.error('数据权限配置失败:', error)
        ElMessage.error('数据权限配置失败')
      } finally {
        loading.value = false
      }
    } else {
      console.log('验证失败:', fields)
    }
  })
}

// 导入导出相关
const importDialog = ref(false)
const importStep = ref(0)
const importFile = ref(null)
const importData = ref([])

const handleFileChange = (file) => {
  importFile.value = file.raw
}

const handleFileRemove = () => {
  importFile.value = null
}

const handleDownloadTemplate = async () => {
  try {
    const response = await downloadTemplate()
    
    const blob = new Blob([response.data])
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', '角色导入模板.xlsx')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    
  ElMessage.success('下载模板成功')
  } catch (error) {
    console.error('下载模板失败:', error)
    ElMessage.error('下载模板失败')
  }
}

const previewImportData = async () => {
  if (!importFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  
  try {
    const formData = new FormData()
    formData.append('file', importFile.value)
    
    const res = await previewImport(formData)
    
    if (res.success) {
      importData.value = res.data || []
  importStep.value = 1
    } else {
      ElMessage.error(res.message || '预览导入数据失败')
    }
  } catch (error) {
    console.error('预览导入数据失败:', error)
    ElMessage.error('预览导入数据失败')
  }
}

const submitImport = async () => {
  try {
    const res = await importRole(importData.value)
    if (res.success) {
  importStep.value = 2
      fetchRoleList() // 刷新列表
    } else {
      ElMessage.error(res.message || '导入角色失败')
    }
  } catch (error) {
    console.error('导入角色失败:', error)
    ElMessage.error('导入角色失败')
  }
}

const cancelImport = () => {
  importDialog.value = false
  importStep.value = 0
  importFile.value = null
  importData.value = []
}

const closeImport = () => {
  cancelImport()
  refreshList()
}

// 获取菜单类型标签
const getMenuTypeTag = (type) => {
  switch(type) {
    case 'DIR': return 'warning'
    case 'MENU': return 'success'
    case 'BUTTON': return 'info'
    default: return ''
  }
}

// 获取菜单类型文本
const getMenuTypeLabel = (type) => {
  switch(type) {
    case 'DIR': return '目录'
    case 'MENU': return '菜单'
    case 'BUTTON': return '按钮'
    default: return '未知'
  }
}

// 生命周期钩子
onMounted(async () => {
  // 初始化时设置加载标志
  isLoading.value = true;
  
  try {
    // 并行发起所有请求
    await Promise.all([
      fetchRoleOverview(), // 获取角色概览
      fetchRoleList(),     // 获取角色列表
      fetchMenuTree(),     // 获取菜单树
      fetchDeptTree(),     // 获取部门树
      fetchUserList()      // 获取用户列表
    ]);
  } catch (error) {
    console.error('初始化数据加载失败:', error);
  } finally {
    // 所有数据加载完成后，延迟一段时间再允许操作
    setTimeout(() => {
      isLoading.value = false;
      console.log('数据初始化完成，允许状态操作');
    }, 1000);
  }
})
</script>

<style scoped>
.role-container {
  padding: 20px;
}

.overview-section {
  margin-bottom: 20px;
}

.overview-card {
  transition: all 0.3s;
  color: #fff;
}

.overview-card:hover {
  transform: translateY(-5px);
}

.overview-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.overview-icon {
  padding: 8px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.2);
}

.overview-info {
  flex: 1;
  margin: 0 16px;
}

.overview-info .value {
  font-size: 24px;
  font-weight: bold;
  line-height: 1;
  margin-bottom: 8px;
}

.overview-info .label {
  font-size: 14px;
  opacity: 0.8;
}

.trend {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.2);
  font-size: 12px;
}

.trend.up {
  background: rgba(82, 196, 26, 0.2);
}

.trend.down {
  background: rgba(255, 77, 79, 0.2);
}

.role-card {
  margin-bottom: 20px;
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

.header-left h3 {
  margin: 0;
}

.header-right {
  display: flex;
  gap: 12px;
}

.search-form {
  margin-bottom: 20px;
  padding: 20px;
  background-color: var(--el-bg-color-page);
  border-radius: 4px;
}

.search-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 16px;
}

.role-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

:deep(.el-card__body) {
  padding: 20px !important;
}

:deep(.el-form-item) {
  margin-bottom: 16px;
}

:deep(.el-date-editor) {
  width: 100%;
}

.code-prefix {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  padding-right: 4px;
}

.auth-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.auth-header {
  margin-bottom: 16px;
}

.auth-toolbar {
  margin-top: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.auth-tree {
  flex: 1;
  overflow: auto;
  padding: 16px;
  background-color: var(--el-bg-color-page);
  border-radius: 4px;
}

.auth-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.user-header {
  margin-bottom: 16px;
}

.user-content {
  flex: 1;
  overflow: auto;
}

.transfer-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
}

.transfer-extra {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

:deep(.el-drawer__body) {
  padding: 20px;
  height: calc(100% - 100px);
}

:deep(.el-transfer) {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.el-transfer__buttons) {
  padding: 0 16px;
}

:deep(.el-transfer-panel) {
  width: 300px;
  height: 100%;
}

:deep(.el-transfer-panel__body) {
  height: calc(100% - 55px);
}

:deep(.el-transfer-panel__list) {
  height: 100%;
}

.data-scope-container {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.custom-scope {
  display: flex;
  gap: 16px;
  height: 400px;
}

.scope-tree {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.tree-container {
  flex: 1;
  overflow: auto;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
}

.scope-preview {
  width: 240px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px;
  background-color: var(--el-bg-color-page);
  border-radius: 4px;
}

.preview-list {
  flex: 1;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  overflow: hidden;
}

.preview-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  border-bottom: 1px solid var(--el-border-color);
}

.preview-item:last-child {
  border-bottom: none;
}

.dept-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.import-container {
  padding: 20px 0;
}

.step-content {
  margin-top: 20px;
}

.mb-4 {
  margin-bottom: 16px;
}

.mt-2 {
  margin-top: 8px;
}
</style> 