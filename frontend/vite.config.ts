import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    host: true, // 监听 0.0.0.0，支持用本机 IP（如 192.168.x.x:5173）访问
    proxy: {
      '/api': 'http://127.0.0.1:8081',
    },
  },
})
