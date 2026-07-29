package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
    @Select("SELECT * FROM favorite WHERE price_alert = 1 AND id > #{afterId} ORDER BY id ASC LIMIT #{limit}")
    List<Favorite> findPriceAlertBatch(@Param("afterId") Long afterId, @Param("limit") int limit);

    @Select("SELECT * FROM favorite WHERE stock_alert = 1 AND id > #{afterId} ORDER BY id ASC LIMIT #{limit}")
    List<Favorite> findStockAlertBatch(@Param("afterId") Long afterId, @Param("limit") int limit);
}
