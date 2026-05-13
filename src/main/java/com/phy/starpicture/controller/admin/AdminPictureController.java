package com.phy.starpicture.controller.admin;

import com.phy.starpicture.annotation.AuthCheck;
import com.phy.starpicture.common.BaseResponse;
import com.phy.starpicture.common.ResultUtils;
import com.phy.starpicture.constant.UserConstant;
import com.phy.starpicture.exception.ErrorCode;
import com.phy.starpicture.exception.ThrowUtils;
import com.phy.starpicture.model.dto.picture.PictureBatchFetchRequest;
import com.phy.starpicture.model.dto.picture.PictureReviewRequest;
import com.phy.starpicture.model.vo.PictureVO;
import com.phy.starpicture.model.vo.UserVO;
import com.phy.starpicture.service.PictureService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 管理员端 — 图片审核控制器
 * 提供图片审核（通过/拒绝）等管理接口，仅管理员可访问。
 */
@Api(tags = "管理员-图片审核")
@RestController
@RequestMapping("/admin/picture")
public class AdminPictureController {

    /** 登录用户状态 key */
    private static final String USER_LOGIN_STATE = "user_login_state";

    @Resource
    private PictureService pictureService;

    /**
     * 从 session 获取当前登录用户
     */
    private UserVO getLoginUser(HttpServletRequest request) {
        HttpSession session = request.getSession();
        UserVO loginUser = (UserVO) session.getAttribute(USER_LOGIN_STATE);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        return loginUser;
    }

    /**
     * 审核图片（管理员操作）
     * 将图片状态改为通过或拒绝，同时记录审核人、审核时间和审核意见。
     */
    @ApiOperation("审核图片（通过/拒绝）")
    @AuthCheck(requiredRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/review")
    public BaseResponse<Boolean> reviewPicture(@RequestBody PictureReviewRequest reviewRequest,
                                               HttpServletRequest request) {
        UserVO loginUser = getLoginUser(request);
        pictureService.reviewPicture(reviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量从网络抓取图片并入库
     * 管理员填写搜索关键词和抓取数量，系统从 Bing 抓取图片上传 COS 并创建记录。
     */
    @ApiOperation("批量抓取图片（Bing）")
    @AuthCheck(requiredRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/fetch/batch")
    public BaseResponse<List<PictureVO>> batchFetchPictures(@RequestBody PictureBatchFetchRequest fetchRequest,
                                                             HttpServletRequest request) {
        UserVO loginUser = getLoginUser(request);
        List<PictureVO> result = pictureService.batchFetchPictures(fetchRequest, loginUser);
        return ResultUtils.success(result);
    }
}
