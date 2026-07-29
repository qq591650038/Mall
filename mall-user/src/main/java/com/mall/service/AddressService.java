package com.mall.service;
import com.mall.entity.Address;
import java.util.List;
public interface AddressService {
    List<Address> listByUserId(Long userId); Address getById(Long id, Long userId); void save(Address address); void update(Address address); void delete(Long id, Long userId); void setDefault(Long id, Long userId);
}
