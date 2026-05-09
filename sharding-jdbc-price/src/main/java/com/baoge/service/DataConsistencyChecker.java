package com.baoge.service;

import com.baoge.entity.ProductPrice;
import com.baoge.mapper.ProductPriceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Slf4j
@Component
public class DataConsistencyChecker {

    @Autowired
    @Qualifier("old-ds")
    private DataSource oldDataSource;

    @Autowired
    private ProductPriceMapper newPriceMapper;

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

    @PostConstruct
    public void init() {
        oldJdbcTemplate = new JdbcTemplate(oldDataSource);
    }

    public void checkAllData() {
        Long totalCountObj = oldJdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_price", Long.class);
        long totalCount = totalCountObj != null ? totalCountObj : 0;
        long checkedCount = 0;
        long errorCount = 0;

        int pageNum = 1;
        int pageSize = 1000;

        log.info("开始全量数据校验，总数据量：{}", totalCount);

        while (true) {
            List<ProductPrice> oldPriceList = oldJdbcTemplate.query(
                "SELECT * FROM product_price ORDER BY id LIMIT ? OFFSET ?",
                PRICE_ROW_MAPPER, pageSize, (pageNum - 1) * pageSize);

            if (oldPriceList.isEmpty()) {
                break;
            }

            for (ProductPrice oldPrice : oldPriceList) {
                ProductPrice newPrice = newPriceMapper.selectById(oldPrice.getId());

                if (newPrice == null) {
                    log.error("数据缺失！id: {}", oldPrice.getId());
                    errorCount++;
                } else if (!isDataEqual(oldPrice, newPrice)) {
                    log.error("数据不一致！id: {}, oldPrice: {}, newPrice: {}",
                             oldPrice.getId(), oldPrice, newPrice);
                    errorCount++;
                }

                checkedCount++;
            }

            log.info("已校验：{}/{}，错误数：{}", checkedCount, totalCount, errorCount);
            pageNum++;
        }

        log.info("数据校验完成，共校验：{} 条，错误数：{}", checkedCount, errorCount);
    }

    public void randomCheck() {
        int sampleSize = 10000;
        Long totalCountObj = oldJdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_price", Long.class);
        long totalCount = totalCountObj != null ? totalCountObj : 0;
        Random random = new Random();

        log.info("开始抽样校验，抽样数量：{}", sampleSize);

        int errorCount = 0;
        int validCount = 0;

        for (int i = 0; i < sampleSize; i++) {
            // 随机选择 offset 分页查询，获取真实存在的记录
            long randomOffset = totalCount > 0 ? (long) (random.nextDouble() * totalCount) : 0;
            int offset = (int) Math.min(randomOffset, Integer.MAX_VALUE - 1);

            List<ProductPrice> samples = oldJdbcTemplate.query(
                "SELECT * FROM product_price ORDER BY id LIMIT 1 OFFSET ?",
                PRICE_ROW_MAPPER, offset);

            if (samples.isEmpty()) {
                continue;
            }

            Long id = samples.get(0).getId();

            List<ProductPrice> oldList = oldJdbcTemplate.query(
                "SELECT * FROM product_price WHERE id = ?",
                PRICE_ROW_MAPPER, id);
            ProductPrice oldPrice = oldList.isEmpty() ? null : oldList.get(0);
            ProductPrice newPrice = newPriceMapper.selectById(id);

            if (oldPrice != null && newPrice != null) {
                validCount++;
                if (!isDataEqual(oldPrice, newPrice)) {
                    log.error("抽样校验失败！id: {}", id);
                    errorCount++;
                }
            }
        }

        log.info("抽样校验完成，有效样本：{}，错误数：{}", validCount, errorCount);
    }

    private boolean isDataEqual(ProductPrice oldPrice, ProductPrice newPrice) {
        return Objects.equals(oldPrice.getProductId(), newPrice.getProductId()) &&
               Objects.equals(oldPrice.getSkuId(), newPrice.getSkuId()) &&
               Objects.equals(oldPrice.getPrice(), newPrice.getPrice()) &&
               Objects.equals(oldPrice.getOriginalPrice(), newPrice.getOriginalPrice()) &&
               Objects.equals(oldPrice.getCostPrice(), newPrice.getCostPrice()) &&
               Objects.equals(oldPrice.getStatus(), newPrice.getStatus());
    }
}
