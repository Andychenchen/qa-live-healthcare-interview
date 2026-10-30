-- QA Live Healthcare 数据库初始化脚本
-- 容器首次启动时自动执行（docker-entrypoint-initdb.d）
-- 注意：MySQL 官方镜像已通过 MYSQL_DATABASE=qa_live 自动建库，此处仅补充字符集设置

USE qa_live;

SET NAMES utf8mb4;

ALTER DATABASE qa_live CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
