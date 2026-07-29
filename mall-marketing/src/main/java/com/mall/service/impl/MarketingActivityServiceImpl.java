package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.result.ErrorCode;
import com.mall.entity.*;
import com.mall.exception.BusinessException;
import com.mall.mapper.*;
import com.mall.service.MarketingActivityService;
import com.mall.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 营销活动服务实现类
 * 支持限时折扣、满减、秒杀、拼团等活动类型
 */
@Slf4j
@Service
public class MarketingActivityServiceImpl implements MarketingActivityService {

    private final MarketingActivityMapper activityMapper;
    private final MarketingActivityItemMapper itemMapper;
    private final MarketingParticipantMapper participantMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final MarketingOrderQueryMapper orderQueryMapper;
    private final ApplicationEventPublisher eventPublisher;

    public MarketingActivityServiceImpl(MarketingActivityMapper activityMapper,
                                        MarketingActivityItemMapper itemMapper,
                                         MarketingParticipantMapper participantMapper,
                                         ProductMapper productMapper,
                                         ProductSkuMapper productSkuMapper,
                                         MarketingOrderQueryMapper orderQueryMapper,
                                         ApplicationEventPublisher eventPublisher) {
        this.activityMapper = activityMapper;
        this.itemMapper = itemMapper;
        this.participantMapper = participantMapper;
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.orderQueryMapper = orderQueryMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Page<MarketingActivity> page(Integer current, Integer size, String type, Integer status) {
        QueryWrapper<MarketingActivity> wrapper = new QueryWrapper<>();
        if (type != null && !type.isEmpty()) {
            wrapper.eq("type", type);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");
        Page<MarketingActivity> page = activityMapper.selectPage(Page.of(current, size), wrapper);

        // 为每条活动查询商品数量
        for (MarketingActivity activity : page.getRecords()) {
            long count = itemMapper.selectCount(
                    new QueryWrapper<MarketingActivityItem>()
                            .eq("activity_id", activity.getId())
                            .eq("status", 1)
            );
            activity.setItemCount(count);
        }
        return page;
    }

    @Override
    public List<MarketingActivity> listActive(String type) {
        LocalDateTime now = LocalDateTime.now();
        QueryWrapper<MarketingActivity> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
                .le("start_time", now)
                .ge("end_time", now);
        if (type != null && !type.isEmpty()) {
            wrapper.eq("type", type);
        }
        wrapper.orderByAsc("sort");
        return activityMapper.selectList(wrapper);
    }

    @Override
    public MarketingActivity getById(Long id) {
        MarketingActivity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        return activity;
    }

    @Override
    @Transactional
    public MarketingActivity create(MarketingActivity activity, List<MarketingActivityItem> items) {
        if (!activity.getStartTime().isBefore(activity.getEndTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "活动开始时间必须早于结束时间");
        }
        LocalDateTime now = LocalDateTime.now();
        if ("GROUP_BUY".equals(activity.getType())) {
            if (activity.getGroupTarget() == null || activity.getGroupTarget() < 2) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "拼团成团人数必须至少为2人");
            }
        } else {
            activity.setGroupTarget(null);
        }
        activity.setStatus(activity.getStartTime().isAfter(now) ? 0 : 1);
        activity.setSort(activity.getSort() != null ? activity.getSort() : 0);
        activity.setCreateTime(now);
        activity.setUpdateTime(now);
        activity.setDeleted(0);
        activityMapper.insert(activity);

        if (items != null && !items.isEmpty()) {
            for (MarketingActivityItem item : items) {
                item.setActivityId(activity.getId());
                item.setSoldCount(0);
                item.setStatus(1);
                item.setCreateTime(LocalDateTime.now());
                item.setUpdateTime(LocalDateTime.now());
                itemMapper.insert(item);
            }
        }
        log.info("创建营销活动: id={}, name={}, type={}", activity.getId(), activity.getName(), activity.getType());
        return activity;
    }

