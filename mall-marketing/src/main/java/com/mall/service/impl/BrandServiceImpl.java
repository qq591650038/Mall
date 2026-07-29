package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.ErrorCode;
import com.mall.entity.Brand;
import com.mall.exception.BusinessException;
import com.mall.mapper.BrandMapper;
import com.mall.service.BrandService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class BrandServiceImpl implements BrandService {

    private final BrandMapper brandMapper;

    public BrandServiceImpl(BrandMapper brandMapper) {
        this.brandMapper = brandMapper;
    }

    @Override
    public Page<Brand> page(Integer current, Integer size, String keyword) {
        Page<Brand> page = new Page<>(current, size);
        QueryWrapper<Brand> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like("name", keyword);
        }
        wrapper.orderByDesc("id");
        return brandMapper.selectPage(page, wrapper);
    }

    @Override
    public List<Brand> listEnabled() {
        return brandMapper.selectList(new QueryWrapper<Brand>().eq("status", 1).orderByAsc("sort").orderByDesc("id"));
    }

    @Override
    public Brand getById(Long id) {
        Brand brand = brandMapper.selectById(id);
        if (brand == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "品牌不存在");
        }
        return brand;
    }

    @Override
    public void save(Brand brand) {
        brand.setCreateTime(LocalDateTime.now());
        brand.setUpdateTime(LocalDateTime.now());
        brand.setDeleted(0);
        brandMapper.insert(brand);
        log.info("新增品牌: {}", brand.getName());
    }

    @Override
    public void update(Brand brand) {
        Brand exist = brandMapper.selectById(brand.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "品牌不存在");
        }
        brand.setUpdateTime(LocalDateTime.now());
        brandMapper.updateById(brand);
        log.info("更新品牌: id={}", brand.getId());
    }

    @Override
    public void deleteById(Long id) {
        Brand exist = brandMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "品牌不存在");
        }
        brandMapper.deleteById(id);
        log.info("删除品牌: id={}", id);
    }
}
