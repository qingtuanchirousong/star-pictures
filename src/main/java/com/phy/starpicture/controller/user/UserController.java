package com.phy.starpicture.controller.user;

import com.phy.starpicture.annotation.AuthCheck;
import com.phy.starpicture.common.BaseResponse;
import com.phy.starpicture.common.ResultUtils;
import com.phy.starpicture.constant.UserConstant;
import com.phy.starpicture.exception.ErrorCode;
import com.phy.starpicture.exception.ThrowUtils;
import com.phy.starpicture.model.dto.user.UserLoginRequest;
import com.phy.starpicture.model.dto.user.UserRegisterRequest;
import com.phy.starpicture.model.vo.UserVO;
import com.phy.starpicture.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 用户端 — 用户控制器
 * 提供注册、登录、注销、获取当前用户等面向普通用户的接口。
 */
@Api(tags = "用户管理")
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     */
    @ApiOperation("用户注册")
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long userId = userService.userRegister(
                request.getUserAccount(),
                request.getUserPassword(),
                request.getCheckPassword());
        return ResultUtils.success(userId);
    }

    /**
     * 用户登录
     * 登录成功后向 session 写入当前用户信息
     */
    @ApiOperation("用户登录")
    @PostMapping("/login")
    public BaseResponse<UserVO> userLogin(@RequestBody UserLoginRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        UserVO userVO = userService.userLogin(
                request.getUserAccount(),
                request.getUserPassword());
        httpRequest.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, userVO);
        return ResultUtils.success(userVO);
    }

    /**
     * 用户注销
     * 清除 session 中的登录状态
     */
    @ApiOperation("用户注销")
    @AuthCheck
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.removeAttribute(UserConstant.USER_LOGIN_STATE);
        }
        return ResultUtils.success(true);
    }

    /**
     * 获取当前登录用户信息
     */
    @ApiOperation("获取当前登录用户")
    @AuthCheck
    @GetMapping("/current")
    public BaseResponse<UserVO> getLoginUser(HttpServletRequest httpRequest) {
        UserVO loginUser = (UserVO) httpRequest.getSession()
                .getAttribute(UserConstant.USER_LOGIN_STATE);
        return ResultUtils.success(loginUser);
    }

    /**
     * 根据 id 获取用户信息（脱敏）
     * 不返回密码等敏感字段
     */
    @ApiOperation("根据 id 获取用户（脱敏）")
    @AuthCheck
    @GetMapping("/get/vo/{id}")
    public BaseResponse<UserVO> getUserVOById(@PathVariable long id) {
        UserVO userVO = userService.getUserVOById(id);
        return ResultUtils.success(userVO);
    }
}
