package com.mall.event;

import com.mall.service.SeckillAsyncService;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.stereotype.Component;

@Component
public class SeckillStockRestoredListener {
    private final SeckillAsyncService seckillAsyncService;

    public SeckillStockRestoredListener(SeckillAsyncService seckillAsyncService) {
        this.seckillAsyncService = seckillAsyncService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockRestored(SeckillStockRestoredEvent event) {
        seckillAsyncService.restoreActivityStock(event.itemId(), event.userId(), event.quantity());
    }
}
