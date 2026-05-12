package com.phy.starpicture.controller;

import com.phy.starpicture.common.BaseResponse;
import com.phy.starpicture.common.ResultUtils;
import com.phy.starpicture.exception.ErrorCode;
import com.phy.starpicture.exception.ThrowUtils;
import com.phy.starpicture.model.dto.user.UserLoginRequest;
import com.phy.starpicture.model.dto.user.UserRegisterRequest;
import com.phy.starpicture.model.vo.UserVO;
import com.phy.starpicture.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

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
    public BaseResponse<UserVO> userLogin(@RequestBody UserLoginRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        UserVO userVO = userService.userLogin(
                request.getUserAccount(),
                request.getUserPassword());
        return ResultUtils.success(userVO);
    }
}
