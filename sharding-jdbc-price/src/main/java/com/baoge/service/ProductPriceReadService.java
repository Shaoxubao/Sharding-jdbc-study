package com.baoge.service;

import com.baoge.entity.ProductPrice;
import com.baoge.entity.ProductPriceHistory;
import com.baoge.mapper.ProductPriceHistoryMapper;
import com.baoge.mapper.ProductPriceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Random;

@Slf4j
@Service
public class ProductPriceReadService {
    
    @Value("${migration.read.source:old}")
    private String readSource;
    
    @Autowired
    private ProductPriceMapper oldPriceMapper;
    
    @Autowired
    private ProductPriceMapper newPriceMapper;
    
    public ProductPrice selectByProductId(Long productId) {
        if ("old".equals(readSource)) {
            return oldPriceMapper.selectByProductId(productId);
        } else if ("new".equals(readSource)) {
            return newPriceMapper.selectByProductId(productId);
        } else {
            ProductPrice oldPrice = oldPriceMapper.selectByProductId(productId);
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
            return oldPriceMapper.selectById(id);
        } else if ("new".equals(readSource)) {
            return newPriceMapper.selectById(id);
        } else {
            ProductPrice oldPrice = oldPriceMapper.selectById(id);
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