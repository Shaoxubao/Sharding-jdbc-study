package com.baoge.mapper;
import com.baoge.entity.Order;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface UserOrderMapper {
    /**
     * @description 保存订单
     */
    @Insert("insert into ksd_user_order(order_number,user_id,create_time,yearmonth) values(#{orderNumber},#{userId},#{createTime},#{yearmonth})")
    @Options(useGeneratedKeys = true, keyColumn = "order_id",keyProperty = "orderId")
    void addUserOrder(Order order);
}