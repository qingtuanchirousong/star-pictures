package com.phy.starpicture.model.dto.picture;

import com.phy.starpicture.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 图片分页查询请求
 * 支持按名称、分类、审核状态筛选
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PictureQueryRequest extends PageRequest implements Serializable {

    /** 图片名称（模糊搜索） */
    private String name;

    /** 分类 */
    private String category;

    /** 审核状态：0=待审核, 1=通过, 2=拒绝，不传则查全部（管理员） */
    private Integer reviewStatus;

    private static final long serialVersionUID = 1L;
}
