package com.baoge.service;

import com.baoge.entity.ProductPrice;
import com.baoge.entity.ProductPriceHistory;
import com.baoge.mapper.ProductPriceHistoryMapper;
import com.baoge.mapper.ProductPriceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ProductPriceMigrationJob {

    @Autowired
    @Qualifier("old-ds")
    private DataSource oldDataSource;

    @Autowired
    private ProductPriceMapper newPriceMapper;

    @Autowired
    private ProductPriceHistoryMapper historyMapper;

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

    private static final int BATCH_SIZE = 1000;
    private static final int THREAD_COUNT = 4;

    private final List<Long> failedIds = new ArrayList<>();

    @PostConstruct
    public void init() {
        oldJdbcTemplate = new JdbcTemplate(oldDataSource);
    }

    public void migrateAllData() {
        Long totalCountObj = oldJdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_price", Long.class);
        long totalCount = totalCountObj != null ? totalCountObj : 0;
        long migratedCount = 0;

        log.info("开始全量数据迁移，总数据量：{}", totalCount);

        int pageNum = 1;
        int pageSize = BATCH_SIZE;

        while (true) {
            List<ProductPrice> priceList = oldJdbcTemplate.query(
                "SELECT * FROM product_price ORDER BY id LIMIT ? OFFSET ?",
                PRICE_ROW_MAPPER, pageSize, (pageNum - 1) * pageSize);

            if (priceList.isEmpty()) {
                break;
            }

            for (ProductPrice price : priceList) {
                try {
                    newPriceMapper.insert(price);

                    ProductPriceHistory history = new ProductPriceHistory();
                    BeanUtils.copyProperties(price, history);
                    history.setPriceId(price.getId());
                    historyMapper.insert(history);

                    migratedCount++;

                } catch (Exception e) {
                    log.error("数据迁移失败，id: {}", price.getId(), e);
                    synchronized (failedIds) {
                        failedIds.add(price.getId());
                    }
                }
            }

            log.info("已迁移：{}/{}", migratedCount, totalCount);
            pageNum++;
        }

        log.info("全量数据迁移完成，共迁移：{} 条，失败：{} 条", migratedCount, failedIds.size());

        if (!failedIds.isEmpty()) {
            log.error("迁移失败的ID列表：{}", failedIds);
        }
    }

    public void migrateDataConcurrently() {
        Long totalCountObj = oldJdbcTemplate.queryForObject("SELECT COUNT(*) FROM product_price", Long.class);
        long totalCount = totalCountObj != null ? totalCountObj : 0;
        long pageSize = BATCH_SIZE;
        long totalPages = (totalCount + pageSize - 1) / pageSize;

        log.info("开始并发数据迁移，总数据量：{}，总页数：{}", totalCount, totalPages);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch((int) totalPages);

        for (long i = 0; i < totalPages; i++) {
            final long pageNum = i;
            executor.submit(() -> {
                try {
                    List<ProductPrice> priceList = oldJdbcTemplate.query(
                        "SELECT * FROM product_price ORDER BY id LIMIT ? OFFSET ?",
                        PRICE_ROW_MAPPER, (int) pageSize, (int) (pageNum * pageSize));

                    for (ProductPrice price : priceList) {
                        try {
                            newPriceMapper.insert(price);

                            ProductPriceHistory history = new ProductPriceHistory();
                            BeanUtils.copyProperties(price, history);
                            history.setPriceId(price.getId());
                            historyMapper.insert(history);

                        } catch (Exception e) {
                            log.error("数据迁移失败，id: {}", price.getId(), e);
                            synchronized (failedIds) {
                                failedIds.add(price.getId());
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            log.error("数据迁移被中断:", e);
        }

        executor.shutdown();

        log.info("并发数据迁移完成，失败：{} 条", failedIds.size());

        if (!failedIds.isEmpty()) {
            log.error("迁移失败的ID列表：{}", failedIds);
        }
    }

    public List<Long> getFailedIds() {
        return failedIds;
    }
}
