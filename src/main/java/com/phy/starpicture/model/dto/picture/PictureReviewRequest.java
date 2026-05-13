package com.phy.starpicture.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片审核请求
 * 管理员审核图片时传入，包含审核结果和审核意见
 */
@Data
public class PictureReviewRequest implements Serializable {

    /** 图片 id */
    private Long id;

    /** 审核状态：1=通过, 2=拒绝 */
    private Integer reviewStatus;

    /** 审核意见（支持通过时写备注和拒绝时写原因） */
    private String reviewMessage;

    private static final long serialVersionUID = 1L;
}
