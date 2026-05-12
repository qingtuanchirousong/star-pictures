import { createRouter, createWebHistory } from 'vue-router'
import { ref } from 'vue'
import Layout from '../views/Layout.vue'
import { ROLE, routeGuard } from '../utils/access'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { public: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: Layout,
    children: [
      {
        path: 'admin/users',
        name: 'UserManage',
        component: () => import('../views/UserManage.vue'),
        meta: { requiredRole: ROLE.ADMIN, title: '用户管理' },
      },
      {
        path: 'admin/pictures',
        name: 'PictureUpload',
        component: () => import('../views/PictureUpload.vue'),
        meta: { requiredRole: ROLE.ADMIN, title: '图片上传' },
      },
      {
        path: '403',
        name: 'Forbidden',
        component: () => import('../views/Forbidden.vue'),
        meta: { title: '无权限' },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/login',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const loginUserRef = ref(null)

router.beforeEach(async (to, from, next) => {
  if (to.meta?.public) return next()
  await routeGuard(to, from, next, loginUserRef)
})

export default router
export { loginUserRef }
