package com.mall.service.impl;

import com.mall.common.result.CursorPageResult;
import com.mall.common.result.ErrorCode;
import com.mall.common.util.CursorCodec;
import com.mall.dto.LoginDTO;
import com.mall.dto.RegisterDTO;
import com.mall.dto.UpdateUserDTO;
import com.mall.entity.User;
import com.mall.exception.BusinessException;
import com.mall.mapper.UserMapper;
import com.mall.security.JwtTokenProvider;
import com.mall.service.UserService;
import com.mall.utils.PasswordUtil;
import com.mall.utils.RedisUtil;
import com.mall.vo.LoginVO;
import com.mall.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisUtil redisUtil;
    private final PasswordUtil passwordUtil;

    public UserServiceImpl(UserMapper userMapper,
                          JwtTokenProvider jwtTokenProvider,
                          RedisUtil redisUtil,
                          PasswordUtil passwordUtil) {
        this.userMapper = userMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisUtil = redisUtil;
        this.passwordUtil = passwordUtil;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(RegisterDTO registerDTO) {
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }

        String verifyKey = registerDTO.getVerifyKey();
        String cachedCode = redisUtil.getString(verifyKey);
        if (cachedCode == null) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_EXPIRED);
        }
        if (!cachedCode.equals(registerDTO.getVerifyCode())) {
            throw new BusinessException(ErrorCode.VERIFY_CODE_ERROR);
        }
        redisUtil.delete(verifyKey);

        checkAccountExists(registerDTO);

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPhone(registerDTO.getPhone());
        user.setEmail(registerDTO.getEmail());
        user.setStatus(1);
        user.setPassword(passwordUtil.encode(registerDTO.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setDeleted(0);
        user.setSalt("1");

        userMapper.insert(user);
        log.info("用户注册成功: username={}", registerDTO.getUsername());
    }

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        User user = findByAccount(loginDTO.getAccount(), loginDTO.getLoginType());
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST, "账号已被禁用");
        }

        if (!passwordUtil.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }

        if (loginDTO.getVerifyKey() != null && loginDTO.getVerifyCode() != null) {
            String verifyKey = loginDTO.getVerifyKey();
            String cachedCode = redisUtil.getString(verifyKey);
            if (cachedCode == null) {
                throw new BusinessException(ErrorCode.VERIFY_CODE_EXPIRED);
            }
            if (!cachedCode.equals(loginDTO.getVerifyCode())) {
                throw new BusinessException(ErrorCode.VERIFY_CODE_ERROR);
            }
            redisUtil.delete(verifyKey);
        }

        String token = jwtTokenProvider.generateUserToken(user.getId(), "USER");

        String loginKey = "login:user:" + user.getId();
        redisUtil.set(loginKey, token, 24, TimeUnit.HOURS);

        user.setLastLoginTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户登录成功: userId={}", user.getId());

        return buildLoginVO(user, token);
    }

    @Override
    public void logout(Long userId) {
        String loginKey = "login:user:" + userId;
        redisUtil.delete(loginKey);
        log.info("用户退出登录: userId={}", userId);
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        return convertToUserVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(Long userId, UpdateUserDTO updateUserDTO) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        }
        user.setNickname(updateUserDTO.getNickname());
        user.setEmail(updateUserDTO.getEmail());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public CursorPageResult<UserVO> cursorPageForAdmin(Integer size, String keyword, Integer status, String cursor) {
        int limit = Math.min(Math.max(size == null ? 20 : size, 1), 100) + 1;
        CursorCodec.Decoded decoded = CursorCodec.decode(cursor);
        java.util.List<User> rows = userMapper.selectAdminCursorPage(keyword, status,
                decoded == null ? null : decoded.createTime(), decoded == null ? null : decoded.id(), limit);
        boolean hasNext = rows.size() == limit;
        java.util.List<User> users = hasNext ? rows.subList(0, limit - 1) : rows;
        String nextCursor = hasNext ? CursorCodec.encode(users.get(users.size() - 1).getCreateTime(), users.get(users.size() - 1).getId()) : null;
        return new CursorPageResult<>(users.stream().map(this::convertToUserVO).toList(), nextCursor, hasNext);
    }

    @Override
    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserVO> pageForAdmin(Integer current, Integer size, String keyword, Integer status) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size);
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> query = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            query.and(wrapper -> wrapper.like("username", keyword)
                    .or().like("nickname", keyword)
                    .or().like("phone", keyword)
                    .or().like("email", keyword));
        }
        if (status != null) query.eq("status", status);
        query.orderByDesc("create_time");
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<User> result = userMapper.selectPage(page, query);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserVO> output = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size);
        output.setTotal(result.getTotal());
        output.setRecords(result.getRecords().stream().map(this::convertToUserVO).toList());
        return output;
    }

    @Override
    public UserVO getUserInfoForAdmin(Long userId) {
        return getUserInfo(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatusForAdmin(Long userId, Integer status) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_EXIST);
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户状态不合法");
        }
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public User findById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User findByPhone(String phone) {
        return userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("phone", phone)
        );
    }

    @Override
    public User findByEmail(String email) {
        return userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("email", email)
        );
    }

    @Override
    public User findByUsername(String username) {
        return userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("username", username)
        );
    }

    private void checkAccountExists(RegisterDTO registerDTO) {
        User existing = findByUsername(registerDTO.getUsername());
        if (existing != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXIST, "用户名已被注册");
        }

        if (registerDTO.getPhone() != null && !registerDTO.getPhone().isEmpty()) {
            existing = findByPhone(registerDTO.getPhone());
            if (existing != null) {
                throw new BusinessException(ErrorCode.PHONE_ALREADY_REGISTERED);
            }
        }

        if (registerDTO.getEmail() != null && !registerDTO.getEmail().isEmpty()) {
            existing = findByEmail(registerDTO.getEmail());
            if (existing != null) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
            }
        }
    }

    private User findByAccount(String account, Integer loginType) {
        if (loginType != null && loginType == 2) {
            return findByPhone(account);
        } else if (loginType != null && loginType == 1) {
            return findByUsername(account);
        }
        User user = findByUsername(account);
        if (user == null) {
            user = findByPhone(account);
        }
        if (user == null) {
            user = findByEmail(account);
        }
        return user;
    }

    private LoginVO buildLoginVO(User user, String token) {
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setExpiresIn(86400L);

        LoginVO.UserInfoVO userInfo = new LoginVO.UserInfoVO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setPhone(user.getPhone());
        userInfo.setEmail(user.getEmail());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setNickname(user.getNickname());
        vo.setUserInfo(userInfo);

        return vo;
    }

    private UserVO convertToUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setNickname(user.getNickname());
        vo.setGender(user.getGender());
        vo.setStatus(user.getStatus());
        vo.setLastLoginIp(user.getLastLoginIp());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
