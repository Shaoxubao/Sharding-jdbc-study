package com.baoge.service;

import com.baoge.entity.ProductPrice;
import com.baoge.entity.ProductPriceHistory;
import com.baoge.mapper.ProductPriceHistoryMapper;
import com.baoge.mapper.ProductPriceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ProductPriceService {
    
    @Autowired
    private ProductPriceMapper productPriceMapper;
    
    @Autowired
    private ProductPriceHistoryMapper historyMapper;
    
    @Transactional
    public void addPrice(ProductPrice productPrice) {
        productPrice.setCreateTime(new Date());
        productPrice.setUpdateTime(new Date());
        productPrice.setStatus(1);
        
        productPriceMapper.insert(productPrice);
        
        ProductPriceHistory history = new ProductPriceHistory();
        BeanUtils.copyProperties(productPrice, history);
        history.setPriceId(productPrice.getId());
        historyMapper.insert(history);
        
        log.info("添加价格成功，productId: {}, price: {}", productPrice.getProductId(), productPrice.getPrice());
    }
    
    @Transactional
    public void updatePrice(ProductPrice productPrice) {
        ProductPrice oldPrice = productPriceMapper.selectById(productPrice.getId());
        
        productPrice.setUpdateTime(new Date());
        productPriceMapper.update(productPrice);
        
        ProductPriceHistory history = new ProductPriceHistory();
        BeanUtils.copyProperties(oldPrice, history);
        history.setPriceId(oldPrice.getId());
        historyMapper.insert(history);
        
        log.info("更新价格成功，id: {}, newPrice: {}", productPrice.getId(), productPrice.getPrice());
    }
    
    public ProductPrice getCurrentPrice(Long productId) {
        return productPriceMapper.selectByProductId(productId);
    }
    
    public ProductPrice getSkuPrice(Long skuId) {
        return productPriceMapper.selectBySkuId(skuId);
    }
    
    public List<ProductPrice> queryByPriceRange(Long productId, BigDecimal minPrice, BigDecimal maxPrice) {
        return productPriceMapper.selectByPriceRange(productId, minPrice, maxPrice);
    }
    
    public List<ProductPrice> queryHistoryPrice(Long productId, int limit) {
        return productPriceMapper.selectHistoryByProductId(productId, limit);
    }
    
    public ProductPrice getPriceById(Long id) {
        return productPriceMapper.selectById(id);
    }
}