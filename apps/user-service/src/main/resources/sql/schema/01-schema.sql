-- MariaDB 최적화된 스키마 (단수형 테이블 네이밍, 외래 키 제약조건 제거 버전)
CREATE TABLE `permission` (
                                            `id` int unsigned NOT NULL AUTO_INCREMENT,
                                            `resource` varchar(100) NULL,
    `description` varchar(255) NULL,
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_active` boolean DEFAULT TRUE,
    `updated_by` bigint unsigned NULL,
    `created_by` bigint unsigned NULL,
    PRIMARY KEY (`id`)
    );

CREATE TABLE `organization` (
                                              `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                              `name` varchar(150) NULL,
    `domain_name` varchar(100) NULL,
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
    );

CREATE TABLE `role` (
                                      `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                      `organization_id` bigint unsigned NULL,
                                      `name` varchar(100) NULL,
    `description` varchar(500) NULL,
    PRIMARY KEY (`id`)
    );

CREATE TABLE `user` (
                                      `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                      `name` varchar(50) NULL,
    `email` varchar(100) NULL,
    `password` varchar(255) NULL,
    `status` varchar(20) NULL,
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
    );

CREATE TABLE `department` (
                                            `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                            `organization_id` bigint unsigned NOT NULL,
                                            `name` varchar(100) NULL,
    PRIMARY KEY (`id`)
    );

CREATE TABLE `position` (
                                          `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                          `organization_id` bigint unsigned NOT NULL,
                                          `title` varchar(100) NULL,
    PRIMARY KEY (`id`)
    );

CREATE TABLE `user_organization` (
                                                   `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                                   `user_id` bigint unsigned NOT NULL,
                                                   `organization_id` bigint unsigned NOT NULL,
                                                   `position_id` bigint unsigned NOT NULL,
                                                   `department_id` bigint unsigned NOT NULL,
                                                   `employee_number` varchar(50) NULL,
    `status` varchar(20) NULL,
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
    );

CREATE TABLE `role_permission` (
                                                 `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                                 `role_id` bigint unsigned NOT NULL,
                                                 `permission_id` int unsigned NOT NULL,
                                                 PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`)
    );

CREATE TABLE `user_role` (
                                           `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                           `role_id` bigint unsigned NOT NULL,
                                           `user_organization_id` bigint unsigned NOT NULL,
                                           PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`role_id`, `user_organization_id`)
    );

-- 성능 최적화를 위한 인덱스
CREATE INDEX `idx_user_email` ON `user` (`email`);
CREATE INDEX `idx_user_status` ON `user` (`status`);
CREATE INDEX `idx_user_organization_user` ON `user_organization` (`user_id`);
CREATE INDEX `idx_user_organization_org` ON `user_organization` (`organization_id`);
CREATE INDEX `idx_user_organization_status` ON `user_organization` (`status`);
CREATE INDEX `idx_role_org` ON `role` (`organization_id`);
CREATE INDEX `idx_permission_resource` ON `permission` (`resource`);
CREATE INDEX `idx_permission_active` ON `permission` (`is_active`);



CREATE TABLE `workflow` (
                                          `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                          `name` varchar(100) NOT NULL UNIQUE,
    `description` text NULL,
    `is_enabled` boolean DEFAULT TRUE,
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    `created_by` bigint unsigned NULL,
    `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` bigint unsigned NULL,
    PRIMARY KEY (`id`)
    );

CREATE TABLE `schedule` (
                                          `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                          `workflow_id` bigint unsigned NOT NULL,
                                          `cron_expression` varchar(50) NULL,
    `parameters` json NULL,
    `is_active` boolean DEFAULT TRUE,
    `last_run_status` varchar(20) NULL,
    `last_run_at` timestamp NULL,
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    `created_by` bigint unsigned NULL,
    `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` bigint unsigned NULL,
    PRIMARY KEY (`id`)
    );

CREATE TABLE `job` (
                                     `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                     `name` varchar(100) NOT NULL UNIQUE,
    `description` text NULL,
    `is_enabled` boolean DEFAULT TRUE,
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    `created_by` bigint unsigned NULL,
    `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `updated_by` bigint unsigned NULL,
    PRIMARY KEY (`id`)
    );

