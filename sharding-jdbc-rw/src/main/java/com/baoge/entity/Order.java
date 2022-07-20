package com.baoge.entity;
import lombok.Data;
import java.util.Date;

@Data
public class Order {
    // 主键
    private Long orderId;
    // 订单编号
    private String orderNumber;
    // 用户ID
    private Long userId;
    // 年月
    private String yearmonth;
    // 创建时间
    private Date createTime;
}