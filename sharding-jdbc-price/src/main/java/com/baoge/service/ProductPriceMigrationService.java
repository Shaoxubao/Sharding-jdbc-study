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
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ProductPriceMigrationService {
    
    @Autowired
    private ProductPriceMapper oldPriceMapper;
    
    @Autowired
    private ProductPriceMapper newPriceMapper;
    
    @Autowired
    private ProductPriceHistoryMapper historyMapper;
    
    @Value("${migration.dual-write.enabled:false}")
    private boolean dualWriteEnabled;
    
    @Value("${migration.dual-write.old-table-write:true}")
    private boolean oldTableWriteEnabled;
    
    @Value("${migration.dual-write.new-table-write:true}")
    private boolean newTableWriteEnabled;
    
    @Transactional
    public void insertWithDualWrite(ProductPrice productPrice) {
        productPrice.setCreateTime(new Date());
        productPrice.setUpdateTime(new Date());
        productPrice.setStatus(1);
        
        if (dualWriteEnabled && oldTableWriteEnabled) {
            oldPriceMapper.insert(productPrice);
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
        if (dualWriteEnabled && oldTableWriteEnabled) {
            oldPriceMapper.update(productPrice);
        }
        
        if (dualWriteEnabled && newTableWriteEnabled) {
            newPriceMapper.update(productPrice);
        }
        
        ProductPriceHistory history = new ProductPriceHistory();
        BeanUtils.copyProperties(productPrice, history);
        history.setPriceId(productPrice.getId());
        historyMapper.insert(history);
    }
}