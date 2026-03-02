# 上传到 GitHub 完整指南

## ✅ 已完成的准备工作

- ✅ 创建了 `.gitignore` 文件（排除 `__pycache__`, `.venv`, `.cache` 等）
- ✅ 创建了详细的 `README.md` 文档
- ✅ 创建了 `requirements.txt` 依赖文件
- ✅ 初始化了 Git 仓库
- ✅ 配置了 Git 用户信息
- ✅ 提交了初始代码（69个文件）
- ✅ 创建了 `GITHUB_SETUP.md` 设置指南
- ✅ 创建了 `QUICK_START.md` 快速开始指南

## 🚀 现在就可以上传！

### 方式1: 使用 HTTPS（推荐新手）

```bash
# 1. 在 GitHub 创建新仓库
# 访问 https://github.com/new
# 填写仓库名: java-ast-parser
# 选择 Public
# 不要初始化任何文件
# 点击 Create repository

# 2. 添加远程仓库（替换 YOUR_USERNAME）
git remote add origin https://github.com/YOUR_USERNAME/java-ast-parser.git

# 3. 推送代码
git branch -M main
git push -u origin main

# 4. 输入 GitHub 用户名和密码（或 Personal Access Token）
```

### 方式2: 使用 SSH（推荐有经验的开发者）

```bash
# 1. 生成 SSH key（如果还没有）
ssh-keygen -t ed25519 -C "your.email@example.com"

# 2. 添加 SSH key 到 GitHub
# 访问 https://github.com/settings/keys
# 点击 New SSH key
# 粘贴 ~/.ssh/id_ed25519.pub 的内容

# 3. 在 GitHub 创建新仓库
# 访问 https://github.com/new
# 填写仓库名: java-ast-parser
# 选择 Public
# 不要初始化任何文件
# 点击 Create repository

# 4. 添加远程仓库
git remote add origin git@github.com:YOUR_USERNAME/java-ast-parser.git

# 5. 推送代码
git branch -M main
git push -u origin main
```

## 📋 逐步操作指南

### 步骤1: 创建 GitHub 仓库

1. 访问 https://github.com/new
2. 填写信息：
   - **Repository name**: `java-ast-parser`
   - **Description**: `Java AST Parser & Neo4j Exporter - A powerful tool for analyzing Java code structure`
   - **Public**: 选中（如果想开源）
   - **Initialize this repository with**: 不要选中任何选项
3. 点击 "Create repository"

### 步骤2: 复制仓库 URL

在新创建的仓库页面，点击绿色的 "Code" 按钮，复制 HTTPS 或 SSH URL

### 步骤3: 添加远程仓库

```bash
# 替换 YOUR_USERNAME 和 URL
git remote add origin https://github.com/YOUR_USERNAME/java-ast-parser.git
```

### 步骤4: 验证远程仓库

```bash
git remote -v
```

应该看到：
```
origin  https://github.com/YOUR_USERNAME/java-ast-parser.git (fetch)
origin  https://github.com/YOUR_USERNAME/java-ast-parser.git (push)
```

### 步骤5: 推送代码

```bash
# 重命名分支为 main（GitHub 默认分支）
git branch -M main

# 推送代码
git push -u origin main
```

### 步骤6: 验证上传

访问 https://github.com/YOUR_USERNAME/java-ast-parser 查看你的仓库

## 🎯 上传后的建议操作

### 1. 添加 License

```bash
# 创建 MIT License 文件
# 访问 https://github.com/YOUR_USERNAME/java-ast-parser/add/main
# 选择 "Create new file"
# 文件名: LICENSE
# 选择 "MIT License" 模板
# 提交

# 或本地操作：
curl https://opensource.org/licenses/MIT > LICENSE
git add LICENSE
git commit -m "Add MIT License"
git push
```

### 2. 添加 GitHub Actions CI/CD

创建 `.github/workflows/tests.yml`:

```yaml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        python-version: ['3.9', '3.10', '3.11']
    
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
      run: pytest tests/
```

### 3. 配置分支保护规则

1. 访问 Settings → Branches
2. 点击 "Add rule"
3. 分支名称: `main`
4. 勾选：
   - "Require a pull request before merging"
   - "Require status checks to pass before merging"
5. 保存

### 4. 添加 Topics

在仓库主页右侧，点击 "Add topics"，添加：
- `java`
- `ast`
- `neo4j`
- `code-analysis`
- `graph-database`

### 5. 编写 Contributing 指南

创建 `.github/CONTRIBUTING.md` 文件，说明如何贡献

## 🔄 后续更新代码

```bash
# 修改文件后
git add .
git commit -m "描述你的更改"
git push

# 或推送到新分支
git checkout -b feature/new-feature
git add .
git commit -m "Add new feature"
git push -u origin feature/new-feature
# 在 GitHub 上创建 Pull Request
```

## 📊 查看仓库统计

上传后，你可以在 GitHub 上看到：
- **Insights** → 代码统计
- **Network** → 分支图
- **Pulse** → 活动统计
- **Contributors** → 贡献者

## 🆘 常见问题

### Q: 推送时出现 "fatal: remote origin already exists"

A: 删除现有的远程仓库：
```bash
git remote remove origin
git remote add origin https://github.com/YOUR_USERNAME/java-ast-parser.git
```

### Q: 推送时要求输入密码

A: 使用 Personal Access Token 代替密码：
1. 访问 https://github.com/settings/tokens
2. 点击 "Generate new token"
3. 选择 "repo" 权限
4. 复制 token
5. 推送时使用 token 作为密码

### Q: 如何修改已推送的提交？

A: 不建议修改已推送的提交。如果必须修改：
```bash
git commit --amend
git push --force-with-lease
```

### Q: 如何删除远程仓库中的文件？

A: 
```bash
git rm --cached filename
git commit -m "Remove filename"
git push
```

## 📚 有用的资源

- [GitHub 文档](https://docs.github.com)
- [Git 官方教程](https://git-scm.com/book/zh/v2)
- [开源指南](https://opensource.guide/zh-hans/)
- [GitHub Markdown 语法](https://guides.github.com/features/mastering-markdown/)

## ✨ 完成！

现在你的项目已经准备好上传到 GitHub 了！

**下一步：**
1. 创建 GitHub 仓库
2. 添加远程仓库
3. 推送代码
4. 分享你的项目！

祝你成功！🎉
