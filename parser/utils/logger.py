

import logging
import logging.handlers
from pathlib import Path
from typing import Optional

class LoggerManager:
    
    
    _instance = None
    _logger = None
    
    def __new__(cls):
        
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance
    
    def __init__(self):
        
        if self._logger is None:
            self._setup_logger()
    
    @classmethod
    def _setup_logger(cls):
        
        # 创建日志目录
        log_dir = Path(__file__).parent.parent.parent / "logs"
        log_dir.mkdir(exist_ok=True)
        
        log_file = log_dir / "service.log"
        
        # 创建日志记录器
        logger = logging.getLogger("service")
        logger.setLevel(logging.DEBUG)
        
        # 移除已有的处理器
        logger.handlers.clear()
        
        # 创建文件处理器(带轮转)
        file_handler = logging.handlers.RotatingFileHandler(
            log_file,
            maxBytes=10 * 1024 * 1024,  # 10MB
            backupCount=5,  # 保留 5 个备份
            encoding='utf-8'
        )
        file_handler.setLevel(logging.DEBUG)
        
        # 创建控制台处理器
        console_handler = logging.StreamHandler()
        console_handler.setLevel(logging.INFO)
        
        # 创建格式化器
        detailed_formatter = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(message)s',
            datefmt='%Y-%m-%d %H:%M:%S'
        )
        
        # 控制台输出格式:更人性化,去掉 INFO 前缀
        simple_formatter = logging.Formatter(
            '%(message)s'
        )
        
        file_handler.setFormatter(detailed_formatter)
        console_handler.setFormatter(simple_formatter)
        
        # 添加处理器
        logger.addHandler(file_handler)
        logger.addHandler(console_handler)
        
        cls._logger = logger
    
    @classmethod
    def get_logger(cls, name: Optional[str] = None) -> logging.Logger:
        
        if cls._logger is None:
            cls._setup_logger()
        
        if name:
            return logging.getLogger(f"service.{name}")
        return cls._logger

# 全局日志记录
_logger_manager = LoggerManager()

def get_logger(name: Optional[str] = None) -> logging.Logger:
    """
    获取日志记录器
    
    参数:
        name: 日志记录器名称(可选)
    
    返回:
        日志记录器
    
    示例:
        logger = get_logger("my_module")
        logger.info("This is an info message")
    """
    return _logger_manager.get_logger(name)