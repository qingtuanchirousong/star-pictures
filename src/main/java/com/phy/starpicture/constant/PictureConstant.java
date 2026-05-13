package com.phy.starpicture.constant;

/**
 * 图片审核状态常量
 */
public interface PictureConstant {

    /** 待审核 */
    int REVIEW_STATUS_PENDING = 0;

    /** 审核通过 */
    int REVIEW_STATUS_APPROVED = 1;

    /** 审核拒绝 */
    int REVIEW_STATUS_REJECTED = 2;
}
