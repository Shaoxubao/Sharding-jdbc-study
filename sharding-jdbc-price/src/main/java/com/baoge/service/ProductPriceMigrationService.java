package com.baoge.service;

import com.baoge.entity.ProductPrice;
import com.baoge.entity.ProductPriceHistory;
import com.baoge.mapper.ProductPriceHistoryMapper;
import com.baoge.mapper.ProductPriceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ProductPriceMigrationService {

    @Autowired
    private ProductPriceMapper newPriceMapper;

    @Autowired
    private ProductPriceHistoryMapper historyMapper;

    @Autowired
    @Qualifier("old-ds")
    private DataSource oldDataSource;

    private JdbcTemplate oldJdbcTemplate;

    private final RowMapper<ProductPrice> PRICE_ROW_MAPPER = (rs, rowNum) -> {
        ProductPrice p = new ProductPrice();
        p.setId(rs.getLong("id"));
        p.setProductId(rs.getLong("product_id"));
        p.setSkuId(rs.getLong("sku_id"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setOriginalPrice(rs.getBigDecimal("original_price"));
        p.setCostPrice(rs.getBigDecimal("cost_price"));
        p.setStartTime(rs.getTimestamp("start_time"));
        p.setEndTime(rs.getTimestamp("end_time"));
        p.setStatus(rs.getInt("status"));
        p.setCreateTime(rs.getTimestamp("create_time"));
        p.setUpdateTime(rs.getTimestamp("update_time"));
        return p;
    };

    @Value("${migration.dual-write.enabled:false}")
    private boolean dualWriteEnabled;

    @Value("${migration.dual-write.old-table-write:true}")
    private boolean oldTableWriteEnabled;

    @Value("${migration.dual-write.new-table-write:true}")
    private boolean newTableWriteEnabled;

    @PostConstruct
    public void init() {
        oldJdbcTemplate = new JdbcTemplate(oldDataSource);
    }

    @Transactional
    public void insertWithDualWrite(ProductPrice productPrice) {
        productPrice.setCreateTime(new Date());
        productPrice.setUpdateTime(new Date());
        productPrice.setStatus(1);

        if (dualWriteEnabled && oldTableWriteEnabled) {
            oldJdbcTemplate.update(
                "INSERT INTO product_price (id, product_id, sku_id, price, original_price, cost_price, start_time, end_time, status, create_time, update_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                productPrice.getId(), productPrice.getProductId(), productPrice.getSkuId(),
                productPrice.getPrice(), productPrice.getOriginalPrice(), productPrice.getCostPrice(),
                productPrice.getStartTime(), productPrice.getEndTime(), productPrice.getStatus(),
                productPrice.getCreateTime(), productPrice.getUpdateTime()
            );
        }

        if (dualWriteEnabled && newTableWriteEnabled) {
            newPriceMapper.insert(productPrice);
        }

        ProductPriceHistory history = new ProductPriceHistory();
        BeanUtils.copyProperties(productPrice, history);
        history.setPriceId(productPrice.getId());
        historyMapper.insert(history);
    }

    @Transactional
    public void updateWithDualWrite(ProductPrice productPrice) {
        // 先查询旧值，用于保存历史记录
        List<ProductPrice> oldPrices = oldJdbcTemplate.query(
            "SELECT * FROM product_price WHERE id = ?",
            PRICE_ROW_MAPPER, productPrice.getId()
        );
        ProductPrice oldPrice = oldPrices.isEmpty() ? null : oldPrices.get(0);

        productPrice.setUpdateTime(new Date());

        if (dualWriteEnabled && oldTableWriteEnabled) {
            oldJdbcTemplate.update(
                "UPDATE product_price SET price = ?, original_price = ?, cost_price = ?, start_time = ?, end_time = ?, status = ?, update_time = ? WHERE id = ?",
                productPrice.getPrice(), productPrice.getOriginalPrice(), productPrice.getCostPrice(),
                productPrice.getStartTime(), productPrice.getEndTime(), productPrice.getStatus(),
                productPrice.getUpdateTime(), productPrice.getId()
            );
        }

        if (dualWriteEnabled && newTableWriteEnabled) {
            newPriceMapper.update(productPrice);
        }

        // 保存更新前的旧值到历史表
        if (oldPrice != null) {
            ProductPriceHistory history = new ProductPriceHistory();
            BeanUtils.copyProperties(oldPrice, history);
            history.setPriceId(oldPrice.getId());
            historyMapper.insert(history);
        }
    }
}
