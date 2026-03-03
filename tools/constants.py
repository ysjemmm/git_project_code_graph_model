from pathlib import Path

from parser.utils.logger import get_logger

logger = get_logger("[CONSTANTS]")

def get_project_root_path() -> Path:
    current_dir = Path(__file__).resolve().parent

    for directory in [current_dir] + list(current_dir.parents):
        if (directory / ".gitignore").exists():
            return directory

    raise FileNotFoundError("未找到 .gitignore 文件，无法确定项目根目录!")

PROJECT_ROOT_PATH = get_project_root_path()
logger.info(f"Root project path is {PROJECT_ROOT_PATH}")