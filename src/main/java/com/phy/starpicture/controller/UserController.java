package com.phy.starpicture.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.phy.starpicture.annotation.AuthCheck;
import com.phy.starpicture.common.BaseResponse;
import com.phy.starpicture.common.DeleteRequest;
import com.phy.starpicture.common.ResultUtils;
import com.phy.starpicture.constant.UserConstant;
import com.phy.starpicture.exception.BusinessException;
import com.phy.starpicture.exception.ErrorCode;
import com.phy.starpicture.exception.ThrowUtils;
import com.phy.starpicture.model.dto.user.UserCreateRequest;
import com.phy.starpicture.model.dto.user.UserLoginRequest;
import com.phy.starpicture.model.dto.user.UserQueryRequest;
import com.phy.starpicture.model.dto.user.UserRegisterRequest;
import com.phy.starpicture.model.dto.user.UserUpdateRequest;
import com.phy.starpicture.model.entity.User;
import com.phy.starpicture.model.vo.UserVO;
import com.phy.starpicture.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Api(tags = "用户管理")
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

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

    @ApiOperation("获取当前登录用户")
    @AuthCheck
    @GetMapping("/current")
    public BaseResponse<UserVO> getLoginUser(HttpServletRequest httpRequest) {
        UserVO loginUser = (UserVO) httpRequest.getSession()
                .getAttribute(UserConstant.USER_LOGIN_STATE);
        return ResultUtils.success(loginUser);
    }

    @ApiOperation("【管理员】创建用户")
    @AuthCheck(requiredRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/create")
    public BaseResponse<Long> userCreate(@RequestBody UserCreateRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        long userId = userService.userCreate(
                request.getUserAccount(),
                request.getUserPassword(),
                request.getUserName(),
                request.getUserRole());
        return ResultUtils.success(userId);
    }

    @ApiOperation("【管理员】根据 id 删除用户")
    @AuthCheck(requiredRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/delete")
    public BaseResponse<Boolean> userDelete(@RequestBody DeleteRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userDelete(request.getId());
        return ResultUtils.success(result);
    }

    @ApiOperation("【管理员】更新用户")
    @AuthCheck(requiredRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/update")
    public BaseResponse<Boolean> userUpdate(@RequestBody UserUpdateRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(request, user);
        boolean result = userService.userUpdate(user);
        return ResultUtils.success(result);
    }

    @ApiOperation("【管理员】分页获取用户列表（脱敏）")
    @AuthCheck(requiredRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/list/page")
    public BaseResponse<Page<UserVO>> userListByPage(@RequestBody UserQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Page<UserVO> voPage = userService.userListByPage(request);
        return ResultUtils.success(voPage);
    }

    @ApiOperation("【管理员】根据 id 获取用户（未脱敏）")
    @AuthCheck(requiredRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/get/{id}")
    public BaseResponse<User> getUserById(@PathVariable long id) {
        User user = userService.getUserById(id);
        return ResultUtils.success(user);
    }

    @ApiOperation("根据 id 获取用户（脱敏）")
    @AuthCheck
    @GetMapping("/get/vo/{id}")
    public BaseResponse<UserVO> getUserVOById(@PathVariable long id) {
        UserVO userVO = userService.getUserVOById(id);
        return ResultUtils.success(userVO);
    }
}
