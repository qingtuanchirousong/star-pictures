package com.phy.starpicture.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("space_user")
public class SpaceUser {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("spaceId")
    private Long spaceId;

    @TableField("userId")
    private Long userId;

    @TableField("spaceRole")
    private String spaceRole;

    @TableField("createTime")
    private Date createTime;

    @TableField("updateTime")
    private Date updateTime;
}
