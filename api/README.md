## Bugfix API（FastAPI）

提供一个 HTTP 接口，用于“生成/可选应用”本地 `forward` 仓库的 bugfix 补丁。

### 安装依赖

只启动接口时建议用最小依赖：

```bash
pip install -r requirements-api.txt
```

如需运行整个工程（包含解析/Neo4j 等），再安装全量依赖：

```bash
pip install -r requirements.txt
```

### 启动

```bash
uvicorn api.main:app --reload --port 8000
```

### 接口

- `GET /api/health`
- `POST /api/bugfix/forward/project-modify-processflow-npe`（SSE）
  - 默认只返回 unified diff，不改文件
  - body: `{"apply": true|false}`，`apply=true` 才会把补丁写入到 `.cache/git_repos/forward/.../ProjectServiceImpl.java`

