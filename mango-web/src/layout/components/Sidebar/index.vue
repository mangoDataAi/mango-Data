<template>
  <div class="sidebar-container">
    <el-menu
      :default-active="activeMenu"
      :collapse="isCollapse"
      :unique-opened="false"
      :collapse-transition="false"
      mode="vertical"
    >
      <!-- 数据集菜单 -->
      <el-sub-menu
        index="/dataset"
        :popper-options="{
          modifiers: [
            {
              name: 'offset',
              options: {
                offset: [0, 0]
              }
            }
          ]
        }"
      >
        <template #title>
          <el-icon><DataAnalysis /></el-icon>
          <span>数据集</span>
        </template>

        <!-- 数据源子菜单 -->
        <el-sub-menu
          index="/dataset/datasource"
          :show-timeout="0"
          :hide-timeout="0"
        >
          <template #title>数据源</template>
          <el-menu-item index="/dataIntegration/source/database">数据库连接池</el-menu-item>
          <el-menu-item index="/dataset/file">文件数据源</el-menu-item>
          <el-menu-item index="/dataset/api">接口数据源</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/dataset/modeling">数据建模</el-menu-item>
        <el-menu-item index="/dataset/etl">数据ETL</el-menu-item>
      </el-sub-menu>

      <!-- 数据服务菜单 -->
      <el-sub-menu index="/dataService">
        <template #title>
          <el-icon><Service /></el-icon>
          <span>数据服务</span>
        </template>
        
        <el-sub-menu index="/dataService/api">
          <template #title>
            <el-icon><Connection /></el-icon>
            <span>API管理</span>
          </template>
          <el-menu-item index="/dataService/api/develop">API开发</el-menu-item>
          <el-menu-item index="/dataService/api/test">API测试</el-menu-item>
          <el-menu-item index="/dataService/api/publish">API发布</el-menu-item>
          <el-menu-item index="/dataService/api/monitor">API监控</el-menu-item>
          <el-menu-item index="/dataService/api/version">版本管理</el-menu-item>
          <el-menu-item index="/dataService/api/doc">文档管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/dataService/service">
          <template #title>
            <el-icon><Service /></el-icon>
            <span>服务管理</span>
          </template>
          <el-menu-item index="/dataService/service/register">服务注册</el-menu-item>
          <el-menu-item index="/dataService/service/config">服务配置</el-menu-item>
          <el-menu-item index="/dataService/service/monitor">服务监控</el-menu-item>
          <el-menu-item index="/dataService/service/statistics">服务统计</el-menu-item>
          <el-menu-item index="/dataService/service/schedule">服务编排</el-menu-item>
          <el-menu-item index="/dataService/service/governance">服务治理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/dataService/auth">
          <template #title>
            <el-icon><Lock /></el-icon>
            <span>权限管理</span>
          </template>
          <el-menu-item index="/dataService/auth/app">应用管理</el-menu-item>
          <el-menu-item index="/dataService/auth/auth">授权管理</el-menu-item>
          <el-menu-item index="/dataService/auth/quota">配额管理</el-menu-item>
          <el-menu-item index="/dataService/auth/billing">计费管理</el-menu-item>
          <el-menu-item index="/dataService/auth/usage">用量统计</el-menu-item>
          <el-menu-item index="/dataService/auth/bill">账单管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="/dataService/ops">
          <template #title>
            <el-icon><Monitor /></el-icon>
            <span>运维监控</span>
          </template>
          <el-menu-item index="/dataService/ops/service">服务监控</el-menu-item>
          <el-menu-item index="/dataService/ops/performance">性能监控</el-menu-item>
          <el-menu-item index="/dataService/ops/alert">告警管理</el-menu-item>
          <el-menu-item index="/dataService/ops/log">日志分析</el-menu-item>
          <el-menu-item index="/dataService/ops/trace">链路追踪</el-menu-item>
          <el-menu-item index="/dataService/ops/capacity">容量规划</el-menu-item>
        </el-sub-menu>
      </el-sub-menu>

      <!-- 其他菜单项 -->
      <sidebar-item
        v-for="route in otherRoutes"
        :key="route.path"
        :item="route"
        :base-path="route.path"
      />
    </el-menu>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DataAnalysis, Service, Connection, Lock, Monitor } from '@element-plus/icons-vue'
import SidebarItem from './SidebarItem.vue'

const route = useRoute()
const router = useRouter()
const activeMenu = computed(() => route.path)

// 处理菜单点击
const handleMenuClick = (path) => {
  router.push(path)
}

// 其他路由（除了数据集以外的）
const otherRoutes = computed(() => {
  return router.options.routes.find(r => r.path === '/').children
    .filter(route => route.path !== '/dataset')
})
</script>

