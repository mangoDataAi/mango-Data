import Layout from "@/layout/index.vue";

export default {
  path: '/system',
  name: 'System',
  component: Layout,
  redirect: '/system/base/params',
  meta: {
    title: '系统设置',
    icon: 'Setting',
    order: 900
  },
  children: [
    {
      path: 'base',
      name: 'BaseSettings',
      component: () => import('@/layout/empty.vue'),
      redirect: '/system/base/user',
      meta: { title: '基础设置', icon: 'Tools' },
      children: [
        {
          path: 'user',
          name: 'UserManagement',
          component: () => import('@/views/system/base/user.vue'),
          meta: { title: '用户管理' }
        },
        {
          path: 'organization',
          name: 'OrganizationManagement',
          component: () => import('@/views/system/base/organization.vue'),
          meta: { title: '机构管理' }
        },
        {
          path: 'menu',
          name: 'MenuManage',
          component: () => import('@/views/system/base/menu.vue'),
          meta: { title: '菜单管理' }
        },
        {
          path: 'role',
          name: 'RolePermission',
          component: () => import('@/views/system/base/role.vue'),
          meta: { title: '角色权限' }
        },
      ]
    },
  ]
}