CREATE TABLE `task` (
                                      `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                      `name` varchar(100) NOT NULL UNIQUE,
    `type` varchar(50) NULL,
    `parameters` json NULL,
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
    );

CREATE TABLE `workflow_job` (
                                              `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                              `workflow_id` bigint unsigned NOT NULL,
                                              `job_id` bigint unsigned NOT NULL,
                                              PRIMARY KEY (`id`),
    UNIQUE KEY `uk_workflow_job` (`workflow_id`, `job_id`)
    );

CREATE TABLE `job_task` (
                                          `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                          `job_id` bigint unsigned NOT NULL,
                                          `task_id` bigint unsigned NOT NULL,
                                          `execution_order` int NULL,
                                          PRIMARY KEY (`id`),
    UNIQUE KEY `uk_job_task` (`job_id`, `task_id`)
    );

CREATE TABLE `execution_log` (
                                               `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                               `execution_type` varchar(20) NULL COMMENT 'task, schedule, job, workflow',
    `source_id` bigint unsigned NULL COMMENT '모든 데이터에 대한 ID ex: job_id, schedule_id, task_id, ...',
    `log_level` varchar(20) NULL,
    `executed_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    `log_message` text NULL,
    `trace_id` char(36) NULL,
    `config_snapshot` json NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_source_id_type` (`source_id`, `execution_type`)
    );

CREATE TABLE `task_io_data` (
                                              `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                              `task_run_id` bigint unsigned NOT NULL,
                                              `io_type` varchar(10) NOT NULL COMMENT 'INPUT, OUTPUT',
    `name` varchar(100) NOT NULL COMMENT '파라미터/변수 이름',
    `data_type` varchar(50) NOT NULL COMMENT 'string, number, json, file, etc',
    `data_value` json NULL COMMENT '실제 데이터 값',
    `data_size` bigint NULL COMMENT '데이터 크기 (bytes)',
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_task_io_task_run_id` (`task_run_id`),
    INDEX `idx_task_io_type` (`io_type`),
    INDEX `idx_task_io_name` (`name`)
    );

CREATE TABLE `config` (
                                        `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                        `target_type` varchar(50) NULL COMMENT 'user, job, workflow',
    `target_id` bigint unsigned NULL,
    `version` int NULL,
    `json` json NULL,
    `is_active` boolean DEFAULT TRUE,
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    `created_by` bigint unsigned NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_target` (`target_type`, `target_id`)
    );

CREATE TABLE `category` (
                                          `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                          `name` varchar(100) NULL,
    `description` text NULL,
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
    );

CREATE TABLE `user_config` (
                                             `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                             `user_id` bigint unsigned NOT NULL,
                                             `type` varchar(50) NULL,
    `name` varchar(100) NULL,
    `json` json NULL,
    `is_active` boolean DEFAULT TRUE,
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
    );

-- 인덱스 추가 (성능 최적화)
CREATE INDEX `idx_schedule_workflow` ON `schedule` (`workflow_id`);
CREATE INDEX `idx_job_enabled` ON `job` (`is_enabled`);
CREATE INDEX `idx_task_type` ON `task` (`type`);
CREATE INDEX `idx_workflow_enabled` ON `workflow` (`is_enabled`);
CREATE UNIQUE INDEX `uk_schedule_workflow` ON `schedule` (`workflow_id`);
CREATE UNIQUE INDEX `uk_job_name` ON `job` (`name`);
CREATE UNIQUE INDEX `uk_task_name` ON `task` (`name`);
CREATE UNIQUE INDEX `uk_workflow_name` ON `workflow` (`name`);
CREATE INDEX `idx_user_config_user` ON `user_config` (`user_id`);



-- 워크플로우 실행 테이블
CREATE TABLE `workflow_run` (
                                              `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                              `workflow_id` bigint unsigned NOT NULL,
                                              `trace_id` char(36) NOT NULL,
    `run_number` varchar(20) NULL,
    `status` varchar(20) NULL COMMENT 'pending, running, success, failed, cancelled',
    `trigger_type` varchar(20) NULL COMMENT 'manual, schedule, push, pull_request',
    `started_at` timestamp NULL,
    `finished_at` timestamp NULL,
    `created_by` bigint unsigned NULL,
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_workflow_run_trace` (`trace_id`),
    INDEX `idx_workflow_run_status` (`status`),
    INDEX `idx_workflow_run_workflow_id` (`workflow_id`),
    INDEX `idx_workflow_run_created_at` (`created_at`)
    );

