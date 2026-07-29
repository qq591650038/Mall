package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.ErrorCode;
import com.mall.entity.MemberLevel;
import com.mall.entity.PointsAccount;
import com.mall.entity.PointsCheckin;
import com.mall.entity.PointsLedger;
import com.mall.entity.PointsProduct;
import com.mall.entity.PointsRedemption;
import com.mall.exception.BusinessException;
import com.mall.mapper.MemberLevelMapper;
import com.mall.mapper.PointsAccountMapper;
import com.mall.mapper.PointsCheckinMapper;
import com.mall.mapper.PointsLedgerMapper;
import com.mall.mapper.PointsProductMapper;
import com.mall.mapper.PointsRedemptionMapper;
import com.mall.mapper.UserMapper;
import com.mall.service.MemberLevelService;
import com.mall.service.PointsService;
import com.mall.service.PointsRewardService;
import com.mall.dto.points.PointsRedeemDTO;
import com.mall.vo.PointsSummaryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 积分服务实现类 */
@Slf4j
@Service
public class PointsServiceImpl implements PointsService {

    private final PointsAccountMapper accountMapper;
    private final PointsLedgerMapper ledgerMapper;
    private final PointsCheckinMapper checkinMapper;
    private final PointsProductMapper productMapper;
    private final PointsRedemptionMapper redemptionMapper;
    private final MemberLevelService memberLevelService;
    private final MemberLevelMapper memberLevelMapper;
    private final UserMapper userMapper;
    private final PointsRewardService pointsRewardService;

    public PointsServiceImpl(PointsAccountMapper accountMapper,
                             PointsLedgerMapper ledgerMapper,
                             PointsCheckinMapper checkinMapper,
                             PointsProductMapper productMapper,
                             PointsRedemptionMapper redemptionMapper,
                             MemberLevelService memberLevelService,
                             MemberLevelMapper memberLevelMapper,
                             UserMapper userMapper,
                             PointsRewardService pointsRewardService) {
        this.accountMapper = accountMapper;
        this.ledgerMapper = ledgerMapper;
        this.checkinMapper = checkinMapper;
        this.productMapper = productMapper;
        this.redemptionMapper = redemptionMapper;
        this.memberLevelService = memberLevelService;
        this.memberLevelMapper = memberLevelMapper;
        this.userMapper = userMapper;
        this.pointsRewardService = pointsRewardService;
    }

    @Override
    public PointsSummaryVO getSummary(Long userId) {
        PointsAccount account = getOrCreateAccountForUpdate(userId);
        return toSummary(account, hasCheckedInToday(userId));
    }

