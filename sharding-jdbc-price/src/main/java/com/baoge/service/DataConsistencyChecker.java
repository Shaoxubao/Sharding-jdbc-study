package com.baoge.service;

import com.baoge.entity.ProductPrice;
import com.baoge.mapper.ProductPriceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
public class DataConsistencyChecker {
    
    @Autowired
    private ProductPriceMapper oldPriceMapper;
    
    @Autowired
    private ProductPriceMapper newPriceMapper;
    
    public void checkAllData() {
        long totalCount = oldPriceMapper.count();
        long checkedCount = 0;
        long errorCount = 0;
        
        int pageNum = 1;
        int pageSize = 1000;
        
        log.info("开始全量数据校验，总数据量：{}", totalCount);
        
        while (true) {
            List<ProductPrice> oldPriceList = oldPriceMapper.selectByPage((pageNum - 1) * pageSize, pageSize);
            
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
        long totalCount = oldPriceMapper.count();
        java.util.Random random = new java.util.Random();
        
        log.info("开始抽样校验，抽样数量：{}", sampleSize);
        
        int errorCount = 0;
        
        for (int i = 0; i < sampleSize; i++) {
            Long id = (long) random.nextInt((int) totalCount);
            
            ProductPrice oldPrice = oldPriceMapper.selectById(id);
            ProductPrice newPrice = newPriceMapper.selectById(id);
            
            if (oldPrice != null && newPrice != null) {
                if (!isDataEqual(oldPrice, newPrice)) {
                    log.error("抽样校验失败！id: {}", id);
                    errorCount++;
                }
            }
        }
        
        log.info("抽样校验完成，错误数：{}", errorCount);
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