<template>
  <div class="layout-container">
    <!-- 移除所有背景特效元素 -->
    <el-container>
      <el-header class="main-header">
        <div class="header-container">
          <div class="logo">
            <div class="logo-container">
              <img src="@/assets/logo.svg" alt="logo" class="logo-img">
              <div class="logo-text">
                <span class="logo-title">芒果数据中台</span>
                <span class="logo-subtitle">MANGO DATA PLATFORM</span>
              </div>
            </div>
          </div>
          <el-menu
              mode="horizontal"
              :default-active="activeMenu"
              router
              class="main-menu"
              @select="handleSelect"
              background-color="#f0f9ff"
              text-color="#2c3e50"
              active-text-color="#409EFF"
          >
            <el-sub-menu 
              v-for="menu in topLevelMenus"
              :key="menu.path" 
              :index="menu.path" 
              popper-class="dataset-submenu"
            >
              <template #title>
                <el-icon>
                  <component :is="iconComponents[menu.icon] || iconComponents['Document']" />
                </el-icon>
                <span>{{ menu.name }}</span>
              </template>
              <el-menu-item 
                v-for="subMenu in menu.children"
                :key="subMenu.path" 
                :index="subMenu.path"
              >
                <el-icon>
                  <component :is="iconComponents[subMenu.icon] || iconComponents['Document']" />
                </el-icon>
                <span>{{ subMenu.name }}</span>
              </el-menu-item>
            </el-sub-menu>
          </el-menu>

          <!-- 添加用户信息及操作区域 -->
          <div class="user-area">
            <el-dropdown trigger="click" @command="handleCommand">
              <div class="user-info">
                <el-avatar :size="32" class="user-avatar">
                  {{ userInfo.username ? userInfo.username.substring(0, 1).toUpperCase() : 'U' }}
                </el-avatar>
                <span class="user-name">{{ userInfo.username || '未登录' }}</span>
                <el-icon><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">
                    <el-icon><User /></el-icon>个人中心
                  </el-dropdown-item>
                  <el-dropdown-item command="settings">
                    <el-icon><Setting /></el-icon>系统设置
                  </el-dropdown-item>
                  <el-dropdown-item divided command="logout">
                    <el-icon><SwitchButton /></el-icon>退出登录
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      <el-container class="main-container">
        <el-aside v-if="showAside" width="200px" class="main-aside">
          <el-menu
              :default-active="route.path"
              class="sub-menu"
              router
              background-color="#f0f9ff"
              text-color="#2c3e50"
              active-text-color="#409EFF"
          >
            <template v-for="menu in currentSubMenus" :key="menu.path">
              <el-menu-item :index="menu.path">
                <template #default>
                  <el-icon>
                    <component :is="iconComponents[menu.icon] || iconComponents['Document']" />
                  </el-icon>
                  <span>{{ menu.title }}</span>
                </template>
              </el-menu-item>
            </template>
          </el-menu>
        </el-aside>
        <el-main :class="['main-content', { 'no-aside': !showAside }]">
          <div class="breadcrumb-nav">
            <div class="nav-left">
              <el-breadcrumb separator="/">
                <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
                <el-breadcrumb-item>{{ route.meta.title }}</el-breadcrumb-item>
              </el-breadcrumb>
            </div>
            <div class="history-nav">
              <el-scrollbar class="history-scrollbar" :native="false">
                <div class="history-list">
                  <span
                      v-for="item in histories"
                      :key="item.path"
                      :class="{ 'history-item': true, 'active': route.path === item.path }"
                      @click="handleHistoryClick(item)"
                  >
                    {{ item.title }}
                    <el-icon
                        class="close-icon"
                        @click.stop="handleHistoryClose(item)"
                    >
                      <Close />
                    </el-icon>
                  </span>
                </div>
              </el-scrollbar>
            </div>
          </div>
          <div class="content-background" :class="{ 'no-aside': !showAside }"></div>
          <router-view></router-view>
        </el-main>
      </el-container>
    </el-container>
    <!-- 添加AI助手组件 -->
    <AIAssistant />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  ArrowDown, 
  Close,
  DataLine, 
  Connection, 
  Box, 
  DataAnalysis, 
  Grid,
  SetUp,
  Files, 
  Histogram, 
  Search, 
  InfoFilled, 
  Upload, 
  List, 
  TrendCharts,
  Check, 
  Notebook, 
  Coin,
  Document,
  Service, 
  Operation, 
  Share, 
  Monitor, 
  Folder,
  MapLocation, 
  Edit,
  Lock, 
  Link,
  Platform,
  DataBoard,
  FolderOpened,
  Timer,
  DocumentCopy,
  Sort,
  Tools,
  Warning,
  MagicStick,
  CircleCheck,
  ChatDotRound,
  Bell,
  PieChart,
  Collection,
  Download,
  Refresh,
  Star,
  Setting,
  VideoCamera,
  Odometer,
  ScaleToOriginal,
  Cpu,
  CircleClose,
  Money,
  User,
  UserFilled,
  OfficeBuilding,
  Wallet,
  Key,
  Hide,
  Aim,
  AlarmClock,
  Guide,
  SwitchButton
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/modules/user'
import { useHistoryStore } from '@/store/modules/history'
import { ElMessage, ElMessageBox } from 'element-plus'
import { AssistantWidget as AIAssistant } from '@/components/AIAssistant'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const historyStore = useHistoryStore()

const activeMenu = computed(() => route.path)
const userInfo = computed(() => {
  // 从localStorage获取用户信息
  const storedUserInfo = localStorage.getItem('userInfo')
  if (storedUserInfo) {
    try {
      return JSON.parse(storedUserInfo)
    } catch (e) {
      return {}
    }
  }
  return userStore.userInfo || {}
})

// 从 localStorage 获取菜单数据
const topLevelMenus = computed(() => {
  const storedMenus = localStorage.getItem('menus')
  if (storedMenus) {
    try {
      const menus = JSON.parse(storedMenus)
      
      // 确保所有一级菜单都被识别
      // 有些菜单可能是parentId=0，有些可能是parentId=null
      return menus.filter(menu => 
        menu.parentId === 0 || 
        menu.parentId === null || 
        !menu.parentId
      ).sort((a, b) => (a.sort || 0) - (b.sort || 0)) // 按排序字段排序
      
    } catch (e) {
      console.error('解析菜单数据失败:', e)
      return []
    }
  }
  
  // 如果localStorage中没有菜单数据，返回空数组
  return []
})

