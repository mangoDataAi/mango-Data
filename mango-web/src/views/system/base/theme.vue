<template>
  <div class="theme-container">
    <!-- 主题概览 -->
    <el-row :gutter="20" class="overview-section">
      <el-col :span="6" v-for="(item, index) in themeOverview" :key="index">
        <el-card shadow="hover" class="overview-card" :class="item.type">
          <div class="overview-content">
            <el-icon :size="40" :class="item.type">
              <component :is="item.icon" />
            </el-icon>
            <div class="overview-info">
              <div class="value">{{ item.value }}</div>
              <div class="label">{{ item.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 主题列表 -->
    <el-card class="theme-list">
      <template #header>
        <div class="list-header">
          <div class="header-left">
            <h3>主题管理</h3>
            <el-tag type="success" effect="dark">
              当前主题: {{ currentTheme }}
            </el-tag>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="createTheme">
              <el-icon><Plus /></el-icon> 新建主题
            </el-button>
            <el-button type="success" @click="importTheme">
              <el-icon><Upload /></el-icon> 导入主题
            </el-button>
            <el-button @click="refreshList">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 主题预览卡片 -->
      <el-row :gutter="20">
        <el-col :span="8" v-for="theme in themeList" :key="theme.id">
          <el-card 
            class="theme-card" 
            :class="{ active: theme.id === activeTheme }"
            shadow="hover"
          >
            <div class="theme-preview" :style="getThemeStyle(theme)">
              <!-- 预览头部 -->
              <div class="preview-header">
                <div class="preview-title">{{ theme.name }}</div>
                <div class="preview-menu">
                  <div class="menu-item" v-for="i in 3" :key="i"></div>
                </div>
              </div>
              <!-- 预览内容 -->
              <div class="preview-content">
                <div class="preview-chart"></div>
                <div class="preview-table">
                  <div class="table-row" v-for="i in 3" :key="i">
                    <div class="table-cell" v-for="j in 4" :key="j"></div>
                  </div>
                </div>
              </div>
            </div>
            <div class="theme-footer">
              <div class="theme-info">
                <span class="theme-name">{{ theme.name }}</span>
                <el-tag size="small" :type="theme.type === 'system' ? 'primary' : 'success'">
                  {{ theme.type === 'system' ? '系统主题' : '自定义主题' }}
                </el-tag>
              </div>
              <div class="theme-actions">
                <el-tooltip 
                  content="应用主题" 
                  placement="top" 
                  v-if="theme.id !== activeTheme"
                >
                  <el-button 
                    type="primary" 
                    link
                    :icon="Check"
                    @click="applyTheme(theme)"
                  />
                </el-tooltip>
                <el-tooltip content="编辑" placement="top">
                  <el-button 
                    type="info" 
                    link
                    :icon="Edit"
                    @click="editTheme(theme)"
                  />
                </el-tooltip>
                <el-tooltip content="导出" placement="top">
                  <el-button 
                    type="success" 
                    link
                    :icon="Download"
                    @click="exportTheme(theme)"
                  />
                </el-tooltip>
                <el-tooltip 
                  content="删除" 
                  placement="top"
                  v-if="theme.type !== 'system'"
                >
                  <el-button 
                    type="danger" 
                    link
                    :icon="Delete"
                    @click="deleteTheme(theme)"
                  />
                </el-tooltip>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 主题编辑对话框 -->
    <el-dialog
      v-model="editDialog"
      :title="isEdit ? '编辑主题' : '新建主题'"
      width="70%"
      :destroy-on-close="true"
    >
      <el-form :model="themeForm" :rules="themeRules" ref="themeFormRef" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="主题名称" prop="name">
              <el-input v-model="themeForm.name" placeholder="请输入主题名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主题类型" prop="type">
              <el-select v-model="themeForm.type" placeholder="请选择主题类型">
                <el-option label="系统主题" value="system" />
                <el-option label="自定义主题" value="custom" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 颜色配置 -->
        <el-card class="color-config">
          <template #header>
            <div class="config-header">
              <h4>颜色配置</h4>
              <el-radio-group v-model="colorMode" size="small">
                <el-radio-button label="basic">基础色</el-radio-button>
                <el-radio-button label="advanced">高级配置</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <!-- 基础颜色配置 -->
          <div v-if="colorMode === 'basic'" class="basic-colors">
            <el-row :gutter="20">
              <el-col :span="8" v-for="(color, key) in basicColors" :key="key">
                <div class="color-item">
                  <span class="color-label">{{ color.label }}</span>
                  <el-color-picker 
                    v-model="themeForm.colors[key]" 
                    :predefine="color.predefine"
                    show-alpha
                  />
                  <span class="color-value">{{ themeForm.colors[key] }}</span>
                </div>
              </el-col>
            </el-row>
          </div>

          <!-- 高级颜色配置 -->
          <div v-else class="advanced-colors">
            <el-tabs v-model="activeColorTab">
              <el-tab-pane label="品牌色" name="brand">
                <div class="color-palette">
                  <div class="primary-color">
                    <el-color-picker 
                      v-model="themeForm.colors.primary" 
                      show-alpha
                      @change="generatePalette"
                    />
                    <span>主色</span>
                  </div>
                  <div class="color-variants">
                    <div 
                      v-for="(variant, index) in colorPalette" 
                      :key="index"
                      class="variant-item"
                      :style="{ backgroundColor: variant }"
                    >
                      <span class="variant-label">{{ index * 10 }}%</span>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
              <el-tab-pane label="功能色" name="function">
                <el-row :gutter="20">
                  <el-col :span="6" v-for="(color, key) in functionColors" :key="key">
                    <div class="color-item">
                      <span class="color-label">{{ color.label }}</span>
                      <el-color-picker 
                        v-model="themeForm.colors[key]" 
                        :predefine="color.predefine"
                        show-alpha
                      />
                    </div>
                  </el-col>
                </el-row>
              </el-tab-pane>
              <el-tab-pane label="中性色" name="neutral">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <div class="color-item">
                      <span class="color-label">背景色</span>
                      <el-color-picker 
                        v-model="themeForm.colors.background" 
                        show-alpha
                      />
                    </div>
                  </el-col>
                  <el-col :span="12">
                    <div class="color-item">
                      <span class="color-label">文字颜色</span>
                      <el-color-picker 
                        v-model="themeForm.colors.text" 
                        show-alpha
                      />
                    </div>
                  </el-col>
                </el-row>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-card>

        <!-- 主题预览 -->
        <el-card class="theme-preview-card">
          <template #header>
            <h4>主题预览</h4>
          </template>
          <div class="preview-components" :style="getThemeStyle(themeForm)">
            <!-- 按钮预览 -->
            <div class="preview-section">
              <div class="section-title">按钮</div>
              <div class="button-preview">
                <el-button type="primary">主要按钮</el-button>
                <el-button type="success">成功按钮</el-button>
                <el-button type="warning">警告按钮</el-button>
                <el-button type="danger">危险按钮</el-button>
              </div>
            </div>
            <!-- 标签预览 -->
            <div class="preview-section">
              <div class="section-title">标签</div>
              <div class="tag-preview">
                <el-tag type="primary">主要标签</el-tag>
                <el-tag type="success">成功标签</el-tag>
                <el-tag type="warning">警告标签</el-tag>
                <el-tag type="danger">危险标签</el-tag>
              </div>
            </div>
            <!-- 表单预览 -->
            <div class="preview-section">
              <div class="section-title">表单</div>
              <div class="form-preview">
                <el-input placeholder="输入框" />
                <el-select placeholder="选择框">
                  <el-option label="选项1" value="1" />
                  <el-option label="选项2" value="2" />
                </el-select>
                <el-switch v-model="previewSwitch" />
              </div>
            </div>
          </div>
        </el-card>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editDialog = false">取消</el-button>
          <el-button type="primary" @click="submitTheme">
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 主题导入对话框 -->
    <el-dialog
      v-model="importDialog"
      title="导入主题"
      width="500px"
      :destroy-on-close="true"
    >
      <el-form :model="importForm" ref="importFormRef" label-width="80px">
        <el-form-item label="主题文件">
          <el-upload
            class="theme-upload"
            drag
            action="#"
            :auto-upload="false"
            :on-change="handleFileChange"
            accept=".json"
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                只能上传 JSON 文件
              </div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="主题名称" v-if="importForm.file">
          <el-input v-model="importForm.name" placeholder="请输入主题名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="importDialog = false">取消</el-button>
          <el-button type="primary" @click="confirmImport" :disabled="!importForm.file">
            导入
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 全局配置抽屉 -->
    <el-drawer
      v-model="configDrawer"
      title="全局配置"
      size="500px"
      :destroy-on-close="true"
    >
      <el-form :model="configForm" label-position="top">
        <!-- 主题切换设置 -->
        <el-card class="config-card">
          <template #header>
            <h4>主题切换</h4>
          </template>
          <el-form-item label="自动切换">
            <el-switch
              v-model="configForm.autoSwitch"
              @change="handleAutoSwitch"
            />
          </el-form-item>
          <template v-if="configForm.autoSwitch">
            <el-form-item label="切换模式">
              <el-radio-group v-model="configForm.switchMode">
                <el-radio-button label="time">时间</el-radio-button>
                <el-radio-button label="system">跟随系统</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <template v-if="configForm.switchMode === 'time'">
              <el-form-item label="日间主题">
                <el-select v-model="configForm.dayTheme" placeholder="选择日间主题">
                  <el-option
                    v-for="theme in themeList"
                    :key="theme.id"
                    :label="theme.name"
                    :value="theme.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="夜间主题">
                <el-select v-model="configForm.nightTheme" placeholder="选择夜间主题">
                  <el-option
                    v-for="theme in themeList"
                    :key="theme.id"
                    :label="theme.name"
                    :value="theme.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="切换时间">
                <el-time-picker
                  v-model="configForm.dayStartTime"
                  placeholder="日间开始时间"
                  format="HH:mm"
                />
                <span class="time-separator">至</span>
                <el-time-picker
                  v-model="configForm.dayEndTime"
                  placeholder="日间结束时间"
                  format="HH:mm"
                />
              </el-form-item>
            </template>
          </template>
        </el-card>

        <!-- 动画效果设置 -->
        <el-card class="config-card">
          <template #header>
            <h4>动画效果</h4>
          </template>
          <el-form-item label="切换动画">
            <el-switch
              v-model="configForm.enableTransition"
              @change="handleTransition"
            />
          </el-form-item>
          <el-form-item label="动画时长" v-if="configForm.enableTransition">
            <el-slider
              v-model="configForm.transitionDuration"
              :min="100"
              :max="1000"
              :step="100"
              :format-tooltip="value => `${value}ms`"
            />
          </el-form-item>
        </el-card>

        <!-- 主题缓存设置 -->
        <el-card class="config-card">
          <template #header>
            <div class="config-header">
              <h4>主题缓存</h4>
              <el-button type="danger" link @click="clearCache">
                清除缓存
              </el-button>
            </div>
          </template>
          <el-form-item label="本地缓存">
            <el-switch
              v-model="configForm.enableCache"
              @change="handleCache"
            />
          </el-form-item>
          <el-form-item label="缓存大小" v-if="configForm.enableCache">
            <el-input-number
              v-model="configForm.cacheSize"
              :min="1"
              :max="10"
              :step="1"
              :controls="false"
            >
              <template #suffix>MB</template>
            </el-input-number>
          </el-form-item>
        </el-card>
      </el-form>

      <template #footer>
        <div class="drawer-footer">
          <el-button @click="resetConfig">恢复默认</el-button>
          <el-button type="primary" @click="saveConfig">保存配置</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import {
  Plus, Upload, Refresh, Check,
  Edit, Download, Delete, Brush,
  Monitor, Setting, UploadFilled,
  PictureFilled
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 主题概览数据
const themeOverview = ref([
  {
    label: '主题总数',
    value: 8,
    type: 'primary',
    icon: 'Brush'
  },
  {
    label: '自定义主题',
    value: 5,
    type: 'success',
    icon: 'PictureFilled'
  },
  {
    label: '系统主题',
    value: 3,
    type: 'warning',
    icon: 'Setting'
  },
  {
    label: '主题使用量',
    value: '1.2k',
    type: 'info',
    icon: 'Monitor'
  }
])

// 主题列表数据
const themeList = ref([
  {
    id: 1,
    name: '默认主题',
    type: 'system',
    colors: {
      primary: '#409EFF',
      success: '#67C23A',
      warning: '#E6A23C',
      danger: '#F56C6C',
      background: '#ffffff',
      text: '#303133'
    }
  },
  {
    id: 2,
    name: '暗黑主题',
    type: 'system',
    colors: {
      primary: '#409EFF',
      success: '#67C23A',
      warning: '#E6A23C',
      danger: '#F56C6C',
      background: '#1f1f1f',
      text: '#ffffff'
    }
  },
  {
    id: 3,
    name: '清新绿',
    type: 'custom',
    colors: {
      primary: '#42b983',
      success: '#67C23A',
      warning: '#E6A23C',
      danger: '#F56C6C',
      background: '#f0f9eb',
      text: '#303133'
    }
  }
])

const currentTheme = ref('默认主题')
const activeTheme = ref(1)

// 获取主题样式
const getThemeStyle = (theme) => {
  return {
    '--theme-primary': theme.colors.primary,
    '--theme-success': theme.colors.success,
    '--theme-warning': theme.colors.warning,
    '--theme-danger': theme.colors.danger,
    '--theme-background': theme.colors.background,
    '--theme-text': theme.colors.text
  }
}

// 主题操作方法
const createTheme = () => {
  isEdit.value = false
  themeForm.value = {
    name: '',
    type: 'custom',
    colors: {
      primary: '#409EFF',
      success: '#67C23A',
      warning: '#E6A23C',
      danger: '#F56C6C',
      background: '#ffffff',
      text: '#303133'
    }
  }
  editDialog.value = true
  generatePalette('#409EFF')
}

// 导入相关
const importDialog = ref(false)
const importFormRef = ref(null)
const importForm = ref({
  file: null,
  name: ''
})

const handleFileChange = (file) => {
  importForm.value.file = file
  // 从文件名中提取主题名称
  importForm.value.name = file.name.replace('.json', '')
}

const confirmImport = () => {
  if (!importForm.value.file) {
    ElMessage.warning('请选择主题文件')
    return
  }
  
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const theme = JSON.parse(e.target.result)
      // 验证主题格式
      if (!theme.colors || !theme.name) {
        throw new Error('无效的主题文件格式')
      }
      ElMessage.success('导入主题成功')
      importDialog.value = false
      refreshList()
    } catch (error) {
      ElMessage.error('主题文件格式错误')
    }
  }
  reader.readAsText(importForm.value.file.raw)
}

const refreshList = () => {
  ElMessage.success('刷新主题列表')
}

const applyTheme = (theme) => {
  ElMessageBox.confirm(
    `确定要应用主题 "${theme.name}" 吗？`,
    '应用确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(() => {
    activeTheme.value = theme.id
    currentTheme.value = theme.name
    ElMessage.success(`已应用主题: ${theme.name}`)
  })
}

