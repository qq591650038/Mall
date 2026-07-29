package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.PointsProduct;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PointsProductMapper extends BaseMapper<PointsProduct> {
    @Update("UPDATE points_product SET stock = stock - 1, update_time = NOW() WHERE id = #{id} AND status = 1 AND stock > 0")
    int decrementStock(Long id);
}