// 获取权限列表
const permissions = computed(() => {
  const storedPermissions = localStorage.getItem('permissions')
  if (storedPermissions) {
    try {
      return JSON.parse(storedPermissions)
    } catch (e) {
      console.error('解析权限数据失败:', e)
      return []
    }
  }
  return []
})

const histories = computed(() => historyStore.histories)

// 监听路由变化，添加历史记录
watch(() => route.path, (newPath) => {
  if (route.meta.title) {  // 只记录有标题的页面
    historyStore.addHistory({
      path: route.path,
      meta: route.meta
    })
  }
}, { immediate: true })

// 处理历史记录点击
const handleHistoryClick = (item) => {
  router.push(item.path)
}

// 处理删除历史记录
const handleHistoryClose = (item) => {
  historyStore.removeHistory(item.path)
}

// 左侧菜单项组件
const currentSubMenus = computed(() => {
  const path = route.path
  
  // 如果是三级菜单，获取第二级路径
  // 例如 /dataIntegration/model/domain 需要提取 /dataIntegration/model
  const pathParts = path.split('/').filter(p => p)
  const mainPath = pathParts.length >= 2 ? '/' + pathParts[0] + '/' + pathParts[1] : path
  
  // 从localStorage中的菜单数据中查找
  const storedMenus = localStorage.getItem('menus')
  if (storedMenus) {
    try {
      const menus = JSON.parse(storedMenus)
      
      // 为了防止菜单丢失，先尝试直接匹配精确的路径
      // 查找所有菜单（包括子菜单）
      const findAllMenus = (menuList) => {
        let allMenus = []
        if (!menuList) return allMenus
        
        menuList.forEach(menu => {
          allMenus.push(menu)
          if (menu.children) {
            allMenus = allMenus.concat(findAllMenus(menu.children))
          }
        })
        return allMenus
      }
      
      const allMenus = findAllMenus(menus)
      
      // 先尝试精确匹配当前路径
      const exactMenu = allMenus.find(m => m.path === mainPath)
      if (exactMenu && exactMenu.children && exactMenu.children.length > 0) {
        return exactMenu.children
          .filter(item => !item.hidden) // 过滤隐藏的菜单
          .sort((a, b) => (a.sort || 0) - (b.sort || 0)) // 按sort字段排序
          .map(menu => ({
            path: menu.path,
            title: menu.name,
            icon: menu.icon || 'Document'
          }))
      }
      
      // 找不到精确匹配，则使用前缀匹配
      // 先查找一级菜单
      const parentMenu = menus.find(m => path.startsWith(m.path) && m.path !== '/')
      if (!parentMenu || !parentMenu.children) return []
      
      // 查找二级菜单
      const secondMenu = parentMenu.children.find(m => path.startsWith(m.path) && m.path !== parentMenu.path)
      if (!secondMenu) {
        // 如果没找到匹配的二级菜单，返回一级菜单下的所有二级菜单作为侧边栏
        return parentMenu.children
          .filter(item => !item.hidden) // 过滤隐藏的菜单
          .sort((a, b) => (a.sort || 0) - (b.sort || 0)) // 按sort字段排序
          .map(menu => ({
            path: menu.path,
            title: menu.name,
            icon: menu.icon || 'Document'
          }))
      }
      
      // 如果找到二级菜单，且二级菜单有子菜单，则显示三级菜单
      if (secondMenu.children && secondMenu.children.length > 0) {
        return secondMenu.children
          .filter(item => !item.hidden) // 过滤隐藏的菜单
          .sort((a, b) => (a.sort || 0) - (b.sort || 0)) // 按sort字段排序
          .map(menu => ({
            path: menu.path,
            title: menu.name,
            icon: menu.icon || 'Document'
          }))
      }
    } catch (e) {
      console.error('解析菜单数据失败:', e)
    }
  }
  
  // 如果没有找到匹配的菜单，则返回空数组
  return []
})

// 是否显示左侧菜单
const showAside = computed(() => {
  return currentSubMenus.value.length > 0
})

// 处理菜单选择
const handleSelect = async (key) => {
  try {
    // 从localStorage中的菜单数据中查找选择的菜单
    const storedMenus = localStorage.getItem('menus')
    if (storedMenus) {
      const menus = JSON.parse(storedMenus)
      // 查找所有菜单（包括子菜单）
      const findAllMenus = (menuList) => {
        let allMenus = []
        if (!menuList) return allMenus
        
        menuList.forEach(menu => {
          allMenus.push(menu)
          if (menu.children) {
            allMenus = allMenus.concat(findAllMenus(menu.children))
          }
        })
        return allMenus
      }
      
      const allMenus = findAllMenus(menus)
      // 查找当前选择的菜单
      const selectedMenu = allMenus.find(menu => menu.path === key)
      
      if (selectedMenu) {
        // 如果菜单有子菜单，导航到第一个子菜单
        if (selectedMenu.children && selectedMenu.children.length > 0) {
          // 找到第一个有效的子菜单（有component的菜单）
          const firstValidChild = selectedMenu.children.find(child => child.component) || selectedMenu.children[0]
          await router.push(firstValidChild.path)
        } else {
          // 如果没有子菜单，直接导航到该菜单
          await router.push(key)
        }
      } else {
        // 如果在菜单中找不到，直接导航到该路径
        await router.push(key)
      }
    } else {
      // 如果没有菜单数据，直接导航到该路径
      await router.push(key)
    }
  } catch (error) {
    console.error('Navigation error:', error)
  }
}

