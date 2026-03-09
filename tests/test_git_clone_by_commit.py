#!/usr/bin/env python3
"""测试通过 Git URL 和 Commit ID 克隆仓库到 .cache/git_repos"""

import sys
import os
from pathlib import Path

# 添加项目根目录到 Python 路径
sys.path.insert(0, str(Path(__file__).parent.parent))


def test_clone_by_commit(repo_url: str, commit_id: str, target_dir: str = ".cache/git_repos"):
    """
    通过 Git URL 和 Commit ID 克隆仓库
    
    Args:
        repo_url: Git 仓库 URL
        commit_id: Git Commit ID (SHA-1 哈希值)
        target_dir: 目标目录，默认为 .cache/git_repos
    
    Returns:
        成功返回 True，失败返回 False
    """
    from git.manager import GitManager
    from urllib.parse import urlparse
    
    print("=" * 70)
    print("Git 克隆测试 - 通过 Commit ID")
    print("=" * 70)
    
    # 从 URL 解析仓库名称
    repo_name = repo_url.rstrip('/').split('/')[-1].replace('.git', '')
    repo_path = os.path.join(target_dir, repo_name)
    
    print(f"\n配置信息:")
    print(f"  - 仓库 URL: {repo_url}")
    print(f"  - Commit ID: {commit_id}")
    print(f"  - 目标路径: {repo_path}")
    
    # 创建 GitManager 实例
    git_manager = GitManager(repo_path)
    
    try:
        # 步骤 1: 克隆仓库（浅克隆）
        print(f"\n步骤 1: 克隆仓库...")
        
        if git_manager.is_repo_exists():
            print(f"[INFO] 仓库已存在: {repo_path}")
            print(f"[INFO] 将执行 fetch 操作...")
            
            # 执行 fetch
            success, msg = git_manager.fetch()
            if not success:
                print(f"[ERROR] Fetch 失败: {msg}")
                return False
            print(f"[OK] {msg}")
        else:
            # 计算动态超时
            timeout = GitManager.calculate_dynamic_timeout(repo_url)
            print(f"[INFO] 动态超时: {timeout} 秒")
            
            # 克隆仓库（不指定分支，克隆默认分支）
            success, msg = git_manager.clone(
                repo_url=repo_url,
                branch=None,  # 不指定分支
                shallow=False,  # 完整克隆以便访问任意 commit
                skip_fsck=True,
                timeout=timeout
            )
            
            if not success:
                print(f"[ERROR] 克隆失败: {msg}")
                return False
            print(f"[OK] {msg}")
        
        # 步骤 2: 切换到指定的 Commit ID
        print(f"\n步骤 2: 切换到 Commit ID: {commit_id}")
        
        import subprocess
        result = subprocess.run(
            ['git', '-C', repo_path, 'checkout', commit_id],
            capture_output=True,
            text=True,
            encoding='utf-8',
            errors='ignore',  # 忽略编码错误
            timeout=60
        )
        
        if result.returncode != 0:
            error_msg = result.stderr if result.stderr else "未知错误"
            print(f"[ERROR] 切换到 Commit ID 失败: {error_msg}")
            return False
        
        print(f"[OK] 成功切换到 Commit ID: {commit_id}")
        
        # 步骤 3: 验证当前状态
        print(f"\n步骤 3: 验证仓库状态")
        
        status = git_manager.get_status()
        current_commit = git_manager.get_current_commit()
        
        print(f"[INFO] 仓库状态:")
        print(f"  - 当前分支: {status.get('branch', 'detached HEAD')}")
        print(f"  - 当前 Commit: {current_commit}")
        print(f"  - 远程 URL: {status.get('remote_url')}")
        
        # 验证 commit ID 是否匹配
        if current_commit == commit_id:
            print(f"\n✅ Commit ID 验证成功")
        else:
            print(f"\n⚠️  Commit ID 不匹配:")
            print(f"  - 期望: {commit_id}")
            print(f"  - 实际: {current_commit}")
        
        # 步骤 4: 显示仓库信息
        print(f"\n步骤 4: 仓库信息")
        
        # 获取 commit 信息
        result = subprocess.run(
            ['git', '-C', repo_path, 'log', '-1', '--pretty=format:%H%n%an%n%ae%n%ad%n%s'],
            capture_output=True,
            text=True,
            encoding='utf-8',
            errors='ignore',  # 忽略编码错误
            timeout=10
        )
        
        if result.returncode == 0 and result.stdout:
            lines = result.stdout.strip().split('\n')
            if len(lines) >= 5:
                print(f"[INFO] Commit 详情:")
                print(f"  - Commit Hash: {lines[0]}")
                print(f"  - 作者: {lines[1]}")
                print(f"  - 邮箱: {lines[2]}")
                print(f"  - 日期: {lines[3]}")
                print(f"  - 消息: {lines[4]}")
        else:
            print(f"[WARN] 无法获取 Commit 详情")
        
        # 统计文件数量
        java_files = []
        for root, dirs, files in os.walk(repo_path):
            # 跳过 .git 目录
            if '.git' in root:
                continue
            for file in files:
                if file.endswith('.java'):
                    java_files.append(os.path.join(root, file))
        
        print(f"\n[INFO] 仓库统计:")
        print(f"  - Java 文件数量: {len(java_files)}")
        
        print("\n" + "=" * 70)
        print("✅ 测试完成")
        print("=" * 70)
        
        return True
    
    except Exception as e:
        print(f"\n[ERROR] 测试异常: {str(e)}")
        import traceback
        traceback.print_exc()
        return False


def main():
    """主函数 - 示例用法"""
    
    # 示例 1: 克隆公开仓库的特定 commit
    # repo_url = "https://github.com/spring-projects/spring-framework.git"
    # commit_id = "v5.3.23"  # 可以是 tag、commit hash
    
    # 示例 2: 克隆私有仓库（需要配置 Git 凭证）
    repo_url = "http://git.timevale.cn:8081/infra-frame/epaas-gateway.git"
    commit_id = "bedabddf3d8c6f9d3da1562aeca66f2fac9fbeb0"  # 使用 HEAD 表示最新提交，或使用具体的 commit hash
    
    # 如果需要指定具体的 commit hash，可以这样：
    # commit_id = "abc123def456..."  # 完整的 40 位 SHA-1 哈希
    
    success = test_clone_by_commit(repo_url, commit_id)
    
    return 0 if success else 1


if __name__ == "__main__":
    sys.exit(main())
