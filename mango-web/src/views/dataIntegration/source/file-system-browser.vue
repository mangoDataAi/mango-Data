<template>
  <div 
    class="file-system-browser"
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
            <div class="current-path">
              <el-breadcrumb separator="/">
                <el-breadcrumb-item @click="navigateToRoot">根目录</el-breadcrumb-item>
                <el-breadcrumb-item v-for="(part, index) in currentPathParts" :key="index" 
                  @click="navigateToPath(index)">{{ part }}</el-breadcrumb-item>
              </el-breadcrumb>
            </div>
          </div>
          <div class="header-right">
            <el-button-group>
              <el-button type="primary" @click="refreshDirectory">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
              <el-button type="primary" @click="showUploadDialog">
                <el-icon><Upload /></el-icon>
                上传
              </el-button>
              <el-button type="primary" @click="handleCreateDirectory">
                <el-icon><FolderAdd /></el-icon>
                新建文件夹
              </el-button>
            </el-button-group>
          </div>
        </div>
      </template>

      <div class="content-wrapper">
        <!-- 左侧目录树 -->
        <div class="left-area">
          <div class="search-area">
            <el-input
              v-model="searchText"
              placeholder="搜索文件/文件夹"
              :prefix-icon="Search"
              size="default"
              clearable
              @clear="handleSearch"
              @input="handleSearch"
            />
          </div>
          <div class="directory-tree-area">
            <el-scrollbar>
              <el-tree
                ref="treeRef"
                :data="directoryTree"
                :props="{ 
                  label: 'name', 
                  children: 'children', 
                  isLeaf: (data) => !data.isDirectory,
                  disabled: false
                }"
                node-key="path"
                :load="loadNode"
                lazy
                :expand-on-click-node="false"
                :check-strictly="true"
                check-on-click-node
                :filter-node-method="filterNode"
                @node-click="handleNodeClick"
                highlight-current
              >
                <template #default="{ node, data }">
                  <div class="tree-node">
                    <el-icon v-if="data.isDirectory">
                      <Folder v-if="!node.expanded" />
                      <FolderOpened v-else />
                    </el-icon>
                    <el-icon v-else>
                      <Document />
                    </el-icon>
                    <span class="node-label">{{ data.name }}</span>
                  </div>
                </template>
              </el-tree>
            </el-scrollbar>
          </div>
        </div>

        <!-- 中间文件/文件夹列表 -->
        <div class="file-list-area">
          <div class="file-view-controls">
            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button label="list">
                <el-icon><Menu /></el-icon>
              </el-radio-button>
              <el-radio-button label="grid">
                <el-icon><Grid /></el-icon>
              </el-radio-button>
            </el-radio-group>
          </div>
          
          <!-- 列表视图 -->
          <el-table
            v-if="viewMode === 'list'"
            :data="filteredDirectoryContents"
            style="width: 100%"
            @row-dblclick="(row, column, event) => handleFileDoubleClick(row, event)"
          >
            <el-table-column label="名称" min-width="250">
              <template #default="{ row }">
                <div class="file-item">
                  <el-icon v-if="row.isDirectory">
                    <Folder />
                  </el-icon>
                  <el-icon v-else>
                    <Document />
                  </el-icon>
                  <span>{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="size" label="大小" width="120">
              <template #default="{ row }">
                {{ row.isDirectory ? '-' : formatFileSize(row.size) }}
              </template>
            </el-table-column>
            <el-table-column prop="modificationTime" label="修改时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.modificationTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button-group>
                  <el-button v-if="row.isDirectory" type="primary" @click="navigateToDirectory(row)" size="small">
                    <el-icon><Right /></el-icon>
                  </el-button>
                  <el-button v-else type="success" @click="(event) => previewFile(row, event)" size="small">
                    <el-icon><View /></el-icon>
                  </el-button>
                  <el-button type="primary" @click="(event) => downloadFile(row, event)" size="small">
                    <el-icon><Download /></el-icon>
                  </el-button>
                  <el-button type="danger" @click="deleteFile(row)" size="small">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </el-button-group>
              </template>
            </el-table-column>
          </el-table>
          
          <!-- 网格视图 -->
          <div v-else class="grid-view">
            <el-scrollbar>
              <div class="grid-container">
                <div v-for="item in filteredDirectoryContents" :key="item.path" class="grid-item"
                  @dblclick="(event) => handleFileDoubleClick(item, event)"
                  @click="(event) => selectItem(item, event)"
                  :class="{ 'selected': selectedItem && selectedItem.path === item.path }"
                >
                  <div class="grid-icon-wrapper" :class="getFileIconClass(item)">
                    <el-icon v-if="item.isDirectory" class="grid-icon">
                      <Folder />
                    </el-icon>
                    <el-icon v-else-if="isImageFile(item.name)" class="grid-icon">
                      <Picture />
                    </el-icon>
                    <el-icon v-else-if="isTextFile(item.name)" class="grid-icon">
                      <Document />
                    </el-icon>
                    <el-icon v-else-if="isArchiveFile(item.name)" class="grid-icon">
                      <Files />
                    </el-icon>
                    <el-icon v-else-if="isCodeFile(item.name)" class="grid-icon">
                      <Edit />
                    </el-icon>
                    <el-icon v-else-if="isPdfFile(item.name)" class="grid-icon">
                      <Tickets />
                    </el-icon>
                    <el-icon v-else-if="isVideoFile(item.name)" class="grid-icon">
                      <VideoPlay />
                    </el-icon>
                    <el-icon v-else-if="isAudioFile(item.name)" class="grid-icon">
                      <Headset />
                    </el-icon>
                    <el-icon v-else class="grid-icon">
                      <Document />
                    </el-icon>
                  </div>
                  <div class="grid-item-name">{{ item.name }}</div>
                  <div class="grid-item-info">{{ formatFileSize(item.size) }}</div>
                </div>
              </div>
            </el-scrollbar>
          </div>
        </div>

        <!-- 右侧文件详情/预览 -->
        <div class="file-preview-area">
          <template v-if="selectedItem">
            <div class="preview-header">
              <h3>{{ selectedItem.name }}</h3>
            </div>
            <div class="preview-details">
              <el-descriptions :column="1" border>
                <el-descriptions-item label="路径">{{ selectedItem.path }}</el-descriptions-item>
                <el-descriptions-item label="类型">{{ selectedItem.isDirectory ? '文件夹' : getFileType(selectedItem.name) }}</el-descriptions-item>
                <el-descriptions-item label="大小">{{ selectedItem.isDirectory ? '-' : formatFileSize(selectedItem.size) }}</el-descriptions-item>
                <el-descriptions-item label="修改时间">{{ formatDate(selectedItem.lastModified) }}</el-descriptions-item>
                <el-descriptions-item label="权限">{{ selectedItem.permissions || '-' }}</el-descriptions-item>
              </el-descriptions>
            </div>
            <div class="preview-actions">
              <el-button-group>
                <el-button v-if="selectedItem.isDirectory" type="primary" @click="navigateToDirectory(selectedItem)">
                  <el-icon><FolderOpened /></el-icon>
                  打开
                </el-button>
                <el-button v-else-if="isPreviewable(selectedItem.name)" type="success" @click="(event) => previewFile(selectedItem, event)">
                  <el-icon><View /></el-icon>
                  预览
                </el-button>
                <el-button type="primary" @click="(event) => downloadFile(selectedItem, event)">
                  <el-icon><Download /></el-icon>
                  下载
                </el-button>
                <el-button type="danger" @click="deleteFile(selectedItem)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </el-button-group>
            </div>
            <div v-if="!selectedItem.isDirectory && previewContent" class="file-content-preview">
              <h4>内容预览</h4>
              <div v-if="isImageFile(selectedItem.name)" class="image-preview">
                <img :src="previewContent" :alt="selectedItem.name">
              </div>
              <div v-else-if="isTextFile(selectedItem.name)" class="text-preview">
                <pre>{{ previewContent }}</pre>
              </div>
              <div v-else-if="previewContent && previewContent.type === 'pdf'" class="pdf-preview">
                <div class="preview-options">
                  <el-button type="primary" size="small" @click="openInNewWindow(previewContent.url)">
                    <el-icon><View /></el-icon> 在新窗口中打开
                  </el-button>
                  <el-button type="success" size="small" @click="(event) => downloadFile(selectedItem, event)">
                    <el-icon><Download /></el-icon> 下载PDF
                  </el-button>
                  <el-button type="warning" size="small" @click="tryGoogleViewerPreview(previewContent.url)">
                    <el-icon><View /></el-icon> 使用Google预览
                  </el-button>
                </div>
                <div class="pdf-container">
                  <iframe 
                    :src="previewContent.googleViewerUrl || previewContent.url"
                    width="100%" 
                    height="400" 
                    frameborder="0"
                  ></iframe>
                  <div class="pdf-fallback">
                    <p>无法预览PDF?</p> 
                    <p>1. <a :href="previewContent.url" target="_blank">点击此处</a>直接查看</p>
                    <p>2. <a :href="previewContent.url" download>下载PDF</a>后查看</p>
                  </div>
                </div>
              </div>
              <div v-else-if="previewContent && previewContent.type === 'video'" class="video-preview">
                <video controls width="100%" height="300">
                  <source :src="previewContent.url" :type="`video/${selectedItem.name.split('.').pop().toLowerCase()}`">
                  您的浏览器不支持视频播放
                </video>
              </div>
              <div v-else-if="previewContent && previewContent.type === 'audio'" class="audio-preview">
                <audio controls style="width: 100%">
                  <source :src="previewContent.url" :type="`audio/${selectedItem.name.split('.').pop().toLowerCase()}`">
                  您的浏览器不支持音频播放
                </audio>
              </div>
              <div v-else-if="previewContent && previewContent.type === 'html'" class="html-preview">
                <a :href="previewContent.url" target="_blank" class="el-button el-button--primary">
                  在新窗口中打开HTML
                </a>
              </div>
              <div v-else class="no-preview">
                <el-empty description="无法预览此类型的文件" />
              </div>
            </div>
          </template>
          <div v-else class="no-item-selected">
            <el-empty description="请选择文件或文件夹查看详情" />
          </div>
        </div>
      </div>
    </el-card>

    <!-- 上传文件对话框 -->
    <el-dialog v-model="uploadDialogVisible" title="上传文件" width="500px">
      <el-upload
        ref="uploadRef"
        :action="uploadActionUrl"
        :headers="uploadHeaders"
        :data="uploadData"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        :before-upload="beforeUpload"
        :with-credentials="true"
        multiple
        drag
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处或 <em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">
            正在上传到: {{ currentPath }}
          </div>
        </template>
      </el-upload>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="uploadDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitUpload">上传</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 新建文件夹对话框 -->
    <el-dialog v-model="newDirDialogVisible" title="新建文件夹" width="400px">
      <el-form :model="newDirForm" label-width="80px">
        <el-form-item label="文件夹名">
          <el-input v-model="newDirForm.name" placeholder="请输入文件夹名称"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="newDirDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitNewDir">创建</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 文件预览对话框 -->
    <el-dialog v-model="previewDialogVisible" :title="previewTitle" width="80%" :fullscreen="isFullscreenPreview">
      <div class="preview-dialog-content">
        <div v-if="isImageFile(currentPreviewFile.name)" class="image-preview-dialog">
          <img :src="previewContent" :alt="currentPreviewFile.name">
        </div>
        <div v-else-if="isTextFile(currentPreviewFile.name)" class="text-preview-dialog">
          <pre>{{ previewContent }}</pre>
        </div>
        <div v-else-if="previewContent && previewContent.type === 'pdf'" class="pdf-preview-dialog">
          <div class="preview-options">
            <el-button type="primary" size="small" @click="openInNewWindow(previewContent.url)">
              <el-icon><View /></el-icon> 在新窗口中打开
            </el-button>
            <el-button type="success" size="small" @click="(event) => downloadFile(currentPreviewFile, event)">
              <el-icon><Download /></el-icon> 下载PDF
            </el-button>
            <el-button type="warning" size="small" @click="tryGoogleViewerPreview(previewContent.url)">
              <el-icon><View /></el-icon> 使用Google预览
            </el-button>
          </div>
          <div class="pdf-container">
            <iframe 
              :src="previewContent.googleViewerUrl || previewContent.url"
              width="100%" 
              height="580" 
              frameborder="0"
            ></iframe>
            <div class="pdf-fallback">
              <p>无法预览PDF?</p> 
              <p>1. <a :href="previewContent.url" target="_blank">点击此处</a>直接查看</p>
              <p>2. <a :href="previewContent.url" download>下载PDF</a>后查看</p>
            </div>
          </div>
        </div>
        <div v-else-if="previewContent && previewContent.type === 'video'" class="video-preview-dialog">
          <video controls width="100%" height="500">
            <source :src="previewContent.url" :type="`video/${currentPreviewFile.name.split('.').pop().toLowerCase()}`">
            您的浏览器不支持视频播放
          </video>
        </div>
        <div v-else-if="previewContent && previewContent.type === 'audio'" class="audio-preview-dialog">
          <audio controls style="width: 100%">
            <source :src="previewContent.url" :type="`audio/${currentPreviewFile.name.split('.').pop().toLowerCase()}`">
            您的浏览器不支持音频播放
          </audio>
        </div>
        <div v-else-if="previewContent && previewContent.type === 'html'" class="html-preview-dialog">
          <a :href="previewContent.url" target="_blank" class="el-button el-button--primary">
            在新窗口中打开HTML
          </a>
        </div>
        <div v-else class="no-preview-dialog">
          <el-empty description="无法预览此类型的文件" />
        </div>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="toggleFullscreenPreview">
            {{ isFullscreenPreview ? '退出全屏' : '全屏预览' }}
          </el-button>
          <el-button type="primary" @click="(event) => downloadFile(currentPreviewFile, event)">下载</el-button>
          <el-button @click="previewDialogVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  ArrowLeft, Menu, Grid, Folder, FolderOpened, Document, 
  Search, Refresh, Upload, FolderAdd, View, Download, 
  Delete, Right, UploadFilled, Picture, Files, Edit,
  Tickets, VideoPlay, Headset
} from '@element-plus/icons-vue'

