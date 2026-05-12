<template>
  <div class="user-manage">
    <div class="page-header fade-up">
      <h2 class="page-title">&#10022; 用户管理</h2>
      <p class="page-desc">管理系统注册用户，支持创建、编辑、删除和搜索</p>
    </div>

    <!-- 搜索 -->
    <div class="card-panel fade-up" style="animation-delay: 0.1s">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="账号">
          <el-input v-model="searchForm.userAccount" placeholder="请输入账号" clearable />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="searchForm.userName" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="searchForm.userRole" placeholder="请选择" clearable>
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 表格 -->
    <div class="card-panel fade-up" style="animation-delay: 0.2s">
      <div class="toolbar">
        <el-button type="primary" @click="handleAdd">+ 新增用户</el-button>
      </div>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userAccount" label="账号" width="150" />
        <el-table-column prop="userName" label="用户名" width="140" />
        <el-table-column label="头像" width="70">
          <template #default="{ row }">
            <el-avatar v-if="row.userAvatar" :src="row.userAvatar" size="small" />
            <span v-else class="no-avatar">&#10022;</span>
          </template>
        </el-table-column>
        <el-table-column prop="userProfile" label="简介" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="profile-text">{{ row.userProfile || '暂无简介' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="userRole" label="角色" width="100">
          <template #default="{ row }">
            <span class="role-tag" :class="row.userRole">
              {{ row.userRole === 'admin' ? '管理员' : '用户' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" text class="btn-delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="pagination.total > 0">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </div>

    <!-- 对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="460px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form :model="form" :rules="formRules" ref="formRef" label-position="top">
        <el-form-item label="账号" prop="userAccount">
          <el-input v-model="form.userAccount" :disabled="!!form.id" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码" prop="userPassword" v-if="!form.id">
          <el-input v-model="form.userPassword" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="用户名" prop="userName">
          <el-input v-model="form.userName" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="角色" prop="userRole">
          <el-select v-model="form.userRole" placeholder="请选择角色" style="width: 100%">
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userListByPage, userCreate, userUpdate, userDelete } from '../api/user'

const searchForm = reactive({ userAccount: '', userName: '', userRole: '' })
const pagination = reactive({ current: 1, pageSize: 10, total: 0 })
const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, userAccount: '', userPassword: '', userName: '', userRole: 'user' })
const dialogTitle = computed(() => (form.id ? '编辑用户' : '新增用户'))

const formRules = {
  userAccount: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 4, message: '账号不能少于4位', trigger: 'blur' },
  ],
  userPassword: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码不能少于6位', trigger: 'blur' },
  ],
  userName: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  userRole: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await userListByPage({
      ...searchForm,
      current: pagination.current,
      pageSize: pagination.pageSize,
    })
    const page = res.data
    tableData.value = page.records || []
    pagination.total = page.total || 0
    pagination.current = page.current || 1
    pagination.pageSize = page.size || 10
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { pagination.current = 1; loadData() }
const handleReset = () => {
  searchForm.userAccount = ''
  searchForm.userName = ''
  searchForm.userRole = ''
  pagination.current = 1
  loadData()
}

const handleAdd = () => { resetForm(); dialogVisible.value = true }

const handleEdit = (row) => {
  resetForm()
  Object.assign(form, {
    id: row.id, userAccount: row.userAccount,
    userName: row.userName, userRole: row.userRole, userPassword: '',
  })
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户「${row.userName}」吗？`, '提示', { type: 'warning' })
    await userDelete({ id: row.id })
    ElMessage.success('删除成功')
    loadData()
  } catch { /* 取消 */ }
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitLoading.value = true
  try {
    if (form.id) {
      await userUpdate({ id: form.id, userName: form.userName, userRole: form.userRole })
      ElMessage.success('更新成功')
    } else {
      await userCreate({ userAccount: form.userAccount, userPassword: form.userPassword, userName: form.userName, userRole: form.userRole })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitLoading.value = false
  }
}

const resetForm = () => {
  form.id = null; form.userAccount = ''; form.userPassword = ''
  form.userName = ''; form.userRole = 'user'
  formRef.value?.clearValidate()
}

onMounted(() => { loadData() })
</script>

<style scoped>
.user-manage {
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

.toolbar {
  margin-bottom: 20px;
}

.role-tag {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
}
.role-tag.admin {
  color: var(--accent);
  background: var(--accent-soft);
  border: 1px solid rgba(91, 79, 196, 0.2);
}
.role-tag.user {
  color: var(--text-secondary);
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid var(--border);
}

.no-avatar { color: var(--accent-star); }
.profile-text { color: var(--text-secondary); }

.btn-delete {
  color: #e05555 !important;
}
.btn-delete:hover {
  color: #ff6b6b !important;
}

.pagination {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}
</style>
