package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.result.ErrorCode;
import com.mall.entity.Address;
import com.mall.entity.ShippingTemplate;
import com.mall.exception.BusinessException;
import com.mall.mapper.ShippingTemplateMapper;
import com.mall.service.ShippingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShippingServiceImpl implements ShippingService {
    private final ShippingTemplateMapper mapper;
    public ShippingServiceImpl(ShippingTemplateMapper mapper) { this.mapper = mapper; }

    @Override public BigDecimal calculate(Long templateId, String deliveryMethod, Address address, BigDecimal goodsAmount) {
        if (templateId == null) return BigDecimal.ZERO;
        ShippingTemplate template = mapper.selectById(templateId);
        if (template == null || !Integer.valueOf(1).equals(template.getStatus())) throw new BusinessException(ErrorCode.BAD_REQUEST, "配送模板不可用");
        if (deliveryMethod != null && !deliveryMethod.equals(template.getDeliveryMethod())) throw new BusinessException(ErrorCode.BAD_REQUEST, "配送方式与模板不匹配");
        String regions = template.getRegions();
        if (regions != null && !regions.isBlank() && address != null && !regions.contains(address.getProvince())) throw new BusinessException(ErrorCode.BAD_REQUEST, "当前地址不支持该配送方式");
        if (template.getFreeAmount() != null && goodsAmount.compareTo(template.getFreeAmount()) >= 0) return BigDecimal.ZERO;
        return template.getBaseFreight() == null ? BigDecimal.ZERO : template.getBaseFreight();
    }
    @Override public List<ShippingTemplate> available() { return mapper.selectList(new LambdaQueryWrapper<ShippingTemplate>().eq(ShippingTemplate::getStatus, 1).orderByAsc(ShippingTemplate::getId)); }
    @Override @Transactional public void save(ShippingTemplate template) { if (template.getName() == null || template.getName().isBlank() || template.getDeliveryMethod() == null || template.getBaseFreight() == null || template.getBaseFreight().signum() < 0) throw new BusinessException(ErrorCode.BAD_REQUEST, "配送模板参数错误"); if (template.getStatus() == null) template.setStatus(1); template.setUpdateTime(LocalDateTime.now()); if (template.getId() == null) { template.setCreateTime(LocalDateTime.now()); mapper.insert(template); } else mapper.updateById(template); }
    @Override @Transactional public void remove(Long id) { mapper.deleteById(id); }
}
