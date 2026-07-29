package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.result.ErrorCode;
import com.mall.entity.Address;
import com.mall.entity.User;
import com.mall.exception.BusinessException;
import com.mall.mapper.AddressMapper;
import com.mall.mapper.UserMapper;
import com.mall.service.PrivacyService;
import com.mall.utils.RedisUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PrivacyServiceImpl implements PrivacyService {
    private final UserMapper users; private final AddressMapper addresses; private final RedisUtil redis;
    public PrivacyServiceImpl(UserMapper users, AddressMapper addresses, RedisUtil redis) { this.users=users; this.addresses=addresses; this.redis=redis; }
    @Override public Map<String,Object> exportPersonalData(Long userId) { User user=users.selectById(userId); if(user==null)throw new BusinessException(ErrorCode.USER_NOT_EXIST); Map<String,Object> data=new LinkedHashMap<>(); Map<String,Object> profile=new LinkedHashMap<>(); profile.put("id",user.getId());profile.put("username",user.getUsername());profile.put("phone",user.getPhone());profile.put("email",user.getEmail());profile.put("nickname",user.getNickname());profile.put("createTime",user.getCreateTime()); data.put("profile",profile); data.put("addresses",addresses.selectList(new LambdaQueryWrapper<Address>().eq(Address::getUserId,userId))); data.put("generatedAt",LocalDateTime.now()); return data; }
    @Override @Transactional public void closeAccount(Long userId) { User user=users.selectById(userId); if(user==null)throw new BusinessException(ErrorCode.USER_NOT_EXIST); user.setStatus(0);user.setDeleted(1);user.setUpdateTime(LocalDateTime.now());users.updateById(user);redis.delete("login:user:"+userId); }
}
