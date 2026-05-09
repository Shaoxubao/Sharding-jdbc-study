package com.baoge.service;

import com.baoge.entity.ProductPrice;
import com.baoge.mapper.ProductPriceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ProductPriceReadService {

    @Value("${migration.read.source:old}")
    private String readSource;

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

    public ProductPrice selectByProductId(Long productId) {
        if ("old".equals(readSource)) {
            List<ProductPrice> list = oldJdbcTemplate.query(
                "SELECT * FROM product_price WHERE product_id = ? AND status = 1 ORDER BY create_time DESC LIMIT 1",
                PRICE_ROW_MAPPER, productId);
            return list.isEmpty() ? null : list.get(0);
        } else if ("new".equals(readSource)) {
            return newPriceMapper.selectByProductId(productId);
        } else {
            List<ProductPrice> oldList = oldJdbcTemplate.query(
                "SELECT * FROM product_price WHERE product_id = ? AND status = 1 ORDER BY create_time DESC LIMIT 1",
                PRICE_ROW_MAPPER, productId);
            ProductPrice oldPrice = oldList.isEmpty() ? null : oldList.get(0);
            ProductPrice newPrice = newPriceMapper.selectByProductId(productId);

            if (!isDataEqual(oldPrice, newPrice)) {
                log.error("数据不一致！productId: {}, oldPrice: {}, newPrice: {}",
                         productId, oldPrice, newPrice);
            }

            return newPrice != null ? newPrice : oldPrice;
        }
    }

    public ProductPrice selectById(Long id) {
        if ("old".equals(readSource)) {
            List<ProductPrice> list = oldJdbcTemplate.query(
                "SELECT * FROM product_price WHERE id = ?",
                PRICE_ROW_MAPPER, id);
            return list.isEmpty() ? null : list.get(0);
        } else if ("new".equals(readSource)) {
            return newPriceMapper.selectById(id);
        } else {
            List<ProductPrice> oldList = oldJdbcTemplate.query(
                "SELECT * FROM product_price WHERE id = ?",
                PRICE_ROW_MAPPER, id);
            ProductPrice oldPrice = oldList.isEmpty() ? null : oldList.get(0);
            ProductPrice newPrice = newPriceMapper.selectById(id);

            if (!isDataEqual(oldPrice, newPrice)) {
                log.error("数据不一致！id: {}, oldPrice: {}, newPrice: {}",
                         id, oldPrice, newPrice);
            }

            return newPrice != null ? newPrice : oldPrice;
        }
    }

    private boolean isDataEqual(ProductPrice oldPrice, ProductPrice newPrice) {
        if (oldPrice == null && newPrice == null) {
            return true;
        }
        if (oldPrice == null || newPrice == null) {
            return false;
        }
        return Objects.equals(oldPrice.getProductId(), newPrice.getProductId()) &&
               Objects.equals(oldPrice.getSkuId(), newPrice.getSkuId()) &&
               Objects.equals(oldPrice.getPrice(), newPrice.getPrice()) &&
               Objects.equals(oldPrice.getOriginalPrice(), newPrice.getOriginalPrice()) &&
               Objects.equals(oldPrice.getCostPrice(), newPrice.getCostPrice()) &&
               Objects.equals(oldPrice.getStatus(), newPrice.getStatus());
    }

    public String getReadSource() {
        return readSource;
    }

    public void setReadSource(String readSource) {
        this.readSource = readSource;
    }
}
