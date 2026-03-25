-- 为 application_projects_cache.project_name 添加唯一索引
-- SQLite 不支持 ADD CONSTRAINT，用 CREATE UNIQUE INDEX 实现
-- IF NOT EXISTS 保证幂等
CREATE UNIQUE INDEX IF NOT EXISTS idx_application_projects_project_name_unique
  ON application_projects_cache(project_name)
  WHERE project_name IS NOT NULL AND project_name != '';
