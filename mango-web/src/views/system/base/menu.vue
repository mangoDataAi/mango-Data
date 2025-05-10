<template>
  <div class="menu-container">
    <!-- 菜单管理卡片 -->
    <el-card class="menu-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <h3>菜单管理</h3>
            <el-tag type="success" effect="dark">
              {{ totalMenus }} 个菜单项
            </el-tag>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="createMenu(null)">
              <el-icon><Plus /></el-icon> 新建菜单
            </el-button>
            <el-button type="success" @click="expandAll">
              <el-icon><Expand /></el-icon> 展开全部
            </el-button>
            <el-button type="info" @click="collapseAll">
              <el-icon><Fold /></el-icon> 折叠全部
            </el-button>
            <el-button @click="refreshList">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" class="search-form" inline>
        <div class="toolbar-right">
          <el-form-item label="菜单名称">
            <el-input
              v-model="searchForm.name"
              placeholder="请输入菜单名称"
              clearable
              @keyup.enter="handleSearch"
              style="width: 220px;"
            />
          </el-form-item>
          <el-form-item label="菜单类型">
            <el-select 
              v-model="searchForm.type" 
              placeholder="请选择菜单类型" 
              clearable
              style="width: 220px;"
            >
              <el-option label="目录" value="DIR" />
              <el-option label="菜单" value="MENU" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select 
              v-model="searchForm.status" 
              placeholder="请选择状态" 
              clearable
              style="width: 220px;"
            >
              <el-option label="启用" value="1" />
              <el-option label="禁用" value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">
              <el-icon><Search /></el-icon> 搜索
            </el-button>
            <el-button @click="resetSearch">
              <el-icon><Refresh /></el-icon> 重置
            </el-button>
          </el-form-item>
        </div>
      </el-form>

      <!-- 菜单树形表格 -->
      <el-table
        v-loading="loading"
        ref="menuTable"
        :data="menuList"
        row-key="id"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        :expand-row-keys="expandedKeys"
        style="width: 100%"
        :height="tableHeight"
        border
      >
        <el-table-column type="index" width="50" align="center" />
        <el-table-column label="菜单名称" min-width="350">
          <template #default="{ row, treeNode, store }">
            <div class="menu-name-cell">
              <!-- 树节点按钮 -->
              <span class="tree-spacer" :style="{ paddingLeft: getPaddingLeft(treeNode) }">
                <!-- 树连接线 -->
                <span v-if="treeNode.level > 0" class="tree-line vertical"></span>
                <span v-if="treeNode.level > 0" class="tree-line horizontal"></span>
                
                <!-- 展开/折叠按钮 -->
                <el-button
                  v-if="row.children && row.children.length"
                  type="primary"
                  link
                  class="expand-button"
                  @click.stop="toggleExpand(row, treeNode, store)"
                >
                  <el-icon>
                    <component :is="treeNode.expanded ? 'ArrowDown' : 'ArrowRight'" />
                  </el-icon>
                </el-button>
                <span v-else class="leaf-node"></span>
              </span>
              
              <!-- 菜单图标 -->
              <el-icon v-if="row.icon" class="menu-icon">
                <component :is="renderIcon(row.icon)" />
              </el-icon>
              <span v-else class="no-icon"></span>
              
              <!-- 菜单类型和名称 -->
              <div class="menu-info">
                <el-tag 
                  size="small" 
                  :type="row.type === 'DIR' ? 'warning' : 'success'"
                  class="menu-type-tag"
                >
                  {{ row.type === 'DIR' ? '目录' : '菜单' }}
                </el-tag>
                <span class="menu-name" :class="`level-${treeNode.level}`">{{ row.name }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="60" align="center" />
        <el-table-column prop="path" label="路由地址" min-width="180" show-overflow-tooltip />
        <el-table-column prop="component" label="组件路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-tooltip content="新建子菜单" placement="top">
                <el-button 
                  type="primary" 
                  link
                  :icon="Plus"
                  @click="createMenu(row)"
                />
              </el-tooltip>
              <el-tooltip content="编辑" placement="top">
                <el-button 
                  type="primary" 
                  link
                  :icon="Edit"
                  @click="editMenu(row)"
                />
              </el-tooltip>
              <el-tooltip content="删除" placement="top">
                <el-button 
                  type="danger" 
                  link
                  :icon="Delete"
                  @click="deleteMenu(row)"
                />
              </el-tooltip>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 菜单编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑菜单' : '新建菜单'"
      width="600px"
      :destroy-on-close="true"
    >
      <el-form
        ref="menuFormRef"
        :model="menuForm"
        :rules="menuRules"
        label-width="100px"
      >
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="上级菜单">
              <el-tree-select
                v-model="menuForm.parentId"
                :data="menuOptions"
                :props="{ label: 'name', value: 'id' }"
                placeholder="选择上级菜单"
                check-strictly
                :render-after-expand="false"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="菜单类型" prop="type">
              <el-radio-group v-model="menuForm.type">
                <el-radio-button value="DIR">目录</el-radio-button>
                <el-radio-button value="MENU">菜单</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="name">
              <el-input v-model="menuForm.name" placeholder="请输入菜单名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="sort">
              <el-input-number
                v-model="menuForm.sort"
                :min="0"
                :max="999"
                controls-position="right"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单图标" prop="icon">
              <div class="icon-selector">
                <div class="selected-icon" @click="showIconSelector = true">
                  <span class="icon-container">
                    <el-icon v-if="menuForm.icon" :size="16" class="preview-icon">
                      <component :is="renderIcon(menuForm.icon)" />
                    </el-icon>
                    <span v-else class="no-icon">无</span>
                  </span>
                  <span class="icon-name">{{ menuForm.icon || '' }}</span>
                </div>
                <el-button type="primary" link @click="showIconSelector = true">
                  选择
                </el-button>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="路由地址" prop="path">
              <el-input v-model="menuForm.path" placeholder="请输入路由地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="menuForm.type === 'MENU'">
            <el-form-item label="组件路径" prop="component">
              <el-input
                v-model="menuForm.component"
                placeholder="请输入组件路径"
              >
                <template #prefix>
                  <span class="path-prefix">@/views/</span>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="菜单状态">
              <el-radio-group v-model="menuForm.status">
                <el-radio :label="1">显示</el-radio>
                <el-radio :label="0">隐藏</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 图标选择器对话框 -->
    <el-dialog
      v-model="showIconSelector"
      title="选择图标"
      width="700px"
      :destroy-on-close="true"
    >
      <div class="icon-search-wrapper">
        <el-input
          v-model="iconSearchKeyword"
          placeholder="搜索图标"
          clearable
          prefix-icon="Search"
        />
      </div>
      <div class="icon-grid">
        <div
          v-for="icon in filteredIcons"
          :key="icon"
          class="icon-item"
          :class="{ 'active': menuForm.icon === icon }"
          @click="selectIcon(icon)"
        >
          <div class="icon-preview">
            <el-icon :size="20">
              <component :is="renderIcon(icon)" />
            </el-icon>
          </div>
          <span class="icon-label">{{ icon }}</span>
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showIconSelector = false">取消</el-button>
          <el-button type="primary" @click="confirmIconSelection">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  Plus, Edit, Delete, Search, Refresh,
  Expand, Fold, Menu as MenuIcon,
  Setting, User, List, Tools,
  ArrowDown, ArrowRight
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import * as ElementPlusIcons from '@element-plus/icons-vue'

// 导入所有Element Plus图标
const allIcons = ref(Object.keys(ElementPlusIcons))

// 搜索表单
const searchForm = ref({
  name: '',
  type: '',
  status: ''
})

// 表格数据
const menuTable = ref(null)
const menuList = ref([])
const loading = ref(false)
const expandedKeys = ref([]) // 默认展开的节点
const tableHeight = ref('calc(100vh - 280px)') // 表格高度

// 计算菜单总数
const totalMenus = computed(() => {
  let count = 0
  const countMenus = (items) => {
    items.forEach(item => {
      count++
      if (item.children) {
        countMenus(item.children)
      }
    })
  }
  countMenus(menuList.value)
  return count
})

// 渲染动态图标的辅助函数
const renderIcon = (iconName) => {
  if (!iconName) return null
  return ElementPlusIcons[iconName]
}

// 菜单类型标签
const getMenuTypeTag = (type) => {
  const types = {
    'DIR': 'warning',
    'MENU': 'success'
  }
  return types[type] || 'info'
}

const getMenuTypeLabel = (type) => {
  const labels = {
    'DIR': '目录',
    'MENU': '菜单'
  }
  return labels[type] || type
}

// 搜索方法
const handleSearch = () => {
  fetchMenuTree()
}

const resetSearch = () => {
  searchForm.value = {
    name: '',
    type: '',
    status: ''
  }
  handleSearch()
}

// 展开/折叠方法
const expandAll = () => {
  // 获取所有节点的 id
  const keys = []
  const getKeys = (items) => {
    items.forEach(item => {
      keys.push(item.id)
      if (item.children) {
        getKeys(item.children)
      }
    })
  }
  getKeys(menuList.value)
  expandedKeys.value = keys
}

const collapseAll = () => {
  expandedKeys.value = []
}

// 刷新列表
const refreshList = () => {
  fetchMenuTree()
  ElMessage.success('刷新菜单列表')
}

// 状态切换
const handleStatusChange = async (row) => {
  try {
    const status = row.status
    const response = await axios.put(`/api/system/menus/status/${row.id}?status=${status}`)
    
    if (response.data && response.data.code === 0) {
      ElMessage.success(response.data.message || `${status === 1 ? '启用' : '禁用'}成功`)
    } else {
      ElMessage.error(response.data.message || '操作失败')
      // 回滚状态
      row.status = status === 1 ? 0 : 1
    }
  } catch (error) {
    console.error('更新菜单状态失败:', error)
    ElMessage.error('更新菜单状态失败')
    // 回滚状态
    row.status = row.status === 1 ? 0 : 1
  }
}

// 菜单编辑相关
const dialogVisible = ref(false)
const isEdit = ref(false)
const menuFormRef = ref(null)
const menuForm = ref({
  id: '',
  parentId: '0',
  type: 'MENU',
  name: '',
  sort: 0,
  icon: '',
  path: '',
  component: '',
  status: 1,
  remark: '',
  creator: '',
  updater: '',
  isExternal: 0,
  isCache: 0,
  isVisible: 1
})

// 表单验证规则
const menuRules = {
  name: [
    { required: true, message: '请输入菜单名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  path: [
    { required: true, message: '请输入路由地址', trigger: 'blur' }
  ],
  component: [
    { required: true, message: '请输入组件路径', trigger: 'blur', when: (rule, value) => menuForm.value.type === 'MENU' }
  ]
}

// 图标选择器相关
const showIconSelector = ref(false)
const iconSearchKeyword = ref('')
const tempSelectedIcon = ref('')

// 使用导入的所有图标替换原来的图标列表
const commonIcons = computed(() => {
  return allIcons.value
})

// 过滤图标
const filteredIcons = computed(() => {
  if (!iconSearchKeyword.value) {
    return commonIcons.value
  }
  return commonIcons.value.filter(icon => 
    icon.toLowerCase().includes(iconSearchKeyword.value.toLowerCase())
  )
})

// 选择图标
const selectIcon = (icon) => {
  tempSelectedIcon.value = icon
}

// 确认图标选择
const confirmIconSelection = () => {
  if (tempSelectedIcon.value) {
    menuForm.value.icon = tempSelectedIcon.value
  }
  showIconSelector.value = false
}

// 查找菜单
const findMenuById = (menus, id) => {
  for (const menu of menus) {
    if (menu.id === id) return menu
    if (menu.children) {
      const found = findMenuById(menu.children, id)
      if (found) return found
    }
  }
  return null
}

// 生成菜单选项
const menuOptions = computed(() => {
  const options = [{ id: '0', name: '主目录', children: [] }]
  const traverse = (menus) => {
    return menus.map(menu => ({
      id: menu.id,
      name: menu.name,
      children: menu.children ? traverse(menu.children) : undefined
    }))
  }
  options[0].children = traverse(menuList.value)
  return options
})

// 获取菜单树
const fetchMenuTree = async () => {
  loading.value = true
  try {
    // 判断是否有搜索条件
    const hasSearchCriteria = searchForm.value.name || searchForm.value.type || searchForm.value.status !== '';
    
    let response;
    if (hasSearchCriteria) {
      // 搜索条件查询 - 使用GET方法而不是POST
      const params = { ...searchForm.value };
      response = await axios.get('/api/system/menus/list', { params });
    } else {
      // 获取完整菜单树
      response = await axios.get('/api/system/menus/tree');
    }
    
    if (response.data && response.data.code === 0) {
      console.log('菜单数据:', response.data.data);
      
      // 确保所有节点的children属性都是数组
      const processTreeData = (items) => {
        if (!items) return [];
        
        return items.map(item => {
          // 将null的children转为空数组
          if (item.children === null) {
            item.children = [];
          }
          
          // 递归处理子节点
          if (Array.isArray(item.children)) {
            item.children = processTreeData(item.children);
          } else {
            item.children = [];
          }
          
          return item;
        });
      };
      
      if (hasSearchCriteria) {
        // 搜索模式 - 处理搜索结果
        menuList.value = processTreeData(response.data.data || []);
      } else {
        // 普通模式 - 处理树形结构
        menuList.value = processTreeData(response.data.data || []);
        
        // 默认展开第一级和第二级
        if (menuList.value.length > 0) {
          const firstLevelKeys = menuList.value.map(item => item.id);
          const secondLevelKeys = [];
          menuList.value.forEach(item => {
            if (item.children && item.children.length > 0) {
              secondLevelKeys.push(...item.children.map(child => child.id));
            }
          });
          expandedKeys.value = [...firstLevelKeys, ...secondLevelKeys];
        }
      }
    } else {
      ElMessage.error(response.data?.message || '获取菜单数据失败');
    }
  } catch (error) {
    console.error('获取菜单数据失败:', error);
    ElMessage.error('获取菜单数据失败');
  } finally {
    loading.value = false;
  }
}

// 修改原有的创建和编辑方法
const createMenu = (parent) => {
  isEdit.value = false
  menuForm.value = {
    id: '',
    parentId: parent ? parent.id : '0',
    type: parent ? 'MENU' : 'DIR',
    name: '',
    sort: 0,
    icon: '',
    path: '',
    component: '',
    status: 1,
    remark: '',
    creator: '',
    updater: '',
    isExternal: 0,
    isCache: 0,
    isVisible: 1
  }
  dialogVisible.value = true
}

const editMenu = async (row) => {
  isEdit.value = true
  try {
    // 获取菜单详情
    const response = await axios.get(`/api/system/menus/${row.id}`)
    
    if (response.data && response.data.code === 0) {
      menuForm.value = response.data.data
    } else {
      menuForm.value = { ...row }
      ElMessage.warning('获取菜单详情失败，使用默认数据')
    }
  } catch (error) {
    console.error('获取菜单详情失败:', error)
    menuForm.value = { ...row }
    ElMessage.warning('获取菜单详情失败，使用默认数据')
  }
  
  dialogVisible.value = true
}

// 提交表单
const submitForm = async () => {
  if (!menuFormRef.value) return
  
  await menuFormRef.value.validate(async (valid, fields) => {
    if (valid) {
      try {
        const formData = { ...menuForm.value }
        
        // 调用真实API
        const method = isEdit.value ? 'put' : 'post'
        const url = '/api/system/menus'
        
        const response = await axios({ method, url, data: formData })
        
        if (response.data && response.data.code === 0) {
          ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
          dialogVisible.value = false
          // 刷新菜单列表
          fetchMenuTree()
        } else {
          ElMessage.error(response.data.message || (isEdit.value ? '更新失败' : '创建失败'))
        }
      } catch (error) {
        console.error(isEdit.value ? '更新菜单失败:' : '创建菜单失败:', error)
        ElMessage.error(isEdit.value ? '更新菜单失败' : '创建菜单失败')
      }
    } else {
      console.log('表单验证失败:', fields)
    }
  })
}

// 删除菜单方法
const deleteMenu = (row) => {
  ElMessageBox.confirm(
    `确定要删除菜单 "${row.name}" 吗？${row.children?.length ? '该菜单下的子菜单也会被删除！' : ''}`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      // 调用真实API
      const response = await axios.delete(`/api/system/menus/${row.id}`)
      
      if (response.data && response.data.code === 0) {
        ElMessage.success('删除成功')
        // 刷新菜单列表
        fetchMenuTree()
      } else {
        ElMessage.error(response.data.message || '删除失败')
      }
    } catch (error) {
      console.error('删除菜单失败:', error)
      ElMessage.error('删除菜单失败')
    }
  }).catch(() => {})
}

