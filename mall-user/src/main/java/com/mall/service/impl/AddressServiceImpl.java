package com.mall.service.impl;

import com.mall.common.result.ErrorCode;
import com.mall.entity.Address;
import com.mall.exception.BusinessException;
import com.mall.mapper.AddressMapper;
import com.mall.service.AddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    public AddressServiceImpl(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    @Override
    public List<Address> listByUserId(Long userId) {
        return addressMapper.selectList(
                new QueryWrapper<Address>().eq("user_id", userId).orderByDesc("is_default", "id")
        );
    }

    @Override
    public Address getById(Long id, Long userId) {
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "地址不存在");
        }
        return address;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Address address) {
        address.setCreateTime(LocalDateTime.now());
        address.setUpdateTime(LocalDateTime.now());
        address.setDeleted(0);
        if (address.getIsDefault() == null) {
            address.setIsDefault(0);
        }
        if (address.getIsDefault() == 1) {
            cancelDefault(address.getUserId());
        }
        addressMapper.insert(address);
        log.info("新增地址: userId={}", address.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Address address) {
        Address exist = addressMapper.selectById(address.getId());
        if (exist == null || !exist.getUserId().equals(address.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "地址不存在");
        }
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            cancelDefault(address.getUserId());
        }
        address.setUpdateTime(LocalDateTime.now());
        addressMapper.updateById(address);
        log.info("更新地址: id={}", address.getId());
    }

    @Override
    public void delete(Long id, Long userId) {
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "地址不存在");
        }
        addressMapper.deleteById(id);
        log.info("删除地址: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefault(Long id, Long userId) {
        Address address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "地址不存在");
        }
        cancelDefault(userId);
        address.setIsDefault(1);
        address.setUpdateTime(LocalDateTime.now());
        addressMapper.updateById(address);
        log.info("设置默认地址: id={}", id);
    }

    private void cancelDefault(Long userId) {
        UpdateWrapper<Address> wrapper = new UpdateWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("is_default", 1)
                .set("is_default", 0)
                .set("update_time", LocalDateTime.now());
        addressMapper.update(null, wrapper);
    }
}