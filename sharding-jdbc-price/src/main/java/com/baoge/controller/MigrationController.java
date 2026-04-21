package com.baoge.controller;

import com.baoge.service.DataConsistencyChecker;
import com.baoge.service.ProductPriceMigrationJob;
import com.baoge.service.ProductPriceReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/migration")
public class MigrationController {
    
    @Autowired
    private ProductPriceReadService readService;
    
    @Autowired
    private ProductPriceMigrationJob migrationJob;
    
    @Autowired
    private DataConsistencyChecker consistencyChecker;
    
    @Value("${migration.read.source:old}")
    private String readSource;
    
    @PostMapping("/switch-read")
    public Map<String, Object> switchReadSource(@RequestParam String source) {
        Map<String, Object> result = new HashMap<>();
        
        if (!Arrays.asList("old", "new", "both").contains(source)) {
            result.put("success", false);
            result.put("message", "error: invalid source, must be old, new or both");
            return result;
        }
        
        readService.setReadSource(source);
        log.info("读流量切换：{}", source);
        
        result.put("success", true);
        result.put("message", "读流量切换成功");
        result.put("source", source);
        return result;
    }
    
    @GetMapping("/read-source")
    public Map<String, Object> getReadSource() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("source", readService.getReadSource());
        return result;
    }
    
    @PostMapping("/migrate-all")
    public Map<String, Object> migrateAllData() {
        Map<String, Object> result = new HashMap<>();
        try {
            migrationJob.migrateAllData();
            result.put("success", true);
            result.put("message", "全量数据迁移完成");
            result.put("failedCount", migrationJob.getFailedIds().size());
        } catch (Exception e) {
            log.error("数据迁移失败", e);
            result.put("success", false);
            result.put("message", "数据迁移失败：" + e.getMessage());
        }
        return result;
    }
    
    @PostMapping("/migrate-concurrent")
    public Map<String, Object> migrateDataConcurrently() {
        Map<String, Object> result = new HashMap<>();
        try {
            migrationJob.migrateDataConcurrently();
            result.put("success", true);
            result.put("message", "并发数据迁移完成");
            result.put("failedCount", migrationJob.getFailedIds().size());
        } catch (Exception e) {
            log.error("并发数据迁移失败", e);
            result.put("success", false);
            result.put("message", "并发数据迁移失败：" + e.getMessage());
        }
        return result;
    }
    
    @PostMapping("/check-all")
    public Map<String, Object> checkAllData() {
        Map<String, Object> result = new HashMap<>();
        try {
            consistencyChecker.checkAllData();
            result.put("success", true);
            result.put("message", "全量数据校验完成");
        } catch (Exception e) {
            log.error("数据校验失败", e);
            result.put("success", false);
            result.put("message", "数据校验失败：" + e.getMessage());
        }
        return result;
    }
    
    @PostMapping("/check-random")
    public Map<String, Object> randomCheck() {
        Map<String, Object> result = new HashMap<>();
        try {
            consistencyChecker.randomCheck();
            result.put("success", true);
            result.put("message", "抽样校验完成");
        } catch (Exception e) {
            log.error("抽样校验失败", e);
            result.put("success", false);
            result.put("message", "抽样校验失败：" + e.getMessage());
        }
        return result;
    }
}