// 表格行类名
const tableRowClassName = ({ row }) => {
  if (!row) return '';
  
  if (row.parentId === '0') {
    return 'level-1-row';
  } else {
    const parentId = row.parentId;
    // 判断父节点是否是顶级节点
    const parentIsTop = menuList.value.some(item => item.id === parentId);
    if (parentIsTop) {
      return 'level-2-row';
    }
    return 'level-3-row';
  }
}

// 获取菜单层级类型
const getMenuLevelType = (row) => {
  if (!row) return '';
  
  if (row.parentId === '0') {
    return 'primary';
  } else {
    const parentId = row.parentId;
    // 判断父节点是否是顶级节点
    const parentIsTop = menuList.value.some(item => item.id === parentId);
    if (parentIsTop) {
      return 'success';
    }
    return 'info';
  }
}

// 获取菜单层级前缀
const getMenuLevelPrefix = (row) => {
  if (!row) return '';
  
  if (row.parentId === '0') {
    return '一级';
  } else {
    const parentId = row.parentId;
    // 判断父节点是否是顶级节点
    const parentIsTop = menuList.value.some(item => item.id === parentId);
    if (parentIsTop) {
      return '二级';
    }
    return '三级';
  }
}

// 切换节点展开状态
const toggleExpand = (row, treeNode, store) => {
  store.toggleTreeExpansion(row);
}

