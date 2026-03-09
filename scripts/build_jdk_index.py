"""
构建 JDK 类索引

支持：
- JDK 8: 扫描 rt.jar
- JDK 9+: 扫描 jmods 目录

使用方式：
    python scripts/build_jdk_index.py --jdk-home /path/to/jdk
    python scripts/build_jdk_index.py --auto-detect
    python scripts/build_jdk_index.py --jdk-version 17
"""
import sys
import os
import argparse
from pathlib import Path

# 添加项目根目录到路径
project_root = Path(__file__).parent.parent
sys.path.insert(0, str(project_root))

from tools.constants import PROJECT_ROOT_PATH
from storage.sqlite import get_jar_class_db, JARScanner


def detect_jdk_home():
    """自动检测 JDK 路径"""
    # 1. 检查 JAVA_HOME 环境变量
    java_home = os.environ.get('JAVA_HOME')
    if java_home and Path(java_home).exists():
        return Path(java_home)
    
    # 2. 检查 java 命令路径
    import subprocess
    try:
        result = subprocess.run(
            ['java', '-XshowSettings:properties', '-version'],
            capture_output=True,
            text=True,
            stderr=subprocess.STDOUT
        )
        
        for line in result.stdout.split('\n'):
            if 'java.home' in line:
                java_home = line.split('=')[1].strip()
                return Path(java_home)
    except Exception:

        pass
    
    return None


def detect_jdk_version(jdk_home: Path) -> int:
    """检测 JDK 版本"""
    # 检查 release 文件
    release_file = jdk_home / "release"
    if release_file.exists():
        with open(release_file, 'r') as f:
            for line in f:
                if line.startswith('JAVA_VERSION='):
                    version_str = line.split('=')[1].strip().strip('"')
                    # 提取主版本号
                    if version_str.startswith('1.'):
                        # JDK 8: "1.8.0_xxx"
                        return int(version_str.split('.')[1])
                    else:
                        # JDK 9+: "17.0.1"
                        return int(version_str.split('.')[0])
    
    # 检查是否存在 jmods 目录（JDK 9+）
    if (jdk_home / "jmods").exists():
        return 9  # 至少是 JDK 9
    
    # 检查是否存在 rt.jar（JDK 8）
    if (jdk_home / "jre" / "lib" / "rt.jar").exists():
        return 8
    
    return 0  # 未知版本


def find_jdk_jars(jdk_home: Path, jdk_version: int) -> list:
    """查找 JDK 的 JAR 文件"""
    jars = []
    
    if jdk_version == 8:
        # JDK 8: rt.jar, jce.jar, jsse.jar 等
        lib_dir = jdk_home / "jre" / "lib"
        if lib_dir.exists():
            for jar_file in lib_dir.glob("*.jar"):
                jars.append(jar_file)
        
        # ext 目录
        ext_dir = lib_dir / "ext"
        if ext_dir.exists():
            for jar_file in ext_dir.glob("*.jar"):
                jars.append(jar_file)
    
    else:
        # JDK 9+: jmods 目录
        jmods_dir = jdk_home / "jmods"
        if jmods_dir.exists():
            for jmod_file in jmods_dir.glob("*.jmod"):
                jars.append(jmod_file)
    
    return jars


def build_jdk_index(jdk_home: Path, output_db: Path = None, force_rescan: bool = True):
    """构建 JDK 索引"""
    print("=" * 80)
    print("构建 JDK 类索引")
    print("=" * 80)
    
    # 检测 JDK 版本
    jdk_version = detect_jdk_version(jdk_home)
    if jdk_version == 0:
        print(f"✗ 无法检测 JDK 版本: {jdk_home}")
        return False
    
    print(f"JDK 路径: {jdk_home}")
    print(f"JDK 版本: {jdk_version}")
    print()
    
    # 查找 JAR 文件
    jars = find_jdk_jars(jdk_home, jdk_version)
    if not jars:
        print(f"✗ 未找到 JDK JAR 文件")
        return False
    
    print(f"找到 {len(jars)} 个 JDK 文件:")
    for jar in jars[:10]:
        print(f"  - {jar.name}")
    if len(jars) > 10:
        print(f"  ... 还有 {len(jars) - 10} 个文件")
    print()
    
    # 初始化数据库
    if output_db is None:
        output_db = PROJECT_ROOT_PATH / ".cache" / f"jdk{jdk_version}_classes.db"
    
    # 如果数据库已存在且不强制重新扫描，询问用户
    if output_db.exists() and not force_rescan:
        response = input(f"数据库已存在: {output_db}\n是否覆盖? (y/n): ")
        if response.lower() != 'y':
            print("取消构建")
            return False
        output_db.unlink()
    
    # 创建数据库
    from storage.sqlite.jar_class_db import JARClassDB
    db = JARClassDB(str(output_db))
    db.initialize_schema()
    
    # 创建扫描器
    scanner = JARScanner(db, batch_size=1000)
    # 扫描所有 JAR
    print("开始扫描...")
    total_classes = 0
    
    for i, jar_path in enumerate(jars, 1):
        try:
            count = scanner.scan_jar(
                str(jar_path),
                include_anonymous=False
            )
            
            total_classes += count
            print(f"  [{i}/{len(jars)}] {jar_path.name}: {count} 个类")
        
        except Exception as e:
            print(f"  [{i}/{len(jars)}] {jar_path.name}: 错误 - {e}")
            print(f"  [{i}/{len(jars)}] {jar_path.name}: 错误 - {e}")
    
    print()
    print("=" * 80)
    print(f"✓ 构建完成")
    print(f"  数据库: {output_db}")
    print(f"  总类数: {total_classes}")
    print(f"  文件大小: {output_db.stat().st_size / 1024 / 1024:.2f} MB")
    print("=" * 80)
    
    return True


def main():
    parser = argparse.ArgumentParser(description='构建 JDK 类索引')
    parser.add_argument('--jdk-home', type=str, help='JDK 安装路径')
    parser.add_argument('--auto-detect', action='store_true', help='自动检测 JDK 路径')
    parser.add_argument('--jdk-version', type=int, help='指定 JDK 版本（用于输出文件名）')
    parser.add_argument('--output', type=str, help='输出数据库路径')
    parser.add_argument('--force', action='store_true', help='强制重新扫描')
    
    args = parser.parse_args()
    
    # 确定 JDK 路径
    jdk_home = None
    
    if args.jdk_home:
        jdk_home = Path(args.jdk_home)
    elif args.auto_detect:
        jdk_home = detect_jdk_home()
        if not jdk_home:
            print("✗ 无法自动检测 JDK 路径")
            print("请使用 --jdk-home 指定 JDK 路径")
            return
    else:
        print("请指定 JDK 路径:")
        print("  --jdk-home /path/to/jdk")
        print("  --auto-detect")
        return
    
    if not jdk_home.exists():
        print(f"✗ JDK 路径不存在: {jdk_home}")
        return
    
    # 确定输出路径
    output_db = None
    if args.output:
        output_db = Path(args.output)
    elif args.jdk_version:
        output_db = PROJECT_ROOT_PATH / ".cache" / f"jdk{args.jdk_version}_classes.db"
    
    # 构建索引
    build_jdk_index(jdk_home, output_db, force_rescan=args.force)


if __name__ == "__main__":
    main()