// 图标组件映射
const iconComponents = {
  Connection,
  Box,
  DataAnalysis,
  Document,
  Link,
  Grid,
  Operation,
  Share,
  SetUp,
  List,
  Monitor,
  Histogram,
  Search,
  InfoFilled,
  Upload,
  TrendCharts,
  Check,
  Folder,
  MapLocation,
  Edit,
  Lock,
  Platform,
  DataBoard,
  FolderOpened,
  Notebook,
  Timer,
  DocumentCopy,
  Sort,
  Tools,
  Warning,
  MagicStick,
  CircleCheck,
  ChatDotRound,
  Bell,
  PieChart,
  DataLine,
  Collection,
  Download,
  Refresh,
  Star,
  Service,
  Setting,
  VideoCamera,
  Odometer,
  ScaleToOriginal,
  Cpu,
  CircleClose,
  Money,
  User,
  UserFilled,
  OfficeBuilding,
  Wallet,
  Key,
  Hide,
  Aim,
  AlarmClock,
  Guide,
  SwitchButton
}

// 处理下拉菜单命令
const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确认退出登录吗?', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      // 清除所有相关的存储内容
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('menus')
      localStorage.removeItem('permissions')
      
      // 清除可能存在的其他相关数据
      sessionStorage.clear() // 清除会话存储
      
      // 跳转到登录页
      router.push('/login')
      ElMessage.success('已安全退出登录')
    }).catch(() => {})
  } else if (command === 'profile') {
    router.push('/system/personal/info')
  } else if (command === 'settings') {
    router.push('/system/base')
  }
}

// 组件挂载和卸载
onMounted(() => {
  // 不再初始化动画和特效
})

onUnmounted(() => {
  // 只保留必要的清理代码
  document.removeEventListener('mousemove', handleMouseMove);
  
  if (animationFrame) {
    cancelAnimationFrame(animationFrame);
  }
})
</script>
<style lang="scss" scoped>
.app-wrapper {
  .main-container {
    padding-left: 0;  // 移除主容器的左内边距
  }
}

.layout-container {
  height: 100vh;
  background: linear-gradient(135deg, #0a1522 0%, #0d1824 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
}

.main-container {
  flex: 1;
  min-height: 0;
}

.main-header {
  background: rgba(10, 25, 50, 0.95);
  border-bottom: 1px solid rgba(64, 158, 255, 0.2);
  padding: 0;
  position: relative;
  z-index: 1000;
}

.header-container {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 20px;
  background: #001529;
}

.logo {
  margin-right: 40px;
  padding: 0 15px;
  background: #001529;
  height: 100%;
  display: flex;
  align-items: center;

.logo-container {
  display: flex;
  align-items: center;
.logo-img {
      height: 32px;
      margin-right: 10px;
    }
.logo-text {
  display: flex;
  flex-direction: column;
.logo-title {
  font-size: 18px;
  font-weight: 600;
        color: #fff;
        line-height: 1.2;
      }
.logo-subtitle {
  font-size: 12px;
        color: rgba(255, 255, 255, 0.7);
}
  }
  }
}

.main-menu {
  flex: 1;
  background: rgba(10, 25, 50, 0.95) !important;
  border: none;

  :deep(.el-menu--horizontal) {
    border: none;
    background: rgba(10, 25, 50, 0.95);
  }

  :deep(.el-menu-item),
  :deep(.el-sub-menu__title) {
    height: 60px;
    line-height: 60px;
    color: rgba(255, 255, 255, 0.85) !important;
    background: rgba(10, 25, 50, 0.95) !important;
    border: none;
    padding: 0 20px;
    transition: all 0.3s ease;

    &:hover {
      color: #409EFF !important;
      background: rgba(15, 40, 80, 0.95) !important;
    }

    .el-icon {
      margin-right: 8px;
      font-size: 18px;
      vertical-align: middle;
    }
  }

  :deep(.el-sub-menu.is-active) {
    .el-sub-menu__title {
      color: #409EFF !important;
      border-bottom: 2px solid #409EFF;
      background: #002140 !important;
    }
  }
}

/* 二级菜单样式 */
:deep(.el-menu--popup) {
  background: #001529 !important;
  backdrop-filter: blur(20px);
  border-radius: 4px;
  padding: 4px 0;
  min-width: 200px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);

  .el-menu-item {
    height: 40px !important;
    line-height: 40px !important;
    margin: 4px 8px;
    border-radius: 4px;
    color: rgba(255, 255, 255, 0.85) !important;
    background: #001529 !important;

    &:hover {
      background: #002140 !important;
      color: #409EFF !important;
    }

    &.is-active {
      background: #002140 !important;
      color: #409EFF !important;
    }
  }
}

.user-info {
  margin-left: 20px;
  .user-name {
    color: #fff;
    cursor: pointer;
    display: flex;
    align-items: center;
    padding: 0 12px;
    height: 40px;
    border-radius: 20px;
    transition: all 0.3s ease;

    &:hover {
      background: #002140;
    }

    .el-icon {
      margin-left: 4px;
      font-size: 12px;
    }
  }
}

/* 左侧菜单样式 */
.main-aside {
  background: rgba(10, 25, 50, 0.9);
  border-right: 1px solid rgba(64, 158, 255, 0.2);
  width: 220px !important;
}

.sub-menu {
  background: rgba(10, 25, 50, 0.95) !important;

  :deep(.el-menu-item) {
    color: rgba(255, 255, 255, 0.85) !important;
    background: rgba(10, 25, 50, 0.95) !important;

    &:hover {
      color: #409EFF !important;
      background: rgba(15, 40, 80, 0.95) !important;
    }

    &.is-active {
      background: rgba(15, 40, 80, 0.95) !important;
      color: #409EFF !important;
    }
  }
}

/* 面包屑导航样式 */
.breadcrumb-nav {
  display: flex;
  align-items: center;
  height: 40px;
  margin-bottom: 16px;
  padding: 0 16px;
  background: rgba(30, 60, 100, 0.6);
  backdrop-filter: blur(4px);
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.breadcrumb-nav :deep(.el-breadcrumb__item) {
  color: rgba(255, 255, 255, 0.8);
}

.breadcrumb-nav :deep(.el-breadcrumb__item .el-breadcrumb__inner) {
  color: rgba(255, 255, 255, 0.8);
}

.breadcrumb-nav :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: white;
}

.main-content {
  background-color: #f0f9ff;
  background-image: 
    linear-gradient(135deg, rgba(10, 30, 60, 0.95) 0%, rgba(15, 35, 70, 0.9) 100%);
  padding: 0 24px 24px;
  height: 100%;
  overflow-y: auto;
  position: relative;
  color: white;
  animation: bgPulse 15s ease-in-out infinite;
}

