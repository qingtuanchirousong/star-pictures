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
@TableName("space")
public class Space {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("spaceName")
    private String spaceName;

    @TableField("spaceLevel")
    private Integer spaceLevel;

    @TableField("maxSize")
    private Long maxSize;

    @TableField("maxCount")
    private Long maxCount;

    @TableField("totalSize")
    private Long totalSize;

    @TableField("totalCount")
    private Long totalCount;

    @TableField("userId")
    private Long userId;

    @TableField("createTime")
    private Date createTime;

    @TableField("editTime")
    private Date editTime;

    @TableField("updateTime")
    private Date updateTime;

    @TableField("isDelete")
    private Integer isDelete;

    @TableField("spaceType")
    private Integer spaceType;
}
