CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255),
    skill_name VARCHAR(255),
    user_message TEXT,
    agent_response TEXT,
    evaluation TEXT,
    timestamp TIMESTAMP,
    duration_ms BIGINT
);