@keyframes bgPulse {
  0%, 100% {
    background-image: 
      linear-gradient(135deg, rgba(10, 30, 60, 0.95) 0%, rgba(15, 35, 70, 0.9) 100%);
  }
  50% {
    background-image: 
      linear-gradient(135deg, rgba(15, 35, 70, 0.95) 0%, rgba(10, 30, 60, 0.9) 100%);
  }
}

.content-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: 
    radial-gradient(circle at 20% 30%, rgba(64, 158, 255, 0.2) 0%, transparent 60%),
    radial-gradient(circle at 80% 70%, rgba(103, 194, 58, 0.15) 0%, transparent 50%);
  background-size: 100% 100%;
  z-index: -1;
  animation: glow 10s ease-in-out infinite;
}

@keyframes glow {
  0%, 100% {
    opacity: 0.6;
  }
  50% {
    opacity: 0.8;
  }
}

.page-container {
  transition: all 0.4s cubic-bezier(0.215, 0.61, 0.355, 1);
  transform-style: preserve-3d;
  perspective: 1000px;
  
  &:hover {
    transform: translateY(-5px) rotateX(2deg);
    box-shadow:
      0 15px 35px rgba(0, 0, 0, 0.3),
      0 5px 15px rgba(0, 0, 0, 0.2);
      
    &::after {
      opacity: 1;
      transform: translateY(10px) scale(0.96);
    }
  }
  
  &::after {
    content: '';
    position: absolute;
    bottom: -10px;
    left: 5%;
    width: 90%;
    height: 20px;
    background: rgba(0, 0, 0, 0.4);
    filter: blur(15px);
    border-radius: 50%;
    z-index: -1;
    opacity: 0;
    transition: all 0.4s cubic-bezier(0.215, 0.61, 0.355, 1);
    transform: translateY(0) scale(0.9);
  }
}

/* 菜单交互动画 */
.main-menu :deep(.el-menu-item),
.main-menu :deep(.el-sub-menu__title) {
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 3px;
    height: 100%;
    background: linear-gradient(to bottom, #409EFF, #1890ff);
    transform: translateX(-4px);
    transition: transform 0.3s ease;
  }
  
  &:hover::before,
  &.is-active::before {
    transform: translateX(0);
  }
  
  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: radial-gradient(
      circle at 0% 50%,
      rgba(64, 158, 255, 0.15) 0%,
      rgba(64, 158, 255, 0) 70%
    );
    opacity: 0;
    transition: opacity 0.4s ease;
  }
  
  &:hover::after {
    opacity: 1;
  }
}

/* 添加按钮动画 */
:deep(.el-button) {
  position: relative;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.215, 0.61, 0.355, 1);
  z-index: 1;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
    transition: all 0.6s;
    z-index: -1;
  }
  
  &:hover::before {
    left: 100%;
  }
  
  &::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 5px;
    height: 5px;
    background: rgba(255, 255, 255, 0.5);
    opacity: 0;
    border-radius: 100%;
    transform: scale(1, 1) translate(-50%);
    transform-origin: 50% 50%;
  }
  
  &:active::after {
    animation: ripple 1s ease-out;
  }
}

@keyframes ripple {
  0% {
    transform: scale(0, 0);
    opacity: 0.5;
  }
  100% {
    transform: scale(25, 25);
    opacity: 0;
  }
}

/* 标题渐变效果 */
.page-title {
  position: relative;
  display: inline-block;
  
  &::after {
    content: '';
    position: absolute;
    bottom: -3px;
    left: 0;
    width: 100%;
    height: 2px;
    background: linear-gradient(to right, #409EFF, transparent);
    transform: scaleX(0);
    transform-origin: left;
    transition: transform 0.4s ease;
  }
  
  &:hover::after {
    transform: scaleX(1);
  }
}

/* 优化滚动条样式 */
.main-content::-webkit-scrollbar {
  width: 6px;
  background: transparent;
}

.main-content::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;

  &:hover {
    background: rgba(255, 255, 255, 0.3);
  }
}

.main-aside {
  background: linear-gradient(180deg,
      rgba(64,158,255,0.05) 0%,
      rgba(64,158,255,0.02) 100%
  );
  width: 220px !important;
  height: 100%;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 999;
  margin-top: -1px;
  :deep(.el-menu-item) {
    font-size: 15px;
  }
  :deep(.el-sub-menu__title) {
    font-size: 15px;
  }
}

.sub-menu {
  :deep(.el-menu-item) {
    display: flex;
    align-items: center;
    padding: 0 20px !important;
    height: 50px;
    line-height: 50px;

    .el-icon {
      margin-right: 12px;
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 18px;
    }

    span {
      flex: 1;
    }
  }
}

.page-container {
  margin-top: 8px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 8px;
  box-shadow:
    0 4px 12px rgba(0, 0, 0, 0.2),
    0 1px 3px rgba(0, 0, 0, 0.1);
  padding: 24px;
  height: 100%;
  display: flex;
  flex-direction: column;
  border: 1px solid rgba(255, 255, 255, 0.1);
  position: relative;
  overflow: hidden;
}

/* 添加微妙的光晕效果 */
.page-container::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(
          circle at center,
          rgba(64,158,255,0.03) 0%,
          transparent 70%
  );
  opacity: 0.8;
  pointer-events: none;
}

.page-header {
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(64,158,255,0.1);
}

