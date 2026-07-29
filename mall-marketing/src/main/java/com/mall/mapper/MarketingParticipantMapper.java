package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.MarketingParticipant;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 营销活动参与者 Mapper
 */
@Mapper
public interface MarketingParticipantMapper extends BaseMapper<MarketingParticipant> {
    @Update("<script>UPDATE marketing_participant SET group_status = 3, update_time = #{now} "
            + "WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "AND group_status IN (1, 3)</script>")
    int markGroupsFailed(@Param("ids") List<Long> ids, @Param("now") LocalDateTime now);
}
