package com.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mall.entity.PointsAccount;
import com.mall.entity.Product;
import com.mall.entity.User;
import com.mall.entity.UserSpending;
import com.mall.mapper.PointsAccountMapper;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.UserMapper;
import com.mall.mapper.UserSpendingMapper;
import com.mall.service.LeaderboardService;
import com.mall.vo.LeaderboardVO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LeaderboardServiceImpl implements LeaderboardService {

    private static final int TOP_LIMIT = 20;

    private final PointsAccountMapper pointsAccountMapper;
    private final ProductMapper productMapper;
    private final UserMapper userMapper;
    private final UserSpendingMapper userSpendingMapper;

    public LeaderboardServiceImpl(PointsAccountMapper pointsAccountMapper,
                                  ProductMapper productMapper,
                                  UserMapper userMapper,
                                  UserSpendingMapper userSpendingMapper) {
        this.pointsAccountMapper = pointsAccountMapper;
        this.productMapper = productMapper;
        this.userMapper = userMapper;
        this.userSpendingMapper = userSpendingMapper;
    }

    @Override
    public LeaderboardVO getLeaderboard() {
        LeaderboardVO result = new LeaderboardVO();
        result.setPoints(getPointsRanking());
        result.setSpending(getSpendingRanking());
        result.setProducts(getProductRanking());
        return result;
    }

    private List<LeaderboardVO.UserEntry> getPointsRanking() {
        List<PointsAccount> accounts = pointsAccountMapper.selectList(new QueryWrapper<PointsAccount>()
                .gt("balance", 0)
                .orderByDesc("balance")
                .orderByAsc("user_id")
                .last("LIMIT " + TOP_LIMIT));
        return toUserEntries(accounts.stream().map(PointsAccount::getUserId).toList(),
                accounts.stream().map(account -> BigDecimal.valueOf(account.getBalance())).toList());
    }

    private List<LeaderboardVO.UserEntry> getSpendingRanking() {
        List<UserSpending> rows = userSpendingMapper.selectList(new QueryWrapper<UserSpending>()
                .gt("total_amount", 0)
                .orderByDesc("total_amount")
                .orderByAsc("user_id")
                .last("LIMIT " + TOP_LIMIT));
        List<Long> userIds = rows.stream().map(UserSpending::getUserId).toList();
        List<BigDecimal> amounts = rows.stream().map(UserSpending::getTotalAmount).toList();
        return toUserEntries(userIds, amounts);
    }

    private List<LeaderboardVO.ProductEntry> getProductRanking() {
        List<Product> products = productMapper.selectList(new QueryWrapper<Product>()
                .select("id", "name", "main_image", "price", "sales")
                .eq("status", 1)
                .gt("sales", 0)
                .orderByDesc("sales")
                .orderByAsc("id")
                .last("LIMIT " + TOP_LIMIT));
        List<LeaderboardVO.ProductEntry> entries = new ArrayList<>();
        for (int index = 0; index < products.size(); index++) {
            Product product = products.get(index);
            LeaderboardVO.ProductEntry entry = new LeaderboardVO.ProductEntry();
            entry.setRank(index + 1);
            entry.setProductId(product.getId());
            entry.setName(product.getName());
            entry.setImage(product.getMainImage());
            entry.setPrice(product.getPrice());
            entry.setSales(product.getSales());
            entries.add(entry);
        }
        return entries;
    }

    private List<LeaderboardVO.UserEntry> toUserEntries(List<Long> userIds, List<BigDecimal> values) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        Map<Long, User> users = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        List<LeaderboardVO.UserEntry> entries = new ArrayList<>();
        for (int index = 0; index < userIds.size(); index++) {
            User user = users.get(userIds.get(index));
            if (user == null) {
                continue;
            }
            LeaderboardVO.UserEntry entry = new LeaderboardVO.UserEntry();
            entry.setRank(entries.size() + 1);
            entry.setNickname(maskName(user.getNickname(), user.getUsername()));
            entry.setAvatar(user.getAvatar());
            entry.setValue(values.get(index));
            entries.add(entry);
        }
        return entries;
    }

    private String maskName(String nickname, String username) {
        String name = Objects.requireNonNullElse(nickname, username);
        if (name == null || name.isBlank()) {
            return "匿名用户";
        }
        if (name.length() == 1) {
            return name + "*";
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1);
    }

}