// API 导入
import { 
  getFileSystemContents, 
  createDirectory as apiCreateDirectory, 
  deleteFileOrDirectory, 
  uploadFile,
  getFileContents
} from '@/api/filesystem'

const router = useRouter()
const route = useRoute()
const fullscreenLoading = ref(false)
const loadingText = ref('加载中...')

// 视图状态
const viewMode = ref('list')
const searchText = ref('')
const treeRef = ref(null)
const uploadRef = ref(null)

// 当前路径
const currentPath = ref('/')
const currentPathParts = computed(() => {
  return currentPath.value.split('/').filter(Boolean)
})

// 目录树
const directoryTree = ref([])
// 当前目录内容
const directoryContents = ref([])
const filteredDirectoryContents = computed(() => {
  if (!searchText.value) return directoryContents.value
  
  const lowercaseSearch = searchText.value.toLowerCase()
  return directoryContents.value.filter(item => 
    item.name.toLowerCase().includes(lowercaseSearch)
  )
})

// 选中的项目
const selectedItem = ref(null)

// 预览
const previewContent = ref(null)
const previewDialogVisible = ref(false)
const previewTitle = computed(() => currentPreviewFile.value ? `预览: ${currentPreviewFile.value.name}` : '文件预览')
const currentPreviewFile = ref(null)
const isFullscreenPreview = ref(false)
const previewLoading = ref(false)

