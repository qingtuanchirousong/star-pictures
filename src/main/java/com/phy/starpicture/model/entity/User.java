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
@TableName("user")
public class User {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("userAccount")
    private String userAccount;

    @TableField("userPassword")
    private String userPassword;

    @TableField("userName")
    private String userName;

    @TableField("userAvatar")
    private String userAvatar;

    @TableField("userProfile")
    private String userProfile;

    @TableField("userRole")
    private String userRole;

    @TableField("editTime")
    private Date editTime;

    @TableField("createTime")
    private Date createTime;

    @TableField("updateTime")
    private Date updateTime;

    @TableField("isDelete")
    private Integer isDelete;

    @TableField("vipExpireTime")
    private Date vipExpireTime;

    @TableField("vipCode")
    private String vipCode;

    @TableField("vipNumber")
    private Long vipNumber;
}
