package com.mall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.entity.PointsLedger;
import com.mall.entity.PointsProduct;
import com.mall.entity.PointsRedemption;
import com.mall.service.PointsService;
import com.mall.dto.points.PointsRedeemDTO;
import com.mall.vo.PointsSummaryVO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/points")
public class PointsController {
    private final PointsService pointsService;

    public PointsController(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    @GetMapping("/summary")
    public Result<PointsSummaryVO> summary(@AuthenticationPrincipal Long userId) {
        return Result.success(pointsService.getSummary(userId));
    }

    @GetMapping("/ledger")
    public Result<PageResult<PointsLedger>> ledger(@AuthenticationPrincipal Long userId,
                                                    @RequestParam(defaultValue = "1") Integer current,
                                                    @RequestParam(defaultValue = "20") Integer size) {
        Page<PointsLedger> page = pointsService.pageLedger(userId, current, size);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), current, size));
    }

    @PostMapping("/check-in")
    public Result<PointsSummaryVO> checkIn(@AuthenticationPrincipal Long userId) {
        return Result.success("签到成功", pointsService.checkIn(userId));
    }
    @GetMapping("/products")
    public Result<java.util.List<PointsProduct>> products() {
        return Result.success(pointsService.listProducts());
    }

    @PostMapping("/redeem/{productId}")
    public Result<PointsRedemption> redeem(@AuthenticationPrincipal Long userId,
                                           @PathVariable Long productId,
                                           @RequestBody(required = false) PointsRedeemDTO dto) {
        return Result.success("兑换成功", pointsService.redeem(userId, productId,
                dto == null ? new PointsRedeemDTO() : dto));
    }

    @GetMapping("/redemptions")
    public Result<PageResult<PointsRedemption>> redemptions(@AuthenticationPrincipal Long userId,
                                                             @RequestParam(defaultValue = "1") Integer current,
                                                             @RequestParam(defaultValue = "20") Integer size) {
        Page<PointsRedemption> page = pointsService.pageRedemptions(userId, current, size);
        return Result.success(new PageResult<>(page.getTotal(), page.getRecords(), current, size));
    }
}
