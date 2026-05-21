CREATE DATABASE IF NOT EXISTS skills_agent
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE skills_agent;

CREATE TABLE IF NOT EXISTS audit_log (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id    VARCHAR(64)  NOT NULL                COMMENT '会话ID',
    skill_name    VARCHAR(128) NOT NULL                COMMENT '调用技能名称',
    user_message  TEXT                                  COMMENT '用户问题',
    agent_response TEXT                                COMMENT 'AI回答',
    evaluation    TEXT                                  COMMENT 'AI质检结果',
    timestamp     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
    duration_ms   BIGINT       DEFAULT 0               COMMENT '耗时(毫秒)',
    deleted       TINYINT      DEFAULT 0               COMMENT '逻辑删除 0-未删 1-已删',
    PRIMARY KEY (id),
    INDEX idx_session_id (session_id),
    INDEX idx_skill_name (skill_name),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';