const editTheme = (theme) => {
  isEdit.value = true
  themeForm.value = JSON.parse(JSON.stringify(theme))
  editDialog.value = true
  generatePalette(theme.colors.primary)
}

const exportTheme = (theme) => {
  ElMessage.success(`导出主题: ${theme.name}`)
}

const deleteTheme = (theme) => {
  ElMessageBox.confirm(
    `确定要删除主题 "${theme.name}" 吗？`,
    '删除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success(`删除主题: ${theme.name}`)
  })
}

// 主题编辑相关
const editDialog = ref(false)
const isEdit = ref(false)
const themeFormRef = ref(null)
const colorMode = ref('basic')
const activeColorTab = ref('brand')
const previewSwitch = ref(false)

const themeForm = ref({
  name: '',
  type: 'custom',
  colors: {
    primary: '#409EFF',
    success: '#67C23A',
    warning: '#E6A23C',
    danger: '#F56C6C',
    background: '#ffffff',
    text: '#303133'
  }
})

const themeRules = {
  name: [
    { required: true, message: '请输入主题名称', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择主题类型', trigger: 'change' }
  ]
}

// 基础颜色配置
const basicColors = {
  primary: {
    label: '主要颜色',
    predefine: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C']
  },
  success: {
    label: '成功颜色',
    predefine: ['#67C23A', '#85ce61', '#4CAF50', '#2E7D32']
  },
  warning: {
    label: '警告颜色',
    predefine: ['#E6A23C', '#F5A623', '#FF9800', '#EF6C00']
  },
  danger: {
    label: '危险颜色',
    predefine: ['#F56C6C', '#ff4949', '#F44336', '#D32F2F']
  },
  background: {
    label: '背景颜色',
    predefine: ['#ffffff', '#f5f7fa', '#1f1f1f', '#000000']
  },
  text: {
    label: '文字颜色',
    predefine: ['#303133', '#606266', '#ffffff', '#000000']
  }
}

// 功能色配置
const functionColors = {
  success: { label: '成功' },
  warning: { label: '警告' },
  danger: { label: '危险' },
  info: { label: '信息' }
}

// 色板生成
const colorPalette = ref([])
const generatePalette = (color) => {
  const base = color.replace('#', '')
  const r = parseInt(base.substr(0, 2), 16)
  const g = parseInt(base.substr(2, 2), 16)
  const b = parseInt(base.substr(4, 2), 16)
  
  colorPalette.value = Array.from({ length: 10 }, (_, i) => {
    const factor = i / 10
    const newR = Math.round(r + (255 - r) * factor)
    const newG = Math.round(g + (255 - g) * factor)
    const newB = Math.round(b + (255 - b) * factor)
    return `#${newR.toString(16).padStart(2, '0')}${newG.toString(16).padStart(2, '0')}${newB.toString(16).padStart(2, '0')}`
  })
}

// 提交主题
const submitTheme = async () => {
  if (!themeFormRef.value) return
  
  await themeFormRef.value.validate((valid, fields) => {
    if (valid) {
      ElMessage.success(`${isEdit.value ? '更新' : '创建'}主题成功`)
      editDialog.value = false
      refreshList()
    } else {
      console.log('验证失败:', fields)
    }
  })
}

// 全局配置相关
const configDrawer = ref(false)
const configForm = ref({
  // 主题切换
  autoSwitch: false,
  switchMode: 'time',
  dayTheme: 1,
  nightTheme: 2,
  dayStartTime: new Date(2000, 0, 1, 6, 0),
  dayEndTime: new Date(2000, 0, 1, 18, 0),
  // 动画效果
  enableTransition: true,
  transitionDuration: 300,
  // 缓存设置
  enableCache: true,
  cacheSize: 5
})

const handleAutoSwitch = (value) => {
  if (value) {
    ElMessage.success('已开启自动切换主题')
  }
}

const handleTransition = (value) => {
  if (!value) {
    ElMessage.warning('关闭动画可能影响用户体验')
  }
}

const handleCache = (value) => {
  if (!value) {
    ElMessage.warning('关闭缓存可能影响加载速度')
  }
}

const clearCache = () => {
  ElMessageBox.confirm(
    '确定要清除主题缓存吗？这可能会影响加载速度。',
    '清除确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success('已清除主题缓存')
  })
}