<style lang="scss" scoped>
.sidebar-container {
  padding-left: 0;
  height: 100%;
  background-color: var(--el-menu-bg-color);
  position: relative;
  overflow: hidden;
  
  /* 减弱渐变背景效果 */
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, rgba(255,255,255,0.01) 0%, rgba(64,158,255,0.02) 100%);
    z-index: 0;
  }
  
  /* 减弱背景网格点效果 */
  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-image: 
      radial-gradient(rgba(64, 158, 255, 0.05) 1px, transparent 1px),
      radial-gradient(rgba(64, 158, 255, 0.03) 1px, transparent 1px);
    background-size: 30px 30px;
    background-position: 0 0, 15px 15px;
    animation: backgroundMove 180s linear infinite;
    opacity: 0.2;
    z-index: 0;
  }

  .el-menu {
    position: relative;
    z-index: 1;
    background: transparent !important;
  }

  .el-menu-item,
  .el-sub-menu__title {
    position: relative;
    overflow: hidden;
    transition: all 0.6s cubic-bezier(0.25, 0.8, 0.25, 1) !important;
    background: transparent !important;
    
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      height: 100%;
      width: 2px;
      background-color: #409EFF;
      opacity: 0;
      transition: opacity 0.5s ease;
    }
    
    &:hover::before {
      opacity: 0.6;
    }
    
    &:hover {
      background: rgba(64, 158, 255, 0.03) !important;
      transform: translateX(1px);
      box-shadow: 0 0 5px rgba(64, 158, 255, 0.05);
    }
    
    // 减弱波纹效果
    &:active::after {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      width: 5px;
      height: 5px;
      background: rgba(255, 255, 255, 0.2);
      opacity: 0.5;
      border-radius: 100%;
      transform: scale(1, 1) translate(-50%, -50%);
      transform-origin: 50% 50%;
      animation: menu-ripple 0.8s ease-out;
    }
  }
  
  .el-menu-item.is-active,
  .el-sub-menu.is-active .el-sub-menu__title {
    &::before {
      opacity: 0.7;
      box-shadow: 0 0 3px rgba(64, 158, 255, 0.3);
    }
    
    /* 减弱发光效果 */
    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(90deg, rgba(64, 158, 255, 0.05) 0%, rgba(64, 158, 255, 0) 100%);
      z-index: -1;
      animation: activeGlow 3s infinite alternate;
    }
  }
  
  // 减弱图标动画
  .svg-icon, .el-icon {
    transition: all 0.5s ease;
    position: relative;
    
    /* 减弱脉冲效果 */
    &::after {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      width: 100%;
      height: 100%;
      background: rgba(64, 158, 255, 0.1);
      border-radius: 50%;
      transform: translate(-50%, -50%) scale(0);
      opacity: 0;
      z-index: -1;
      transition: transform 0.5s, opacity 0.5s;
    }
  }
  
  .el-menu-item:hover .svg-icon,
  .el-sub-menu__title:hover .svg-icon,
  .el-menu-item:hover .el-icon,
  .el-sub-menu__title:hover .el-icon {
    transform: scale(1.05);
    color: #409EFF;
    
    &::after {
      transform: translate(-50%, -50%) scale(1.2);
      opacity: 0.1;
    }
  }
}

/* 减弱波纹动画 */
@keyframes menu-ripple {
  0% {
    transform: scale(0, 0) translate(-50%, -50%);
    opacity: 0.3;
  }
  100% {
    transform: scale(10, 10) translate(-50%, -50%);
    opacity: 0;
  }
}

/* 减缓背景移动动画 */
@keyframes backgroundMove {
  0% {
    background-position: 0 0, 10px 10px;
  }
  100% {
    background-position: 500px 500px, 510px 510px;
  }
}

/* 减弱发光动画 */
@keyframes activeGlow {
  0% {
    opacity: 0.1;
  }
  100% {
    opacity: 0.2;
  }
}

.el-menu {
  border-right: none;
}

/* 调整子菜单样式 */
.el-menu--popup {
  min-width: 160px;
  padding: 5px 0;
  backdrop-filter: blur(5px);
  background-color: rgba(255, 255, 255, 0.85) !important;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1), 0 0 20px rgba(64, 158, 255, 0.1);
  border-radius: 6px;
  overflow: hidden;
}

.el-sub-menu__title {
  padding-right: 40px !important;
}

/* 确保子菜单悬浮时保持显示 */
.el-menu--vertical .el-menu--popup {
  max-height: none;
}

.el-menu--vertical .el-menu-item {
  height: 40px;
  line-height: 40px;
  position: relative;
  overflow: hidden;
  
  /* Add subtle hover effect for submenu items */
  &::after {
    content: '';
    position: absolute;
    left: 0;
    bottom: 0;
    width: 100%;
    height: 1px;
    background: linear-gradient(90deg, transparent, rgba(64, 158, 255, 0.2), transparent);
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }
  
  &:hover::after {
    transform: translateX(0);
  }
}

/* 调整子菜单的显示位置 */
.el-menu--vertical .el-menu.el-menu--popup {
  position: fixed;
}

/* 子菜单项hover效果减弱 */
.el-menu-item:hover {
  background-color: rgba(64, 158, 255, 0.03) !important;
  transform: translateX(1px);
}

/* 确保子菜单在最上层 */
.el-menu--popup-container {
  z-index: 2100 !important;
}

/* 调整子菜单的过渡效果 */
.el-menu--collapse-transition {
  transition: 0.3s ease-in-out !important;
}
</style>
