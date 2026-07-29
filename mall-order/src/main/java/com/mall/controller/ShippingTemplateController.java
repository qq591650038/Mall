package com.mall.controller;

import com.mall.common.result.Result;
import com.mall.entity.ShippingTemplate;
import com.mall.service.ShippingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController @RequestMapping("/api/shipping/templates")
public class ShippingTemplateController {
    private final ShippingService service; public ShippingTemplateController(ShippingService service) { this.service = service; }
    @GetMapping public Result<List<ShippingTemplate>> list() { return Result.success(service.available()); }
}
