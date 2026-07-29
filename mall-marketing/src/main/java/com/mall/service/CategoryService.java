package com.mall.service;
import com.mall.entity.Category;
import java.util.List;
public interface CategoryService {
    List<Category> listAll();
    List<Category> listByParentId(Long parentId);
    Category getById(Long id);
    void save(Category category);
    void update(Category category);
    void deleteById(Long id);
}