// 上传
const uploadDialogVisible = ref(false)
const uploadActionUrl = computed(() => {
  // 确保路径不包含多余的逗号或特殊字符，避免路径重复
  const cleanPath = currentPath.value.replace(/,/g, '').replace(/\/+/g, '/');
  return `/api/filesystem/${route.params.id}/upload?path=${encodeURIComponent(cleanPath)}`
})
const uploadHeaders = computed(() => {
  return {
    Authorization: `Bearer ${localStorage.getItem('token')}`
  }
})
const uploadData = computed(() => {
  // 确保不传递带逗号的路径，避免路径重复
  const cleanPath = currentPath.value.replace(/,/g, '').replace(/\/+/g, '/');
  
  console.log('上传路径:', cleanPath); // 调试日志，查看上传路径
  
  return {
    path: cleanPath
  }
})

// 新建文件夹
const newDirDialogVisible = ref(false)
const newDirForm = ref({
  name: ''
})

// 过滤节点方法
const filterNode = (value, data) => {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}

// 处理搜索
const handleSearch = () => {
  treeRef.value?.filter(searchText.value)
}

// 加载子节点
const loadNode = async (node, resolve) => {
  try {
    // 根节点
    if (node.level === 0) {
      if (node.data) {
        // 有初始数据的情况
        resolve(node.data);
      } else {
        // 根目录情况
        const response = await getFileSystemContents(route.params.id, '/', false);
        // 处理数据以确保isDirectory字段存在
        const data = response.data || [];
        data.forEach(item => {
          if (item.isDirectory === undefined) {
            item.isDirectory = item.type === 'directory';
          }
          // 确保所有目录都可展开
          item.isLeaf = !item.isDirectory;
          
          // 规范化路径，确保路径始终以 / 开头
          if (item.path && !item.path.startsWith('/')) {
            item.path = '/' + item.path;
          }
          
          // 如果没有path属性，从fullPath提取
          if (!item.path && item.fullPath) {
            const parts = item.fullPath.split('://');
            if (parts.length > 1) {
              // HDFS路径格式处理 hdfs://host:port/path/to/file
              const hostAndPath = parts[1];
              const pathStartIndex = hostAndPath.indexOf('/', hostAndPath.indexOf(':'));
              if (pathStartIndex !== -1) {
                item.path = hostAndPath.substring(pathStartIndex);
              } else {
                item.path = '/';
              }
            }
          }
        });
        resolve(data);
      }
      return;
    }
    
    // 非根节点
    try {
      const path = node.data.path;
      console.info(`加载目录: ${path}, 节点数据:`, node.data);
      
      const response = await getFileSystemContents(route.params.id, node.data.path, false);
      
      // 处理数据以确保文件夹可展开
      const children = response.data || [];
      
      // 如果是空文件夹，返回空数组但不标记为叶子节点
      if (children.length === 0) {
        console.info(`${path} 是空文件夹`);
        resolve([]);
        return;
      }
      
      children.forEach(item => {
        if (item.isDirectory === undefined) {
          // 如果API没返回isDirectory字段，尝试从type字段判断
          item.isDirectory = item.type === 'directory';
        }
        // 确保所有目录都可展开，文件是叶子节点
        item.isLeaf = !item.isDirectory;
        
        // 规范化路径，确保路径始终以 / 开头
        if (item.path && !item.path.startsWith('/')) {
          item.path = '/' + item.path;
        }
        
        // 如果没有path属性，从fullPath提取
        if (!item.path && item.fullPath) {
          const parts = item.fullPath.split('://');
          if (parts.length > 1) {
            // HDFS路径格式处理 hdfs://host:port/path/to/file
            const hostAndPath = parts[1];
            const pathStartIndex = hostAndPath.indexOf('/', hostAndPath.indexOf(':'));
            if (pathStartIndex !== -1) {
              item.path = hostAndPath.substring(pathStartIndex);
            } else {
              item.path = '/';
            }
          }
        }
      });
      
      console.info(`${path} 加载了 ${children.length} 个子项`);
      resolve(children);
    } catch (error) {
      console.error('加载子目录失败:', error);
      // 出错时也返回空数组，避免加载失败导致树节点无法展开
      resolve([]);
    }
  } catch (error) {
    console.error('加载节点失败:', error);
    resolve([]);
  }
}

