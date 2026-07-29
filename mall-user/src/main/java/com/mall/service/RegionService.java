package com.mall.service;
import com.mall.entity.Region;
import java.util.List;
public interface RegionService {
    List<Region> getProvinces(); List<Region> getCitiesByProvince(Long provinceId); List<Region> getDistrictsByCity(Long cityId);
}
