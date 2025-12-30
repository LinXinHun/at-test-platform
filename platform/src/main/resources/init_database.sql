-- 创建数据库
CREATE DATABASE IF NOT EXISTS `test_execution_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE `test_execution_platform`;

-- 创建执行节点表
CREATE TABLE IF NOT EXISTS `execution_nodes` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '执行节点ID',
  `node_id` VARCHAR(255) NOT NULL COMMENT '执行节点标识',
  `name` VARCHAR(255) NOT NULL COMMENT '执行节点名称',
  `description` TEXT COMMENT '执行节点描述',
  `host` VARCHAR(255) NOT NULL COMMENT '执行节点主机地址',
  `port` INT COMMENT '执行节点端口',
  `status` VARCHAR(50) NOT NULL COMMENT '执行节点状态',
  `os_info` TEXT COMMENT '操作系统信息',
  `cpu_info` TEXT COMMENT 'CPU信息',
  `memory_info` TEXT COMMENT '内存信息',
  `last_heartbeat` DATETIME COMMENT '最后心跳时间',
  `created_at` DATETIME NOT NULL COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_node_id` (`node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建测试脚本表
CREATE TABLE IF NOT EXISTS `test_scripts` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '测试脚本ID',
  `name` VARCHAR(255) NOT NULL COMMENT '测试脚本名称',
  `description` TEXT COMMENT '测试脚本描述',
  `file_path` VARCHAR(255) COMMENT '测试脚本文件路径',
  `script_content` TEXT COMMENT '测试脚本内容',
  `parameters` TEXT COMMENT '测试脚本参数',
  `script_type` VARCHAR(100) COMMENT '测试脚本类型',
  `timeout` INT COMMENT '测试脚本超时时间（秒）',
  `retry_count` INT COMMENT '测试脚本重试次数',
  `created_at` DATETIME NOT NULL COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建测试计划表
CREATE TABLE IF NOT EXISTS `test_plans` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '测试计划ID',
  `name` VARCHAR(255) NOT NULL COMMENT '测试计划名称',
  `description` TEXT COMMENT '测试计划描述',
  `last_execution_status` VARCHAR(50) COMMENT '最后执行状态',
  `last_execution_time` DATETIME COMMENT '最后执行时间',
  `last_execution_node_id` BIGINT COMMENT '最后执行节点ID',
  `created_at` DATETIME NOT NULL COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `fk_last_execution_node` (`last_execution_node_id`),
  CONSTRAINT `fk_last_execution_node` FOREIGN KEY (`last_execution_node_id`) REFERENCES `execution_nodes` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建测试计划与脚本的多对多关联表
CREATE TABLE IF NOT EXISTS `test_plan_scripts` (
  `plan_id` BIGINT NOT NULL COMMENT '测试计划ID',
  `script_id` BIGINT NOT NULL COMMENT '测试脚本ID',
  PRIMARY KEY (`plan_id`, `script_id`),
  KEY `fk_plan_script_plan` (`plan_id`),
  KEY `fk_plan_script_script` (`script_id`),
  CONSTRAINT `fk_plan_script_plan` FOREIGN KEY (`plan_id`) REFERENCES `test_plans` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_plan_script_script` FOREIGN KEY (`script_id`) REFERENCES `test_scripts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建测试执行任务表
CREATE TABLE IF NOT EXISTS `test_execution_tasks` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '测试执行任务ID',
  `plan_id` BIGINT NOT NULL COMMENT '测试计划ID',
  `status` VARCHAR(255) NOT NULL COMMENT '任务状态',
  `priority` VARCHAR(100) COMMENT '任务优先级',
  `environment` VARCHAR(255) COMMENT '执行环境',
  `executor` VARCHAR(255) COMMENT '执行者',
  `execution_node_id` VARCHAR(255) COMMENT '执行节点ID',
  `error_message` TEXT COMMENT '错误信息',
  `start_time` DATETIME COMMENT '开始时间',
  `end_time` DATETIME COMMENT '结束时间',
  `created_at` DATETIME NOT NULL COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `fk_task_plan` (`plan_id`),
  CONSTRAINT `fk_task_plan` FOREIGN KEY (`plan_id`) REFERENCES `test_plans` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建测试计划执行表
CREATE TABLE IF NOT EXISTS `test_plan_executions` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '测试计划执行ID',
  `plan_id` BIGINT NOT NULL COMMENT '测试计划ID',
  `node_id` BIGINT NOT NULL COMMENT '执行节点ID',
  `status` VARCHAR(255) NOT NULL COMMENT '执行状态',
  `total_scripts` INT COMMENT '总脚本数',
  `success_scripts` INT COMMENT '成功脚本数',
  `failed_scripts` INT COMMENT '失败脚本数',
  `start_time` DATETIME COMMENT '开始时间',
  `end_time` DATETIME COMMENT '结束时间',
  `created_at` DATETIME NOT NULL COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `fk_execution_plan` (`plan_id`),
  KEY `fk_execution_node` (`node_id`),
  CONSTRAINT `fk_execution_plan` FOREIGN KEY (`plan_id`) REFERENCES `test_plans` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_execution_node` FOREIGN KEY (`node_id`) REFERENCES `execution_nodes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建测试计划执行日志表
CREATE TABLE IF NOT EXISTS `test_plan_execution_logs` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '执行日志ID',
  `execution_id` BIGINT NOT NULL COMMENT '测试计划执行ID',
  `script_id` BIGINT NOT NULL COMMENT '测试脚本ID',
  `status` VARCHAR(255) NOT NULL COMMENT '执行状态',
  `result` TEXT COMMENT '执行结果',
  `error_message` TEXT COMMENT '错误信息',
  `execution_time` BIGINT COMMENT '执行时间（毫秒）',
  `log_content` TEXT COMMENT '日志内容',
  `start_time` DATETIME COMMENT '开始时间',
  `end_time` DATETIME COMMENT '结束时间',
  `created_at` DATETIME NOT NULL COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `fk_log_execution` (`execution_id`),
  KEY `fk_log_script` (`script_id`),
  CONSTRAINT `fk_log_execution` FOREIGN KEY (`execution_id`) REFERENCES `test_plan_executions` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_log_script` FOREIGN KEY (`script_id`) REFERENCES `test_scripts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建测试结果表
CREATE TABLE IF NOT EXISTS `test_results` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '测试结果ID',
  `task_id` BIGINT NOT NULL COMMENT '测试执行任务ID',
  `script_id` BIGINT NOT NULL COMMENT '测试脚本ID',
  `status` VARCHAR(255) NOT NULL COMMENT '执行状态',
  `output` TEXT COMMENT '输出内容',
  `error` TEXT COMMENT '错误信息',
  `duration` BIGINT COMMENT '执行时间（毫秒）',
  `executed_at` DATETIME NOT NULL COMMENT '执行时间',
  `created_at` DATETIME NOT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `fk_result_task` (`task_id`),
  KEY `fk_result_script` (`script_id`),
  CONSTRAINT `fk_result_task` FOREIGN KEY (`task_id`) REFERENCES `test_execution_tasks` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_result_script` FOREIGN KEY (`script_id`) REFERENCES `test_scripts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建测试执行结果表
CREATE TABLE IF NOT EXISTS `test_execution_results` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '测试执行结果ID',
  `script_id` BIGINT NOT NULL COMMENT '测试脚本ID',
  `status` VARCHAR(255) NOT NULL COMMENT '执行状态',
  `output` TEXT COMMENT '输出内容',
  `error` TEXT COMMENT '错误信息',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME COMMENT '结束时间',
  `execution_time` BIGINT COMMENT '执行时间（毫秒）',
  `log_file_path` VARCHAR(255) COMMENT '日志文件路径',
  PRIMARY KEY (`id`),
  KEY `fk_execution_result_script` (`script_id`),
  CONSTRAINT `fk_execution_result_script` FOREIGN KEY (`script_id`) REFERENCES `test_scripts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建测试报告表
CREATE TABLE IF NOT EXISTS `test_reports` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '测试报告ID',
  `execution_id` BIGINT COMMENT '测试计划执行ID',
  `plan_name` VARCHAR(255) NOT NULL COMMENT '测试计划名称',
  `name` VARCHAR(255) NOT NULL COMMENT '报告名称',
  `total_scripts` INT COMMENT '总脚本数',
  `success_scripts` INT COMMENT '成功脚本数',
  `failed_scripts` INT COMMENT '失败脚本数',
  `pass_rate` DOUBLE COMMENT '通过率',
  `generation_time` DATETIME NOT NULL COMMENT '生成时间',
  `generated_at` DATETIME NOT NULL COMMENT '生成时间戳',
  `report_data` TEXT COMMENT 'JSON格式，用于存储图表数据',
  `status` VARCHAR(255) NOT NULL COMMENT '报告状态',
  `created_at` DATETIME NOT NULL COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `fk_report_execution` (`execution_id`),
  CONSTRAINT `fk_report_execution` FOREIGN KEY (`execution_id`) REFERENCES `test_plan_executions` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;