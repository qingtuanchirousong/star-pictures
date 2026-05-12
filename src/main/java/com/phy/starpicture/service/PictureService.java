package com.phy.starpicture.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.phy.starpicture.model.entity.Picture;
import com.phy.starpicture.model.vo.PictureVO;
import com.phy.starpicture.model.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片服务接口
 */
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片（首次上传或重新上传）
     *
     * @param file      图片文件
     * @param pictureId 已有图片 id（重新上传时传入，首次上传为 null）
     * @param loginUser 当前登录用户
     * @return 图片视图对象（含上传用户信息）
     */
    PictureVO uploadPicture(MultipartFile file, Long pictureId, UserVO loginUser);
}
