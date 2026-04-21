package com.baoge.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ProductPriceHistory {
    private Long id;
    private Long priceId;
    private Long productId;
    private Long skuId;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private BigDecimal costPrice;
    private Date startTime;
    private Date endTime;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}