package com.phy.starpicture.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.phy.starpicture.annotation.AuthCheck;
import com.phy.starpicture.common.BaseResponse;
import com.phy.starpicture.common.DeleteRequest;
import com.phy.starpicture.common.ResultUtils;
import com.phy.starpicture.constant.UserConstant;
import com.phy.starpicture.exception.ErrorCode;
import com.phy.starpicture.exception.ThrowUtils;
import com.phy.starpicture.model.dto.user.UserCreateRequest;
import com.phy.starpicture.model.dto.user.UserQueryRequest;
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

/**
 * 管理员端 — 用户管理控制器
 * 提供用户的创建、删除、更新、分页查询等管理接口，仅管理员可访问。
 */
@Api(tags = "管理员-用户管理")
@RestController
@RequestMapping("/admin/user")
public class AdminUserController {

    @Resource
    private UserService userService;

    /**
     * 创建用户（管理员操作）
     */
    @ApiOperation("创建用户")
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

    /**
     * 删除用户（管理员操作）
     */
    @ApiOperation("删除用户")
    @AuthCheck(requiredRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/delete")
    public BaseResponse<Boolean> userDelete(@RequestBody DeleteRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userDelete(request.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新用户信息（管理员操作）
     */
    @ApiOperation("更新用户")
    @AuthCheck(requiredRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/update")
    public BaseResponse<Boolean> userUpdate(@RequestBody UserUpdateRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(request, user);
        boolean result = userService.userUpdate(user);
        return ResultUtils.success(result);
    }

    /**
     * 分页查询用户列表（管理员操作，返回脱敏数据）
     */
    @ApiOperation("分页获取用户列表")
    @AuthCheck(requiredRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/list/page")
    public BaseResponse<Page<UserVO>> userListByPage(@RequestBody UserQueryRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Page<UserVO> voPage = userService.userListByPage(request);
        return ResultUtils.success(voPage);
    }

    /**
     * 根据 id 获取用户原始信息（管理员操作，含敏感字段）
     */
    @ApiOperation("根据 id 获取用户（未脱敏）")
    @AuthCheck(requiredRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/get/{id}")
    public BaseResponse<User> getUserById(@PathVariable long id) {
        User user = userService.getUserById(id);
        return ResultUtils.success(user);
    }
}
