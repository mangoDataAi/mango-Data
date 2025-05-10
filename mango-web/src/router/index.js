import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'
import dataIntegration from './modules/dataIntegration'
import systemSettings from './modules/system'

const routes = [
  {
    path: '/login',
    component: () => import('@/views/login/index.vue'),
    hidden: true
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dataIntegration/source/database',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'House' }
      },
      {
        path: '/dataset',
        name: 'Dataset',
        meta: {
          title: '数据集管理',
          icon: 'dataset',
          affix: true
        },
        redirect: '/dataIntegration/source/database',
        children: [
          {
            path: 'database',
            name: 'DatasetDatabase',
            component: () => import('@/views/dataIntegration/source/database.vue'),
            meta: { title: '数据库连接池' }
          },
          {
            path: 'database/tables/:id',
            name: 'DatabaseTables',
            component: () => import('@/views/dataIntegration/source/tables.vue'),
            meta: { title: '数据库表', icon: 'table' }
          },
          {
            path: 'file-system/:id',
            name: 'FileSystemBrowser',
            component: () => import('@/views/dataIntegration/source/file-system-browser.vue'),
            meta: { title: '文件浏览', icon: 'folder' }
          },
          {
            path: 'message-queue/:id',
            name: 'MessageQueueTopics',
            component: () => import('@/views/dataIntegration/source/message-queue-topics.vue'),
            meta: { title: '消息队列主题', icon: 'message' }
          },
          {
            path: 'nosql/:id',
            name: 'NoSQLCollections',
            component: () => import('@/views/dataIntegration/source/nosql-collections.vue'),
            meta: { title: 'NoSQL集合', icon: 'collection' }
          },
          {
            path: 'graph-db/:id',
            name: 'GraphDBVisualizer',
            component: () => import('@/views/dataIntegration/source/graph-db-visualizer.vue'),
            meta: { title: '图数据库可视化', icon: 'connection' }
          },
          {
            path: 'time-series/:id',
            name: 'TimeSeriesBrowser',
            component: () => import('@/views/dataIntegration/source/time-series-browser.vue'),
            meta: { title: '时序数据浏览', icon: 'time' }
          },
        ]
      },
    ]
  },
  dataIntegration,
  systemSettings,
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/404.vue'),
    hidden: true
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
  try {
    const token = localStorage.getItem('token')

    if (to.path === '/login') {
      if (token) {
        next('/')
      } else {
        next()
      }
    } else {
      if (token) {
        next()
      } else {
        next('/login')
      }
    }
  } catch (error) {
    console.error('路由错误:', error)
    next('/login')
  }
})

export default router
