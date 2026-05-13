package com.phy.starpicture.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片编辑请求
 * 修改图片的名称、简介、分类、标签等元数据
 */
@Data
public class PictureEditRequest implements Serializable {

    /** 图片 id */
    private Long id;

    /** 图片名称 */
    private String name;

    /** 图片简介 */
    private String introduction;

    /** 分类 */
    private String category;

    /** 标签（JSON 数组字符串） */
    private String tags;

    private static final long serialVersionUID = 1L;
}