const resetConfig = () => {
  ElMessageBox.confirm(
    '确定要恢复默认配置吗？',
    '恢复确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    configForm.value = {
      autoSwitch: false,
      switchMode: 'time',
      dayTheme: 1,
      nightTheme: 2,
      dayStartTime: new Date(2000, 0, 1, 6, 0),
      dayEndTime: new Date(2000, 0, 1, 18, 0),
      enableTransition: true,
      transitionDuration: 300,
      enableCache: true,
      cacheSize: 5
    }
    ElMessage.success('已恢复默认配置')
  })
}

const saveConfig = () => {
  ElMessage.success('保存配置成功')
  configDrawer.value = false
}

// 修改原有的导入方法
const importTheme = () => {
  importDialog.value = true
}
</script>

<style scoped>
.theme-container {
  padding: 20px;
}

.overview-section {
  margin-bottom: 20px;
}

.overview-card {
  transition: all 0.3s;
}

.overview-card:hover {
  transform: translateY(-5px);
}

.overview-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.overview-info {
  margin: 16px 0;
}

.overview-info .value {
  font-size: 24px;
  font-weight: bold;
  line-height: 1;
  margin-bottom: 8px;
}

.overview-info .label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.theme-list {
  margin-bottom: 20px;
}

.list-header {
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

.theme-card {
  margin-bottom: 20px;
  transition: all 0.3s;
}

.theme-card:hover {
  transform: translateY(-5px);
}

.theme-card.active {
  border: 2px solid var(--el-color-primary);
}

.theme-preview {
  height: 200px;
  border-radius: 4px;
  overflow: hidden;
  background-color: var(--theme-background);
  color: var(--theme-text);
}

.preview-header {
  padding: 12px;
  background-color: var(--theme-primary);
  color: #fff;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.preview-menu {
  display: flex;
  gap: 8px;
}

.menu-item {
  width: 20px;
  height: 4px;
  background-color: rgba(255, 255, 255, 0.7);
  border-radius: 2px;
}

.preview-content {
  padding: 12px;
}

.preview-chart {
  height: 60px;
  background: linear-gradient(90deg, 
    var(--theme-primary) 25%, 
    var(--theme-success) 50%, 
    var(--theme-warning) 75%, 
    var(--theme-danger) 100%
  );
  border-radius: 4px;
  margin-bottom: 12px;
}

.preview-table {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.table-row {
  display: flex;
  gap: 8px;
}

.table-cell {
  flex: 1;
  height: 12px;
  background-color: rgba(128, 128, 128, 0.1);
  border-radius: 2px;
}

.theme-footer {
  margin-top: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.theme-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.theme-name {
  font-weight: bold;
}

.theme-actions {
  display: flex;
  gap: 8px;
}

.el-icon {
  &.primary {
    color: var(--el-color-primary);
  }
  &.success {
    color: var(--el-color-success);
  }
  &.warning {
    color: var(--el-color-warning);
  }
  &.info {
    color: var(--el-color-info);
  }
}

.color-config {
  margin: 20px 0;
}

.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.config-header h4 {
  margin: 0;
}

.basic-colors {
  padding: 20px 0;
}

.color-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.color-label {
  width: 80px;
  color: var(--el-text-color-regular);
}

.color-value {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.color-palette {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 20px 0;
}

.primary-color {
  display: flex;
  align-items: center;
  gap: 12px;
}

.color-variants {
  display: flex;
  gap: 8px;
}

.variant-item {
  flex: 1;
  height: 40px;
  border-radius: 4px;
  position: relative;
  cursor: pointer;
  transition: transform 0.2s;
}

.variant-item:hover {
  transform: translateY(-2px);
}

.variant-label {
  position: absolute;
  bottom: 4px;
  left: 4px;
  font-size: 12px;
  color: #fff;
  text-shadow: 0 0 2px rgba(0, 0, 0, 0.5);
}

.theme-preview-card {
  margin: 20px 0;
}

.preview-components {
  padding: 20px;
  background-color: var(--theme-background);
  border-radius: 4px;
}

.preview-section {
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }
}

.section-title {
  margin-bottom: 12px;
  color: var(--theme-text);
  font-weight: bold;
}

.button-preview,
.tag-preview,
.form-preview {
  display: flex;
  gap: 12px;
  align-items: center;
}

.form-preview {
  max-width: 600px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.theme-upload {
  width: 100%;
}

.config-card {
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
}

.time-separator {
  margin: 0 12px;
  color: var(--el-text-color-secondary);
}

.drawer-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px;
  background-color: var(--el-bg-color);
  border-top: 1px solid var(--el-border-color-lighter);
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

:deep(.el-drawer__body) {
  padding: 20px;
  padding-bottom: 70px;
}
</style> 