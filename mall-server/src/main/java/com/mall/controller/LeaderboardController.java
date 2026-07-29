package com.mall.controller;

import com.mall.common.result.Result;
import com.mall.service.LeaderboardService;
import com.mall.vo.LeaderboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboard")
@Tag(name = "排行榜", description = "用户积分、用户消费和商品销量排行榜")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    @Operation(summary = "获取排行榜")
    public Result<LeaderboardVO> getLeaderboard() {
        return Result.success(leaderboardService.getLeaderboard());
    }
}
