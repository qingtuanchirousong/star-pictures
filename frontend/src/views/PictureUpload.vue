<template>
  <div class="picture-page">
    <div class="page-header fade-up">
      <h2 class="page-title">&#10022; 图片上传</h2>
      <p class="page-desc">上传图片到 COS，支持预览和下载</p>
    </div>

    <div class="card-panel fade-up" style="animation-delay: 0.1s">
      <el-upload
        class="upload-area"
        drag
        :http-request="handleUpload"
        :show-file-list="false"
        accept="image/*"
      >
        <div class="upload-content">
          <span class="upload-icon">&#10022;</span>
          <p>将图片拖到此处，或点击上传</p>
          <p class="upload-hint">支持 JPG / PNG / WebP，单文件不超过 10MB</p>
        </div>
      </el-upload>

      <div v-if="submitting" class="upload-progress">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>正在上传...</span>
      </div>
    </div>

    <div v-if="pictures.length" class="card-panel fade-up" style="animation-delay: 0.2s">
      <h3 class="section-title">已上传图片（{{ pictures.length }}）</h3>
      <div class="picture-grid">
        <div v-for="(pic, index) in pictures" :key="index" class="picture-card">
          <div class="picture-preview">
            <img :src="pic.url" :alt="pic.name" />
            <div class="picture-overlay">
              <a :href="getDownloadUrl(pic.key)" class="btn-download">
                <span>&#8595;</span> 下载
              </a>
            </div>
          </div>
          <div class="picture-info">
            <span class="picture-name" :title="pic.name">{{ pic.name }}</span>
            <span class="picture-size">{{ formatSize(pic.size) }}</span>
          </div>
        </div>
      </div>
      <div class="actions">
        <el-button @click="pictures = []">清空列表</el-button>
      </div>
    </div>

    <div v-else-if="!submitting" class="empty-hint fade-up">
      <p>&#9734; 还没有上传图片，拖入图片开始吧</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { uploadFile, getDownloadUrl } from '../api/file'

const pictures = ref([])
const submitting = ref(false)

const handleUpload = async (options) => {
  const file = options.file
  try {
    await ElMessageBox.confirm(
      `确认上传「${file.name}」（${formatSize(file.size)}）？`,
      '上传确认',
      { confirmButtonText: '确认上传', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }
  submitting.value = true
  try {
    const res = await uploadFile(file)
    const url = res.data
    const key = url.substring(url.lastIndexOf('.com/') + '.com/'.length)
    pictures.value.unshift({
      name: file.name,
      size: file.size,
      url,
      key,
    })
    ElMessage.success('上传成功')
  } catch {
    // error handled by interceptor
  } finally {
    submitting.value = false
  }
}

const formatSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>

<style scoped>
.picture-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.page-title {
  font-family: var(--font-display);
  font-size: 28px;
  font-weight: 400;
  color: var(--text-primary);
}
.page-desc {
  font-size: 14px;
  color: var(--text-muted);
  margin-top: 4px;
}

.card-panel {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 24px;
  box-shadow: var(--shadow-card);
}

/* Upload */
.upload-area {
  width: 100%;
}
.upload-content {
  padding: 40px 0;
  text-align: center;
  color: var(--text-secondary);
}
.upload-icon {
  font-size: 40px;
  color: var(--accent-star);
  display: block;
  margin-bottom: 12px;
  animation: twinkle 3s ease-in-out infinite;
}
.upload-hint {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 8px;
}

.upload-progress {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px 0 0;
  color: var(--accent);
  font-size: 14px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 16px;
}

/* Picture Grid */
.picture-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}
.picture-card {
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 1px solid var(--border);
  background: var(--bg-surface);
  transition: box-shadow 0.3s;
}
.picture-card:hover {
  box-shadow: var(--shadow-hover);
}
.picture-preview {
  position: relative;
  aspect-ratio: 16 / 10;
  overflow: hidden;
  background: var(--bg-hover);
}
.picture-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.picture-card:hover .picture-preview img {
  transform: scale(1.05);
}
.picture-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}
.picture-card:hover .picture-overlay {
  opacity: 1;
}
.btn-download {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  background: var(--accent-star);
  color: #fff;
  border-radius: 999px;
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
  transition: transform 0.2s;
}
.btn-download:hover {
  transform: scale(1.05);
}
.picture-info {
  padding: 10px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}
.picture-name {
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 140px;
}
.picture-size {
  color: var(--text-muted);
  font-size: 12px;
  white-space: nowrap;
}

.actions {
  margin-top: 20px;
}

.empty-hint {
  text-align: center;
  padding: 60px 0;
  color: var(--text-muted);
  font-size: 15px;
}
</style>