// 加载目录内容
const loadDirectoryContents = async () => {
  try {
    fullscreenLoading.value = true;
    loadingText.value = '正在加载目录内容...';
    
    console.info(`加载目录内容: ${currentPath.value}`);
    const response = await getFileSystemContents(route.params.id, currentPath.value, false);
    directoryContents.value = response.data || [];
    
    // 处理数据，确保isDirectory字段存在
    directoryContents.value.forEach(item => {
      if (item.isDirectory === undefined) {
        // 如果API没返回isDirectory字段，尝试从type字段判断
        item.isDirectory = item.type === 'directory';
      }
      
      // 对于HDFS文件系统，确保目录可以被下钻
      if (item.isDirectory) {
        item.hasChildren = true; // 默认假设目录有子节点
      }
      
      // 规范化路径，确保路径始终以 / 开头
      if (item.path && !item.path.startsWith('/')) {
        item.path = '/' + item.path;
      }
      
      // 如果没有path属性，从fullPath提取
      if (!item.path && item.fullPath) {
        const parts = item.fullPath.split('://');
        if (parts.length > 1) {
          // HDFS路径格式处理 hdfs://host:port/path/to/file
          const hostAndPath = parts[1];
          const pathStartIndex = hostAndPath.indexOf('/', hostAndPath.indexOf(':'));
          if (pathStartIndex !== -1) {
            item.path = hostAndPath.substring(pathStartIndex);
          } else {
            item.path = '/';
          }
        }
      }
    });
    
    console.info(`目录 ${currentPath.value} 加载了 ${directoryContents.value.length} 个项目`);
    
    selectedItem.value = null;
    previewContent.value = null;
    fullscreenLoading.value = false;
  } catch (error) {
    console.error(`加载目录内容失败 (${currentPath.value}):`, error);
    ElMessage.error(`加载目录内容失败: ${error.message || '未知错误'}`);
    fullscreenLoading.value = false;
  }
}

// 获取文件详细信息
const loadFileInfo = async (file) => {
  try {
    // 不使用getFileInfo API，使用已有的文件对象或通过列表查询获取信息
    if (file && Object.keys(file).length > 0) {
      // 如果file对象已包含足够信息，直接返回
      return file;
    } else {
      // 尝试通过重新获取目录内容来查找文件信息
      const response = await getFileSystemContents(route.params.id, file.path.substring(0, file.path.lastIndexOf('/')), false);
      const files = response.data || [];
      const fileInfo = files.find(f => f.path === file.path);
      return fileInfo || file;
    }
  } catch (error) {
    console.error('获取文件信息失败:', error)
    ElMessage.error('获取文件信息失败')
    return file; // 返回原始文件对象
  }
}

