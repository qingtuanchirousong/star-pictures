package com.phy.starpicture.controller;

import com.phy.starpicture.annotation.AuthCheck;
import com.phy.starpicture.common.BaseResponse;
import com.phy.starpicture.common.ResultUtils;
import com.phy.starpicture.constant.UserConstant;
import com.phy.starpicture.exception.ErrorCode;
import com.phy.starpicture.exception.ThrowUtils;
import com.phy.starpicture.model.vo.PictureVO;
import com.phy.starpicture.model.vo.UserVO;
import com.phy.starpicture.service.PictureService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 图片管理控制器
 * 提供图片上传（含重新上传）等接口。
 */
@Api(tags = "图片管理")
@RestController
@RequestMapping("/picture")
public class PictureController {

    /** 登录用户状态 key（与 UserConstant 保持一致） */
    private static final String USER_LOGIN_STATE = "user_login_state";

    @Resource
    private PictureService pictureService;

    /**
     * 上传图片
     * 支持首次上传和重新上传（传入 pictureId 时只更新图片文件，基础信息不变）。
     *
     * @param file      上传的图片文件
     * @param pictureId 已有图片 id（重新上传时传入，首次上传不传）
     * @param request   HTTP 请求（用于获取 session 中的登录用户）
     * @return 图片视图对象（含上传用户信息）
     */
    @ApiOperation("上传图片（支持重新上传）")
    @AuthCheck
    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadPicture(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "id", required = false) Long pictureId,
            HttpServletRequest request) {
        ThrowUtils.throwIf(file == null, ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 从 session 获取当前登录用户
        HttpSession session = request.getSession();
        UserVO loginUser = (UserVO) session.getAttribute(USER_LOGIN_STATE);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "请先登录");

        PictureVO pictureVO = pictureService.uploadPicture(file, pictureId, loginUser);
        return ResultUtils.success(pictureVO);
    }
}