// 获取节点缩进
const getPaddingLeft = (treeNode) => {
  return (treeNode.level * 18) + 'px';
}

// 生命周期钩子
onMounted(() => {
  // 加载初始数据
  fetchMenuTree()
})
</script>

<style scoped>
.menu-container {
  padding: 20px;
}

.menu-card {
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

/* 树形结构样式 */
.menu-name-cell {
  display: flex;
  align-items: center;
  position: relative;
  width: 100%;
  height: 100%;
}

.tree-spacer {
  display: flex;
  align-items: center;
  position: relative;
  height: 100%;
}

.tree-line {
  position: absolute;
  background-color: var(--el-border-color-darker);
}

.tree-line.vertical {
  width: 1px;
  height: 100%;
  left: 8px;
  top: -50%;
  z-index: 1;
}

.tree-line.horizontal {
  width: 16px;
  height: 1px;
  left: 9px;
  z-index: 1;
}

.expand-button {
  margin-right: 6px;
  position: relative;
  z-index: 2;
}

.leaf-node {
  width: 24px;
  height: 24px;
  display: inline-block;
  position: relative;
}

.leaf-node::after {
  content: '';
  position: absolute;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: var(--el-color-info-light-5);
  left: 9px;
  top: 9px;
}

.menu-icon {
  margin: 0 8px;
  padding: 5px;
  border-radius: 4px;
  background-color: rgba(var(--el-color-primary-rgb), 0.1);
  color: var(--el-color-primary);
}

.no-icon {
  width: 24px;
  margin: 0 8px;
}

.menu-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.menu-type-tag {
  min-width: 40px;
  text-align: center;
}

.menu-name {
  font-weight: 500;
}

/* 层级样式 */
.level-0 {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-color-primary);
}