// 点击树节点
const handleNodeClick = (data) => {
  console.info('点击节点:', data);
  if (data.isDirectory) {
    currentPath.value = data.path;
    // 加载当前目录内容到右侧
    loadDirectoryContents();
  } else {
    // 选择文件
    selectItem(data);
  }
}

// 选择项目
const selectItem = (item, event) => {
  // 阻止事件冒泡和默认行为，避免触发下载
  if (event) {
    event.preventDefault()
    event.stopPropagation()
  }
  
  selectedItem.value = item
  if (!item.isDirectory && isPreviewable(item.name)) {
    loadFilePreview(item)
  } else {
    previewContent.value = null
  }
}

// 双击文件/文件夹
const handleFileDoubleClick = (item, event) => {
  console.info('双击项目:', item);
  if (item.isDirectory) {
    // 导航到该目录
    navigateToDirectory(item);
  } else {
    // 预览文件
    previewFile(item, event);
  }
}

// 导航到指定目录
const navigateToDirectory = (dir) => {
  console.info('导航到目录:', dir.path);
  currentPath.value = dir.path;
  loadDirectoryContents();
  
  // 尝试在树中展开该节点
  const node = treeRef.value?.getNode(dir.path);
  if (node && !node.expanded) {
    treeRef.value?.expandNode(node);
  }
}

// 导航到路径部分
const navigateToPath = (index) => {
  const parts = currentPath.value.split('/').filter(Boolean)
  const newPath = '/' + parts.slice(0, index + 1).join('/')
  currentPath.value = newPath
  loadDirectoryContents()
}

// 刷新当前目录
const refreshDirectory = () => {
  loadDirectoryContents()
}

// 预览文件
const previewFile = async (file, event) => {
  // 阻止事件冒泡和默认行为
  if (event) {
    event.preventDefault()
    event.stopPropagation()
  }
  
  if (!isPreviewable(file.name)) {
    ElMessage.warning('此类型的文件不支持预览')
    return
  }
  
  currentPreviewFile.value = file
  await loadFilePreview(file)
  previewDialogVisible.value = true
}

// 加载文件预览
const loadFilePreview = async (file) => {
  previewLoading.value = true
  previewContent.value = null
  currentPreviewFile.value = file
  
  try {
    // 文本文件使用content接口
    if (isTextFile(file.name)) {
      const response = await getFileContents(
        route.params.id,
        file.path,
        false
      )
      previewContent.value = response
    } 
    // 对于二进制文件(图片、PDF、视频、音频等)，使用专门的preview接口
    else if (isImageFile(file.name) || isPdfFile(file.name) || isVideoFile(file.name) || isAudioFile(file.name) || isHtmlFile(file.name)) {
      // 添加时间戳以避免缓存问题，并清理路径中可能的问题
      const timestamp = new Date().getTime()
      const cleanPath = file.path.replace(/\/+/g, '/') // 替换连续的斜杠为单个斜杠
      
      // 对于PDF文件，特别处理
      if (isPdfFile(file.name)) {
        console.log('加载PDF文件:', file.name)
        console.log('PDF文件路径:', cleanPath)
        
        // 尝试添加noCache参数并使用公共PDF预览URL
        const previewUrl = `/api/filesystem/${route.params.id}/preview?path=${encodeURIComponent(cleanPath)}&t=${timestamp}&noCache=true`
        console.log('PDF预览URL:', previewUrl)
        
        // 获取完整URL
        const fullPdfUrl = window.location.origin + previewUrl
        // 创建Google Docs Viewer URL
        const googleViewerUrl = `https://docs.google.com/viewer?url=${encodeURIComponent(fullPdfUrl)}&embedded=true`
        console.log('Google Viewer URL:', googleViewerUrl)
        
        previewContent.value = { 
          type: 'pdf',
          url: previewUrl,
          googleViewerUrl: googleViewerUrl
        }
      } else {
        const previewUrl = `/api/filesystem/${route.params.id}/preview?path=${encodeURIComponent(cleanPath)}&t=${timestamp}`
        
        if (isImageFile(file.name)) {
          // 图片可以直接使用URL
          previewContent.value = previewUrl
        } else {
          // 视频、音频等返回带类型的对象
          previewContent.value = { 
            type: getFileTypeFromExtension(file.name),
            url: previewUrl
          }
        }
      }
    } else {
      previewContent.value = '暂不支持该文件类型的预览'
    }
  } catch (error) {
    console.error('获取文件内容失败:', error)
    ElMessage.error(`获取文件内容失败: ${error.message || '未知错误'}`)
    previewContent.value = '加载文件内容失败'
  } finally {
    previewLoading.value = false
  }
}

// 根据文件名获取MIME类型
const getContentTypeFromFilename = (filename) => {
  const extension = filename.split('.').pop().toLowerCase()
  
  const mimeMap = {
    'pdf': 'application/pdf',
    'mp4': 'video/mp4',
    'webm': 'video/webm',
    'ogg': 'video/ogg',
    'mp3': 'audio/mpeg',
    'wav': 'audio/wav',
    'flac': 'audio/flac',
    'html': 'text/html',
    'htm': 'text/html',
    'jpg': 'image/jpeg',
    'jpeg': 'image/jpeg',
    'png': 'image/png',
    'gif': 'image/gif',
    'svg': 'image/svg+xml'
  }
  
  return mimeMap[extension] || 'application/octet-stream'
}

