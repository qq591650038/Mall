package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.common.result.ErrorCode;
import com.mall.entity.Category;
import com.mall.exception.BusinessException;
import com.mall.mapper.CategoryMapper;
import com.mall.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.mall.utils.RedisUtil;
import java.util.concurrent.TimeUnit;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final RedisUtil redisUtil;

    public CategoryServiceImpl(CategoryMapper categoryMapper, RedisUtil redisUtil) {
        this.categoryMapper = categoryMapper; this.redisUtil = redisUtil;
    }

    @Override
    public List<Category> listAll() {
        Object cached = redisUtil.get("category:all"); if ("__EMPTY__".equals(cached)) return List.of(); if (cached instanceof List) return (List<Category>) cached;
        List<Category> result = categoryMapper.selectList(
                new QueryWrapper<Category>().orderByAsc("sort", "id")
        );
        redisUtil.set("category:all", result.isEmpty() ? "__EMPTY__" : result, 10, TimeUnit.MINUTES); return result;
    }

    @Override
    public List<Category> listByParentId(Long parentId) {
        return categoryMapper.selectList(
                new QueryWrapper<Category>().eq("parent_id", parentId).orderByAsc("sort", "id")
        );
    }

    @Override
    public Category getById(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        return category;
    }

    @Override
    public void save(Category category) {
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        category.setDeleted(0);
        categoryMapper.insert(category);
        redisUtil.delete("category:all");
        log.info("新增分类: {}", category.getName());
    }

    @Override
    public void update(Category category) {
        Category exist = categoryMapper.selectById(category.getId());
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        category.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(category);
        redisUtil.delete("category:all");
        log.info("更新分类: id={}", category.getId());
    }

    @Override
    public void deleteById(Long id) {
        Category exist = categoryMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "分类不存在");
        }
        long childCount = categoryMapper.selectCount(
                new QueryWrapper<Category>().eq("parent_id", id)
        );
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "存在子分类，无法删除");
        }
        categoryMapper.deleteById(id);
        redisUtil.delete("category:all");
        log.info("删除分类: id={}", id);
    }
}