.level-1 {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-color-success);
}

.level-2, .level-3 {
  font-size: 13px;
  color: var(--el-text-color-regular);
}

/* 行样式 */
:deep(.el-table__row) {
  transition: all 0.3s;
}

:deep(.el-table__row:hover) {
  background-color: var(--el-color-primary-light-9) !important;
}

.path-prefix {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  padding-right: 4px;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

.icon-container {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background-color: rgba(var(--el-color-primary-rgb), 0.1);
  border-radius: 4px;
}

.icon-selector {
  display: flex;
  align-items: center;
  gap: 10px;
}

.selected-icon {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 10px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  cursor: pointer;
  flex-grow: 1;
  height: 32px;
}

.preview-icon {
  color: var(--el-color-primary);
}

.no-icon {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.icon-name {
  color: var(--el-text-color-regular);
  font-size: 14px;
  margin-left: 4px;
}

.icon-search-wrapper {
  margin-bottom: 20px;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 15px;
  max-height: 350px;
  overflow-y: auto;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 80px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  padding: 10px;
}

.icon-item:hover {
  border-color: var(--el-color-primary);
  background-color: rgba(var(--el-color-primary-rgb), 0.1);
}

.icon-item.active {
  border-color: var(--el-color-primary);
  background-color: rgba(var(--el-color-primary-rgb), 0.2);
  box-shadow: 0 0 5px rgba(var(--el-color-primary-rgb), 0.3);
}

.icon-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background-color: rgba(var(--el-color-primary-rgb), 0.1);
  border-radius: 4px;
  margin-bottom: 8px;
}

.table-icon-container {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background-color: rgba(var(--el-color-primary-rgb), 0.1);
  border-radius: 4px;
  color: var(--el-color-primary);
}

.search-form :deep(.el-form-item) {
  margin-bottom: 18px;
  margin-right: 18px;
}

.search-form :deep(.el-input__wrapper),
.search-form :deep(.el-select__wrapper) {
  height: 36px;
}

.search-form :deep(.el-select .el-input) {
  width: 100%;
}
</style> 