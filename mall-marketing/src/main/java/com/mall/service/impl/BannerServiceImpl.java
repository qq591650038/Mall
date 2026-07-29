package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.common.result.ErrorCode;
import com.mall.entity.Banner;
import com.mall.exception.BusinessException;
import com.mall.mapper.BannerMapper;
import com.mall.service.BannerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.mall.utils.RedisUtil;
import java.util.concurrent.TimeUnit;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class BannerServiceImpl implements BannerService {

    private final BannerMapper bannerMapper;
    private final RedisUtil redisUtil;

    public BannerServiceImpl(BannerMapper bannerMapper, RedisUtil redisUtil) {
        this.bannerMapper = bannerMapper; this.redisUtil = redisUtil;
    }

    @Override
    public List<Banner> listActive() {
        Object cached = redisUtil.get("banner:active"); if ("__EMPTY__".equals(cached)) return List.of(); if (cached instanceof List) return (List<Banner>) cached;
        List<Banner> result = bannerMapper.selectList(
                new QueryWrapper<Banner>()
                        .eq("status", 1)
                        .le("start_time", LocalDateTime.now())
                        .ge("end_time", LocalDateTime.now())
                        .orderByAsc("sort")
        ); redisUtil.set("banner:active", result.isEmpty() ? "__EMPTY__" : result, 5, TimeUnit.MINUTES); return result;
    }

    @Override
    public List<Banner> listAll() {
        return bannerMapper.selectList(
                new QueryWrapper<Banner>().orderByAsc("sort", "id")
        );
    }

    @Override
    public Banner getById(Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "轮播图不存在");
        }
        return banner;
    }

    @Override
    public void save(Banner banner) {
        banner.setCreateTime(LocalDateTime.now());
        banner.setUpdateTime(LocalDateTime.now());
        banner.setDeleted(0);
        bannerMapper.insert(banner);
        redisUtil.delete("banner:active");
        log.info("新增轮播图: {}", banner.getTitle());
    }

    @Override
    public void update(Banner banner) {
        Banner exist = bannerMapper.selectById(banner.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "轮播图不存在");
        }
        banner.setUpdateTime(LocalDateTime.now());
        bannerMapper.updateById(banner);
        redisUtil.delete("banner:active");
        log.info("更新轮播图: id={}", banner.getId());
    }

    @Override
    public void deleteById(Long id) {
        Banner exist = bannerMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "轮播图不存在");
        }
        bannerMapper.deleteById(id);
        redisUtil.delete("banner:active");
        log.info("删除轮播图: id={}", id);
    }
}