-- Job 실행 테이블
CREATE TABLE `job_run` (
                                         `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                         `workflow_run_id` bigint unsigned NOT NULL,
                                         `job_id` bigint unsigned NOT NULL,
                                         `status` varchar(20) NULL COMMENT 'pending, running, success, failed, cancelled, skipped',
    `started_at` timestamp NULL,
    `finished_at` timestamp NULL,
    `execution_order` int NULL,
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_job_run_workflow_run_id` (`workflow_run_id`),
    INDEX `idx_job_run_status` (`status`),
    INDEX `idx_job_run_job_id` (`job_id`)
    );

-- Task 실행 테이블
CREATE TABLE `task_run` (
                                          `id` bigint unsigned NOT NULL AUTO_INCREMENT,
                                          `job_run_id` bigint unsigned NOT NULL,
                                          `task_id` bigint unsigned NOT NULL,
                                          `status` varchar(20) NULL COMMENT 'pending, running, success, failed, cancelled, skipped',
    `started_at` timestamp NULL,
    `finished_at` timestamp NULL,
    `execution_order` int NULL,
    `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_task_run_job_run_id` (`job_run_id`),
    INDEX `idx_task_run_status` (`status`),
    INDEX `idx_task_run_task_id` (`task_id`)
    );

-- v0.0.3
DROP TABLE IF EXISTS `config`;

ALTER TABLE `workflow_job`
    ADD COLUMN `execution_order` INT NULL AFTER `job_id`;


ALTER TABLE `schedule`
    ADD COLUMN `schedule_text` varchar(20) NULL;

ALTER TABLE `workflow`
    ADD COLUMN `default_config`json NULL;


ALTER TABLE `user`
    ADD COLUMN `joined_at` timestamp NULL;

ALTER TABLE `department`
    ADD COLUMN `description` varchar(100) NULL;

-- v0.4
-- 기존 execution_log 테이블 수정
-- 컬럼 추가 (한 번에 하나씩)
-- 컬럼 추가
ALTER TABLE execution_log ADD COLUMN run_id BIGINT NULL;
ALTER TABLE execution_log ADD COLUMN status VARCHAR(20) NULL;
ALTER TABLE execution_log ADD COLUMN duration_ms INT NULL;
ALTER TABLE execution_log ADD COLUMN error_code VARCHAR(50) NULL;
ALTER TABLE execution_log ADD COLUMN reserved1 VARCHAR(100) NULL;
ALTER TABLE execution_log ADD COLUMN reserved2 VARCHAR(100) NULL;
ALTER TABLE execution_log ADD COLUMN reserved3 INT NULL;
ALTER TABLE execution_log ADD COLUMN reserved4 json NULL;
ALTER TABLE execution_log ADD COLUMN reserved5 TIMESTAMP NULL;

-- 컬럼 수정
ALTER TABLE execution_log MODIFY COLUMN log_message VARCHAR(500) NOT NULL;
ALTER TABLE execution_log MODIFY COLUMN executed_at TIMESTAMP NOT NULL;

-- 컬럼 삭제
ALTER TABLE execution_log DROP COLUMN config_snapshot;

-- 인덱스 생성 (CREATE INDEX 별도)
CREATE INDEX idx_run_id ON execution_log(run_id);
CREATE INDEX idx_log_level_status ON execution_log(log_level, status);
CREATE INDEX idx_error_code ON execution_log(error_code);
CREATE INDEX idx_duration ON execution_log(duration_ms);
CREATE INDEX idx_execution_type_source ON execution_log(execution_type, source_id);

-- v0.5
-- schedule 테이블 workflow_id unique 조건 제거
ALTER TABLE schedule DROP INDEX uk_schedule_workflow;
ALTER TABLE schedule ADD UNIQUE KEY uk_schedule_workflow_cron (workflow_id, cron_expression);