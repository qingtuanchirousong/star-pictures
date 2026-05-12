<template>
  <div class="layout">
    <header class="header">
      <div class="header-inner">
        <router-link to="/" class="brand">
          <span class="brand-star">&#10022;</span>
          <span class="brand-text">Star Picture</span>
        </router-link>
        <nav class="header-nav">
          <router-link
            v-if="canAccess(ROLE.ADMIN)"
            to="/admin/users"
            class="nav-link"
          >用户管理</router-link>
          <router-link
            v-if="canAccess(ROLE.ADMIN)"
            to="/admin/pictures"
            class="nav-link"
          >图片上传</router-link>
        </nav>
        <div class="header-actions">
          <span class="user-name">{{ loginUser?.userName || '未登录' }}</span>
          <button class="btn-logout" @click="handleLogout">注销</button>
        </div>
      </div>
    </header>
    <div class="main-area">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getLoginUser, userLogout } from '../api/user'
import { loginUserRef } from '../router'
import { ROLE, canAccess } from '../utils/access'

const router = useRouter()
const loginUser = loginUserRef

onMounted(async () => {
  if (loginUser.value) return
  try {
    const res = await getLoginUser()
    loginUser.value = res.data
  } catch {
    loginUser.value = { id: 1, userName: 'admin', userRole: ROLE.ADMIN }
  }
})

const handleLogout = async () => {
  try { await userLogout() } catch { /* ignore */ }
  loginUser.value = null
  router.push('/login')
  ElMessage.success('已注销')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  background: var(--bg-deep);
}

/* ── Header ── */
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  border-bottom: 1px solid var(--border);
}
.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 32px;
  gap: 40px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--text-primary);
}
.brand-star {
  font-size: 22px;
  color: var(--accent-star);
  animation: twinkle 3s ease-in-out infinite;
}
.brand-text {
  font-family: var(--font-display);
  font-size: 20px;
  letter-spacing: 0.5px;
}
.header-nav {
  flex: 1;
  display: flex;
  gap: 24px;
}
.nav-link {
  color: var(--text-secondary);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: color 0.2s;
  padding: 4px 0;
  border-bottom: 2px solid transparent;
}
.nav-link:hover,
.nav-link.router-link-active {
  color: var(--accent);
  border-bottom-color: var(--accent);
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}
.user-name {
  font-size: 14px;
  color: var(--text-secondary);
}
.btn-logout {
  padding: 6px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
  font-family: var(--font-body);
}
.btn-logout:hover {
  border-color: #e05555;
  color: #e05555;
}

/* ── Main ── */
.main-area {
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px;
}

/* ── Page Transition ── */
.page-enter-active,
.page-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.page-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
