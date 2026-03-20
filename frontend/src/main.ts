import { createApp } from 'vue'
import './style.css'
import App from './App.vue'

import Antd from 'ant-design-vue'
import { message } from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import 'highlight.js/styles/github-dark.css'

import router from './router'

// 全局“响应拦截器”（fetch/fetch 的 Response.json 级别）：
// 1) 后端返回 JSON 但字段 `ok === false`：自动 message.error
// 2) 响应体不是 JSON 或为空（如 Unexpected end of JSON input）：自动 message.error，并返回 { ok:false, message }，
//    尽量让业务不必感知解析异常即可继续跑（避免大量 alert 弹窗/报错）。
const _originalResponseJson = Response.prototype.json
// 防止 HMR 重复覆盖
if (!(Response.prototype as any).__autoErrorIntercepted) {
  ;(Response.prototype as any).__autoErrorIntercepted = true
  let lastToastText = ''
  let lastToastAt = 0
  const toastOnce = (text: string, duration = 4) => {
    const now = Date.now()
    if (text === lastToastText && now - lastToastAt < 1500) return
    lastToastText = text
    lastToastAt = now
    message.error({ content: text, duration })
  }
  Response.prototype.json = async function (...args: any[]) {
    const status = (this as any).status
    const headers = (this as any).headers
    const contentType = headers?.get?.('content-type') || ''
    const contentLength = headers?.get?.('content-length')

    const looksLikeJson = /application\/json|\/json\b|\+json/i.test(contentType)
    const isEmptyByHeader = contentLength === '0'

    try {
      // 如果不是 JSON（比如 HTML 错误页），直接返回结构化错误，避免 "Unexpected end of JSON input"
      if (!looksLikeJson && !isEmptyByHeader) {
        const msg = `响应不是 JSON（HTTP ${status}）`
        toastOnce(msg, 4)
        return { ok: false, message: msg }
      }

      if (isEmptyByHeader) {
        const msg = '响应为空（可能后端未启动或返回异常）'
        toastOnce(msg, 4)
        return { ok: false, message: msg }
      }

      const data = await _originalResponseJson.apply(this, args as any)
      if (data && typeof data === 'object' && (data as any).ok === false) {
        const msg = (data as any).message || '请求失败'
        toastOnce(msg, 4)
      }
      return data
    } catch (e: any) {
      // 典型：空响应 / 截断响应导致 JSON.parse 失败
      const msg = '响应不是有效 JSON（可能后端未启动或返回异常）'
      toastOnce(msg, 4)
      return { ok: false, message: msg }
    }
  }
}

createApp(App).use(Antd).use(router).mount('#app')
