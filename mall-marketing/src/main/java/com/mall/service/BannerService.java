package com.mall.service;
import com.mall.entity.Banner;
import java.util.List;
public interface BannerService {
    List<Banner> listActive();
    List<Banner> listAll();
    Banner getById(Long id);
    void save(Banner banner);
    void update(Banner banner);
    void deleteById(Long id);
}
