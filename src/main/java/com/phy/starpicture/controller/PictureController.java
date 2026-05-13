package com.phy.starpicture.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.phy.starpicture.annotation.AuthCheck;
import com.phy.starpicture.common.BaseResponse;
import com.phy.starpicture.common.DeleteRequest;
import com.phy.starpicture.common.ResultUtils;
import com.phy.starpicture.constant.UserConstant;
import com.phy.starpicture.exception.ErrorCode;
import com.phy.starpicture.exception.ThrowUtils;
import com.phy.starpicture.model.dto.picture.PictureEditRequest;
import com.phy.starpicture.model.dto.picture.PictureQueryRequest;
import com.phy.starpicture.model.dto.picture.PictureReviewRequest;
import com.phy.starpicture.model.entity.Picture;
import com.phy.starpicture.model.vo.PictureVO;
import com.phy.starpicture.model.vo.UserVO;
import com.phy.starpicture.service.PictureService;
import com.phy.starpicture.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 图片管理控制器
 * 提供图片上传、分页查询、审核、编辑、删除等接口。
 */
@Api(tags = "图片管理")
@RestController
@RequestMapping("/picture")
public class PictureController {

    /** 登录用户状态 key（与 UserConstant 保持一致） */
    private static final String USER_LOGIN_STATE = "user_login_state";

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    /**
     * 获取当前登录用户（从 session 中取出）
     */
    private UserVO getLoginUser(HttpServletRequest request) {
        HttpSession session = request.getSession();
        UserVO loginUser = (UserVO) session.getAttribute(USER_LOGIN_STATE);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        return loginUser;
    }

    /**
     * 上传图片
     * 支持首次上传和重新上传（传入 pictureId 时只更新图片文件，基础信息不变）。
     */
    @ApiOperation("上传图片（支持重新上传）")
    
    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadPicture(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "id", required = false) Long pictureId,
            HttpServletRequest request) {
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        UserVO loginUser = getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(file, pictureId, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页查询图片列表
     * 管理员可查看所有状态，普通用户只能看到审核通过的图片。
     */
    @ApiOperation("分页查询图片列表")
    @AuthCheck
    @PostMapping("/list/page")
    public BaseResponse<Page<PictureVO>> listPictureByPage(@RequestBody PictureQueryRequest queryRequest,
                                                           HttpServletRequest request) {
        UserVO loginUser = getLoginUser(request);
        Page<PictureVO> page = pictureService.listPictureByPage(queryRequest, loginUser);
        return ResultUtils.success(page);
    }

    /**
     * 根据 id 获取单张图片详情
     */
    @ApiOperation("获取图片详情")
    @AuthCheck
    @GetMapping("/get/{id}")
    public BaseResponse<PictureVO> getPictureById(@PathVariable Long id, HttpServletRequest request) {
        UserVO loginUser = getLoginUser(request);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null || picture.getIsDelete() == 1,
                ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 普通用户只能查看已审核通过的图片
        boolean isAdmin = UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole());
        if (!isAdmin && picture.getReviewStatus() != 1) {
            ThrowUtils.throwIf(true, ErrorCode.NOT_FOUND_ERROR, "图片不存在或未审核通过");
        }

        // 填充上传者信息
        PictureVO vo = PictureVO.of(picture);
        UserVO uploader = userService.getUserVOById(picture.getUserId());
        vo.setUser(uploader);
        return ResultUtils.success(vo);
    }

    /**
     * 管理员审核图片（通过或拒绝，填写审核意见）
     */
    @ApiOperation("审核图片（管理员）")
    @AuthCheck(requiredRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/review")
    public BaseResponse<Boolean> reviewPicture(@RequestBody PictureReviewRequest reviewRequest,
                                               HttpServletRequest request) {
        UserVO loginUser = getLoginUser(request);
        pictureService.reviewPicture(reviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 编辑图片元数据（名称、简介、分类、标签）
     */
    @ApiOperation("编辑图片信息")
    @AuthCheck
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest editRequest,
                                             HttpServletRequest request) {
        UserVO loginUser = getLoginUser(request);
        pictureService.editPicture(editRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 删除图片（逻辑删除）
     */
    @ApiOperation("删除图片")
    @AuthCheck
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest,
                                               HttpServletRequest request) {
        UserVO loginUser = getLoginUser(request);
        pictureService.deletePicture(deleteRequest.getId(), loginUser);
        return ResultUtils.success(true);
    }
}
