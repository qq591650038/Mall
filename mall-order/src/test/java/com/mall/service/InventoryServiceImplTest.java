package com.mall.service;

import com.mall.exception.BusinessException;
import com.mall.mapper.InventoryLogMapper;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.ProductSkuMapper;
import com.mall.service.impl.InventoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InventoryServiceImplTest {
    private final ProductMapper productMapper = mock(ProductMapper.class);
    private final ProductSkuMapper skuMapper = mock(ProductSkuMapper.class);
    private final InventoryLogMapper logMapper = mock(InventoryLogMapper.class);
    private final InventoryServiceImpl service = new InventoryServiceImpl(productMapper, skuMapper, logMapper);

    @Test
    void reserveRejectsInsufficientStock() {
        when(skuMapper.decrementStock(1L, 2)).thenReturn(0);
        assertThrows(BusinessException.class, () -> service.reserve(1L, 1L, 2));
        verify(productMapper, never()).decrementTotalStock(any(), any());
    }

    @Test
    void reserveAndReleaseWriteInventoryLog() {
        when(skuMapper.decrementStock(1L, 2)).thenReturn(1);
        when(productMapper.decrementTotalStock(1L, 2)).thenReturn(1);
        when(skuMapper.incrementStock(1L, 2)).thenReturn(1);
        when(productMapper.incrementTotalStock(1L, 2)).thenReturn(1);
        service.reserve(1L, 1L, 2);
        service.release(1L, 1L, 2);
        verify(logMapper, times(2)).insert(any());
        verify(skuMapper).incrementStock(1L, 2);
        verify(productMapper).incrementTotalStock(1L, 2);
    }

    @Test
    void releaseRollsBackSkuWhenProductRestoreFails() {
        when(skuMapper.incrementStock(1L, 2)).thenReturn(1);
        when(productMapper.incrementTotalStock(1L, 2)).thenReturn(0);
        assertThrows(RuntimeException.class, () -> service.release(1L, 1L, 2));
        verify(skuMapper).decrementStock(1L, 2);
        verify(logMapper, atLeastOnce()).updateById(any());
    }
}
