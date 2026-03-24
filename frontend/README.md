# Bugfix 前端项目

基于 Vue 3 + Vite + Ant Design Vue 构建的 Bug 修复辅助系统。

## 功能特性

- 🤖 **AI Bug 分析** - 基于大模型的智能 Bug 分析与修复建议
- 🔗 **流程编排** - 可视化工作流引擎，支持项目配置 → AI 分析 → 人工确认 → 自动修复
- 📊 **代码图谱** - 基于 Neo4j 的代码依赖关系可视化
- 📦 **应用管理** - Maven 项目依赖分析与管理
- 🛠️ **二方包规则** - 二方包版本冲突检测与治理

## 登录认证

本系统采用 CAS 统一认证，遵循标准的单点登录流程：

### 认证流程

1. **首次访问系统**
   - 系统检查本地是否存在有效的 CAS ticket
   - 如果没有 ticket，自动跳转到 CAS 登录页面

2. **CAS 登录**
   - 用户在 CAS 登录页面输入凭据
   - 登录成功后，CAS 服务器设置必要的 Cookie
   - 自动重定向回系统，并携带 ticket 参数

3. **回调验证**
   - 系统接收 CAS 回调，提取 ticket
   - 调用 `https://testmanage.esign.cn/user/infor?ticket={ticket}` 验证 ticket 有效性
   - 获取用户详细信息并缓存到本地

4. **后续访问**
   - 系统使用本地缓存的 ticket 和用户信息
   - **每次页面加载时都会调用** `https://testmanage.esign.cn/user/infor` 验证认证状态
   - ticket 过期或验证失败时会自动重新跳转到登录页面

### 配置说明

- **认证地址**: https://cas-test.esign.cn/cas/login
- **服务地址**: https://testmanage.esign.cn/user/shiro-cas
- **用户信息接口**: https://testmanage.esign.cn/user/infor

### 开发环境配置

如需本地调试 CAS 回调，请确保：
1. 本地开发服务器可通过网络访问（如使用内网穿透）
2. CAS 服务中的 `service` 参数指向你的实际服务地址

## 快速开始

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
```

## 项目结构

```
src/
├── api/           # API 接口定义
├── auth/          # 认证相关逻辑
├── components/    # 公共组件
├── pages/         # 页面组件
├── router/        # 路由配置
├── types/         # TypeScript 类型定义
└── main.ts        # 应用入口
```

## 环境变量

创建 `.env.local` 文件：

```bash
# API 基础地址
VITE_API_BASE_URL=https://testmanage.esign.cn

# CAS 相关配置
VITE_CAS_LOGIN_URL=https://cas-test.esign.cn/cas/login
VITE_CAS_SERVICE_URL=https://testmanage.esign.cn/user/shiro-cas
VITE_USER_INFO_URL=https://testmanage.esign.cn/user/infor
```

## 技术栈

- Vue 3 Composition API
- Vite
- Ant Design Vue
- Vue Router 4
- Axios
- TypeScript
