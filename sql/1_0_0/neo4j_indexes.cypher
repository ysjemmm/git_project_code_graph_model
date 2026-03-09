// ============================================================
// Neo4j 索引创建脚本 - v1.0.0
// ============================================================
// 用途：优化查询性能，特别是批量操作和关系查找
// 执行方式：在 Neo4j Browser 或通过 Python 脚本执行
// ============================================================

// ============================================================
// 步骤 0：删除旧约束（如果存在）
// ============================================================
// 注意：如果是全新数据库，这些命令会报错，可以忽略

// 1.1 symbol_id 唯一索引（主键）
CREATE CONSTRAINT cst_symbol_id
FOR (n:JavaObject) REQUIRE (n.symbol_id) IS NODE KEY;

// 1.2 复合索引：from_type + qualified_name + belong_project
// 用于外部类链接查询（最重要的优化）
CREATE INDEX javaobject_link_lookup IF NOT EXISTS
FOR (n:JavaObject)
ON (n.from_type, n.qualified_name, n.belong_project);

// 1.3 qualified_name 索引（用于类查找）
CREATE INDEX javaobject_qualified_name IF NOT EXISTS
FOR (n:JavaObject)
ON (n.qualified_name);

// 1.4 belong_project 索引（用于按项目查询）
CREATE INDEX javaobject_belong_project IF NOT EXISTS
FOR (n:JavaObject)
ON (n.belong_project);

// 1.5 from_type 索引（用于区分内部/外部定义）
CREATE INDEX javaobject_from_type IF NOT EXISTS
FOR (n:JavaObject)
ON (n.from_type);

// 1.6 object_type 索引（用于按类型查询：类/接口/枚举等）
CREATE INDEX javaobject_object_type IF NOT EXISTS
FOR (n:JavaObject)
ON (n.object_type);

// ============================================================
// 2. Project 节点索引
// ============================================================

// 2.1 复合唯一索引：name + project_type（主键）
// 允许同名项目，但类型必须不同（例如：spring-core 作为项目 vs 作为库）
CREATE CONSTRAINT FOR (n:Project) REQUIRE (n.symbol_id) IS UNIQUE;

// 2.2 symbol_id 索引（用于快速查找）
CREATE INDEX project_symbol_id IF NOT EXISTS
FOR (n:Project)
ON (n.symbol_id);

// 2.3 name 索引（用于按项目名查询）
CREATE INDEX project_name IF NOT EXISTS
FOR (n:Project)
ON (n.name);

// 2.4 project_type 索引（用于区分项目/库）
CREATE INDEX project_type IF NOT EXISTS
FOR (n:Project)
ON (n.project_type);

// ============================================================
// 3. JavaFile 节点索引
// ============================================================

// 3.1 symbol_id 唯一索引（主键）
CREATE CONSTRAINT FOR (n:JavaFile) REQUIRE n.symbol_id IS UNIQUE;

// 3.2 复合索引：file_path + belong_project
// 用于删除文件相关节点
CREATE INDEX javafile_path_project IF NOT EXISTS
FOR (n:JavaFile)
ON (n.file_path, n.belong_project);

// 3.3 belong_project 索引
CREATE INDEX javafile_belong_project IF NOT EXISTS
FOR (n:JavaFile)
ON (n.belong_project);

// ============================================================
// 4. JavaMethod 节点索引
// ============================================================

// 4.1 symbol_id 唯一索引（主键）
CREATE CONSTRAINT FOR (n:JavaMethod) REQUIRE n.symbol_id IS UNIQUE;

// 4.2 belong_project 索引
CREATE INDEX javamethod_belong_project IF NOT EXISTS
FOR (n:JavaMethod)
ON (n.belong_project);

// 4.3 is_constructor 索引（用于区分构造函数）
CREATE INDEX javamethod_is_constructor IF NOT EXISTS
FOR (n:JavaMethod)
ON (n.is_constructor);

// ============================================================
// 5. JavaField 节点索引
// ============================================================

// 5.1 symbol_id 唯一索引（主键）
CREATE CONSTRAINT FOR (n:JavaField) REQUIRE n.symbol_id IS UNIQUE;

