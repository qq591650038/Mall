package com.mall.service;
public interface RiskControlService { boolean isBlocked(Long userId); void block(Long userId,String type,String reason,Long adminId); void unblock(Long userId,String type); }
