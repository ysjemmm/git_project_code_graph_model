<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <h2>Bugfix 管理台</h2>
        <p>请登录后访问系统</p>
      </div>
      
      <div class="login-content">
        <a-button type="primary" size="large" @click="handleLogin" :loading="loading">
          <template #icon><UserOutlined /></template>
          CAS 统一登录
        </a-button>
        
        <div class="login-info">
          <p>点击上方按钮将跳转到 CAS 登录页面</p>
          <p>登录成功后会自动返回本系统</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { UserOutlined } from '@ant-design/icons-vue'
import { redirectToLogin } from '../auth'

const loading = ref(false)

function handleLogin() {
  loading.value = true
  redirectToLogin()
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  overflow: hidden;
}

.login-header {
  padding: 40px 30px 20px;
  text-align: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.login-header h2 {
  margin: 0 0 10px;
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.login-header p {
  margin: 0;
  color: #666;
  font-size: 14px;
}

.login-content {
  padding: 30px;
  text-align: center;
}

.login-info {
  margin-top: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.login-info p {
  margin: 8px 0;
  font-size: 13px;
  color: #666;
  line-height: 1.5;
}
</style>