    @Override
    public Page<PointsLedger> pageLedger(Long userId, Integer current, Integer size) {
        Page<PointsLedger> page = new Page<>(current, size);
        return ledgerMapper.selectPage(page, new QueryWrapper<PointsLedger>()
                .eq("user_id", userId)
                .orderByDesc("create_time"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointsSummaryVO checkIn(Long userId) {
        LocalDate today = LocalDate.now();
        if (hasCheckedInToday(userId)) {
            return getSummary(userId);
        }

        PointsAccount account = getOrCreateAccountForUpdate(userId);
        int reward = 10;
        account.setBalance(account.getBalance() + reward);
        account.setTotalEarned(account.getTotalEarned() + reward);
        account.setUpdateTime(LocalDateTime.now());
        accountMapper.updateById(account);

        PointsCheckin checkin = new PointsCheckin();
        checkin.setUserId(userId);
        checkin.setCheckinDate(today);
        checkin.setPoints(reward);
        checkinMapper.insert(checkin);

        PointsLedger ledger = new PointsLedger();
        ledger.setUserId(userId);
        ledger.setAmount(reward);
        ledger.setBalanceAfter(account.getBalance());
        ledger.setEventType("CHECK_IN");
        ledger.setRemark("每日签到奖励");
        ledger.setCreateTime(LocalDateTime.now());
        ledgerMapper.insert(ledger);
        return toSummary(account, true);
    }

    @Override
    public List<PointsProduct> listProducts() {
        return productMapper.selectList(new QueryWrapper<PointsProduct>()
                .eq("status", 1)
                .gt("stock", 0)
                .orderByAsc("points_cost")
                .orderByDesc("id"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PointsRedemption redeem(Long userId, Long productId, PointsRedeemDTO dto) {
        PointsProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.POINTS_PRODUCT_NOT_EXIST);
        }
        if (product.getStatus() != 1) {
            throw new BusinessException(ErrorCode.POINTS_PRODUCT_OFF_SHELF);
        }
        if (product.getStock() <= 0) {
            throw new BusinessException(ErrorCode.POINTS_STOCK_INSUFFICIENT);
        }
        pointsRewardService.validateConfiguration(product);

        PointsAccount account = getOrCreateAccountForUpdate(userId);
        if (account.getBalance() < product.getPointsCost()) {
            throw new BusinessException(ErrorCode.POINTS_INSUFFICIENT);
        }

        int updated = productMapper.decrementStock(productId);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.POINTS_STOCK_INSUFFICIENT);
        }

        account.setBalance(account.getBalance() - product.getPointsCost());
        account.setTotalSpent(account.getTotalSpent() + product.getPointsCost());
        account.setUpdateTime(LocalDateTime.now());
        accountMapper.updateById(account);

        PointsLedger ledger = new PointsLedger();
        ledger.setUserId(userId);
        ledger.setAmount(-product.getPointsCost());
        ledger.setBalanceAfter(account.getBalance());
        ledger.setEventType("REDEEM");
        ledger.setRemark("兑换：" + product.getName());
        ledger.setCreateTime(LocalDateTime.now());
        ledgerMapper.insert(ledger);

        PointsRewardService.FulfillmentResult fulfillment = pointsRewardService.fulfill(
                userId, product, dto == null ? null : dto.getAddressId());

        PointsRedemption redemption = new PointsRedemption();
        redemption.setUserId(userId);
        redemption.setProductId(productId);
        redemption.setPoints(product.getPointsCost());
        redemption.setRedemptionCode(fulfillment.orderId() != null
                ? "ORDER-" + fulfillment.orderId() : "COUPON-" + fulfillment.userCouponId());
        redemption.setRewardType(product.getRewardType());
        redemption.setRewardRefId(product.getRewardRefId());
        redemption.setRewardSkuId(product.getRewardSkuId());
        redemption.setOrderId(fulfillment.orderId());
        redemption.setUserCouponId(fulfillment.userCouponId());
        redemption.setFulfillmentStatus(fulfillment.fulfillmentStatus());
        redemption.setUpdateTime(LocalDateTime.now());
        redemption.setCreateTime(LocalDateTime.now());
        redemptionMapper.insert(redemption);
        return redemption;
    }

    @Override
    public Page<PointsRedemption> pageRedemptions(Long userId, Integer current, Integer size) {
        return redemptionMapper.selectPage(new Page<>(current, size), new QueryWrapper<PointsRedemption>()
                .eq("user_id", userId)
                .orderByDesc("create_time"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void earn(Long userId, int points, String remark, Long businessId) {
        if (points <= 0) {
            log.warn("积分必须为正数: userId={}, points={}", userId, points);
            return;
        }
        PointsAccount account = getOrCreateAccountForUpdate(userId);
        if (businessId != null && ledgerMapper.selectCount(new QueryWrapper<PointsLedger>()
                .eq("event_type", "PAYMENT_EARN").eq("business_id", businessId)) > 0) {
            return;
        }
        account.setBalance(account.getBalance() + points);
        account.setTotalEarned(account.getTotalEarned() + points);
        account.setUpdateTime(LocalDateTime.now());
        accountMapper.updateById(account);

        PointsLedger ledger = new PointsLedger();
        ledger.setUserId(userId);
        ledger.setAmount(points);
        ledger.setBalanceAfter(account.getBalance());
        ledger.setEventType("PAYMENT_EARN");
        ledger.setRemark(remark);
        ledger.setBusinessId(businessId);
        ledger.setCreateTime(LocalDateTime.now());
        ledgerMapper.insert(ledger);

        log.info("积分赚取: userId={}, points={}, remark={}", userId, points, remark);

        // 自动升级会员等级
        autoUpgradeLevel(userId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void earnForPayment(Long userId, BigDecimal paidAmount, Long orderId, String orderNo) {
        if (paidAmount == null || paidAmount.signum() <= 0) return;
        PointsAccount account = getOrCreateAccountForUpdate(userId);
        if (ledgerMapper.selectCount(new QueryWrapper<PointsLedger>()
                .eq("event_type", "PAYMENT_EARN").eq("business_id", orderId)) > 0) {
            return;
        }
        MemberLevel currentLevel = account.getMemberLevelId() == null
                ? memberLevelService.resolveLevelByPoints(account.getTotalEarned())
                : memberLevelMapper.selectById(account.getMemberLevelId());
        BigDecimal rate = currentLevel != null && Integer.valueOf(1).equals(currentLevel.getStatus())
                && currentLevel.getPointsRate() != null ? currentLevel.getPointsRate() : BigDecimal.ONE;
        int earnedPoints = paidAmount.multiply(rate).setScale(0, RoundingMode.DOWN).intValueExact();
        earn(userId, earnedPoints, "支付订单 " + orderNo + " 获得积分（倍率: " + rate + "）", orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reversePaymentPoints(Long userId, Long orderId, String orderNo) {
        if (ledgerMapper.selectCount(new QueryWrapper<PointsLedger>()
                .eq("event_type", "PAYMENT_REFUND").eq("business_id", orderId)) > 0) {
            return;
        }
        PointsLedger earned = ledgerMapper.selectOne(new QueryWrapper<PointsLedger>()
                .eq("event_type", "PAYMENT_EARN").eq("business_id", orderId).last("LIMIT 1"));
        if (earned == null || earned.getAmount() == null || earned.getAmount() <= 0) return;
        PointsAccount account = getOrCreateAccountForUpdate(userId);
        int reversed = earned.getAmount();
        account.setBalance(account.getBalance() - reversed);
        account.setTotalEarned(Math.max(0, account.getTotalEarned() - earned.getAmount()));
        account.setUpdateTime(LocalDateTime.now());
        accountMapper.updateById(account);

        PointsLedger reversal = new PointsLedger();
        reversal.setUserId(userId);
        reversal.setAmount(-reversed);
        reversal.setBalanceAfter(account.getBalance());
        reversal.setEventType("PAYMENT_REFUND");
        reversal.setRemark("订单 " + orderNo + " 全额退款，撤销支付赠送积分");
        reversal.setBusinessId(orderId);
        reversal.setCreateTime(LocalDateTime.now());
        ledgerMapper.insert(reversal);
        autoUpgradeLevel(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoUpgradeLevel(Long userId) {
        PointsAccount account = accountMapper.selectOne(new QueryWrapper<PointsAccount>()
                .eq("user_id", userId).last("FOR UPDATE"));
        if (account == null) return;

        MemberLevel targetLevel = memberLevelService.resolveLevelByPoints(account.getTotalEarned());
        if (targetLevel == null) return;

        // 如果等级发生变化，则更新
        if (account.getMemberLevelId() == null || !account.getMemberLevelId().equals(targetLevel.getId())) {
            account.setMemberLevelId(targetLevel.getId());
            account.setUpdateTime(LocalDateTime.now());
            accountMapper.updateById(account);
            com.mall.entity.User user = userMapper.selectById(userId);
            if (user != null) {
                user.setMemberLevelId(targetLevel.getId());
                user.setUpdateTime(LocalDateTime.now());
                userMapper.updateById(user);
            }
            log.info("会员等级升级: userId={}, newLevel={}", userId, targetLevel.getName());
        }
    }

    private PointsAccount getOrCreateAccount(Long userId) {
        PointsAccount account = accountMapper.selectOne(new QueryWrapper<PointsAccount>().eq("user_id", userId));
        if (account != null) {
            return account;
        }
        account = new PointsAccount();
        account.setUserId(userId);
        account.setBalance(0);
        account.setTotalEarned(0);
        account.setTotalSpent(0);
        // Resolve the configured lowest active level instead of assuming its physical ID.
        MemberLevel defaultLevel = memberLevelMapper.selectOne(
                new QueryWrapper<MemberLevel>().eq("status", 1).orderByAsc("level").last("LIMIT 1"));
        account.setMemberLevelId(defaultLevel != null ? defaultLevel.getId() : null);
        account.setUpdateTime(LocalDateTime.now());
        try {
            accountMapper.insert(account);
            return account;
        } catch (DuplicateKeyException ignored) {
            return accountMapper.selectOne(new QueryWrapper<PointsAccount>()
                    .eq("user_id", userId).last("FOR UPDATE"));
        }
    }

    private PointsAccount getOrCreateAccountForUpdate(Long userId) {
        PointsAccount account = accountMapper.selectOne(new QueryWrapper<PointsAccount>()
                .eq("user_id", userId).last("FOR UPDATE"));
        return account != null ? account : getOrCreateAccount(userId);
    }

    private boolean hasCheckedInToday(Long userId) {
        return checkinMapper.selectCount(new QueryWrapper<PointsCheckin>()
                .eq("user_id", userId)
                .eq("checkin_date", LocalDate.now())) > 0;
    }

    /**
     * 根据会员等级表动态计算会员等级信息
     */
    private PointsSummaryVO toSummary(PointsAccount account, boolean checkedIn) {
        int earned = account.getTotalEarned();
        Long levelId = account.getMemberLevelId();

        // 获取当前等级信息
        MemberLevel currentLevel = null;
        if (levelId != null) {
            currentLevel = memberLevelMapper.selectById(levelId);
        }
        if (currentLevel == null) {
            currentLevel = memberLevelService.resolveLevelByPoints(earned);
        }

        PointsSummaryVO summary = new PointsSummaryVO();
        summary.setBalance(account.getBalance());
        summary.setTotalEarned(earned);
        summary.setTotalSpent(account.getTotalSpent());
        summary.setCheckedInToday(checkedIn);

        if (currentLevel != null) {
            summary.setMemberLevel(currentLevel.getName());
            summary.setMemberLevelId(currentLevel.getId());
            summary.setPointsRate(currentLevel.getPointsRate() != null ? currentLevel.getPointsRate() : BigDecimal.ONE);

            // 查找下一级别
            List<MemberLevel> allLevels = memberLevelService.listAll();
            MemberLevel nextLevel = null;
            for (MemberLevel lvl : allLevels) {
                if (lvl.getLevel() > currentLevel.getLevel()) {
                    if (nextLevel == null || lvl.getLevel() < nextLevel.getLevel()) {
                        nextLevel = lvl;
                    }
                }
            }

            if (nextLevel != null) {
                summary.setNextLevelName(nextLevel.getName());
                summary.setNextLevelPoints(nextLevel.getMinPoints());
                summary.setPointsToNextLevel(Math.max(0, nextLevel.getMinPoints() - earned));
            } else {
                summary.setNextLevelName("已满级");
                summary.setNextLevelPoints(0);
                summary.setPointsToNextLevel(0);
            }
        } else {
            summary.setMemberLevel("普通会员");
            summary.setMemberLevelId(null);
            summary.setPointsRate(BigDecimal.ONE);
            summary.setNextLevelPoints(1000);
            summary.setPointsToNextLevel(Math.max(0, 1000 - earned));
        }

        return summary;
    }
}
