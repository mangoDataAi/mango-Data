import Layout from '@/layout/index.vue'

export default {
  path: '/dataIntegration',
  component: Layout,
  redirect: '/dataIntegration/source/database',
  meta: {
    title: '数据集成',
    icon: 'Connection'
  },
  children: [
    {
      path: 'source',
      name: 'DataSource',
      component: () => import('@/layout/empty.vue'),
      redirect: '/dataIntegration/source/database',
      meta: { title: '数据源管理', icon: 'Files' },
      children: [
        {
          path: 'database',
          name: 'DatasetDatabases',
          component: () => import('@/views/dataIntegration/source/database.vue'),
          meta: { title: '数据库连接池' }
        },
        {
          path: 'file',
          name: 'FileDataSources',
          component: () => import('@/views/dataIntegration/source/file.vue'),
          meta: { title: '文件数据源' }
        },
        {
          path: 'api',
          name: 'DatasetApis',
          component: () => import('@/views/dataIntegration/source/api.vue'),
          meta: { title: '接口数据源' }
        }
      ]
    },
    {
      path: 'model',
      name: 'DataModel',
      component: () => import('@/layout/empty.vue'),
      redirect: '/dataIntegration/model/model',
      meta: { title: '模型设计', icon: 'Grid' },
      children: [
        {
          path: 'model',
          name: 'ModelList',
          component: () => import('@/views/dataIntegration/model/index.vue'),
          redirect: '/dataIntegration/model/model/list',
          meta: { title: '模型管理', icon: 'DataBoard' },
          children: [
            {
              path: 'list',
              name: 'ModelListPage',
              component: () => import('@/views/dataIntegration/model/modelList.vue'),
              meta: { title: '模型列表' }
            },
            {
              path: 'designer',
              name: 'ModelDesigner',
              component: () => import('@/views/dataIntegration/model/designer.vue'),
              meta: { title: '模型设计器', activeMenu: '/dataIntegration/model/model' }
            }
          ]
        },
        {
          path: 'verify',
          name: 'ModelVerify',
          component: () => import('@/views/dataIntegration/model/verify.vue'),
          meta: { title: '模型验证', icon: 'Check' }
        },
        {
          path: 'publish',
          name: 'ModelPublish',
          component: () => import('@/views/dataIntegration/model/publish.vue'),
          meta: { title: '模型发布', icon: 'Upload' }
        },
        {
          path: 'version',
          name: 'ModelVersion',
          component: () => import('@/views/dataIntegration/model/version.vue'),
          meta: { title: '版本管理', icon: 'DocumentCopy' }
        },
        {
          path: 'change',
          name: 'ModelChange',
          component: () => import('@/views/dataIntegration/model/change.vue'),
          meta: { title: '变更管理', icon: 'Sort' }
        }
      ]
    },
  ]
}