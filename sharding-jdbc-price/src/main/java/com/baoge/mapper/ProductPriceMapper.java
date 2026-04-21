package com.baoge.mapper;

import com.baoge.entity.ProductPrice;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Mapper
@Repository
public interface ProductPriceMapper {
    
    @Insert("INSERT INTO product_price (product_id, sku_id, price, original_price, cost_price, start_time, end_time, status, create_time, update_time) " +
            "VALUES (#{productId}, #{skuId}, #{price}, #{originalPrice}, #{costPrice}, #{startTime}, #{endTime}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyColumn = "id", keyProperty = "id")
    int insert(ProductPrice productPrice);
    
    @Update("UPDATE product_price SET price = #{price}, original_price = #{originalPrice}, " +
            "cost_price = #{costPrice}, start_time = #{startTime}, end_time = #{endTime}, " +
            "status = #{status}, update_time = #{updateTime} WHERE id = #{id}")
    int update(ProductPrice productPrice);
    
    @Select("SELECT * FROM product_price WHERE id = #{id}")
    ProductPrice selectById(Long id);
    
    @Select("SELECT * FROM product_price WHERE product_id = #{productId} AND status = 1 ORDER BY create_time DESC LIMIT 1")
    ProductPrice selectByProductId(Long productId);
    
    @Select("SELECT * FROM product_price WHERE sku_id = #{skuId} AND status = 1 ORDER BY create_time DESC LIMIT 1")
    ProductPrice selectBySkuId(Long skuId);
    
    @Select("SELECT * FROM product_price WHERE product_id = #{productId} AND price BETWEEN #{minPrice} AND #{maxPrice} AND status = 1")
    List<ProductPrice> selectByPriceRange(@Param("productId") Long productId, 
                                          @Param("minPrice") BigDecimal minPrice, 
                                          @Param("maxPrice") BigDecimal maxPrice);
    
    @Select("SELECT * FROM product_price WHERE product_id = #{productId} ORDER BY create_time DESC LIMIT #{limit}")
    List<ProductPrice> selectHistoryByProductId(@Param("productId") Long productId, @Param("limit") int limit);
    
    @Select("SELECT COUNT(*) FROM product_price")
    long count();
    
    @Select("SELECT * FROM product_price ORDER BY id LIMIT #{offset}, #{pageSize}")
    List<ProductPrice> selectByPage(@Param("offset") int offset, @Param("pageSize") int pageSize);
    
    @Delete("DELETE FROM product_price WHERE id = #{id}")
    int deleteById(Long id);
}