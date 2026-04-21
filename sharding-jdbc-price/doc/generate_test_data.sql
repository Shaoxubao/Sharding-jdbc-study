-- 生成测试数据的存储过程
DELIMITER $$

USE price_db$$

DROP PROCEDURE IF EXISTS generate_test_data$$

CREATE PROCEDURE generate_test_data(IN total_count INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE product_id BIGINT;
    DECLARE sku_id BIGINT;
    DECLARE price DECIMAL(10,2);
    DECLARE original_price DECIMAL(10,2);
    DECLARE cost_price DECIMAL(10,2);
    
    WHILE i <= total_count DO
        SET product_id = FLOOR(1 + RAND() * 1000000);
        SET sku_id = product_id * 10 + FLOOR(1 + RAND() * 9);
        SET price = ROUND(10 + RAND() * 1000, 2);
        SET original_price = ROUND(price * (1 + RAND() * 0.3), 2);
        SET cost_price = ROUND(price * (0.5 + RAND() * 0.2), 2);
        
        INSERT INTO product_price (product_id, sku_id, price, original_price, cost_price, status)
        VALUES (product_id, sku_id, price, original_price, cost_price, 1);
        
        SET i = i + 1;
        
        IF i % 10000 = 0 THEN
            COMMIT;
            SELECT CONCAT('已插入 ', i, ' 条数据') AS progress;
        END IF;
    END WHILE;
    
    COMMIT;
    SELECT CONCAT('完成！共插入 ', total_count, ' 条测试数据') AS result;
END$$

DELIMITER ;

-- 调用存储过程生成测试数据
-- CALL generate_test_data(30000000);