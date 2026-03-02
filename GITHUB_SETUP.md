# 上传到 GitHub 的步骤

## 前置条件

1. 已安装 Git
2. 有 GitHub 账户
3. 已配置 Git 用户信息（已完成）

## 步骤

### 1. 在 GitHub 创建新仓库

访问 https://github.com/new，填写：
- Repository name: `java-ast-parser`
- Description: `Java AST Parser & Neo4j Exporter`
- Public/Private: 选择 Public
- **不要** 初始化 README（我们已经有了）

### 2. 添加远程仓库

```bash
# 替换 YOUR_USERNAME 为你的 GitHub 用户名
git remote add origin https://github.com/YOUR_USERNAME/java-ast-parser.git
```

### 3. 验证远程仓库

```bash
git remote -v
```

应该看到：
```
origin  https://github.com/YOUR_USERNAME/java-ast-parser.git (fetch)
origin  https://github.com/YOUR_USERNAME/java-ast-parser.git (push)
```

### 4. 推送代码

```bash
# 推送到 main 分支（GitHub 默认分支）
git branch -M main
git push -u origin main
```

或保持 master 分支：
```bash
git push -u origin master
```

### 5. 验证

访问 https://github.com/YOUR_USERNAME/java-ast-parser 查看你的仓库

## 后续操作

### 添加 License

```bash
# 创建 MIT License
echo "MIT License content..." > LICENSE
git add LICENSE
git commit -m "Add MIT License"
git push
```

### 创建 GitHub Actions CI/CD

创建 `.github/workflows/tests.yml`:

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        python-version: [3.9, 3.10, 3.11]
    
    steps:
    - uses: actions/checkout@v3
    - name: Set up Python
      uses: actions/setup-python@v4
      with:
        python-version: ${{ matrix.python-version }}
    - name: Install dependencies
      run: |
        python -m pip install --upgrade pip
        pip install -r requirements.txt
    - name: Run tests
      run: |
        pytest tests/
```

### 添加 .github/CONTRIBUTING.md

```markdown
# 贡献指南

感谢你对本项目的兴趣！

## 如何贡献

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 代码风格

- 使用 Enum 代替魔法值
- 使用 dataclass 代替 dict
- 完整的类型注解
- 清晰的方法命名

## 报告 Bug

使用 GitHub Issues 报告 bug，包括：
- 清晰的标题
- 详细的描述
- 复现步骤
- 预期行为
- 实际行为
```

## 常见问题

### 如何更新代码？

```bash
# 修改文件后
git add .
git commit -m "描述你的更改"
git push
```

### 如何创建新分支？

```bash
git checkout -b feature/new-feature
# 做一些更改
git add .
git commit -m "Add new feature"
git push -u origin feature/new-feature
# 在 GitHub 上创建 Pull Request
```

### 如何同步远程更改？

```bash
git pull origin main
```

## 推荐的 GitHub 设置

1. **Branch Protection Rules**
   - 设置 main 分支保护
   - 要求 Pull Request 审查
   - 要求通过 CI/CD 检查

2. **Collaborators**
   - 添加协作者
   - 设置权限级别

3. **Webhooks**
   - 集成 CI/CD 工具
   - 集成通知服务

## 更多资源

- [GitHub 文档](https://docs.github.com)
- [Git 教程](https://git-scm.com/book/zh/v2)
- [开源指南](https://opensource.guide/zh-hans/)
