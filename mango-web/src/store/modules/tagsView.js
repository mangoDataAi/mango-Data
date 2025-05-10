import { defineStore } from 'pinia'
import router from '@/router'
import { getGroupInfo } from '@/api/group'

// 固定的路由标题映射
const fixedTitles = {
  'PhysicalModeling': '物理建模分组'
}

// 用于获取动态标题的函数
const getDynamicTitle = async (view) => {
  if (view.meta?.dynamicTitle && view.params?.groupId) {
    try {
      // 从后端获取分组信息
      const res = await getGroupInfo(view.params.groupId)
      if (res.code === 0 && res.data) {
        return res.data.name || view.meta.title
      }
    } catch (error) {
      console.error('获取分组信息失败:', error)
    }
  }
  return fixedTitles[view.name] || view.meta?.title || 'no-name'
}

// 用于序列化路由对象的函数
const serializeView = (view) => {
  return {
    name: view.name,
    path: view.path,
    fullPath: view.fullPath || view.path,
    meta: { ...view.meta },
    title: view.meta?.title,
    query: { ...view.query },
    params: { ...view.params }
  }
}

// 用于从存储中恢复路由对象的函数
const deserializeView = (stored) => {
  const route = router.resolve({
    name: stored.name,
    params: stored.params || {},
    query: stored.query || {}
  })
  return {
    ...stored,
    fullPath: route.fullPath
  }
}

export const useTagsViewStore = defineStore('tagsView', {
  state: () => ({
    visitedViews: [],
    cachedViews: []
  }),

  actions: {
    addView(view) {
      this.addVisitedView(view)
      this.addCachedView(view)
      this.saveViewsToStorage()
    },

    addVisitedView(view) {
      if (this.visitedViews.some(v => v.path === view.path)) {
        // 如果已存在，更新标题
        this.updateVisitedView(view)
        return
      }
      this.visitedViews.push(serializeView(view))
    },

    addCachedView(view) {
      if (this.cachedViews.includes(view.name)) return
      if (view.meta?.keepAlive) {
        this.cachedViews.push(view.name)
      }
    },

    saveViewsToStorage() {
      try {
        const viewsToSave = this.visitedViews.map(serializeView)
        localStorage.setItem('tagsView', JSON.stringify(viewsToSave))
        localStorage.setItem('cachedViews', JSON.stringify(this.cachedViews))
      } catch (error) {
        console.error('保存视图失败:', error)
      }
    },

    restoreViews() {
      try {
        // 恢复访问记录
        const storedViews = localStorage.getItem('tagsView')
        if (storedViews) {
          const views = JSON.parse(storedViews)
          this.visitedViews = views.map(deserializeView)
              .filter(view => router.hasRoute(view.name))  // 过滤掉不存在的路由
        }

        // 恢复缓存记录
        const storedCache = localStorage.getItem('cachedViews')
        if (storedCache) {
          this.cachedViews = JSON.parse(storedCache)
        }
      } catch (error) {
        console.error('恢复视图失败:', error)
        this.visitedViews = []
        this.cachedViews = []
        localStorage.removeItem('tagsView')
        localStorage.removeItem('cachedViews')
      }
    },

    delView(view) {
      return new Promise(resolve => {
        this.delVisitedView(view)
        this.delCachedView(view)
        this.saveViewsToStorage()
        resolve({
          visitedViews: [...this.visitedViews],
          cachedViews: [...this.cachedViews]
        })
      })
    },

    delVisitedView(view) {
      const index = this.visitedViews.findIndex(v => v.path === view.path)
      if (index > -1) {
        this.visitedViews.splice(index, 1)
      }
    },

    delCachedView(view) {
      const index = this.cachedViews.indexOf(view.name)
      if (index > -1) {
        this.cachedViews.splice(index, 1)
      }
    },

    delOthersViews(view) {
      this.visitedViews = this.visitedViews.filter(v => {
        return v.meta?.affix || v.path === view.path
      })
      this.saveViewsToStorage()
    },

    delAllViews() {
      // 保留固定的标签
      this.visitedViews = this.visitedViews.filter(v => v.meta?.affix)
      this.cachedViews = []
      this.saveViewsToStorage()
    },

    updateVisitedView(view) {
      for (let v of this.visitedViews) {
        if (v.path === view.path) {
          // 保留原有属性，只更新需要更新的部分
          Object.assign(v, {
            meta: view.meta,
            title: view.meta?.title
          })
          break
        }
      }
      this.saveViewsToStorage()
    }
  }
})
