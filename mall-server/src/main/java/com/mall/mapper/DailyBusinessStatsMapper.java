package com.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.DailyBusinessStats;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DailyBusinessStatsMapper extends BaseMapper<DailyBusinessStats> {
    DailyBusinessStats selectDailySource(LocalDate statDate);

    List<DailyBusinessStats> selectBetween(LocalDate startDate, LocalDate endDate);

    DailyBusinessStats selectTotals();

    int replace(DailyBusinessStats stats);
}
