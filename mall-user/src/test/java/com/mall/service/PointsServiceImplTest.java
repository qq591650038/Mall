package com.mall.service;

import com.mall.entity.MemberLevel;
import com.mall.entity.PointsAccount;
import com.mall.entity.PointsLedger;
import com.mall.mapper.*;
import com.mall.service.impl.PointsServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PointsServiceImplTest {
    @Test
    void paymentPointsKeepFractionalMultiplier() {
        PointsAccountMapper accountMapper = mock(PointsAccountMapper.class);
        PointsLedgerMapper ledgerMapper = mock(PointsLedgerMapper.class);
        MemberLevelMapper levelMapper = mock(MemberLevelMapper.class);
        MemberLevelService levelService = mock(MemberLevelService.class);
        PointsAccount account = new PointsAccount();
        account.setId(1L);
        account.setUserId(7L);
        account.setMemberLevelId(2L);
        account.setBalance(0);
        account.setTotalEarned(0);
        account.setTotalSpent(0);
        MemberLevel level = new MemberLevel();
        level.setId(2L);
        level.setStatus(1);
        level.setPointsRate(new BigDecimal("1.50"));
        when(accountMapper.selectOne(any())).thenReturn(account);
        when(ledgerMapper.selectCount(any())).thenReturn(0L);
        when(levelMapper.selectById(2L)).thenReturn(level);
        when(levelService.resolveLevelByPoints(anyInt())).thenReturn(level);
        PointsServiceImpl service = new PointsServiceImpl(accountMapper, ledgerMapper,
                mock(PointsCheckinMapper.class), mock(PointsProductMapper.class),
                mock(PointsRedemptionMapper.class), levelService, levelMapper, mock(UserMapper.class),
                mock(PointsRewardService.class));

        service.earnForPayment(7L, new BigDecimal("12.50"), 9L, "O9");

        ArgumentCaptor<PointsLedger> captor = ArgumentCaptor.forClass(PointsLedger.class);
        verify(ledgerMapper).insert(captor.capture());
        assertEquals(18, captor.getValue().getAmount());
        assertEquals(18, account.getBalance());
    }
}
