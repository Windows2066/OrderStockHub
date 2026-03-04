
-- ------------------------------------------------------------
-- 订单主表：保存订单头信息（谁下单、订单号、状态、总金额）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_order (
    -- 自增主键，数据库内部唯一标识
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 业务订单号，面向业务系统流转，必须唯一
    order_no VARCHAR(64) NOT NULL UNIQUE,

    -- 下单用户ID
    user_id BIGINT NOT NULL,

    -- 订单状态：0-CREATED(已创建)、1-PAID(已支付)、2-CANCELED(已取消)
    status TINYINT NOT NULL COMMENT '0-CREATED,1-PAID,2-CANCELED',

    -- 订单总金额，保留两位小数
    total_amount DECIMAL(18,2) NOT NULL,

    -- 记录创建时间
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 记录最后更新时间
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 订单明细表：一个订单可包含多个商品行
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_order_item (
    -- 自增主键
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 关联订单主表ID（此处未加外键，便于开发阶段快速迭代）
    order_id BIGINT NOT NULL,

    -- 商品SKU编码
    sku_code VARCHAR(64) NOT NULL,

    -- 商品名称快照（下单时写入，避免后续商品改名影响历史订单展示）
    sku_name VARCHAR(128) NOT NULL,

    -- 商品下单时单价
    price DECIMAL(18,2) NOT NULL,

    -- 购买数量
    quantity INT NOT NULL,

    -- 记录创建时间
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 记录最后更新时间
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- 常用查询索引：通过订单ID查询订单行
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 库存主表：按 SKU 维护可售库存与锁定库存
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_inventory (
    -- 自增主键
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- SKU唯一编码（唯一约束保证一条SKU仅一行库存）
    sku_code VARCHAR(64) NOT NULL UNIQUE,

    -- 可售库存：下单扣减的目标字段
    available_qty INT NOT NULL,

    -- 锁定库存：预留给后续“锁库存-确认扣减”流程，当前版本先保留
    locked_qty INT NOT NULL DEFAULT 0,

    -- 乐观锁版本号：并发更新时可用于冲突检测
    version INT NOT NULL DEFAULT 0,

    -- 记录创建时间
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 记录最后更新时间
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 库存流水表：审计每一次库存变化，支持追踪与补偿
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS t_inventory_log (
    -- 自增主键
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 业务关联号：通常使用订单号或请求幂等号
    biz_no VARCHAR(64) NOT NULL,

    -- 变更SKU
    sku_code VARCHAR(64) NOT NULL,

    -- 变更类型：例如 DEDUCT、ROLLBACK
    change_type VARCHAR(32) NOT NULL,

    -- 变更数量（正数）
    change_qty INT NOT NULL,

    -- 变更前库存快照
    before_qty INT NOT NULL,

    -- 变更后库存快照
    after_qty INT NOT NULL,

    -- 备注信息
    remark VARCHAR(255),

    -- 记录创建时间
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- 幂等约束：同一业务号 + SKU + 类型只能写入一次
    UNIQUE KEY uk_biz_sku_type (biz_no, sku_code, change_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------
-- 初始化测试数据：预置一个SKU用于本地联调下单与扣库存流程
-- ------------------------------------------------------------
INSERT INTO t_inventory (sku_code, available_qty, locked_qty, version)
VALUES ('SKU-1001', 100, 0, 0)
ON DUPLICATE KEY UPDATE sku_code = VALUES(sku_code);
