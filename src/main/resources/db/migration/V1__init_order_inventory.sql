-- 初始化订单与库存核心表结构
CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(64) NOT NULL UNIQUE,
    request_id VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    status TINYINT NOT NULL COMMENT '0-CREATED,1-PAID,2-CANCELED',
    total_amount DECIMAL(18,2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_request_id (request_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    sku_name VARCHAR(128) NOT NULL,
    price DECIMAL(18,2) NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_order_id (order_id),
    INDEX idx_sku_code (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sku_code VARCHAR(64) NOT NULL UNIQUE,
    available_qty INT NOT NULL,
    locked_qty INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_inventory_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    biz_no VARCHAR(64) NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    change_qty INT NOT NULL,
    before_qty INT NOT NULL,
    after_qty INT NOT NULL,
    remark VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_biz_sku_type (biz_no, sku_code, change_type),
    INDEX idx_biz_no (biz_no),
    INDEX idx_sku_code (sku_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 本地联调用初始化库存数据
INSERT INTO t_inventory (sku_code, available_qty, locked_qty, version)
VALUES ('SKU-1001', 100, 0, 0)
ON DUPLICATE KEY UPDATE sku_code = VALUES(sku_code);