.page-title {
  font-size: 20px;
  font-weight: 500;
  color: white;
  margin-bottom: 16px;
  position: relative;
  padding-left: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 16px;
  background: linear-gradient(to bottom, #409EFF, #36cfc9);
  border-radius: 2px;
}

.custom-table {
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(4px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
}

.pagination {
  margin-top: auto;
  padding-top: 16px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid rgba(64,158,255,0.1);
  background: rgba(255, 255, 255, 0.3);
  padding: 16px;
  border-radius: 0 0 8px 8px;
  backdrop-filter: blur(4px);
}

:deep(.el-pagination) {
  --el-pagination-bg-color: transparent;
  --el-pagination-hover-color: #409EFF;
}

:deep(.el-pagination button) {
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(64,158,255,0.1);
}

:deep(.el-pagination .el-pager li) {
  background: rgba(255, 255, 255, 0.5);
  border: 1px solid rgba(64,158,255,0.1);
}

:deep(.el-pagination .el-pager li.active) {
  background: #409EFF;
  color: #fff;
}

:deep(.el-button--link) {
  padding: 4px 8px;
  border-radius: 4px;
}

:deep(.el-button--link:hover) {
  background: rgba(64,158,255,0.1);
  color: #409EFF;
}

:deep(.el-button--link.el-button--danger:hover) {
  background: rgba(245,108,108,0.1);
  color: #f56c6c;
}

/* 对话框样式 */
:deep(.el-dialog) {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  box-shadow:
      0 8px 24px rgba(64,158,255,0.15),
      0 2px 4px rgba(64,158,255,0.1);
  overflow: hidden;
}

:deep(.el-dialog__header) {
  margin: 0;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(64,158,255,0.1);
  background: linear-gradient(135deg, #f0f9ff 0%, #e6f7ff 100%);
}

:deep(.el-dialog__body) {
  padding: 24px;
}

:deep(.el-dialog__footer) {
  margin: 0;
  padding: 20px 24px;
  border-top: 1px solid rgba(64,158,255,0.1);
  background: rgba(64,158,255,0.02);
}

/* 菜单项样式优化 */
.el-menu--horizontal > .el-menu-item,
.el-menu--horizontal > .el-sub-menu .el-sub-menu__title {
  height: 64px;
  line-height: 64px;
  border-bottom: none;
  margin: 0;
  border-radius: 0;
  background-color: transparent !important;
}

.el-menu--horizontal > .el-menu-item.is-active,
.el-menu--horizontal > .el-sub-menu.is-active .el-sub-menu__title {
  position: relative;
      background-color: rgba(64, 158, 255, 0.1) !important;
      color: #409EFF !important;
    }
    
/* 活动菜单项的指示器 */
.el-menu--horizontal > .el-menu-item.is-active::after,
.el-menu--horizontal > .el-sub-menu.is-active .el-sub-menu__title::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 2px;
  background: #409EFF;
  border-radius: 1px;
  box-shadow: 0 0 4px rgba(64,158,255,0.4);
}

/* 子菜单样式优化 */
  .el-menu-item {
    height: 40px;
    line-height: 40px;
  padding: 0 20px;
  transition: all 0.3s;
  border-radius: 4px;
  margin: 4px 8px;
}

.el-menu-item:hover {
  background-color: rgba(64, 158, 255, 0.08) !important;
      color: #409EFF !important;
    }
    
.el-menu-item.is-active {
      background-color: #409EFF !important;
      color: #fff !important;
  font-weight: 500;
  box-shadow: 0 2px 4px rgba(64,158,255,0.2);
}

/* 图标样式优化 */
.el-menu-item .el-icon,
.el-sub-menu .el-icon {
  margin-right: 10px;
    font-size: 18px;
}

/* 数据集子菜单样式 */
.dataset-submenu {
  min-width: 160px !important;
:deep(.el-tag) {
  margin-right: 8px;
  }
}

.el-menu--horizontal .el-menu .el-menu-item-group__title {
  padding: 6px 20px;
  font-size: 13px;
  color: #909399;
}

/* 调整子菜单项的样式 */
:deep(.el-menu--popup) {
  min-width: 160px;
  padding: 8px;
  background-color: #f0f9ff;
  box-shadow: 0 2px 12px rgba(64,158,255,0.1);
  border-radius: 4px;

  .el-menu-item-group__title {
    padding: 6px 20px;
    font-size: 13px;
    color: #909399;
  }

  .el-menu-item {
    height: 40px;
    line-height: 40px;
    margin: 4px 0;
    padding: 0 20px !important;
    
    &:hover {
      background-color: rgba(64, 158, 255, 0.1) !important;
      color: #409EFF !important;
    }
    
    &.is-active {
      background-color: #409EFF !important;
      color: #fff !important;
    }
  }
}

/* 确保弹出菜单的定位和层级 */
.el-menu--popup-container {
  z-index: 2100 !important;
}

/* 调整水平菜单的子菜单样式 */
.el-menu--horizontal {
  .el-menu {
    .el-menu-item-group {
      .el-menu-item {
        height: 40px;
        line-height: 40px;
        padding: 0 20px !important;
        
        &:hover {
          background-color: rgba(64, 158, 255, 0.1) !important;
          color: #409EFF !important;
        }
        
        &.is-active {
          background-color: #409EFF !important;
          color: #fff !important;
        }
      }
    }
  }
}

/* 统一所有弹出菜单的背景和样式 */
.dataset-submenu {
  background-color: #f0f9ff !important;
  border: 1px solid rgba(64,158,255,0.1) !important;
  border-radius: 4px !important;
  box-shadow: 0 2px 12px rgba(64,158,255,0.1) !important;
  
  .el-menu-item {
    margin: 4px 8px !important;
    border-radius: 4px !important;
    
    &:hover {
      background-color: rgba(64, 158, 255, 0.1) !important;
      color: #409EFF !important;
    }
    
    &.is-active {
      background-color: #409EFF !important;
      color: #fff !important;
    }
  }
}

/* 调整滚动条样式 */
.sub-menu::-webkit-scrollbar {
  width: 6px;
}

.sub-menu::-webkit-scrollbar-thumb {
  background-color: rgba(64, 158, 255, 0.2);
  border-radius: 3px;
}

.sub-menu::-webkit-scrollbar-track {
  background-color: transparent;
}

/* 修改右侧装饰线条 */
.main-aside::after {
  content: '';
  position: absolute;
  top: -64px;
  right: 0;
  bottom: 0;
  width: 1px;
  background: linear-gradient(to bottom,
      rgba(64, 158, 255, 0.1),
      rgba(64, 158, 255, 0.2),
      rgba(64, 158, 255, 0.3),
      transparent
  );
}

/* 统一表单控件样式 */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper) {
  background: rgba(255, 255, 255, 0.7);
  box-shadow: 0 0 0 1px rgba(64,158,255,0.1);
  transition: all 0.3s;
}

:deep(.el-input__wrapper:hover),
:deep(.el-select__wrapper:hover) {
  background: #fff;
  box-shadow: 0 0 0 1px #409EFF;
}

/* 添加一些微妙的动画效果 */
.page-container {
  animation: fadeIn 0.3s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 优化滚动条样式 */
.main-content::-webkit-scrollbar {
  width: 6px;
}

.main-content::-webkit-scrollbar-thumb {
  background-color: rgba(64,158,255,0.2);
  border-radius: 3px;
}

.main-content::-webkit-scrollbar-track {
  background-color: transparent;
}

/* 表格行样式 */
:deep(.el-table tr) {
  background: transparent !important;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped) {
  background: rgba(64,158,255,0.02) !important;
}

:deep(.el-table__body tr:hover > td) {
  background: rgba(64,158,255,0.1) !important;
}

:deep(.el-table thead tr th) {
  background: rgba(64,158,255,0.05) !important;
  border-bottom: 1px solid rgba(64,158,255,0.1);
}

:deep(.el-table__body tr.current-row > td) {
  background: rgba(64,158,255,0.15) !important;
}

:deep(.el-table td),
:deep(.el-table th) {
  border-color: rgba(64,158,255,0.08);
}

/* 表格内标签样式优化 */
:deep(.el-tag) {
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(4px);
  border: 1px solid rgba(64,158,255,0.2);
}

:deep(.el-tag--success) {
  background: rgba(103, 194, 58, 0.1);
  border-color: rgba(103, 194, 58, 0.2);
  color: #67c23a;
}

:deep(.el-tag--danger) {
  background: rgba(245, 108, 108, 0.1);
  border-color: rgba(245, 108, 108, 0.2);
  color: #f56c6c;
}

:deep(.el-tag--warning) {
  background: rgba(230, 162, 60, 0.1);
  border-color: rgba(230, 162, 60, 0.2);
  color: #e6a23c;
}



.user-info:hover {
  background: rgba(64,158,255,0.05);
}

.user-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #2c3e50;
  cursor: pointer;
  padding: 0 4px;
  border-radius: 4px;
  transition: all 0.3s;
}

.user-name .el-icon {
  font-size: 12px;
  color: #909399;
  transition: all 0.3s;
}

.user-name:hover .el-icon {
  color: #409EFF;
  transform: translateY(2px);
}

/* 下拉菜单样式优化 */
:deep(.el-dropdown-menu) {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(64,158,255,0.1);
  box-shadow: 0 4px 12px rgba(64,158,255,0.1);
}

:deep(.el-dropdown-menu__item) {
  color: #2c3e50;
  transition: all 0.3s;
}

:deep(.el-dropdown-menu__item:hover) {
  background: rgba(64,158,255,0.05);
  color: #409EFF;
}

:deep(.el-dropdown-menu__item i) {
  margin-right: 8px;
  color: #909399;
}

:deep(.el-dropdown-menu__item:hover i) {
  color: #409EFF;
}

/* 历史导航样式 */
.breadcrumb-nav {
  display: flex;
  align-items: center;
  height: 40px;
  margin-bottom: 16px;
  padding: 0 16px;
  background: rgba(30, 60, 100, 0.6);
  backdrop-filter: blur(4px);
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.breadcrumb-nav :deep(.el-breadcrumb__item) {
  color: rgba(255, 255, 255, 0.8);
}

.breadcrumb-nav :deep(.el-breadcrumb__item .el-breadcrumb__inner) {
  color: rgba(255, 255, 255, 0.8);
}

.breadcrumb-nav :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: white;
}

/* 用户信息区域样式 */
.user-area {
  margin-left: auto;
  padding-left: 20px;
}

.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.15);
  transition: all 0.3s;
}

