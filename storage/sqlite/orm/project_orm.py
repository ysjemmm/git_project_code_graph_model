from datetime import datetime

from sqlalchemy import Column, Integer, String, DateTime, Boolean
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()

class ProjectORM(Base):
    __tablename__ = 'application_projects_cache'

    id = Column(Integer, primary_key=True, autoincrement=True)
    project_name = Column(String)
    project_key = Column(String)
    project_type = Column(String)
    repo_url = Column(String)
    last_update_time = Column(DateTime, update_default=datetime.now)
    cache_dir = Column(String)
    repo_exists = Column(Boolean)
    head_branch = Column(String)
    head_commit = Column(String)
    app_type = Column(String, default='backend')
    language = Column(String, default='java')
    created_at = Column(DateTime, insert_default=datetime.now)
    updated_at = Column(DateTime, insert_default=datetime.now, update_default=datetime.now)