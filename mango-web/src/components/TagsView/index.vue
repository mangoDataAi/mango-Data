<template>
  <div class="tags-view-container">
    <scroll-pane ref="scrollPane" class="tags-view-wrapper">
      <router-link
          v-for="tag in visitedViews"
          :key="tag.path"
          :class="isActive(tag) ? 'active' : ''"
          :to="{ path: tag.path, query: tag.query, fullPath: tag.fullPath }"
          class="tags-view-item"
          @click.middle="!isAffix(tag) && closeSelectedTag(tag)"
          @contextmenu.prevent="openMenu(tag, $event)"
      >
        {{ tag.title }}
        <span v-if="!isAffix(tag)" class="el-icon-close" @click.prevent.stop="closeSelectedTag(tag)" />
      </router-link>
    </scroll-pane>
    <ul v-show="visible" :style="{left: left+'px', top: top+'px'}" class="contextmenu">
      <li @click="refreshSelectedTag(selectedTag)">刷新页面</li>
      <li v-if="!isAffix(selectedTag)" @click="closeSelectedTag(selectedTag)">关闭当前</li>
      <li @click="closeOthersTags">关闭其他</li>
      <li @click="closeAllTags(selectedTag)">关闭所有</li>
    </ul>
  </div>
</template>

<style lang="scss" scoped>
.tags-view-container {
  height: 34px;
  width: 100%;
  background: #fff;
  border-bottom: 1px solid #d8dce5;
  box-shadow: 0 1px 3px 0 rgba(0, 0, 0, .12), 0 0 3px 0 rgba(0, 0, 0, .04);

  .tags-view-wrapper {
    .tags-view-item {
      display: inline-block;
      position: relative;
      cursor: pointer;
      height: 26px;
      line-height: 26px;
      border: 1px solid #d8dce5;
      color: #495060;
      background: #fff;
      padding: 0 8px;
      font-size: 14px;  // 增大字体大小
      margin-left: 5px;
      margin-top: 4px;
      text-decoration: none;

      &:first-of-type {
        margin-left: 15px;
      }

      &:last-of-type {
        margin-right: 15px;
      }

      &.active {
        background-color: #42b983;
        color: #fff;
        border-color: #42b983;

        &::before {
          content: '';
          background: #fff;
          display: inline-block;
          width: 8px;
          height: 8px;
          border-radius: 50%;
          position: relative;
          margin-right: 4px;
        }
      }
    }
  }

  .contextmenu {
    margin: 0;
    background: #fff;
    z-index: 3000;
    position: absolute;
    list-style-type: none;
    padding: 5px 0;
    border-radius: 4px;
    font-size: 14px;  // 增大右键菜单字体大小
    font-weight: 400;
    color: #333;
    box-shadow: 2px 2px 3px 0 rgba(0, 0, 0, .3);

    li {
      margin: 0;
      padding: 7px 16px;
      cursor: pointer;

      &:hover {
        background: #eee;
      }
    }
  }
}
</style>

<script setup>
import { ref, computed, onMounted, watch, nextTick, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTagsViewStore } from '@/store/modules/tagsView'
import ScrollPane from './ScrollPane.vue'

const route = useRoute()
const router = useRouter()
const tagsViewStore = useTagsViewStore()

// 视图相关的状态
const visible = ref(false)
const top = ref(0)
const left = ref(0)
const selectedTag = ref(null)
const scrollPane = ref(null)

const menuLeft = ref(0)
const menuTop = ref(0)
const menuMinWidth = 105

// 计算属性
const visitedViews = computed(() => tagsViewStore.visitedViews)

// 过滤固定标签
const filterAffixTags = (routes, basePath = '/') => {
  let tags = []
  routes.forEach(route => {
    if (route.meta && route.meta.affix) {
      const tagPath = route.path
      tags.push({
        fullPath: tagPath,
        path: tagPath,
        name: route.name,
        meta: { ...route.meta }
      })
    }
    if (route.children) {
      const childTags = filterAffixTags(route.children, route.path)
      if (childTags.length >= 1) {
        tags = [...tags, ...childTags]
      }
    }
  })
  return tags
}

// 移动到当前标签
const moveToCurrentTag = () => {
  nextTick(() => {
    for (const tag of visitedViews.value) {
      if (tag.path === route.path) {
        scrollPane.value?.moveToTarget(tag)
        if (tag.fullPath !== route.fullPath) {
          tagsViewStore.updateVisitedView(route)
        }
        break
      }
    }
  })
}

// 初始化
onMounted(() => {
  initTags()
  addTags()
})

// 初始化标签
const initTags = () => {
  tagsViewStore.restoreViews()
  const affixTags = filterAffixTags(router.getRoutes())
  for (const tag of affixTags) {
    if (tag.name) {
      tagsViewStore.addVisitedView(tag)
    }
  }
  // 添加当前路由
  if (route.name) {
    tagsViewStore.addView(route)
  }
}

// 判断是否是活动标签
const isActive = (tag) => {
  return tag.path === route.path
}

// 判断是否是固定标签
const isAffix = (tag) => {
  return tag.meta && tag.meta.affix
}

// 关闭选中标签
const closeSelectedTag = async (view) => {
  await tagsViewStore.delView(view)
  if (isActive(view)) {
    toLastView()
  }
}

// 关闭其他标签
const closeOthersTags = () => {
  tagsViewStore.delOthersViews(route)
}

// 关闭所有标签
const closeAllTags = (view) => {
  tagsViewStore.delAllViews()
  if (view.meta?.affix) return
  toLastView()
}

// 刷新选中标签
const refreshSelectedTag = (view) => {
  tagsViewStore.delCachedView(view)
  router.replace({
    path: '/redirect' + view.path
  })
}

// 打开右键菜单
const openMenu = (tag, e) => {
  const menuContainer = document.querySelector('.tags-view-container')
  const offsetLeft = menuContainer.getBoundingClientRect().left
  const offsetWidth = menuContainer.offsetWidth
  const maxLeft = offsetWidth - menuMinWidth
  const menuLeftValue = e.clientX - offsetLeft + 15

  if (menuLeftValue > maxLeft) {
    menuLeft.value = maxLeft
  } else {
    menuLeft.value = menuLeftValue
  }
  menuTop.value = e.clientY
  visible.value = true
  selectedTag.value = tag
}

// 关闭右键菜单
const closeMenu = () => {
  this.visible = false
}

// 跳转到最后一个标签
const toLastView = () => {
  const latestView = visitedViews.value.slice(-1)[0]
  if (latestView) {
    router.push(latestView.fullPath)
  } else {
    router.push('/')
  }
}

// 监听路由变化
watch(route, () => {
  addTags()
  moveToCurrentTag()
})

// 添加标签
const addTags = () => {
  if (route.name) {
    tagsViewStore.addView(route)
  }
}

// 监听点击事件关闭菜单
onMounted(() => {
  document.addEventListener('click', closeMenu)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeMenu)
})
</script>
