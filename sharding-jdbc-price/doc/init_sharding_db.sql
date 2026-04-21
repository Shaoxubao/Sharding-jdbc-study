-- 创建新的分片数据库
CREATE DATABASE IF NOT EXISTS price_db_0 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS price_db_1 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS price_db_2 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS price_db_3 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 在每个数据库中创建分片表
USE price_db_0;
CREATE TABLE IF NOT EXISTS product_price_0 (
    id BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    original_price DECIMAL(10,2) COMMENT '原价',
    cost_price DECIMAL(10,2) COMMENT '成本价',
    start_time DATETIME COMMENT '生效开始时间',
    end_time DATETIME COMMENT '生效结束时间',
    status TINYINT DEFAULT 1 COMMENT '状态：1-有效，0-无效',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id),
    INDEX idx_sku_id (sku_id),
    INDEX idx_price (price),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品价格表_0';

CREATE TABLE IF NOT EXISTS product_price_1 LIKE product_price_0;
CREATE TABLE IF NOT EXISTS product_price_2 LIKE product_price_0;
CREATE TABLE IF NOT EXISTS product_price_3 LIKE product_price_0;
CREATE TABLE IF NOT EXISTS product_price_4 LIKE product_price_0;
CREATE TABLE IF NOT EXISTS product_price_5 LIKE product_price_0;
CREATE TABLE IF NOT EXISTS product_price_6 LIKE product_price_0;
CREATE TABLE IF NOT EXISTS product_price_7 LIKE product_price_0;

CREATE TABLE IF NOT EXISTS product_price_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    price_id BIGINT NOT NULL COMMENT '原价格表ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    sku_id BIGINT NOT NULL COMMENT 'SKU ID',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    original_price DECIMAL(10,2) COMMENT '原价',
    cost_price DECIMAL(10,2) COMMENT '成本价',
    start_time DATETIME COMMENT '生效开始时间',
    end_time DATETIME COMMENT '生效结束时间',
    status TINYINT DEFAULT 1 COMMENT '状态',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product_id (product_id),
    INDEX idx_sku_id (sku_id),
    INDEX idx_price_id (price_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品价格历史表';

USE price_db_1;
CREATE TABLE IF NOT EXISTS product_price_0 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_1 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_2 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_3 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_4 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_5 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_6 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_7 LIKE price_db_0.product_price_0;

CREATE TABLE IF NOT EXISTS product_price_history LIKE price_db_0.product_price_history;

USE price_db_2;
CREATE TABLE IF NOT EXISTS product_price_0 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_1 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_2 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_3 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_4 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_5 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_6 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_7 LIKE price_db_0.product_price_0;

CREATE TABLE IF NOT EXISTS product_price_history LIKE price_db_0.product_price_history;

USE price_db_3;
CREATE TABLE IF NOT EXISTS product_price_0 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_1 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_2 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_3 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_4 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_5 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_6 LIKE price_db_0.product_price_0;
CREATE TABLE IF NOT EXISTS product_price_7 LIKE price_db_0.product_price_0;

CREATE TABLE IF NOT EXISTS product_price_history LIKE price_db_0.product_price_history;