
import os
import platform
import subprocess
from typing import Optional, Dict, Tuple
from urllib.parse import urlparse

from parser.utils.logger import get_logger

logger = get_logger("git_manager")

class GitManager:
    
    
    def __init__(self, repo_path: str):
        
        self.repo_path = repo_path
        self.git_dir = os.path.join(repo_path, '.git')
        
        # 根据操作系统自动选择编码
        # Windows 使用 GBK，其他系统使用 UTF-8
        if platform.system() == 'Windows':
            self.encoding = 'gbk'
        else:
            self.encoding = 'utf-8'
        
        logger.info(f"[INFO] Git 输出编码设置为: {self.encoding}")
    
    def is_repo_exists(self) -> bool:
        
        return os.path.isdir(self.git_dir)
    
    def clone(self, repo_url: str, branch: str = "master", shallow: bool = True, skip_fsck: bool = True, timeout: int = 600, git_config: Dict[str, str] = None) -> Tuple[bool, str]:
        
        try:
            # 创建父目录
            os.makedirs(os.path.dirname(self.repo_path), exist_ok=True)
            
            # 构建克隆命令
            cmd = ['git', 'clone']
            if shallow:
                cmd.extend(['--depth', '1'])
            if branch:
                cmd.extend(['-b', branch])
            if skip_fsck:
                cmd.extend(['-c', 'transfer.fsckObjects=false'])
            
            # 添加自定义git 配置
            if git_config:
                for key, value in git_config.items():
                    cmd.extend(['-c', f'{key}={value}'])
            
            cmd.extend([repo_url, self.repo_path])
            
            # 执行克隆
            result = subprocess.run(
                cmd,
                capture_output=True,
                encoding=self.encoding,
                errors='replace',  # 遇到无法解码的字符时替换为 �
                timeout=timeout
            )
            
            if result.returncode == 0:
                # 克隆成功后,配置 fetch 所有分
                subprocess.run(
                    ['git', '-C', self.repo_path, 'config', 'remote.origin.fetch', '+refs/heads/*:refs/remotes/origin/*'],
                    capture_output=True,
                    encoding=self.encoding,
                    errors='replace',
                    timeout=30
                )
                
                # 对于浅克隆,需fetch 所有分支信
                if shallow:
                    subprocess.run(
                        ['git', '-C', self.repo_path, 'fetch', '--all', '--depth=1'],
                        capture_output=True,
                        encoding=self.encoding,
                        errors='replace',
                        timeout=300
                    )
                
                return True, f"成功克隆仓库: {repo_url}"
            else:
                return False, f"克隆失败: {result.stderr}"
        
        except subprocess.TimeoutExpired:
            return False, f"克隆超时({timeout}秒)"
        except Exception as e:
            return False, f"克隆异常: {str(e)}"
    
    def fetch(self) -> Tuple[bool, str]:
        
        if not self.is_repo_exists():
            return False, "本地仓库不存在"
        
        try:
            result = subprocess.run(
                ['git', '-C', self.repo_path, 'fetch', 'origin'],
                capture_output=True,
                encoding=self.encoding,
                errors='replace',
                timeout=300
            )
            
            if result.returncode == 0:
                return True, "成功拉取最新代码"
            else:
                return False, f"拉取失败: {result.stderr}"
        
        except subprocess.TimeoutExpired:
            return False, "拉取超时00秒)"
        except Exception as e:
            return False, f"拉取异常: {str(e)}"
    
    def checkout(self, branch: str) -> Tuple[bool, str]:
        
        if not self.is_repo_exists():
            return False, "本地仓库不存在"
        
        try:
            # 首先尝试直接切换(如果本地分支已存在
            result = subprocess.run(
                ['git', '-C', self.repo_path, 'checkout', branch],
                capture_output=True,
                encoding=self.encoding,
                errors='replace',
                timeout=60
            )
            
            if result.returncode == 0:
                return True, f"成功切换到分{branch}"
            
            # 如果直接切换失败,先 fetch 目标分支
            logger.info(f"[INFO] 本地分支不存在,执行 fetch: {branch}")
            fetch_result = subprocess.run(
                ['git', '-C', self.repo_path, 'fetch', 'origin', branch],
                capture_output=True,
                encoding=self.encoding,
                errors='replace',
                timeout=300
            )
            
            if fetch_result.returncode != 0:
                error_msg = f"Fetch 分支失败: {fetch_result.stderr}"
                logger.error(f"[ERROR] {error_msg}")
                return False, error_msg
            
            logger.info(f"[INFO] Fetch 成功,现在尝checkout")
            
            # 然后尝试 checkout
            checkout_result = subprocess.run(
                ['git', '-C', self.repo_path, 'checkout', '-b', branch, f'origin/{branch}'],
                capture_output=True,
                encoding=self.encoding,
                errors='replace',
                timeout=60
            )
            
            if checkout_result.returncode == 0:
                return True, f"成功切换到分{branch}"
            else:
                error_msg = f"Checkout 失败: {checkout_result.stderr}"
                logger.error(f"[ERROR] {error_msg}")
                logger.error(f"[ERROR] 保留原项目文件夹,请检查日志")
                return False, error_msg
        
        except subprocess.TimeoutExpired:
            error_msg = "切换分支超时0秒)"
            logger.error(f"[ERROR] {error_msg}")
            return False, error_msg
        except Exception as e:
            error_msg = f"切换分支异常: {str(e)}"
            logger.error(f"[ERROR] {error_msg}")
            return False, error_msg
    
    def get_current_branch(self) -> Optional[str]:
        
        if not self.is_repo_exists():
            return None
        
        try:
            result = subprocess.run(
                ['git', '-C', self.repo_path, 'rev-parse', '--abbrev-ref', 'HEAD'],
                capture_output=True,
                encoding=self.encoding,
                errors='replace',
                timeout=10
            )
            
            if result.returncode == 0:
                return result.stdout.strip()
            return None
        except Exception:
            return None
    
    def get_current_commit(self) -> Optional[str]:
        
        if not self.is_repo_exists():
            return None
        
        try:
            result = subprocess.run(
                ['git', '-C', self.repo_path, 'rev-parse', 'HEAD'],
                capture_output=True,
                encoding=self.encoding,
                errors='replace',
                timeout=10
            )
            
            if result.returncode == 0:
                return result.stdout.strip()
            return None
        except Exception:
            return None
    
    def get_remote_url(self) -> Optional[str]:
        
        if not self.is_repo_exists():
            return None
        
        try:
            result = subprocess.run(
                ['git', '-C', self.repo_path, 'config', '--get', 'remote.origin.url'],
                capture_output=True,
                encoding=self.encoding,
                errors='replace',
                timeout=10
            )
            
            if result.returncode == 0:
                return result.stdout.strip()
            return None
        except Exception:
            return None
    
    def get_branch_list(self) -> list:
        """获取所有分支列表"""
        if not self.is_repo_exists():
            return []
        
        try:
            result = subprocess.run(
                ['git', '-C', self.repo_path, 'branch', '-a'],
                capture_output=True,
                encoding=self.encoding,
                errors='replace',
                timeout=10
            )
            
            if result.returncode == 0:
                branches = [line.strip().lstrip('* ') for line in result.stdout.split('\n') if line.strip()]
                return branches
            return []
        except Exception:
            return []
    
    def pull(self, branch: str = None) -> Tuple[bool, str]:
        
        if not self.is_repo_exists():
            return False, "本地仓库不存在"
        
        try:
            # 执行fetch
            success, msg = self.fetch()
            if not success:
                return False, msg
            
            # 如果指定了分支,先切
            if branch:
                success, msg = self.checkout(branch)
                if not success:
                    return False, msg
            
            # 执行 pull
            result = subprocess.run(
                ['git', '-C', self.repo_path, 'pull', 'origin'],
                capture_output=True,
                encoding=self.encoding,
                errors='replace',
                timeout=300
            )
            
            if result.returncode == 0:
                return True, "成功拉取并合并代码"
            else:
                return False, f"拉取合并失败: {result.stderr}"
        
        except subprocess.TimeoutExpired:
            return False, "拉取合并超时00秒)"
        except Exception as e:
            return False, f"拉取合并异常: {str(e)}"
    
    def get_status(self) -> Dict:
        """获取仓库状态"""
        if not self.is_repo_exists():
            return {
                'exists': False,
                'branch': None,
                'commit': None,
                'remote_url': None
            }
        
        return {
            'exists': True,
            'branch': self.get_current_branch(),
            'commit': self.get_current_commit(),
            'remote_url': self.get_remote_url()
        }
    
    def cleanup(self) -> Tuple[bool, str]:
        
        try:
            import shutil
            if os.path.isdir(self.repo_path):
                shutil.rmtree(self.repo_path)
                return True, f"成功清理仓库: {self.repo_path}"
            return True, "仓库不存在,无需清理"
        except Exception as e:
            return False, f"清理失败: {str(e)}"
    
    @staticmethod
    def calculate_dynamic_timeout(repo_url: str, base_timeout: int = 60) -> int:
        
        try:
            # 提取主机
            parsed_url = urlparse(repo_url)
            hostname = parsed_url.hostname
            
            if not hostname:
                return base_timeout
            
            # 第一步:Ping 测试获取网络延迟
            ping_time = GitManager._get_ping_time(hostname)
            
            # 第二步:获取仓库大小(通过 git ls-remote
            repo_size_mb = GitManager._get_repo_size(repo_url)
            
            # 第三步:计算超时时间
            # 基础时间 + 网络延迟 * 2 + 仓库大小 * 下载速率系数
            # 假设平均下载速率MB/s,网络延迟影响系数为 2
            network_factor = max(ping_time * 2, 10)  # 最0秒网络开销
            size_factor = max(repo_size_mb * 2, 30)  # 每MB预留2秒,最0
            
            calculated_timeout = base_timeout + network_factor + size_factor
            
            # 最0秒,最800秒(30分钟
            final_timeout = max(60, min(calculated_timeout, 1800))
            
            logger.info(f"[INFO] 动态超时计算")
            logger.info(f"  - 主机: {hostname}")
            logger.info(f"  - Ping 时间: {ping_time:.2f}ms")
            logger.info(f"  - 仓库大小: {repo_size_mb:.2f}MB")
            logger.info(f"  - 计算超时: {final_timeout}")
            
            return int(final_timeout)
        
        except Exception as e:
            logger.info(f"[WARN] 动态超时计算失{e},使用默认值{base_timeout}")
            return base_timeout
    
    @staticmethod
    def _get_ping_time(hostname: str) -> float:
        
        try:
            if platform.system() == 'Windows':
                cmd = ['ping', '-n', '1', hostname]
            else:
                cmd = ['ping', '-c', '1', hostname]
            
            result = subprocess.run(
                cmd,
                capture_output=True,
                encoding='utf-8',
                errors='replace',
                timeout=10
            )
            
            if result.returncode == 0:
                # 解析 ping 输出获取时间
                output = result.stdout
                if 'time=' in output:
                    # Windows: "time=50ms", Linux: "time=50.1 ms"
                    time_str = output.split('time=')[1].split('ms')[0].strip()
                    return float(time_str)
                elif 'time ' in output:
                    time_str = output.split('time ')[1].split('ms')[0].strip()
                    return float(time_str)
            
            return 50.0  # 默认 50ms
        
        except Exception as e:
            logger.info(f"[WARN] Ping 测试失败: {e}")
            return 50.0
    
    @staticmethod
    def _get_repo_size(repo_url: str) -> float:
        try:
            # 使用 git ls-remote 获取仓库信息
            # 添加 GIT_TERMINAL_PROMPT=0 禁用交互式认证(防止打开浏览器)
            cmd = ['git', 'ls-remote', '--heads', repo_url]
            env = os.environ.copy()
            env['GIT_TERMINAL_PROMPT'] = '0'  # 禁用交互式认证
            
            result = subprocess.run(
                cmd,
                capture_output=True,
                encoding='utf-8',
                errors='replace',
                timeout=10,
                env=env
            )
            
            if result.returncode == 0:
                # 通过 commit 数量估算仓库大小
                # 平均每个 commit -2KB
                lines = result.stdout.strip().split('\n')
                commit_count = len([l for l in lines if l.strip()])
                
                # 估算大小:commit* 平均大小(KB/ 1024
                estimated_size_mb = max(commit_count * 1.5 / 1024, 10)
                return estimated_size_mb
            
            return 50.0  # 默认 50MB
        
        except Exception as e:
            logger.info(f"[WARN] 获取仓库大小失败: {e}")
            return 50.0