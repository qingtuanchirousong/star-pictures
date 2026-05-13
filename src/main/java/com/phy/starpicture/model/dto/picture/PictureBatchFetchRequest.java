package com.phy.starpicture.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 批量抓取图片请求
 * 管理员填写搜索关键词和抓取数量，后端从网络抓取图片并入库。
 */
@Data
public class PictureBatchFetchRequest implements Serializable {

    /** 搜索关键词（如 "风景壁纸"） */
    private String keyword;

    /** 抓取数量，默认 10，最大 30 */
    private Integer count = 10;

    private static final long serialVersionUID = 1L;
}
