package com.baoge.controller;

import com.baoge.entity.ProductPrice;
import com.baoge.entity.ProductPriceHistory;
import com.baoge.service.DataConsistencyChecker;
import com.baoge.service.ProductPriceMigrationJob;
import com.baoge.service.ProductPriceReadService;
import com.baoge.service.ProductPriceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/price")
public class ProductPriceController {
    
    @Autowired
    private ProductPriceService priceService;
    
    @Autowired
    private ProductPriceReadService readService;
    
    @Autowired
    private ProductPriceMigrationJob migrationJob;
    
    @Autowired
    private DataConsistencyChecker consistencyChecker;
    
    @Value("${migration.read.source:old}")
    private String readSource;
    
    @PostMapping("/add")
    public Map<String, Object> addPrice(@RequestBody ProductPrice productPrice) {
        Map<String, Object> result = new HashMap<>();
        try {
            priceService.addPrice(productPrice);
            result.put("success", true);
            result.put("message", "添加价格成功");
            result.put("data", productPrice);
        } catch (Exception e) {
            log.error("添加价格失败", e);
            result.put("success", false);
            result.put("message", "添加价格失败：" + e.getMessage());
        }
        return result;
    }
    
    @PostMapping("/update")
    public Map<String, Object> updatePrice(@RequestBody ProductPrice productPrice) {
        Map<String, Object> result = new HashMap<>();
        try {
            priceService.updatePrice(productPrice);
            result.put("success", true);
            result.put("message", "更新价格成功");
        } catch (Exception e) {
            log.error("更新价格失败", e);
            result.put("success", false);
            result.put("message", "更新价格失败：" + e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/current/{productId}")
    public Map<String, Object> getCurrentPrice(@PathVariable Long productId) {
        Map<String, Object> result = new HashMap<>();
        try {
            ProductPrice price = priceService.getCurrentPrice(productId);
            result.put("success", true);
            result.put("data", price);
        } catch (Exception e) {
            log.error("查询价格失败", e);
            result.put("success", false);
            result.put("message", "查询价格失败：" + e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/sku/{skuId}")
    public Map<String, Object> getSkuPrice(@PathVariable Long skuId) {
        Map<String, Object> result = new HashMap<>();
        try {
            ProductPrice price = priceService.getSkuPrice(skuId);
            result.put("success", true);
            result.put("data", price);
        } catch (Exception e) {
            log.error("查询SKU价格失败", e);
            result.put("success", false);
            result.put("message", "查询SKU价格失败：" + e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/range")
    public Map<String, Object> queryByPriceRange(@RequestParam Long productId,
                                                   @RequestParam BigDecimal minPrice,
                                                   @RequestParam BigDecimal maxPrice) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ProductPrice> prices = priceService.queryByPriceRange(productId, minPrice, maxPrice);
            result.put("success", true);
            result.put("data", prices);
        } catch (Exception e) {
            log.error("按价格范围查询失败", e);
            result.put("success", false);
            result.put("message", "按价格范围查询失败：" + e.getMessage());
        }
        return result;
    }
    
    @GetMapping("/history/{productId}")
    public Map<String, Object> queryHistoryPrice(@PathVariable Long productId,
                                                   @RequestParam(defaultValue = "10") int limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<ProductPriceHistory> prices = priceService.queryHistoryPrice(productId, limit);
            result.put("success", true);
            result.put("data", prices);
        } catch (Exception e) {
            log.error("查询历史价格失败", e);
            result.put("success", false);
            result.put("message", "查询历史价格失败：" + e.getMessage());
        }
        return result;
    }
}