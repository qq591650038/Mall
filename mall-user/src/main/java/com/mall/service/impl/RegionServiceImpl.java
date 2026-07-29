package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.entity.Region;
import com.mall.mapper.RegionMapper;
import com.mall.service.RegionService;
import com.mall.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class RegionServiceImpl implements RegionService {

    private final RegionMapper regionMapper;
    private final RedisUtil redisUtil;

    private static final String PROVINCES_KEY = "region:provinces";
    private static final String CITIES_KEY_PREFIX = "region:cities:";
    private static final String DISTRICTS_KEY_PREFIX = "region:districts:";

    public RegionServiceImpl(RegionMapper regionMapper, RedisUtil redisUtil) {
        this.regionMapper = regionMapper;
        this.redisUtil = redisUtil;
    }

    @Override
    public List<Region> getProvinces() {
        Object cached = redisUtil.get(PROVINCES_KEY);
        if (cached instanceof List) {
            return (List<Region>) cached;
        }
        List<Region> result = regionMapper.selectList(
                new QueryWrapper<Region>().eq("level", 1).orderByAsc("sort", "id")
        );
        redisUtil.set(PROVINCES_KEY, result.isEmpty() ? "__EMPTY__" : result, 1, TimeUnit.HOURS);
        return result;
    }

    @Override
    public List<Region> getCitiesByProvince(Long provinceId) {
        String key = CITIES_KEY_PREFIX + provinceId;
        Object cached = redisUtil.get(key);
        if (cached instanceof List) {
            return (List<Region>) cached;
        }
        List<Region> result = regionMapper.selectList(
                new QueryWrapper<Region>().eq("parent_id", provinceId).eq("level", 2).orderByAsc("sort", "id")
        );
        redisUtil.set(key, result.isEmpty() ? "__EMPTY__" : result, 1, TimeUnit.HOURS);
        return result;
    }

    @Override
    public List<Region> getDistrictsByCity(Long cityId) {
        String key = DISTRICTS_KEY_PREFIX + cityId;
        Object cached = redisUtil.get(key);
        if (cached instanceof List) {
            return (List<Region>) cached;
        }
        List<Region> result = regionMapper.selectList(
                new QueryWrapper<Region>().eq("parent_id", cityId).eq("level", 3).orderByAsc("sort", "id")
        );
        redisUtil.set(key, result.isEmpty() ? "__EMPTY__" : result, 1, TimeUnit.HOURS);
        return result;
    }
}