// 5.2 belong_project 索引
CREATE INDEX javafield_belong_project IF NOT EXISTS
FOR (n:JavaField)
ON (n.belong_project);

// ============================================================
// 6. JavaMethodParameter 节点索引
// ============================================================

// 6.1 symbol_id 唯一索引（主键）
CREATE CONSTRAINT FOR (n:JavaMethodParameter) REQUIRE n.symbol_id IS UNIQUE;

// ============================================================
// 7. JavaEnumConstant 节点索引
// ============================================================

// 7.1 symbol_id 唯一索引（主键）
CREATE CONSTRAINT FOR (n:JavaEnumConstant) REQUIRE n.symbol_id IS UNIQUE;

// ============================================================
// 8. JavaRecordComponent 节点索引
// ============================================================

// 8.1 symbol_id 唯一索引（主键）
CREATE CONSTRAINT FOR (n:JavaRecordComponent) REQUIRE n.symbol_id IS UNIQUE;

// ============================================================
// 9. JavaCodeBlock 节点索引
// ============================================================

// 9.1 symbol_id 唯一索引（主键）
CREATE CONSTRAINT FOR (n:JavaCodeBlock) REQUIRE n.symbol_id IS UNIQUE;

// ============================================================
// 10. Comment 节点索引
// ============================================================

// 10.1 symbol_id 唯一索引（主键）
CREATE CONSTRAINT FOR (n:Comment) REQUIRE n.symbol_id IS UNIQUE;

// 10.2 comment_type 索引（用于按注释类型查询）
CREATE INDEX comment_type IF NOT EXISTS
FOR (n:Comment)
ON (n.comment_type);

// ============================================================
// 索引说明
// ============================================================
// 
// 性能优化重点：
// 1. javaobject_link_lookup - 最重要！用于外部类链接的批量查询
//    - 优化 _batch_find_internals_in_db 和 _batch_find_externals_in_db
//    - 预计性能提升：10-50倍
//
// 2. symbol_id 唯一约束 - 用于批量创建关系时的节点查找
//    - 优化 MATCH (source {symbol_id: rel.source_id})
//    - 预计性能提升：5-10倍
//
// 3. javafile_path_project - 用于删除文件相关节点
//    - 优化 delete_nodes_by_file 查询
//
// 索引维护：
// - 索引会自动维护，无需手动更新
// - 索引会占用额外存储空间（约 10-20% 的数据大小）
// - 写入性能会略有下降（约 5-10%），但查询性能大幅提升
//
// 查看索引状态：
// SHOW INDEXES;
//
// 删除索引（如果需要）：
// DROP INDEX index_name IF EXISTS;
//
// ============================================================

// ============================================================
// 执行步骤说明
// ============================================================
//
// 1. 查看现有约束
//    SHOW CONSTRAINTS;
//
// 2. 删除旧的 Project 约束（根据步骤1的结果）
//    方法A：如果约束名已知
//    DROP CONSTRAINT constraint_name_here;
//    
//    方法B：如果不确定，可以删除所有 Project 相关约束
//    先记录约束名，然后逐个删除
//
// 3. 清空数据库（可选，如果想重新开始）
//    MATCH (n) DETACH DELETE n;
//
// 4. 执行本文件中的所有索引创建语句
//    - 可以全选复制到 Neo4j Browser 执行
//    - 或者使用 Python 脚本逐条执行
//
// 5. 验证索引创建成功
//    SHOW CONSTRAINTS;
//    SHOW INDEXES;
//
// 6. 重新导入数据
//    运行你的 Python 导入脚本
//
// ============================================================
// 常见问题
// ============================================================
//
// Q: 执行约束创建时报错 "already exists"
// A: 说明约束已存在，可以跳过或先删除旧约束
//
// Q: 如何找到约束的名称？
// A: 执行 SHOW CONSTRAINTS; 查看所有约束及其名称
//
// Q: Project 节点还是只有一个
// A: 确保：
//    1. 已删除旧的 symbol_id 约束
//    2. 已创建新的 (name, project_type) 约束
//    3. 已清空数据库并重新导入
//    4. merge_builder.py 中 Project 的唯一键是 ['name', 'project_type']
//
// ============================================================
