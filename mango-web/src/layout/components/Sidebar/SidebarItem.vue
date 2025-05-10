<template>
  <div v-if="!item.hidden">
    <template v-if="hasOneShowingChild(item.children, item)">
      <app-link v-if="onlyOneChild.meta" :to="resolvePath(onlyOneChild.path)">
        <el-menu-item :index="resolvePath(onlyOneChild.path)">
          <el-icon v-if="onlyOneChild.meta.icon"><component :is="onlyOneChild.meta.icon" /></el-icon>
          <template #title>{{ onlyOneChild.meta.title }}</template>
        </el-menu-item>
      </app-link>
    </template>
    <el-sub-menu 
      v-else 
      :index="resolvePath(item.path)"
      popper-class="custom-popper"
      :show-timeout="100"
      :hide-timeout="100"
    >
      <template #title>
        <el-icon v-if="item.meta && item.meta.icon"><component :is="item.meta.icon" /></el-icon>
        <span>{{ item.meta.title }}</span>
      </template>
      <template v-if="item.children">
        <template v-for="child in item.children" :key="child.path">
          <template v-if="item.name === 'Metadata'">
            <el-menu-item :index="resolvePath(child.path)">
              <template #title>{{ child.meta.title }}</template>
            </el-menu-item>
          </template>
          <sidebar-item
            v-else
            :item="child"
            :base-path="resolvePath(child.path)"
          />
        </template>
      </template>
    </el-sub-menu>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import path from 'path-browserify'

const props = defineProps({
  item: {
    type: Object,
    required: true
  },
  basePath: {
    type: String,
    default: ''
  }
})

// 判断是否有子菜单
const hasChildren = (route) => {
  return route.children && route.children.length > 0
}

// 解析路径
const resolvePath = (routePath) => {
  if (path.isAbsolute(routePath)) {
    return routePath
  }
  return path.resolve(props.basePath, routePath)
}

// 判断是否只有一个子菜单
const hasOneShowingChild = (children, item) => {
  const showingChildren = children.filter(child => !child.hidden)
  const showingCount = showingChildren.length
  if (showingCount === 1) {
    props.onlyOneChild = showingChildren[0]
    return true
  }
  return false
}
</script>

<style scoped>
.el-menu--popup {
  min-width: 200px;
}

.custom-popper {
  background-color: #fff !important;
}

/* 悬浮展示子菜单 */
.el-sub-menu:hover > .el-sub-menu__title + .el-menu {
  display: block !important;
}

.el-menu--popup {
  position: absolute;
  z-index: 100;
  border-radius: 4px;
  padding: 5px 0;
}

.el-menu-item {
  height: 40px;
  line-height: 40px;
  padding: 0 20px;
  list-style: none;
  cursor: pointer;
  transition: border-color .3s,background-color .3s,color .3s;
}

.el-menu-item:hover {
  background-color: #ecf5ff;
}

/* 元数据子菜单样式 */
:deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: var(--el-menu-active-color);
}

:deep(.el-menu--popup) {
  min-width: 160px;
  padding: 5px 0;
}

:deep(.el-menu--popup .el-menu-item) {
  height: 36px;
  line-height: 36px;
  padding: 0 16px;
}
</style> 