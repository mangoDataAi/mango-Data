import { defineStore } from 'pinia'

export const useHistoryStore = defineStore('history', {
  state: () => ({
    histories: []
  }),
  
  actions: {
    addHistory(route) {
      // 防止重复添加
      if (!this.histories.find(h => h.path === route.path)) {
        this.histories.push({
          path: route.path,
          title: route.meta.title || '未命名页面',
          timestamp: Date.now()
        })
        
        // 限制历史记录数量为10条
        if (this.histories.length > 10) {
          this.histories.shift()
        }
      }
    },
    
    removeHistory(path) {
      const index = this.histories.findIndex(h => h.path === path)
      if (index > -1) {
        this.histories.splice(index, 1)
      }
    },
    
    clearHistories() {
      this.histories = []
    }
  },
  
  persist: {
    enabled: true,
    strategies: [
      {
        key: 'mango-histories',
        storage: localStorage,
      },
    ],
  },
}) 