package com.phy.starpicture.controller;

import com.phy.starpicture.common.BaseResponse;
import com.phy.starpicture.common.ResultUtils;
import com.phy.starpicture.exception.ErrorCode;
import com.phy.starpicture.exception.ThrowUtils;
import com.phy.starpicture.model.dto.user.UserRegisterRequest;
import com.phy.starpicture.model.entity.User;
import com.phy.starpicture.model.vo.UserVO;
import com.phy.starpicture.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
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
        String userAccount = request.getUserAccount();
        String userPassword = request.getUserPassword();
        String checkPassword = request.getCheckPassword();
        long userId = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(userId);
    }

    private UserVO getUserVO(User user) {
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }
}
