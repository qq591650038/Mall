package com.mall.service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.entity.Brand;
import java.util.List;
public interface BrandService {
    Page<Brand> page(Integer current, Integer size, String keyword);
    List<Brand> listEnabled();
    Brand getById(Long id);
    void save(Brand brand);
    void update(Brand brand);
    void deleteById(Long id);
}
