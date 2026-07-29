package com.mall.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.entity.User;
import org.apache.ibatis.annotations.Mapper;
@Mapper
public interface UserMapper extends BaseMapper<User> {
    java.util.List<User> selectAdminCursorPage(@org.apache.ibatis.annotations.Param("keyword") String keyword,
                                               @org.apache.ibatis.annotations.Param("status") Integer status,
                                               @org.apache.ibatis.annotations.Param("cursorTime") java.time.LocalDateTime cursorTime,
                                               @org.apache.ibatis.annotations.Param("cursorId") Long cursorId,
                                               @org.apache.ibatis.annotations.Param("limit") int limit);
}
