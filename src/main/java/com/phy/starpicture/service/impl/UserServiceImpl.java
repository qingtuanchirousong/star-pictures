package com.phy.starpicture.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.phy.starpicture.exception.ErrorCode;
import com.phy.starpicture.exception.ThrowUtils;
import com.phy.starpicture.mapper.UserMapper;
import com.phy.starpicture.model.dto.user.UserQueryRequest;
import com.phy.starpicture.model.entity.User;
import com.phy.starpicture.model.vo.UserVO;
import com.phy.starpicture.service.UserService;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String SALT = "star-picture";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        validateAccountAndPassword(userAccount, userPassword);
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PASSWORD_NOT_EQUAL);

        checkAccountUnique(userAccount);

        String encryptPassword = DigestUtil.md5Hex(SALT + userPassword);
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(userAccount);
        user.setUserRole("user");
        boolean saved = this.save(user);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "注册失败，请稍后再试");
        return user.getId();
    }

    @Override
    public UserVO userLogin(String userAccount, String userPassword) {
        validateAccountAndPassword(userAccount, userPassword);

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        User user = this.getOne(queryWrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_EXIST, "账号或密码错误");

        String encryptPassword = DigestUtil.md5Hex(SALT + userPassword);
        ThrowUtils.throwIf(!encryptPassword.equals(user.getUserPassword()),
                ErrorCode.PASSWORD_ERROR, "账号或密码错误");

        return toUserVO(user);
    }

    @Override
    public long userCreate(String userAccount, String userPassword, String userName, String userRole) {
        validateAccountAndPassword(userAccount, userPassword);
        checkAccountUnique(userAccount);

        String encryptPassword = DigestUtil.md5Hex(SALT + userPassword);
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(StrUtil.isBlank(userName) ? userAccount : userName);
        user.setUserRole(StrUtil.isBlank(userRole) ? "user" : userRole);
        boolean saved = this.save(user);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "创建失败，请稍后再试");
        return user.getId();
    }

    @Override
    public boolean userDelete(long id) {
        boolean removed = this.removeById(id);
        ThrowUtils.throwIf(!removed, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return true;
    }

    @Override
    public boolean userUpdate(User user) {
        User dbUser = this.getById(user.getId());
        ThrowUtils.throwIf(dbUser == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        boolean updated = this.updateById(user);
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "更新失败，请稍后再试");
        return true;
    }

    @Override
    public Page<UserVO> userListByPage(UserQueryRequest queryRequest) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        if (StrUtil.isNotBlank(queryRequest.getUserAccount())) {
            queryWrapper.like("userAccount", queryRequest.getUserAccount());
        }
        if (StrUtil.isNotBlank(queryRequest.getUserName())) {
            queryWrapper.like("userName", queryRequest.getUserName());
        }
        if (StrUtil.isNotBlank(queryRequest.getUserRole())) {
            queryWrapper.eq("userRole", queryRequest.getUserRole());
        }

        Page<User> page = this.page(
                new Page<>(queryRequest.getCurrent(), queryRequest.getPageSize()),
                queryWrapper);

        List<UserVO> voList = page.getRecords().stream()
                .map(this::toUserVO)
                .collect(Collectors.toList());

        Page<UserVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public User getUserById(long id) {
        User user = this.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return user;
    }

    @Override
    public UserVO getUserVOById(long id) {
        User user = this.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        return toUserVO(user);
    }

    private void validateAccountAndPassword(String userAccount, String userPassword) {
        ThrowUtils.throwIf(userAccount == null || userAccount.length() < 4,
                ErrorCode.ACCOUNT_FORMAT_ERROR, "账号不能少于4位");
        ThrowUtils.throwIf(userPassword == null || userPassword.length() < 6,
                ErrorCode.PASSWORD_FORMAT_ERROR, "密码不能少于6位");
    }

    private void checkAccountUnique(String userAccount) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.count(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.USER_EXIST);
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