.user-info:hover {
  background: rgba(255, 255, 255, 0.25);
}

.user-avatar {
  background: linear-gradient(135deg, #409EFF, #36CBCB);
  color: #fff;
  font-weight: bold;
  margin-right: 8px;
}

.user-name {
  color: #fff;
  font-size: 14px;
  margin-right: 4px;
}

:deep(.el-dropdown-menu) {
  min-width: 150px;
  border-radius: 8px;
  padding: 8px 0;
  background: #fff;
  border: 1px solid rgba(64, 158, 255, 0.2);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

:deep(.el-dropdown-menu__item) {
  padding: 10px 20px;
  font-size: 14px;
  display: flex;
  align-items: center;
}

:deep(.el-dropdown-menu__item .el-icon) {
  margin-right: 8px;
  font-size: 16px;
}

:deep(.el-dropdown-menu__item:hover) {
  background-color: rgba(64, 158, 255, 0.1);
  color: #409EFF;
}

:deep(.el-dropdown-menu__item.is-disabled) {
  color: #C0C4CC;
  cursor: not-allowed;
}

.nav-left {
  display: flex;
  align-items: center;
  padding-right: 24px;
  border-right: 1px solid rgba(0, 0, 0, 0.06);
  height: 100%;
}

.history-nav {
  flex: 1;
  margin-left: 16px;
  height: 100%;
  display: flex;
  align-items: center;
}

.history-scrollbar {
  flex: 1;
  height: 100%;
}

.history-list {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 100%;
  padding: 0 8px;
}

.history-item {
  cursor: pointer;
  user-select: none;
  transition: all 0.2s;
  font-size: 13px;
  height: 40px;
  line-height: 40px;
  white-space: nowrap;
  color: #666;
  display: flex;
  align-items: center;
  gap: 6px;
  position: relative;
  padding: 0 4px;
}

/* 粒子效果 */
.particles-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  pointer-events: none;
  z-index: 0;
}

.particle {
  position: absolute;
  border-radius: 50%;
  background: linear-gradient(to right, #1890ff, #52c41a);
  z-index: 0;
  opacity: 0.2;
  animation: float 40s linear infinite;
  pointer-events: none;
  box-shadow: 
    0 0 10px rgba(24, 144, 255, 0.5),
    0 0 20px rgba(24, 144, 255, 0.3);
  filter: blur(1px);
}

@keyframes float {
  0% {
    transform: translateY(0) translateX(0) rotate(0deg);
  }
  25% {
    transform: translateY(-30vh) translateX(20vw) rotate(90deg);
  }
  50% {
    transform: translateY(10vh) translateX(-10vw) rotate(180deg);
  }
  75% {
    transform: translateY(20vh) translateX(30vw) rotate(270deg);
  }
  100% {
    transform: translateY(0) translateX(0) rotate(360deg);
  }
}

/* 鼠标跟随效果 */
.mouse-follower {
  position: fixed;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  background: radial-gradient(
    circle at center,
    rgba(24, 144, 255, 0.15),
    rgba(24, 144, 255, 0.08),
    rgba(24, 144, 255, 0)
  );
  transform: translate(-50%, -50%);
  pointer-events: none;
  z-index: 1;
  filter: blur(8px);
  animation: pulse 5s infinite alternate;
}

@keyframes pulse {
  0% {
    width: 200px;
    height: 200px;
    opacity: 0.15;
  }
  100% {
    width: 250px;
    height: 250px;
    opacity: 0.2;
  }
}

/* 提升内容层级 */
.el-container {
  position: relative;
  z-index: 10;
}

/* 表格行动画效果 */
.custom-table {
  :deep(.el-table__row) {
    position: relative;
    transition: transform 0.3s ease, box-shadow 0.3s ease;
    
    &:hover {
      transform: translateX(5px);
      z-index: 2;
      
      td {
        position: relative;
        background: rgba(64, 158, 255, 0.1) !important;
        
        &:first-child::before {
          content: '';
          position: absolute;
          left: 0;
          top: 0;
          height: 100%;
          width: 4px;
          background: linear-gradient(to bottom, #409EFF, #1890ff);
        }
      }
    }
  }
}

/* 数据加载动画 */
.loading-container {
  position: relative;
  min-height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.data-loading {
  display: inline-block;
  position: relative;
  width: 80px;
  height: 80px;
  
  div {
    position: absolute;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    background: #409EFF;
    animation: data-loading 1.2s linear infinite;
    
    &:nth-child(1) {
      top: 8px;
      left: 8px;
      animation-delay: 0s;
    }
    
    &:nth-child(2) {
      top: 8px;
      left: 32px;
      animation-delay: -0.4s;
    }
    
    &:nth-child(3) {
      top: 8px;
      left: 56px;
      animation-delay: -0.8s;
    }
    
    &:nth-child(4) {
      top: 32px;
      left: 8px;
      animation-delay: -0.4s;
    }
    
    &:nth-child(5) {
      top: 32px;
      left: 32px;
      animation-delay: -0.8s;
    }
    
    &:nth-child(6) {
      top: 32px;
      left: 56px;
      animation-delay: -1.2s;
    }
    
    &:nth-child(7) {
      top: 56px;
      left: 8px;
      animation-delay: -0.8s;
    }
    
    &:nth-child(8) {
      top: 56px;
      left: 32px;
      animation-delay: -1.2s;
    }
    
    &:nth-child(9) {
      top: 56px;
      left: 56px;
      animation-delay: -1.6s;
    }
  }
}

@keyframes data-loading {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(0.5);
  }
}

/* 添加表单输入框动画 */
:deep(.el-input__inner),
:deep(.el-textarea__inner) {
  transition: all 0.3s;
  box-shadow: 0 0 0 rgba(64, 158, 255, 0);
  
  &:focus {
    box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.1);
    transform: translateY(-1px);
  }
}

/* 鼠标交互式卡片效果 */
:deep(.el-card) {
  transform-style: preserve-3d;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-5px) rotateX(4deg);
    box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
    
    .el-card__header {
      background: linear-gradient(to right, #409EFF, #36CFC9);
      color: white;
    }
  }
  
  &.is-hoverable {
    position: relative;
    z-index: 1;
    
    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: radial-gradient(
        800px circle at var(--mouse-x) var(--mouse-y),
        rgba(64, 158, 255, 0.1),
        transparent 40%
      );
      z-index: 0;
      opacity: 0;
      transition: opacity 0.3s;
    }
    
    &:hover::after {
      opacity: 1;
    }
  }
}

