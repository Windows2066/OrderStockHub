-- 新增 Outbox 事件表：在本地事务内先落库，再异步投递 MQ
CREATE TABLE IF NOT EXISTS t_outbox_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(64) NOT NULL,
    biz_key VARCHAR(64) NOT NULL,
    topic VARCHAR(128) NOT NULL,
    tags VARCHAR(64),
    payload TEXT NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0-PENDING,1-PUBLISHED,2-FAILED',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME,
    last_error VARCHAR(512),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_event_type_biz_key (event_type, biz_key),
    INDEX idx_status_next_retry (status, next_retry_at),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

