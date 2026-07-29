package com.mall.service;

import com.mall.entity.User;
import com.mall.mapper.UserMapper;
import com.mall.security.JwtTokenProvider;
import com.mall.service.impl.UserServiceImpl;
import com.mall.utils.PasswordUtil;
import com.mall.utils.RedisUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UserServiceImplTest {
    private final UserMapper userMapper = mock(UserMapper.class);
    private final UserServiceImpl service = new UserServiceImpl(userMapper, mock(JwtTokenProvider.class),
            mock(RedisUtil.class), new PasswordUtil(new BCryptPasswordEncoder()));

    @Test
    void loginRejectsUnknownAccount() {
        when(userMapper.selectOne(any())).thenReturn(null);
        com.mall.dto.LoginDTO dto = new com.mall.dto.LoginDTO();
        dto.setAccount("missing");
        dto.setPassword("secret");
        assertThrows(RuntimeException.class, () -> service.login(dto));
    }

    @Test
    void loginRejectsWrongPassword() {
        User user = new User();
        user.setStatus(1);
        user.setPassword(new PasswordUtil(new BCryptPasswordEncoder()).encode("correct"));
        when(userMapper.selectOne(any())).thenReturn(user);
        com.mall.dto.LoginDTO dto = new com.mall.dto.LoginDTO();
        dto.setAccount("user");
        dto.setPassword("wrong");
        assertThrows(RuntimeException.class, () -> service.login(dto));
    }
}
