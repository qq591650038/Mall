package com.mall.controller.admin;

import com.mall.common.result.Result;
import com.mall.entity.ShippingTemplate;
import com.mall.service.ShippingService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/admin/shipping/templates")
public class AdminShippingTemplateController {
    private final ShippingService service; public AdminShippingTemplateController(ShippingService service) { this.service = service; }
    @GetMapping public Result<List<ShippingTemplate>> list() { return Result.success(service.available()); }
    @PostMapping public Result<Void> save(@RequestBody ShippingTemplate template) { service.save(template); return Result.success("保存成功", null); }
    @PutMapping public Result<Void> update(@RequestBody ShippingTemplate template) { service.save(template); return Result.success("保存成功", null); }
    @DeleteMapping("/{id}") public Result<Void> remove(@PathVariable Long id) { service.remove(id); return Result.success("删除成功", null); }
}
