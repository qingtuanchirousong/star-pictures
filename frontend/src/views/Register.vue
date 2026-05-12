<template>
  <div class="auth-page">
    <!-- 星星装饰 -->
    <span class="star-deco s1">&#10022;</span>
    <span class="star-deco s2">&#10025;</span>
    <span class="star-deco s3">&#9734;</span>
    <span class="star-deco s4">&#10017;</span>
    <span class="star-deco s5">&#10022;</span>
    <span class="star-deco s6">&#9734;</span>

    <div class="auth-card">
      <div class="card-header">
        <span class="card-icon">&#10022;</span>
        <h1 class="card-title">创建账号</h1>
        <p class="card-desc">加入 Star Picture，发现浩瀚星图</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top" @keyup.enter="handleRegister">
        <el-form-item label="账号" prop="userAccount">
          <el-input v-model="form.userAccount" placeholder="请输入账号" size="large" />
        </el-form-item>
        <el-form-item label="密码" prop="userPassword">
          <el-input v-model="form.userPassword" type="password" placeholder="请输入密码" size="large" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="checkPassword">
          <el-input v-model="form.checkPassword" type="password" placeholder="请再次输入密码" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" :loading="loading" size="large" class="submit-btn">
            注册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="card-footer">
        <span>已有账号？</span>
        <router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { userRegister } from '../api/user'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ userAccount: '', userPassword: '', checkPassword: '' })

const validateCheckPass = (rule, value, callback) => {
  if (value !== form.userPassword) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  userAccount: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, message: '账号不能少于4位', trigger: 'blur' },
  ],
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码不能少于6位', trigger: 'blur' },
  ],
  checkPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateCheckPass, trigger: 'blur' },
  ],
}

const handleRegister = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userRegister(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background:
    radial-gradient(ellipse 70% 60% at 60% 30%, rgba(91, 79, 196, 0.04) 0%, transparent 60%),
    radial-gradient(ellipse 50% 50% at 30% 70%, rgba(232, 180, 75, 0.05) 0%, transparent 60%),
    linear-gradient(180deg, #faf9f7 0%, #f0eeea 100%);
  position: relative;
  overflow: hidden;
}

.star-deco {
  position: absolute;
  pointer-events: none;
  color: var(--accent-star);
  animation: twinkle 3s ease-in-out infinite;
}
.s1 { top: 10%; left: 15%; font-size: 22px; animation-delay: 0s; }
.s2 { top: 18%; right: 12%; font-size: 18px; animation-delay: 0.7s; }
.s3 { top: 55%; left: 10%; font-size: 14px; animation-delay: 1.4s; }
.s4 { top: 70%; right: 18%; font-size: 26px; animation-delay: 2.1s; }
.s5 { top: 35%; right: 8%;  font-size: 10px; animation-delay: 1.0s; }
.s6 { top: 80%; left: 20%;  font-size: 16px; animation-delay: 2.8s; }

.auth-card {
  position: relative;
  width: 420px;
  padding: 48px 40px 36px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  animation: fadeUp 0.6s ease-out both;
}

.card-header {
  text-align: center;
  margin-bottom: 36px;
}
.card-icon {
  font-size: 36px;
  color: var(--accent-star);
  display: block;
  margin-bottom: 12px;
  animation: float 3s ease-in-out infinite;
}
.card-title {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 400;
  color: var(--text-primary);
  margin-bottom: 6px;
}
.card-desc {
  font-size: 14px;
  color: var(--text-muted);
}

.submit-btn {
  width: 100%;
  font-weight: 600;
  letter-spacing: 1px;
}

.card-footer {
  text-align: center;
  font-size: 14px;
  color: var(--text-muted);
  margin-top: 8px;
}
.card-footer a {
  color: var(--accent);
  text-decoration: none;
  font-weight: 500;
  margin-left: 4px;
}
.card-footer a:hover {
  text-decoration: underline;
}
</style>
