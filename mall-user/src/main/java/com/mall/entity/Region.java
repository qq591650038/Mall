package com.mall.entity;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.io.Serializable;
@Data @TableName("region") public class Region implements Serializable { @TableId(type=IdType.AUTO) private Long id; private Long parentId; private String name; private Integer level; private Integer sort; }
