<script setup>
import HelloWorld from './components/HelloWorld.vue'
</script>

<template>
  <router-view></router-view>
</template>

<style>
:root {
  /* 主题色系 */
  --primary-gradient: linear-gradient(135deg, #00B4DB 0%, #0083B0 100%);
  --secondary-gradient: linear-gradient(135deg, #1A2980 0%, #26D0CE 100%);
  --dark-gradient: linear-gradient(to right, #141E30 0%, #243B55 100%);
  --glass-gradient: linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.05) 100%);

  /* 主要颜色 */
  --primary-color: #00B4DB;
  --secondary-color: #26D0CE;
  --background-color: #0F1924;
  --surface-color: #1A2634;
  --border-color: rgba(255, 255, 255, 0.1);

  /* 文字颜色 */
  --text-primary: rgba(255, 255, 255, 0.95);
  --text-secondary: rgba(255, 255, 255, 0.7);
  --text-disabled: rgba(255, 255, 255, 0.4);
}

body {
  background-color: var(--background-color);
  color: var(--text-primary);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
}

#app {
  background: var(--dark-gradient);
  min-height: 100vh;
}

/* 布局容器样式 */
.arco-layout {
  background: var(--dark-gradient) !important;
}

/* 顶部导航样式 */
.arco-layout-header {
  background: var(--surface-color) !important;
  border-bottom: 1px solid var(--border-color) !important;
  backdrop-filter: blur(10px);
}

/* 侧边栏样式 */
.arco-layout-sider {
  background: var(--surface-color) !important;
  border-right: 1px solid var(--border-color) !important;
  backdrop-filter: blur(10px);
}

/* 主要内容区域样式 */
.arco-layout-content {
  background: transparent !important;
}

/* 导航菜单样式 */
.arco-menu {
  background: transparent !important;
}

.arco-menu-dark {
  background: var(--surface-color) !important;
}

.arco-menu-item {
  color: var(--text-secondary) !important;
}

.arco-menu-item:hover {
  color: var(--text-primary) !important;
  background: var(--glass-gradient) !important;
}

.arco-menu-item.arco-menu-selected {
  color: white !important;
  background: var(--primary-gradient) !important;
  border-radius: 4px;
}

.arco-menu-inline-header {
  color: var(--text-secondary) !important;
}

.arco-menu-inline-header:hover {
  color: var(--text-primary) !important;
  background: var(--glass-gradient) !important;
}

.arco-menu-sub {
  background: transparent !important;
}

/* Logo 区域样式 */
.logo-container {
  background: var(--surface-color) !important;
  border-bottom: 1px solid var(--border-color);
}

/* 面包屑导航样式 */
.arco-breadcrumb {
  color: var(--text-secondary) !important;
}

.arco-breadcrumb-item:last-child {
  color: var(--text-primary) !important;
}

/* 按钮样式 */
.arco-btn {
  border-radius: 6px;
  backdrop-filter: blur(10px);
  border: 1px solid var(--border-color) !important;
}

.arco-btn.arco-btn-primary {
  background: var(--primary-gradient) !important;
  border: none !important;
  box-shadow: 0 4px 15px rgba(0, 180, 219, 0.3);
}

.arco-btn.arco-btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 180, 219, 0.4);
}

.arco-btn.arco-btn-secondary {
  background: var(--glass-gradient) !important;
  backdrop-filter: blur(10px);
}

.arco-btn.arco-btn-secondary:hover {
  background: rgba(255, 255, 255, 0.15) !important;
}

/* 卡片样式 */
.arco-card {
  background: var(--surface-color) !important;
  border: 1px solid var(--border-color) !important;
  border-radius: 12px;
  backdrop-filter: blur(10px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.arco-card .arco-card-header {
  border-bottom: 1px solid var(--border-color);
}

/* 表格样式 */
.arco-table {
  background: transparent !important;
  color: var(--text-primary);
}

.arco-table .arco-table-th {
  background: rgba(255, 255, 255, 0.05) !important;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border-color);
}

.arco-table .arco-table-td {
  border-bottom: 1px solid var(--border-color);
  color: var(--text-primary);
}

.arco-table .arco-table-tr:hover td {
  background: rgba(255, 255, 255, 0.05) !important;
}

/* 标签样式 */
.arco-tag {
  background: var(--glass-gradient) !important;
  border: none !important;
  backdrop-filter: blur(5px);
  color: var(--text-primary) !important;
}

/* 输入框样式 */
.arco-input-wrapper {
  background: rgba(255, 255, 255, 0.05) !important;
  border: 1px solid var(--border-color) !important;
  border-radius: 6px;
}

.arco-input-wrapper input {
  color: var(--text-primary) !important;
}

.arco-input-wrapper:hover,
.arco-input-wrapper:focus {
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 2px rgba(0, 180, 219, 0.2) !important;
}

/* 选择器样式 */
.arco-select-view {
  background: rgba(255, 255, 255, 0.05) !important;
  border: 1px solid var(--border-color) !important;
  border-radius: 6px;
  color: var(--text-primary) !important;
}

/* 模态框样式 */
.arco-modal {
  background: var(--surface-color) !important;
  border-radius: 16px;
  backdrop-filter: blur(20px);
  border: 1px solid var(--border-color);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

.arco-modal .arco-modal-header {
  background: transparent;
  border-bottom: 1px solid var(--border-color);
}

/* 下拉菜单样式 */
.arco-dropdown-menu {
  background: var(--surface-color) !important;
  border: 1px solid var(--border-color) !important;
  border-radius: 8px;
  backdrop-filter: blur(10px);
}

.arco-dropdown-menu .arco-dropdown-menu-item {
  color: var(--text-secondary) !important;
}

.arco-dropdown-menu .arco-dropdown-menu-item:hover {
  background: var(--glass-gradient) !important;
  color: var(--text-primary) !important;
}

/* 滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}
</style>
