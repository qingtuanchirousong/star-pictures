<template>
  <div class="forbidden-page">
    <span class="icon">&#10022;</span>
    <h1>403</h1>
    <p>抱歉，您没有权限访问此页面</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.forbidden-page {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  text-align: center;
  gap: 16px;
}
.icon {
  font-size: 48px;
  color: var(--accent-star);
  animation: twinkle 3s ease-in-out infinite;
}
h1 {
  font-family: var(--font-display);
  font-size: 72px;
  color: var(--text-muted);
}
p {
  font-size: 16px;
  color: var(--text-secondary);
  margin-bottom: 16px;
}
</style>
