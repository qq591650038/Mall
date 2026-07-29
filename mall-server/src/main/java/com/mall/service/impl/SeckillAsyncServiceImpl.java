package com.mall.service.impl;

import com.mall.common.result.ErrorCode;
import com.mall.entity.Address;
import com.mall.entity.MarketingActivity;
import com.mall.entity.MarketingActivityItem;
import com.mall.entity.MarketingParticipant;
import com.mall.entity.SeckillRequest;
import com.mall.exception.BusinessException;
import com.mall.mapper.MarketingActivityItemMapper;
import com.mall.mapper.MarketingActivityMapper;
import com.mall.mapper.MarketingParticipantMapper;
import com.mall.mapper.SeckillRequestMapper;
import com.mall.service.AddressService;
import com.mall.service.OrderService;
import com.mall.service.SeckillAsyncService;
import com.mall.dto.order.CreateOrderDTO;
import com.mall.vo.MarketingParticipateVO;
import com.mall.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class SeckillAsyncServiceImpl implements SeckillAsyncService {
    private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>("""
            local stock = redis.call('GET', KEYS[1])
            if not stock then return -2 end
            local bought = tonumber(redis.call('GET', KEYS[2]) or '0')
            local quantity = tonumber(ARGV[1])
            local limit = tonumber(ARGV[2])
            if tonumber(stock) < quantity then return 0 end
            if bought + quantity > limit then return -1 end
            redis.call('DECRBY', KEYS[1], quantity)
            redis.call('INCRBY', KEYS[2], quantity)
            redis.call('EXPIRE', KEYS[2], tonumber(ARGV[3]))
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> COMPENSATE_SCRIPT = new DefaultRedisScript<>("""
            redis.call('INCRBY', KEYS[1], tonumber(ARGV[1]))
            local bought = tonumber(redis.call('GET', KEYS[2]) or '0') - tonumber(ARGV[1])
            if bought <= 0 then redis.call('DEL', KEYS[2]) else redis.call('SET', KEYS[2], bought) end
            return 1
            """, Long.class);

    private final StringRedisTemplate redis;
    private final SeckillRequestMapper requestMapper;
    private final MarketingActivityMapper activityMapper;
    private final MarketingActivityItemMapper itemMapper;
    private final MarketingParticipantMapper participantMapper;
    private final AddressService addressService;
    private final OrderService orderService;
    private final ObjectProvider<DefaultMQProducer> producerProvider;
    private final String topic;

    public SeckillAsyncServiceImpl(StringRedisTemplate redis, SeckillRequestMapper requestMapper,
                                   MarketingActivityMapper activityMapper, MarketingActivityItemMapper itemMapper,
                                   MarketingParticipantMapper participantMapper, AddressService addressService,
                                   OrderService orderService, ObjectProvider<DefaultMQProducer> producerProvider,
                                   @Value("${rocketmq.topic}") String topic) {
        this.redis = redis;
        this.requestMapper = requestMapper;
        this.activityMapper = activityMapper;
        this.itemMapper = itemMapper;
        this.participantMapper = participantMapper;
        this.addressService = addressService;
        this.orderService = orderService;
        this.producerProvider = producerProvider;
        this.topic = topic;
    }

    @Override
    public MarketingParticipateVO submit(Long userId, Long activityId, Long itemId, Integer quantity) {
        int actualQuantity = quantity == null ? 1 : quantity;
        MarketingActivity activity = activityMapper.selectById(activityId);
        MarketingActivityItem item = itemMapper.selectById(itemId);
        if (activity == null || item == null || !activityId.equals(item.getActivityId()) || !"SECKILL".equals(activity.getType())
                || activity.getStatus() != 1 || LocalDateTime.now().isBefore(activity.getStartTime()) || LocalDateTime.now().isAfter(activity.getEndTime())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Seckill activity is unavailable");
        }
        if (actualQuantity <= 0 || item.getSkuId() == null)
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid seckill quantity");
        int limit = item.getLimitPerUser() == null ? 1 : item.getLimitPerUser();
        Address address = addressService.listByUserId(userId).stream().filter(value -> Integer.valueOf(1).equals(value.getIsDefault()))
                .findFirst().orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Default address is required"));
        String stockKey = stockKey(itemId);
        redis.opsForValue().setIfAbsent(stockKey, String.valueOf(item.getStock()), java.time.Duration.ofDays(2));
        long result = redis.execute(RESERVE_SCRIPT, List.of(stockKey, userKey(itemId, userId)), String.valueOf(actualQuantity), String.valueOf(limit), "172800");
        if (result == 0)
            throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "Seckill stock is insufficient");
        if (result == -1) throw new BusinessException(ErrorCode.BAD_REQUEST, "Purchase limit exceeded");
        if (result == -2)
            throw new BusinessException(ErrorCode.CONFLICT, "Seckill stock is initializing; retry shortly");

        String requestId = UUID.randomUUID().toString().replace("-", "");
        SeckillRequest request = new SeckillRequest();
        request.setRequestId(requestId);
        request.setActivityId(activityId);
        request.setActivityItemId(itemId);
        request.setUserId(userId);
        request.setQuantity(actualQuantity);
        request.setAddressId(address.getId());
        request.setStatus(0);
        request.setCompensated(0);
        request.setCreateTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        requestMapper.insert(request);
        try {
            DefaultMQProducer producer = producerProvider.getIfAvailable();
            if (producer == null) throw new IllegalStateException("RocketMQ is disabled");
            producer.send(new Message(topic, requestId.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            requestMapper.markFailed(requestId, "Queue submission failed");
            compensate(request);
            throw new BusinessException(ErrorCode.CONFLICT, "Seckill request submission failed");
        }
        MarketingParticipateVO response = new MarketingParticipateVO();
        response.setRequestId(requestId);
        response.setStatus(0);
        return response;
    }

    @Override
    public SeckillRequest getRequest(Long userId, String requestId) {
        SeckillRequest request = requestMapper.selectById(requestId);
        if (request == null || !userId.equals(request.getUserId()))
            throw new BusinessException(ErrorCode.NOT_FOUND, "Seckill request not found");
        return request;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void process(String requestId) {
        SeckillRequest request = requestMapper.selectForUpdate(requestId);
        if (request == null || request.getStatus() != 0) return;
        try {
            MarketingActivityItem item = itemMapper.selectById(request.getActivityItemId());
            MarketingActivity activity = activityMapper.selectById(request.getActivityId());
            if (item == null || activity == null || !"SECKILL".equals(activity.getType()) || itemMapper.deductForSeckill(item.getId(), request.getQuantity()) != 1) {
                throw new BusinessException(ErrorCode.STOCK_INSUFFICIENT, "Seckill stock is insufficient");
            }
            CreateOrderDTO dto = new CreateOrderDTO();
            dto.setAddressId(request.getAddressId());
            dto.setRemark("Seckill " + request.getActivityId());
            CreateOrderDTO.OrderItemDTO orderItem = new CreateOrderDTO.OrderItemDTO();
            orderItem.setProductId(item.getProductId());
            orderItem.setSkuId(item.getSkuId());
            orderItem.setQuantity(request.getQuantity());
            dto.setItems(List.of(orderItem));
            OrderVO order = orderService.createMarketingOrder(request.getUserId(), dto, Map.of(item.getSkuId(), item.getActivityPrice()));
            MarketingParticipant participant = new MarketingParticipant();
            participant.setRequestId(requestId);
            participant.setActivityId(request.getActivityId());
            participant.setActivityItemId(item.getId());
            participant.setUserId(request.getUserId());
            participant.setOrderId(order.getId());
            participant.setQuantity(request.getQuantity());
            participant.setStatus(0);
            participant.setCreateTime(LocalDateTime.now());
            participant.setUpdateTime(LocalDateTime.now());
            participantMapper.insert(participant);
            requestMapper.markSucceeded(requestId, order.getId());
        } catch (RuntimeException ex) {
            requestMapper.markFailed(requestId, message(ex));
            throw ex;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void failAndCompensate(String requestId, String reason) {
        SeckillRequest request = requestMapper.selectForUpdate(requestId);
        if (request == null || request.getStatus() == 1) return;
        requestMapper.markFailed(requestId, reason);
        compensate(request);
    }

    @Override
    public void restoreActivityStock(Long itemId, Long userId, Integer quantity) {
        if (itemId == null || userId == null || quantity == null || quantity <= 0) return;
        redis.execute(COMPENSATE_SCRIPT, List.of(stockKey(itemId), userKey(itemId, userId)), String.valueOf(quantity));
    }

    private void compensate(SeckillRequest request) {
        if (request.getCompensated() != null && request.getCompensated() == 1) return;
        if (requestMapper.markCompensated(request.getRequestId()) == 1) redis.execute(COMPENSATE_SCRIPT,
                List.of(stockKey(request.getActivityItemId()), userKey(request.getActivityItemId(), request.getUserId())), String.valueOf(request.getQuantity()));
    }

    private String stockKey(Long itemId) {
        return "seckill:stock:" + itemId;
    }

    private String userKey(Long itemId, Long userId) {
        return "seckill:user:" + itemId + ":" + userId;
    }

    private String message(Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage().substring(0, Math.min(500, exception.getMessage().length()));
    }
}
