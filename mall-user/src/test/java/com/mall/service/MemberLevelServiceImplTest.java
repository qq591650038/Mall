package com.mall.service;

import com.mall.entity.MemberLevel;
import com.mall.mapper.MemberLevelMapper;
import com.mall.mapper.PointsAccountMapper;
import com.mall.service.impl.MemberLevelServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberLevelServiceImplTest {
    private final MemberLevelMapper levelMapper = mock(MemberLevelMapper.class);
    private final MemberLevelServiceImpl service = new MemberLevelServiceImpl(levelMapper, mock(PointsAccountMapper.class));

    @Test
    void zeroMaximumMeansNoUpperLimit() {
        MemberLevel base = level(1L, 1, 0, 999, "1.00");
        MemberLevel top = level(4L, 4, 20000, 0, "2.00");
        when(levelMapper.selectList(any())).thenReturn(List.of(base, top));

        assertEquals(4L, service.resolveLevelByPoints(25000).getId());
    }

    private MemberLevel level(Long id, int number, int min, int max, String rate) {
        MemberLevel level = new MemberLevel();
        level.setId(id);
        level.setName("L" + number);
        level.setLevel(number);
        level.setMinPoints(min);
        level.setMaxPoints(max);
        level.setPointsRate(new BigDecimal(rate));
        level.setStatus(1);
        return level;
    }
}