/* 动态3D菜单项效果 */
.main-menu :deep(.el-menu-item), 
.main-menu :deep(.el-sub-menu__title) {
  transform-style: preserve-3d;
  perspective: 800px;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 4px;
    height: 100%;
    background: linear-gradient(to bottom, #409EFF, #36CFC9);
    transform: translateX(-4px);
    transition: transform 0.3s ease;
    z-index: 1;
  }
  
  &:hover {
    transform: translateZ(10px);
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);
    
    &::before {
      transform: translateX(0);
    }
  }
  
  .el-icon {
    transition: all 0.3s ease;
    
    svg {
      transition: all 0.3s ease;
    }
  }
  
  &:hover .el-icon {
    transform: translateZ(20px) scale(1.2);
    color: #409EFF;
    
    svg {
      filter: drop-shadow(0 0 2px rgba(64, 158, 255, 0.5));
    }
  }
}

/* 更强大的鼠标跟踪效果 */
.mouse-follower {
  position: fixed;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  pointer-events: none;
  z-index: 1;
  opacity: 0.7;
  mix-blend-mode: screen;
  background: radial-gradient(
    circle at center,
    rgba(64, 158, 255, 0.3),
    rgba(54, 207, 201, 0.15),
    transparent 70%
  );
  filter: blur(7px);
  transition: width 0.3s, height 0.3s, opacity 0.3s;
}

/* 互动表格行效果 */
:deep(.el-table__row) {
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  transform-origin: center;
  
  &:hover {
    transform: scale(1.01) translateX(5px);
    z-index: 2;
    
    td {
      background: rgba(64, 158, 255, 0.08) !important;
      
      &:first-child {
        position: relative;
        
        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 0;
          height: 100%;
          width: 3px;
          background: #409EFF;
          box-shadow: 0 0 8px rgba(64, 158, 255, 0.6);
          animation: pulseLight 2s infinite alternate;
        }
      }
    }
  }
}

@keyframes pulseLight {
  0% { opacity: 0.7; }
  100% { opacity: 1; }
}

