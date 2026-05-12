package com.phy.starpicture.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.phy.starpicture.exception.ErrorCode;
import com.phy.starpicture.exception.ThrowUtils;
import com.phy.starpicture.mapper.UserMapper;
import com.phy.starpicture.model.entity.User;
import com.phy.starpicture.model.vo.UserVO;
import com.phy.starpicture.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String SALT = "star-picture";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        ThrowUtils.throwIf(userAccount == null || userAccount.length() < 4,
                ErrorCode.ACCOUNT_FORMAT_ERROR, "账号不能少于4位");
        ThrowUtils.throwIf(userPassword == null || userPassword.length() < 6,
                ErrorCode.PASSWORD_FORMAT_ERROR, "密码不能少于6位");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword),
                ErrorCode.PASSWORD_NOT_EQUAL);

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.count(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.USER_EXIST);

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
        ThrowUtils.throwIf(userAccount == null || userAccount.length() < 4,
                ErrorCode.ACCOUNT_FORMAT_ERROR, "账号不能少于4位");
        ThrowUtils.throwIf(userPassword == null || userPassword.length() < 6,
                ErrorCode.PASSWORD_FORMAT_ERROR, "密码不能少于6位");

        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        User user = this.getOne(queryWrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.USER_NOT_EXIST, "账号或密码错误");

        String encryptPassword = DigestUtil.md5Hex(SALT + userPassword);
        ThrowUtils.throwIf(!encryptPassword.equals(user.getUserPassword()),
                ErrorCode.PASSWORD_ERROR, "账号或密码错误");

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }
}
