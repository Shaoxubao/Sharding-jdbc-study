package com.baoge.mapper;

import com.baoge.entity.ProductPriceHistory;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface ProductPriceHistoryMapper {
    
    @Insert("INSERT INTO product_price_history (price_id, product_id, sku_id, price, original_price, cost_price, start_time, end_time, status, create_time, update_time) " +
            "VALUES (#{priceId}, #{productId}, #{skuId}, #{price}, #{originalPrice}, #{costPrice}, #{startTime}, #{endTime}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int insert(ProductPriceHistory history);
    
    @Select("SELECT * FROM product_price_history WHERE product_id = #{productId} ORDER BY create_time DESC LIMIT #{limit}")
    java.util.List<ProductPriceHistory> selectByProductId(Long productId, int limit);
}