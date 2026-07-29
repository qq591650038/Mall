package com.mall.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.PageResult;
import com.mall.common.result.Result;
import com.mall.entity.OperationLog;
import com.mall.mapper.OperationLogMapper;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/operation-logs")
public class AdminOperationLogController {
    private final OperationLogMapper mapper;

    public AdminOperationLogController(OperationLogMapper m) {
        mapper = m;
    }

    @GetMapping("/page")
    public Result<PageResult<OperationLog>> page(@RequestParam(defaultValue = "1") Integer current, @RequestParam(defaultValue = "20") Integer size, @RequestParam(required = false) Integer status, @RequestParam(required = false) String eventType, @RequestParam(required = false) Long userId) {
        Page<OperationLog> p = new Page<>(current, size);
        QueryWrapper<OperationLog> w = new QueryWrapper<>();
        if (status != null) w.eq("status", status);
        if (eventType != null && !eventType.isBlank()) w.eq("event_type", eventType);
        if (userId != null) w.eq("user_id", userId);
        w.orderByDesc("create_time");
        p = mapper.selectPage(p, w);
        return Result.success(new PageResult<>(p.getTotal(), p.getRecords(), current, size));
    }
}
