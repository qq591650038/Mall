package com.mall.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.entity.MemberLevel;

import java.math.BigDecimal;
import java.util.List;

/** 会员等级服务接口 */
public interface MemberLevelService {

    /** 分页获取会员等级列表 */
    Page<MemberLevel> page(Integer current, Integer size, Integer status);

    /** 获取所有启用的会员等级 */
    List<MemberLevel> listAll();

    /** 根据ID获取会员等级 */
    MemberLevel getById(Long id);

    /** 创建会员等级 */
    MemberLevel create(MemberLevel level);

    /** 更新会员等级 */
    MemberLevel update(Long id, MemberLevel level);

    /** 删除会员等级 */
    void delete(Long id);

    /** 根据积分获取用户应属的等级 */
    MemberLevel resolveLevelByPoints(int totalPoints);

    /** 获取等级对应的积分倍率 */
    BigDecimal getPointsRate(Long levelId);
}