// 获取文件类型
const getFileTypeFromExtension = (filename) => {
  const extension = filename.split('.').pop().toLowerCase()
  
  if (isPdfFile(filename)) return 'pdf'
  if (isVideoFile(filename)) return 'video'
  if (isAudioFile(filename)) return 'audio'
  if (isHtmlFile(filename)) return 'html'
  
  return 'unknown'
}

// 下载文件
const downloadFile = async (file, event) => {
  // 阻止事件冒泡，避免其他处理程序被触发
  if (event) {
    event.preventDefault()
    event.stopPropagation()
  }
  
  try {
    // 创建一个直接的下载链接，而不是调用不存在的API
    const downloadUrl = `/api/filesystem/${route.params.id}/download?path=${encodeURIComponent(file.path)}`
    
    // 创建一个临时链接并点击
    const link = document.createElement('a')
    link.href = downloadUrl
    link.setAttribute('download', file.name)
    link.setAttribute('target', '_blank')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  } catch (error) {
    console.error('下载文件失败:', error)
    ElMessage.error('下载文件失败')
  }
}

// 删除文件或文件夹
const deleteFile = async (file) => {
  try {
    await ElMessageBox.confirm(`确认删除${file.isDirectory ? '文件夹' : '文件'} "${file.name}"？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    fullscreenLoading.value = true
    loadingText.value = '正在删除...'
    
    await deleteFileOrDirectory(route.params.id, file.path, file.isDirectory)  // 调用 /api/filesystem/{datasourceId}/delete
    
    ElMessage.success('删除成功')
    // 刷新目录
    loadDirectoryContents()
    
    // 如果删除的是当前选中的项目，清除选择
    if (selectedItem.value && selectedItem.value.path === file.path) {
      selectedItem.value = null
      previewContent.value = null
    }
    
    fullscreenLoading.value = false
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
    fullscreenLoading.value = false
  }
}

// 显示上传对话框
const showUploadDialog = () => {
  uploadDialogVisible.value = true
}

// 上传前检查
const beforeUpload = (file) => {
  // 可以在这里添加文件大小、类型等限制
  return true
}

// 提交上传
const submitUpload = () => {
  uploadRef.value.submit()
}

// 上传成功处理
const handleUploadSuccess = (response, uploadFile) => {
  console.log('上传成功响应:', response);
  ElMessage.success(`上传文件 ${uploadFile.name} 成功`);
  // 上传完成后刷新目录
  loadDirectoryContents();
  // 关闭上传对话框
  uploadDialogVisible.value = false;
}

// 上传错误处理
const handleUploadError = (error, uploadFile) => {
  console.error('上传失败:', error);
  ElMessage.error(`上传文件 ${uploadFile.name} 失败: ${error.toString()}`);
}

// 创建文件夹
const handleCreateDirectory = () => {
  newDirForm.value.name = ''
  newDirDialogVisible.value = true
}

// 提交新建文件夹
const submitNewDir = async () => {
  if (!newDirForm.value.name) {
    ElMessage.warning('请输入文件夹名称')
    return
  }
  
  try {
    fullscreenLoading.value = true
    loadingText.value = '正在创建文件夹...'
    
    const newPath = `${currentPath.value}/${newDirForm.value.name}`.replace(/\/\//g, '/')
    await apiCreateDirectory(route.params.id, newPath)  // 调用 /api/filesystem/{datasourceId}/mkdir
    
    ElMessage.success('创建文件夹成功')
    newDirDialogVisible.value = false
    
    // 刷新目录
    loadDirectoryContents()
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('创建文件夹失败:', error)
    ElMessage.error('创建文件夹失败')
    fullscreenLoading.value = false
  }
}

// 切换全屏预览
const toggleFullscreenPreview = () => {
  isFullscreenPreview.value = !isFullscreenPreview.value
}

// 格式化文件大小
const formatFileSize = (size) => {
  if (size === null || size === undefined) return '-'
  
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let index = 0
  let formattedSize = size
  
  while (formattedSize >= 1024 && index < units.length - 1) {
    formattedSize /= 1024
    index++
  }
  
  return `${formattedSize.toFixed(2)} ${units[index]}`
}

// 格式化日期
const formatDate = (timestamp) => {
  if (!timestamp) return '-'
  
  const date = new Date(timestamp)
  return date.toLocaleString()
}

// 获取文件类型
const getFileType = (filename) => {
  const extension = filename.split('.').pop().toLowerCase()
  
  const typeMap = {
    txt: '文本文件',
    pdf: 'PDF文档',
    docx: 'Word文档',
    xlsx: 'Excel表格',
    pptx: 'PowerPoint演示文稿',
    jpg: 'JPEG图片',
    jpeg: 'JPEG图片',
    png: 'PNG图片',
    gif: 'GIF图片',
    zip: 'ZIP压缩包',
    rar: 'RAR压缩包',
    csv: 'CSV表格',
    json: 'JSON文件',
    xml: 'XML文件',
    html: 'HTML文件',
    css: 'CSS样式表',
    js: 'JavaScript文件',
    py: 'Python脚本',
    java: 'Java源码',
    cpp: 'C++源码',
    h: 'C/C++头文件',
    md: 'Markdown文档'
  }
  
  return typeMap[extension] || `${extension.toUpperCase()}文件`
}

// 判断文件是否可以预览
const isPreviewable = (filename) => {
  return isTextFile(filename) || isImageFile(filename) || isPdfFile(filename) || isVideoFile(filename) || isAudioFile(filename) || isHtmlFile(filename)
}

// 判断是否为文本文件
const isTextFile = (filename) => {
  const textExtensions = ['txt', 'log', 'csv', 'json', 'xml', 'html', 'css', 'js', 'py', 'java', 'cpp', 'h', 'md']
  const extension = filename.split('.').pop().toLowerCase()
  return textExtensions.includes(extension)
}

// 判断是否为图片文件
const isImageFile = (filename) => {
  const imageExtensions = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp', 'svg']
  const extension = filename.split('.').pop().toLowerCase()
  return imageExtensions.includes(extension)
}

// 判断是否为PDF文件
const isPdfFile = (filename) => {
  return filename.toLowerCase().endsWith('.pdf')
}

// 判断是否为HTML文件
const isHtmlFile = (filename) => {
  const lowerCaseName = filename.toLowerCase()
  return lowerCaseName.endsWith('.html') || lowerCaseName.endsWith('.htm')
}

// 判断是否为视频文件
const isVideoFile = (filename) => {
  const lowerCaseName = filename.toLowerCase()
  const videoExtensions = ['.mp4', '.avi', '.mov', '.wmv', '.flv', '.mkv', '.webm']
  return videoExtensions.some(ext => lowerCaseName.endsWith(ext))
}

// 判断是否为音频文件
const isAudioFile = (filename) => {
  const lowerCaseName = filename.toLowerCase()
  const audioExtensions = ['.mp3', '.wav', '.ogg', '.flac', '.m4a', '.aac']
  return audioExtensions.some(ext => lowerCaseName.endsWith(ext))
}

// 复制文件
const copyFileItem = async (source, destination, overwrite = false) => {
  try {
    fullscreenLoading.value = true
    loadingText.value = '正在复制...'
    
    // 由于API尚未实现，使用临时方案
    ElMessage.info('复制功能尚未实现')
    // await apiCopyFile(route.params.id, source, destination, overwrite)  // 调用 /api/filesystem/{datasourceId}/copy
    
    // 刷新目录
    loadDirectoryContents()
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('复制文件失败:', error)
    ElMessage.error('复制文件失败')
    fullscreenLoading.value = false
  }
}

// 移动文件
const moveFileItem = async (source, destination, overwrite = false) => {
  try {
    fullscreenLoading.value = true
    loadingText.value = '正在移动...'
    
    // 由于API尚未实现，使用临时方案
    ElMessage.info('移动功能尚未实现')
    // await apiMoveFile(route.params.id, source, destination, overwrite)  // 调用 /api/filesystem/{datasourceId}/move
    
    // 刷新目录
    loadDirectoryContents()
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('移动文件失败:', error)
    ElMessage.error('移动文件失败')
    fullscreenLoading.value = false
  }
}

// 重命名文件
const renameFileItem = async (file, newName) => {
  try {
    fullscreenLoading.value = true
    loadingText.value = '正在重命名...'
    
    // 由于API尚未实现，使用临时方案
    ElMessage.info('重命名功能尚未实现')
    // await apiRenameFile(route.params.id, file.path, newName)  // 调用 /api/filesystem/{datasourceId}/rename
    
    // 刷新目录
    loadDirectoryContents()
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('重命名文件失败:', error)
    ElMessage.error('重命名文件失败')
    fullscreenLoading.value = false
  }
}

// 搜索文件
const searchFilesInSystem = async (query, recursive = true, fileType = null) => {
  try {
    fullscreenLoading.value = true
    loadingText.value = '正在搜索...'
    
    // 由于API尚未实现，使用临时方案
    ElMessage.info('搜索功能尚未实现')
    // const response = await apiSearchFiles(
    //   route.params.id, 
    //   currentPath.value, 
    //   query, 
    //   recursive, 
    //   fileType
    // )  // 调用 /api/filesystem/{datasourceId}/search
    
    // const results = response.data || []
    const results = []
    fullscreenLoading.value = false
    return results
  } catch (error) {
    console.error('搜索文件失败:', error)
    ElMessage.error('搜索文件失败')
    fullscreenLoading.value = false
    return []
  }
}

// 获取文件系统统计信息
const loadFileSystemStats = async () => {
  try {
    // 由于API尚未实现，使用临时方案
    // const response = await getFileSystemStats(route.params.id)  // 调用 /api/filesystem/{datasourceId}/stats
    // return response.data
    ElMessage.info('获取文件系统统计信息功能尚未实现')
    return {}
  } catch (error) {
    console.error('获取文件系统统计信息失败:', error)
    ElMessage.error('获取文件系统统计信息失败')
    return {}
  }
}

// 保存文件内容
const saveFileContents = async (file, content, encoding = 'utf-8') => {
  try {
    fullscreenLoading.value = true
    loadingText.value = '正在保存...'
    
    // 由于API尚未实现，使用临时方案
    ElMessage.info('保存文件内容功能尚未实现')
    // await saveFileContent(route.params.id, file.path, content, encoding)  // 调用 /api/filesystem/{datasourceId}/save
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('保存文件内容失败:', error)
    ElMessage.error('保存文件内容失败')
    fullscreenLoading.value = false
  }
}

// 初始化数据 - 修改为懒加载方式
const initializeData = async () => {
  try {
    fullscreenLoading.value = true;
    loadingText.value = '正在加载文件系统...';
    
    // 加载当前目录内容
    await loadDirectoryContents();
    
    fullscreenLoading.value = false;
  } catch (error) {
    console.error('加载文件系统失败:', error);
    ElMessage.error('加载文件系统失败');
    fullscreenLoading.value = false;
  }
}

// 初始化
onMounted(() => {
  initializeData()
})

// 监听路径变化
watch(currentPath, () => {
  // 可以在这里更新URL参数，方便分享或保存当前路径
  const query = { ...route.query, path: currentPath.value }
  router.replace({ query })
})

// 导航到根目录
const navigateToRoot = () => {
  currentPath.value = '/'
  loadDirectoryContents()
}

// 添加文件类型判断函数
// 判断是否为压缩文件
const isArchiveFile = (filename) => {
  const archiveExtensions = ['zip', 'rar', 'tar', 'gz', '7z']
  const extension = filename.split('.').pop().toLowerCase()
  return archiveExtensions.includes(extension)
}

// 判断是否为代码文件
const isCodeFile = (filename) => {
  const codeExtensions = ['js', 'py', 'java', 'cpp', 'c', 'h', 'css', 'html', 'php', 'ts', 'jsx', 'tsx', 'vue']
  const extension = filename.split('.').pop().toLowerCase()
  return codeExtensions.includes(extension)
}

// 根据文件类型获取图标类
const getFileIconClass = (item) => {
  if (item.isDirectory) return 'folder-icon'
  
  const filename = item.name.toLowerCase()
  
  if (isImageFile(filename)) return 'image-icon'
  if (isTextFile(filename)) return 'text-icon'
  if (isArchiveFile(filename)) return 'archive-icon'
  if (isCodeFile(filename)) return 'code-icon'
  if (isPdfFile(filename)) return 'pdf-icon'
  if (isVideoFile(filename)) return 'video-icon'
  if (isAudioFile(filename)) return 'audio-icon'
  
  return 'default-icon'
}

// 在新窗口中打开文件
const openInNewWindow = (url) => {
  window.open(url, '_blank')
}

// 使用Google Viewer预览PDF
const tryGoogleViewerPreview = (pdfUrl) => {
  // 获取完整的URL，包括协议和主机名
  const fullPdfUrl = window.location.origin + pdfUrl
  console.log('使用Google Viewer预览:', fullPdfUrl)
  
  // 创建Google Docs Viewer URL
  const googleViewerUrl = `https://docs.google.com/viewer?url=${encodeURIComponent(fullPdfUrl)}&embedded=true`
  
  // 更新预览内容
  if (previewContent.value && previewContent.value.type === 'pdf') {
    previewContent.value.googleViewerUrl = googleViewerUrl
  }
}
</script>
// 更新CSS样式
<style scoped>
.file-system-browser {
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

.current-path {
  margin-left: 20px;
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

.directory-tree-area {
  flex: 1;
  overflow: auto;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-list-area {
  flex: 1;
  padding: 0 16px;
  display: flex;
  flex-direction: column;
}

.file-view-controls {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.grid-view {
  flex: 1;
  overflow: auto;
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
  padding: 16px;
}

.grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  background-color: #f8f9fa;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
  position: relative;
}

.grid-item:hover {
  background-color: #f0f2f5;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.grid-item.selected {
  background-color: #e6f2ff;
  border: 1px solid #409eff;
}

.grid-icon-wrapper {
  width: 64px;
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  margin-bottom: 8px;
  color: white;
}

.grid-icon {
  font-size: 28px;
}

.folder-icon {
  background-color: #ffca28;
  color: #fff;
}

.image-icon {
  background-color: #42a5f5;
  color: #fff;
}

.text-icon {
  background-color: #66bb6a;
  color: #fff;
}

.archive-icon {
  background-color: #ef5350;
  color: #fff;
}

.code-icon {
  background-color: #7e57c2;
  color: #fff;
}

.pdf-icon {
  background-color: #ec407a;
  color: #fff;
}

.video-icon {
  background-color: #26c6da;
  color: #fff;
}

.audio-icon {
  background-color: #29b6f6;
  color: #fff;
}

.default-icon {
  background-color: #78909c;
  color: #fff;
}

.grid-item-name {
  font-size: 14px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 100%;
  margin-top: 8px;
  font-weight: 500;
}

.grid-item-info {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.file-preview-area {
  width: 320px;
  border-left: 1px solid #ebeef5;
  padding-left: 16px;
  overflow: auto;
}

.preview-header {
  margin-bottom: 16px;
}

.preview-details {
  margin-bottom: 16px;
}

.preview-actions {
  margin-bottom: 16px;
}

.file-content-preview {
  margin-top: 20px;
}

.image-preview {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.image-preview img {
  max-width: 100%;
  max-height: 300px;
  object-fit: contain;
}

.text-preview {
  margin-top: 16px;
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  max-height: 300px;
  overflow: auto;
}

.text-preview pre {
  font-family: monospace;
  white-space: pre-wrap;
  margin: 0;
}

.no-item-selected {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.preview-dialog-content {
  max-height: 70vh;
  overflow: auto;
}

.image-preview-dialog {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.image-preview-dialog img {
  max-width: 100%;
  object-fit: contain;
}

.text-preview-dialog {
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  max-height: 70vh;
  overflow: auto;
}

.text-preview-dialog pre {
  font-family: monospace;
  white-space: pre-wrap;
  margin: 0;
}

.pdf-preview {
  margin-top: 16px;
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  max-height: 400px;
  overflow: auto;
}

.video-preview {
  margin-top: 16px;
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  max-height: 300px;
  overflow: auto;
}

.audio-preview {
  margin-top: 16px;
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  max-height: 300px;
  overflow: auto;
}

.pdf-preview-dialog {
  margin-top: 16px;
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  max-height: 600px;
  overflow: auto;
}

.video-preview-dialog {
  margin-top: 16px;
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  max-height: 500px;
  overflow: auto;
}

.audio-preview-dialog {
  margin-top: 16px;
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  max-height: 300px;
  overflow: auto;
}

.html-preview {
  margin-top: 16px;
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
  max-height: 600px;
  overflow: auto;
}

.preview-options {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.pdf-container {
  position: relative;
}

.pdf-fallback {
  margin-top: 10px;
  font-size: 13px;
  color: #909399;
  text-align: center;
}
</style>