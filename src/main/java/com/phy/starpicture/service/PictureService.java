package com.phy.starpicture.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.phy.starpicture.model.dto.picture.PictureEditRequest;
import com.phy.starpicture.model.dto.picture.PictureQueryRequest;
import com.phy.starpicture.model.dto.picture.PictureReviewRequest;
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

    /**
     * 分页查询图片列表
     * 管理员可查看全部状态，普通用户只能看到审核通过的图片
     *
     * @param queryRequest 查询条件（名称、分类、审核状态等）
     * @param loginUser    当前登录用户
     * @return 分页结果，PictureVO 中包含上传者 UserVO 信息
     */
    Page<PictureVO> listPictureByPage(PictureQueryRequest queryRequest, UserVO loginUser);

    /**
     * 管理员审核图片
     * 将图片审核状态改为通过或拒绝，并记录审核人、时间和意见
     *
     * @param reviewRequest 审核请求（图片 id、目标状态、审核意见）
     * @param loginUser     当前登录的管理员
     */
    void reviewPicture(PictureReviewRequest reviewRequest, UserVO loginUser);

    /**
     * 编辑图片元数据（名称、简介、分类、标签）
     * 仅图片所有者或管理员可编辑
     *
     * @param editRequest 编辑请求
     * @param loginUser   当前登录用户
     */
    void editPicture(PictureEditRequest editRequest, UserVO loginUser);

    /**
     * 删除图片（逻辑删除）
     * 仅图片所有者或管理员可删除
     *
     * @param id        图片 id
     * @param loginUser 当前登录用户
     */
    void deletePicture(Long id, UserVO loginUser);
}
