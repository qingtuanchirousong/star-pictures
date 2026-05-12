package com.phy.starpicture.model.vo;

import com.phy.starpicture.model.entity.Picture;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

@Data
public class PictureVO implements Serializable {

    private Long id;

    private String url;

    private String thumbnailUrl;

    private String name;

    private String introduction;

    private String category;

    private String tags;

    private Long picSize;

    private Integer picWidth;

    private Integer picHeight;

    private Double picScale;

    private String picFormat;

    private String picColor;

    private Long userId;

    private Long spaceId;

    private Date createTime;

    private Date editTime;

    /**
     * 上传用户信息
     */
    private UserVO user;

    private static final long serialVersionUID = 1L;

    public static PictureVO of(Picture picture) {
        PictureVO vo = new PictureVO();
        BeanUtils.copyProperties(picture, vo);
        return vo;
    }
}
