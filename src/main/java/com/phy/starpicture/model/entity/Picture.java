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
@TableName("picture")
public class Picture {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("url")
    private String url;

    @TableField("name")
    private String name;

    @TableField("introduction")
    private String introduction;

    @TableField("category")
    private String category;

    @TableField("tags")
    private String tags;

    @TableField("picSize")
    private Long picSize;

    @TableField("picWidth")
    private Integer picWidth;

    @TableField("picHeight")
    private Integer picHeight;

    @TableField("picScale")
    private Double picScale;

    @TableField("picFormat")
    private String picFormat;

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

    @TableField("reviewStatus")
    private Integer reviewStatus;

    @TableField("reviewMessage")
    private String reviewMessage;

    @TableField("reviewerId")
    private Long reviewerId;

    @TableField("reviewTime")
    private Date reviewTime;

    @TableField("thumbnailUrl")
    private String thumbnailUrl;

    @TableField("spaceId")
    private Long spaceId;

    @TableField("picColor")
    private String picColor;
}
