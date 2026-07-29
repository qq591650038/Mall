package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.ErrorCode;
import com.mall.entity.MemberLevel;
import com.mall.exception.BusinessException;
import com.mall.mapper.MemberLevelMapper;
import com.mall.mapper.PointsAccountMapper;
import com.mall.service.MemberLevelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 会员等级服务实现类 */
@Slf4j
@Service
public class MemberLevelServiceImpl implements MemberLevelService {

    private final MemberLevelMapper memberLevelMapper;
    private final PointsAccountMapper pointsAccountMapper;

    public MemberLevelServiceImpl(MemberLevelMapper memberLevelMapper,
                                  PointsAccountMapper pointsAccountMapper) {
        this.memberLevelMapper = memberLevelMapper;
        this.pointsAccountMapper = pointsAccountMapper;
    }

    @Override
    public Page<MemberLevel> page(Integer current, Integer size, Integer status) {
        QueryWrapper<MemberLevel> wrapper = new QueryWrapper<>();
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByAsc("level");
        return memberLevelMapper.selectPage(Page.of(current, size), wrapper);
    }

    @Override
    public List<MemberLevel> listAll() {
        QueryWrapper<MemberLevel> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1).orderByAsc("level");
        return memberLevelMapper.selectList(wrapper);
    }

    @Override
    public MemberLevel getById(Long id) {
        MemberLevel level = memberLevelMapper.selectById(id);
        if (level == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会员等级不存在");
        }
        return level;
    }

    @Override
    @Transactional
    public MemberLevel create(MemberLevel level) {
        validate(level, null);
        level.setCreateTime(LocalDateTime.now());
        level.setUpdateTime(LocalDateTime.now());
        if (level.getStatus() == null)
            level.setStatus(1);
        if (level.getSort() == null)
            level.setSort(0);
        if (level.getPointsRate() == null)
            level.setPointsRate(BigDecimal.ONE);
        if (level.getDiscountRate() == null)
            level.setDiscountRate(BigDecimal.ONE);
        memberLevelMapper.insert(level);
        log.info("创建会员等级: id={}, name={}, level={}", level.getId(), level.getName(), level.getLevel());
        return level;
    }

    @Override
    @Transactional
    public MemberLevel update(Long id, MemberLevel level) {
        MemberLevel exist = memberLevelMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会员等级不存在");
        }
        MemberLevel candidate = merge(exist, level);
        validate(candidate, id);
        if (Integer.valueOf(0).equals(candidate.getStatus())
                && pointsAccountMapper.selectCount(new QueryWrapper<com.mall.entity.PointsAccount>()
                        .eq("member_level_id", id)) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该会员等级正在使用，不能禁用");
        }
        if (level.getName() != null)
            exist.setName(level.getName());
        if (level.getLevel() != null)
            exist.setLevel(level.getLevel());
        if (level.getMinPoints() != null)
            exist.setMinPoints(level.getMinPoints());
        if (level.getMaxPoints() != null)
            exist.setMaxPoints(level.getMaxPoints());
        if (level.getPointsRate() != null)
            exist.setPointsRate(level.getPointsRate());
        if (level.getDiscountRate() != null)
            exist.setDiscountRate(level.getDiscountRate());
        if (level.getIcon() != null)
            exist.setIcon(level.getIcon());
        if (level.getDescription() != null)
            exist.setDescription(level.getDescription());
        if (level.getStatus() != null)
            exist.setStatus(level.getStatus());
        if (level.getSort() != null)
            exist.setSort(level.getSort());
        exist.setUpdateTime(LocalDateTime.now());
        memberLevelMapper.updateById(exist);
        log.info("更新会员等级: id={}", id);
        return exist;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MemberLevel level = memberLevelMapper.selectById(id);
        if (level == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会员等级不存在");
        }
        if (pointsAccountMapper.selectCount(new QueryWrapper<com.mall.entity.PointsAccount>()
                .eq("member_level_id", id)) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该会员等级正在使用，不能删除");
        }
        memberLevelMapper.deleteById(id);
        log.info("删除会员等级: id={}", id);
    }

    @Override
    public MemberLevel resolveLevelByPoints(int totalPoints) {
        List<MemberLevel> all = listAll().stream()
                .sorted((left, right) -> Integer.compare(right.getMinPoints(), left.getMinPoints()))
                .toList();
        for (MemberLevel level : all) {
            int min = level.getMinPoints() != null ? level.getMinPoints() : 0;
            int max = level.getMaxPoints() == null || level.getMaxPoints() == 0
                    ? Integer.MAX_VALUE : level.getMaxPoints();
            if (totalPoints >= min && totalPoints <= max) {
                return level;
            }
        }
        // 默认返回最低等级
        return all.isEmpty() ? null : all.get(all.size() - 1);
    }

    @Override
    public BigDecimal getPointsRate(Long levelId) {
        if (levelId == null)
            return BigDecimal.ONE;
        MemberLevel level = memberLevelMapper.selectById(levelId);
        if (level == null || level.getStatus() != 1)
            return BigDecimal.ONE;
        return level.getPointsRate() != null ? level.getPointsRate() : BigDecimal.ONE;
    }

    private MemberLevel merge(MemberLevel existing, MemberLevel update) {
        MemberLevel merged = new MemberLevel();
        merged.setName(update.getName() != null ? update.getName() : existing.getName());
        merged.setLevel(update.getLevel() != null ? update.getLevel() : existing.getLevel());
        merged.setMinPoints(update.getMinPoints() != null ? update.getMinPoints() : existing.getMinPoints());
        merged.setMaxPoints(update.getMaxPoints() != null ? update.getMaxPoints() : existing.getMaxPoints());
        merged.setPointsRate(update.getPointsRate() != null ? update.getPointsRate() : existing.getPointsRate());
        merged.setDiscountRate(update.getDiscountRate() != null ? update.getDiscountRate() : existing.getDiscountRate());
        merged.setStatus(update.getStatus() != null ? update.getStatus() : existing.getStatus());
        return merged;
    }

    private void validate(MemberLevel level, Long excludedId) {
        if (level.getName() == null || level.getName().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "等级名称不能为空");
        }
        if (level.getLevel() == null || level.getLevel() < 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "等级数值必须大于0");
        }
        if (level.getMinPoints() == null || level.getMaxPoints() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "积分区间不能为空");
        }
        int min = level.getMinPoints();
        int max = level.getMaxPoints();
        if (min < 0 || (max != 0 && max < min)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "积分区间无效");
        }
        if (level.getPointsRate() != null && (level.getPointsRate().compareTo(new BigDecimal("0.01")) < 0
                || level.getPointsRate().compareTo(new BigDecimal("10.00")) > 0)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "积分倍率必须在0.01到10之间");
        }
        if (level.getDiscountRate() != null && (level.getDiscountRate().compareTo(new BigDecimal("0.01")) < 0
                || level.getDiscountRate().compareTo(BigDecimal.ONE) > 0)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "折扣率必须在0.01到1之间");
        }
        if (level.getStatus() != null && level.getStatus() != 0 && level.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "等级状态无效");
        }
        QueryWrapper<MemberLevel> sameLevel = new QueryWrapper<MemberLevel>().eq("level", level.getLevel());
        if (excludedId != null) sameLevel.ne("id", excludedId);
        if (memberLevelMapper.selectCount(sameLevel) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "等级数值已存在");
        }
        List<MemberLevel> others = memberLevelMapper.selectList(new QueryWrapper<MemberLevel>()
                .ne(excludedId != null, "id", excludedId));
        long candidateMax = max == 0 ? Long.MAX_VALUE : max;
        for (MemberLevel other : others) {
            long otherMin = other.getMinPoints() == null ? 0 : other.getMinPoints();
            long otherMax = other.getMaxPoints() == null || other.getMaxPoints() == 0
                    ? Long.MAX_VALUE : other.getMaxPoints();
            if (min <= otherMax && otherMin <= candidateMax) {
                throw new BusinessException(ErrorCode.CONFLICT, "积分区间与等级“" + other.getName() + "”重叠");
            }
        }
    }
}
