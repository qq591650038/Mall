package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.mall.vo.ProductReviewSummaryVO;
import java.util.Collection;
import java.util.List;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {
    List<ProductReviewSummaryVO> selectProductSummaries(@Param("productIds") Collection<Long> productIds);
}
