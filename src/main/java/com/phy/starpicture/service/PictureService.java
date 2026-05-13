package com.phy.starpicture.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.phy.starpicture.model.dto.picture.PictureBatchFetchRequest;
import com.phy.starpicture.model.dto.picture.PictureEditRequest;
import com.phy.starpicture.model.dto.picture.PictureQueryRequest;
import com.phy.starpicture.model.dto.picture.PictureReviewRequest;
import com.phy.starpicture.model.entity.Picture;
import com.phy.starpicture.model.vo.PictureVO;
import com.phy.starpicture.model.vo.UserVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 图片服务接口
 */
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片（首次上传或重新上传）
     */
    PictureVO uploadPicture(MultipartFile file, Long pictureId, UserVO loginUser);

    /**
     * 分页查询图片列表
     */
    Page<PictureVO> listPictureByPage(PictureQueryRequest queryRequest, UserVO loginUser);

    /**
     * 管理员审核图片
     */
    void reviewPicture(PictureReviewRequest reviewRequest, UserVO loginUser);

    /**
     * 编辑图片元数据
     */
    void editPicture(PictureEditRequest editRequest, UserVO loginUser);

    /**
     * 删除图片（逻辑删除）
     */
    void deletePicture(Long id, UserVO loginUser);

    /**
     * 批量从网络抓取图片并入库
     */
    List<PictureVO> batchFetchPictures(PictureBatchFetchRequest fetchRequest, UserVO loginUser);

    /**
     * 根据 id 获取图片详情（带权限校验和缓存）
     * 普通用户只能看到已通过的图片，管理员不受限。
     *
     * @param id        图片 id
     * @param loginUser 当前登录用户
     * @return PictureVO（含上传者信息）
     */
    PictureVO getPictureDetailById(Long id, UserVO loginUser);
}