    @Override
    @Transactional
    public MarketingActivity update(Long id, MarketingActivity activity, List<MarketingActivityItem> items) {
        MarketingActivity exist = activityMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        if (exist.getStatus() != 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "活动已开始或已结束，不能修改");
        }
        if (activity.getName() != null) exist.setName(activity.getName());
        if (activity.getType() != null && "GROUP_BUY".equals(activity.getType())
                && (activity.getGroupTarget() == null || activity.getGroupTarget() < 2)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "拼团成团人数必须至少为2人");
        }
        if (activity.getType() != null && !"GROUP_BUY".equals(activity.getType())) exist.setGroupTarget(null);
        else if (activity.getGroupTarget() != null) exist.setGroupTarget(activity.getGroupTarget());
        if (activity.getDescription() != null) exist.setDescription(activity.getDescription());
        if (activity.getStartTime() != null) exist.setStartTime(activity.getStartTime());
        if (activity.getEndTime() != null) exist.setEndTime(activity.getEndTime());
        if (activity.getSort() != null) exist.setSort(activity.getSort());
        exist.setUpdateTime(LocalDateTime.now());
        activityMapper.updateById(exist);

        if (items != null) {
            itemMapper.delete(new QueryWrapper<MarketingActivityItem>().eq("activity_id", id));
            for (MarketingActivityItem item : items) {
                item.setId(null);
                item.setActivityId(id);
                item.setSoldCount(0);
                item.setStatus(1);
                item.setCreateTime(LocalDateTime.now());
                item.setUpdateTime(LocalDateTime.now());
                itemMapper.insert(item);
            }
        }
        log.info("更新营销活动: id={}", id);
        return exist;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        MarketingActivity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        if (activity.getStatus() == 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "活动进行中，不能删除");
        }
        itemMapper.delete(new QueryWrapper<MarketingActivityItem>().eq("activity_id", id));
        activityMapper.deleteById(id);
        log.info("删除营销活动: id={}", id);
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        MarketingActivity activity = activityMapper.selectById(id);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        if (activity.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "活动未在进行中");
        }
        activity.setStatus(3);
        activity.setUpdateTime(LocalDateTime.now());
        activityMapper.updateById(activity);
        log.info("取消营销活动: id={}", id);
    }

    @Override
    public List<MarketingActivityItemVO> listItems(Long activityId) {
        List<MarketingActivityItem> items = itemMapper.selectList(
                new QueryWrapper<MarketingActivityItem>()
                        .eq("activity_id", activityId)
                        .eq("status", 1)
        );
        if (items.isEmpty()) {
            return List.of();
        }
        // 批量查询商品信息
        List<Long> productIds = items.stream()
                .map(MarketingActivityItem::getProductId)
                .distinct()
                .toList();
        Map<Long, Product> productMap = productIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        productMapper::selectById,
                        (a, b) -> a
                ));

        return items.stream().map(item -> {
            MarketingActivityItemVO vo = new MarketingActivityItemVO();
            vo.setId(item.getId());
            vo.setActivityId(item.getActivityId());
            vo.setProductId(item.getProductId());
            vo.setSkuId(item.getSkuId());
            vo.setActivityPrice(item.getActivityPrice());
            vo.setOriginalPrice(item.getOriginalPrice());

            int activityStock = item.getStock() == null ? 0 : item.getStock();
            int soldCount = item.getSoldCount() == null ? 0 : item.getSoldCount();
            // 活动商品的剩余库存 = 活动配置库存 - 已售数量
            int remainingActivityStock = Math.max(0, activityStock);
            // 同时不能超过 SKU 实际库存
            ProductSku sku = item.getSkuId() == null ? null : productSkuMapper.selectById(item.getSkuId());
            int skuStock = (sku == null || sku.getStock() == null) ? Integer.MAX_VALUE : sku.getStock();
            int remainingStock = Math.max(0, Math.min(remainingActivityStock, skuStock));

            vo.setStock(remainingStock);
            vo.setSoldCount(soldCount);
            vo.setRemainingStock(remainingStock);
            vo.setLimitPerUser(item.getLimitPerUser());
            vo.setStatus(item.getStatus());
            Product product = productMap.get(item.getProductId());
            if (product != null) {
                vo.setProductName(product.getName());
                vo.setProductImage(product.getMainImage());
            }
            return vo;
        }).toList();
    }

    @Override
    public long countItems(Long activityId) {
        return itemMapper.selectCount(
                new QueryWrapper<MarketingActivityItem>()
                        .eq("activity_id", activityId)
                        .eq("status", 1)
        );
    }

    @Override
    @Transactional
    public MarketingParticipateVO participate(Long activityId, Long itemId, Long userId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "购买数量必须大于0");
        }
        // 获取活动信息
        MarketingActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "活动尚未开始");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "活动已结束");
        }
        if (activity.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "活动已结束或已取消");
        }

        // 获取活动商品明细
        MarketingActivityItem item = itemMapper.selectOne(
                new QueryWrapper<MarketingActivityItem>().eq("id", itemId).last("FOR UPDATE"));
        if (item == null || !item.getActivityId().equals(activityId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "活动商品不存在");
        }
        if (item.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "活动商品已下架");
        }

        // 检查库存
        int currentStock = item.getStock() == null ? 0 : item.getStock();
        if (quantity > currentStock) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "库存不足");
        }
        ProductSku sku = item.getSkuId() == null ? null : productSkuMapper.selectById(item.getSkuId());
        if (sku == null || sku.getStock() == null || sku.getStock() < quantity) {
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "SKU库存不足");
        }

        // 检查限购
        int limitPerUser = item.getLimitPerUser() != null ? item.getLimitPerUser() : 1;
        if (quantity > limitPerUser) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "超过限购数量");
        }

        // 检查用户是否已参与
        Long participatedCount = participantMapper.selectCount(
                new QueryWrapper<MarketingParticipant>()
                        .eq("activity_id", activityId)
                        .eq("activity_item_id", itemId)
                        .eq("user_id", userId)
                        .in("status", 0, 1)
        );
        if (participatedCount > 0) {
            List<MarketingParticipant> existing = participantMapper.selectList(
                    new QueryWrapper<MarketingParticipant>()
                            .eq("activity_id", activityId)
                            .eq("activity_item_id", itemId)
                            .eq("user_id", userId)
                            .in("status", 0, 1)
            );
            int totalQuantity = existing.stream().mapToInt(MarketingParticipant::getQuantity).sum();
            if (totalQuantity + quantity > limitPerUser) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "已达到限购数量");
            }
        }

        item.setStock(currentStock - quantity);
        item.setSoldCount((item.getSoldCount() == null ? 0 : item.getSoldCount()) + quantity);
        item.setUpdateTime(now);
        itemMapper.updateById(item);

        // 创建参与者记录
        MarketingParticipant participant = new MarketingParticipant();
        participant.setActivityId(activityId);
        participant.setActivityItemId(itemId);
        participant.setUserId(userId);
        participant.setQuantity(quantity);
        participant.setStatus(0);
        participant.setCreateTime(LocalDateTime.now());
        participant.setUpdateTime(LocalDateTime.now());

        if ("GROUP_BUY".equals(activity.getType())) {
            int groupTarget = activity.getGroupTarget() == null ? 0 : activity.getGroupTarget();
            List<MarketingParticipant> activeParticipants = participantMapper.selectList(
                    new QueryWrapper<MarketingParticipant>()
                            .eq("activity_id", activityId)
                            .eq("activity_item_id", itemId)
                            .in("status", 0, 1)
                            .isNotNull("group_no")
                            .orderByAsc("create_time"));
            String groupNo = null;
            for (MarketingParticipant existingParticipant : activeParticipants) {
                String candidate = existingParticipant.getGroupNo();
                long occupied = activeParticipants.stream()
                        .filter(p -> candidate.equals(p.getGroupNo()))
                        .count();
                if (occupied < groupTarget) {
                    groupNo = candidate;
                    break;
                }
            }
            if (groupNo == null) {
                groupNo = "GROUP_" + UUID.randomUUID().toString().replace("-", "")
                        .substring(0, 16).toUpperCase();
            }
            participant.setGroupNo(groupNo);
            participant.setGroupStatus(1);
            participant.setParentId(null);
        }

        participantMapper.insert(participant);
        log.info("参与活动: activityId={}, itemId={}, userId={}, quantity={}", activityId, itemId, userId, quantity);

        MarketingParticipateVO vo = new MarketingParticipateVO();
        vo.setParticipantId(participant.getId());
        vo.setActivityId(activityId);
        vo.setItemId(itemId);
        vo.setProductId(item.getProductId());
        vo.setSkuId(item.getSkuId());
        vo.setQuantity(quantity);
        vo.setStatus(0);
        vo.setActivityPrice(item.getActivityPrice());
        return vo;
    }

    @Override
    @Transactional
    public void onPaymentSuccess(Long participantId, Long orderId) {
        MarketingParticipant participant = participantMapper.selectById(participantId);
        if (participant == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "参与记录不存在");
        }
        participant.setOrderId(orderId);
        participant.setStatus(1);
        participant.setUpdateTime(LocalDateTime.now());
        participantMapper.updateById(participant);
        log.info("活动支付成功: participantId={}, orderId={}", participantId, orderId);
    }

    @Override
    @Transactional
    public void linkOrder(Long participantId, Long orderId) {
        MarketingParticipant participant = participantMapper.selectById(participantId);
        if (participant == null) throw new BusinessException(ErrorCode.NOT_FOUND, "活动参与记录不存在");
        participant.setOrderId(orderId);
        participant.setUpdateTime(LocalDateTime.now());
        participantMapper.updateById(participant);
    }

    @Override
    @Transactional
    public void onOrderCancel(Long participantId) {
        MarketingParticipant participant = participantMapper.selectById(participantId);
        if (participant == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "参与记录不存在");
        }
        if (participant.getStatus() == 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "订单已支付，不能取消");
        }
        MarketingActivityItem item = itemMapper.selectOne(new QueryWrapper<MarketingActivityItem>()
                .eq("id", participant.getActivityItemId()).last("FOR UPDATE"));
        if (item != null) {
            int quantity = participant.getQuantity() == null ? 0 : participant.getQuantity();
            item.setStock((item.getStock() == null ? 0 : item.getStock()) + quantity);
            item.setSoldCount(Math.max(0, (item.getSoldCount() == null ? 0 : item.getSoldCount()) - quantity));
            item.setUpdateTime(LocalDateTime.now());
            itemMapper.updateById(item);
        }
        participant.setStatus(2);
        participant.setUpdateTime(LocalDateTime.now());
        participantMapper.updateById(participant);
        log.info("活动订单取消: participantId={}", participantId);
    }

    @Override
    @Transactional
    public GroupPaymentResult onPaymentSuccessByOrderId(Long orderId) {
        MarketingParticipant participant = participantMapper.selectOne(
                new QueryWrapper<MarketingParticipant>().eq("order_id", orderId).last("LIMIT 1"));
        if (participant == null) return GroupPaymentResult.normalOrder();
        itemMapper.selectOne(new QueryWrapper<MarketingActivityItem>()
                .eq("id", participant.getActivityItemId()).last("FOR UPDATE"));
        if (participant.getStatus() == 0) onPaymentSuccess(participant.getId(), orderId);
        MarketingActivity activity = activityMapper.selectById(participant.getActivityId());
        if (activity == null || !"GROUP_BUY".equals(activity.getType())) {
            return GroupPaymentResult.marketingOrder(orderId);
        }
        List<MarketingParticipant> paid = participantMapper.selectList(
                new QueryWrapper<MarketingParticipant>()
                        .eq("activity_id", participant.getActivityId())
                        .eq("activity_item_id", participant.getActivityItemId())
                        .eq("group_no", participant.getGroupNo())
                        .eq("status", 1));
        int count = paid.size();
        if (count < activity.getGroupTarget()) return GroupPaymentResult.groupPending();
        for (MarketingParticipant member : paid) {
            member.setGroupStatus(2);
            member.setUpdateTime(LocalDateTime.now());
            participantMapper.updateById(member);
        }
        return GroupPaymentResult.groupFormed(paid.stream().map(MarketingParticipant::getOrderId)
                .filter(java.util.Objects::nonNull).toList());
    }

    @Override
    @Transactional
    public void onOrderCancelByOrderId(Long orderId) {
        MarketingParticipant participant = participantMapper.selectOne(
                new QueryWrapper<MarketingParticipant>().eq("order_id", orderId).eq("status", 0).last("LIMIT 1"));
        if (participant != null) {
            onOrderCancel(participant.getId());
            publishSeckillStockRestore(participant);
        }
    }

    @Override
    @Transactional
    public void onRefundSuccessByOrderId(Long orderId) {
        MarketingParticipant participant = participantMapper.selectOne(
                new QueryWrapper<MarketingParticipant>().eq("order_id", orderId).eq("status", 1).last("LIMIT 1"));
        if (participant == null) return;
        MarketingActivityItem item = itemMapper.selectOne(
                new QueryWrapper<MarketingActivityItem>().eq("id", participant.getActivityItemId()).last("FOR UPDATE"));
        if (item != null) {
            int quantity = participant.getQuantity() == null ? 0 : participant.getQuantity();
            item.setStock((item.getStock() == null ? 0 : item.getStock()) + quantity);
            item.setSoldCount(Math.max(0, (item.getSoldCount() == null ? 0 : item.getSoldCount()) - quantity));
            item.setUpdateTime(LocalDateTime.now());
            itemMapper.updateById(item);
        }
        participant.setStatus(2);
        participant.setUpdateTime(LocalDateTime.now());
        participantMapper.updateById(participant);
        publishSeckillStockRestore(participant);
    }

    private void publishSeckillStockRestore(MarketingParticipant participant) {
        MarketingActivity activity = activityMapper.selectById(participant.getActivityId());
        if (activity != null && "SECKILL".equals(activity.getType())) {
            eventPublisher.publishEvent(new com.mall.event.SeckillStockRestoredEvent(
                    participant.getActivityId(), participant.getActivityItemId(), participant.getUserId(), participant.getQuantity()));
        }
    }

    @Override
    @Transactional
    public List<Long> expireUnformedGroups() {
        LocalDateTime now = LocalDateTime.now();
        List<MarketingActivity> activities = activityMapper.selectList(
                new QueryWrapper<MarketingActivity>().eq("type", "GROUP_BUY").lt("end_time", now));
        if (activities.isEmpty()) return List.of();
        List<Long> activityIds = activities.stream().map(MarketingActivity::getId).toList();
        List<MarketingParticipant> unformed = participantMapper.selectList(
                new QueryWrapper<MarketingParticipant>().in("activity_id", activityIds)
                        .in("group_status", 1, 3).isNotNull("group_no"));
        participantMapper.markGroupsFailed(unformed.stream().map(MarketingParticipant::getId).toList(), now);
        return unformed.stream().filter(p -> Integer.valueOf(1).equals(p.getStatus()))
                .map(MarketingParticipant::getOrderId).filter(java.util.Objects::nonNull).distinct().toList();
    }

    @Override
    public List<MarketingGroupVO> listGroups(Long activityId) {
        MarketingActivity activity = activityMapper.selectById(activityId);
        if (activity == null) throw new BusinessException(ErrorCode.NOT_FOUND, "活动不存在");
        List<MarketingParticipant> participants = participantMapper.selectList(
                new QueryWrapper<MarketingParticipant>().eq("activity_id", activityId)
                        .isNotNull("group_no").orderByAsc("create_time"));
        Map<String, List<MarketingParticipant>> grouped = participants.stream()
                .collect(Collectors.groupingBy(MarketingParticipant::getGroupNo, java.util.LinkedHashMap::new, Collectors.toList()));
        List<Long> orderIds = participants.stream().map(MarketingParticipant::getOrderId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, MarketingOrderQueryMapper.OrderStatusRow> statusMap = orderIds.isEmpty() ? Map.of()
                : orderQueryMapper.findStatuses(orderIds).stream()
                .collect(Collectors.toMap(row -> row.id, row -> row));
        List<Long> userIds = participants.stream().map(MarketingParticipant::getUserId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, String> usernameMap = userIds.isEmpty() ? Map.of()
                : orderQueryMapper.findUsers(userIds).stream()
                .collect(Collectors.toMap(row -> row.id, row -> row.username));
        return grouped.entrySet().stream().map(entry -> {
            MarketingGroupVO group = new MarketingGroupVO();
            group.setGroupNo(entry.getKey());
            group.setTarget(activity.getGroupTarget());
            int joined = (int) entry.getValue().stream()
                    .filter(p -> p.getStatus() != null && p.getStatus() != 2).count();
            group.setJoinedQuantity(joined);
            int groupStatus = entry.getValue().stream().map(MarketingParticipant::getGroupStatus)
                    .filter(java.util.Objects::nonNull).max(Integer::compareTo).orElse(1);
            group.setGroupStatus(groupStatus);
            group.setMembers(entry.getValue().stream().map(p -> {
                MarketingGroupMemberVO member = new MarketingGroupMemberVO();
                member.setParticipantId(p.getId()); member.setUserId(p.getUserId()); member.setUsername(usernameMap.get(p.getUserId())); member.setQuantity(p.getQuantity());
                member.setOrderId(p.getOrderId()); member.setParticipantStatus(p.getStatus());
                MarketingOrderQueryMapper.OrderStatusRow row = statusMap.get(p.getOrderId());
                if (row != null) { member.setOrderNo(row.orderNo); member.setOrderStatus(row.orderStatus); member.setPayStatus(row.payStatus); member.setOrderStatusText(com.mall.common.result.OrderStatus.getTextByCode(row.orderStatus)); }
                return member;
            }).toList());
            return group;
        }).toList();
    }

    @Override
    @Transactional
    public void updateActivityStatus() {
        LocalDateTime now = LocalDateTime.now();
        List<MarketingActivity> notStarted = activityMapper.selectList(
                new QueryWrapper<MarketingActivity>()
                        .eq("status", 0)
                        .le("start_time", now)
        );
        for (MarketingActivity activity : notStarted) {
            activity.setStatus(1);
            activity.setUpdateTime(now);
            activityMapper.updateById(activity);
            log.info("活动开始: id={}, name={}", activity.getId(), activity.getName());
        }

        List<MarketingActivity> active = activityMapper.selectList(
                new QueryWrapper<MarketingActivity>()
                        .eq("status", 1)
                        .lt("end_time", now)
        );
        for (MarketingActivity activity : active) {
            activity.setStatus(2);
            activity.setUpdateTime(now);
            activityMapper.updateById(activity);
            log.info("活动结束: id={}, name={}", activity.getId(), activity.getName());
        }
    }
}