/* 文字悬停动画效果 */
.page-title, 
h1, h2, h3, h4, h5,
:deep(.el-descriptions__title) {
  position: relative;
  display: inline-block;
  
  &::after {
    content: '';
    position: absolute;
    bottom: -2px;
    left: 0;
    width: 100%;
    height: 2px;
    background: linear-gradient(to right, #409EFF, transparent);
    transform: scaleX(0);
    transform-origin: left;
    transition: transform 0.4s cubic-bezier(0.23, 1, 0.32, 1);
  }
  
  &:hover::after {
    transform: scaleX(1);
  }
}

/* 表单控件交互动画 */
:deep(.el-input),
:deep(.el-select),
:deep(.el-date-editor) {
  &:hover {
    .el-input__wrapper {
      box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.5);
    }
  }
  
  .el-input__wrapper {
    position: relative;
    overflow: hidden;
    transition: all 0.3s ease, box-shadow 0.3s ease;
    
    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      width: 100%;
      height: 2px;
      background: linear-gradient(to right, #409EFF, #36CFC9);
      transform: translateY(2px);
      transition: transform 0.3s ease;
    }
    
    &:focus-within {
      box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.2);
      transform: translateY(-1px);
      
      &::after {
        transform: translateY(0);
      }
    }
  }
}

/* 交互式背景样式 */
.interactive-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
}

/* 高级粒子效果 */
.particles-system {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 1;
}

.particle-advanced {
  position: absolute;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: rgba(64, 158, 255, 0.3);
  box-shadow: 0 0 8px rgba(64, 158, 255, 0.3);
  z-index: 1;
  animation: float-advanced 40s linear infinite;
  transition: transform 1s cubic-bezier(0.23, 1, 0.32, 1);
  
  @for $i from 1 through 30 {
    &:nth-child(#{$i}) {
      top: random(100) * 1%;
      left: random(100) * 1%;
      width: (random(5) + 2) * 1px;
      height: (random(5) + 2) * 1px;
      opacity: (random(6) + 2) * 0.1;
      animation-duration: (random(40) + 20) * 1s;
      animation-delay: random(10) * 1s;
      filter: blur(random(2) * 1px);
    }
  }
}

/* 粒子爆发效果 */
.particle-burst {
  position: absolute;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: rgba(64, 158, 255, 0.8);
  box-shadow: 0 0 10px rgba(64, 158, 255, 0.5);
  z-index: 2;
  animation: burst 2s forwards cubic-bezier(0.215, 0.61, 0.355, 1);
}

@keyframes burst {
  0% {
    transform: scale(1) translate(0, 0);
    opacity: 1;
  }
  100% {
    transform: scale(0) translate(calc(var(--vx) * 25), calc(var(--vy) * 25));
    opacity: 0;
  }
}

@keyframes float-advanced {
  0% {
    transform: translate(0, 0) rotate(0deg);
  }
  25% {
    transform: translate(-20vw, -15vh) rotate(90deg);
  }
  50% {
    transform: translate(15vw, -25vh) rotate(180deg);
  }
  75% {
    transform: translate(-10vw, 20vh) rotate(270deg);
  }
  100% {
    transform: translate(0, 0) rotate(360deg);
  }
}

/* 动态网格 */
.dynamic-grid {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.grid-cell {
  position: absolute;
  border-radius: 2px;
  transition: all 0.5s cubic-bezier(0.23, 1, 0.32, 1);
}

/* 波纹效果 */
.ripple-container {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
  pointer-events: none;
}

.ripple {
  position: absolute;
  width: 0;
  height: 0;
  border-radius: 50%;
  border: 1px solid rgba(64, 158, 255, 0.5);
  transform: translate(-50%, -50%);
  transition: all 0.2s ease-out;
}

/* 视差背景 */
.parallax-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 0;
}

.parallax-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  transition: transform 0.2s cubic-bezier(0.23, 1, 0.32, 1);
  background-repeat: no-repeat;
  background-position: center;
  background-size: cover;
  opacity: 0.1;
}

.layer-1 {
  background-image: radial-gradient(
    circle at 30% 40%,
    rgba(64, 158, 255, 0.1),
    transparent 70%
  );
}

.layer-2 {
  background-image: radial-gradient(
    circle at 70% 60%,
    rgba(103, 194, 58, 0.1),
    transparent 70%
  );
}

.layer-3 {
  background-image: radial-gradient(
    circle at 50% 30%,
    rgba(245, 108, 108, 0.05),
    transparent 60%
  );
}

/* 禁用粒子效果 */
.particles-container, .particle, .interactive-background, .mouse-follower {
  display: none !important;
}

/* 移除表格行动画效果 */
.custom-table :deep(.el-table__row) {
  transition: none !important;
}

.custom-table :deep(.el-table__row:hover) {
  transform: none !important;
}

/* 移除菜单项悬停动画 */
.main-menu :deep(.el-menu-item), 
.main-menu :deep(.el-sub-menu__title) {
  transform-style: flat !important;
  transition: background-color 0.3s ease !important;
}

.main-menu :deep(.el-menu-item:hover), 
.main-menu :deep(.el-sub-menu__title:hover) {
  transform: none !important;
}

.main-menu :deep(.el-menu-item:hover .el-icon), 
.main-menu :deep(.el-sub-menu__title:hover .el-icon) {
  transform: none !important;
}

/* 移除卡片悬停效果 */
:deep(.el-card) {
  transition: none !important;
}

:deep(.el-card:hover) {
  transform: none !important;
}

/* 移除页面容器动画 */
.page-container {
  animation: none !important;
  transform: none !important;
  transition: none !important;
}

/* 移除鼠标交互特效 */
.particles-system, .dynamic-grid, .ripple-container, .parallax-bg {
  display: none !important;
}

/* 移除文字悬停效果 */
.page-title::after, h1::after, h2::after, h3::after {
  display: none !important;
}

/* 为确保稳定的动画体验，重写关键过渡效果 */
* {
  transition-duration: 0.3s !important;
  animation-duration: 0.3s !important;
  transition-timing-function: ease !important;
  animation-timing-function: ease !important;
}

@keyframes none {
  0%, 100% { opacity: 1; }
}

.user-name:hover .el-icon {
  transform: none !important;
}

/* 移除背景脉动效果 */
@keyframes bgPulse {
  0%, 100% {
    background-image: linear-gradient(135deg, rgba(10, 30, 60, 0.95) 0%, rgba(15, 35, 70, 0.9) 100%);
  }
}
</style>

