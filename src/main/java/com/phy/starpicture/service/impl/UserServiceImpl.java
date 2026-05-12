package com.phy.starpicture.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.phy.starpicture.exception.ErrorCode;
import com.phy.starpicture.exception.ThrowUtils;
import com.phy.starpicture.mapper.UserMapper;
import com.phy.starpicture.model.entity.User;
import com.phy.starpicture.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private static final String SALT = "star-picture";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 校验参数
        ThrowUtils.throwIf(userAccount == null || userAccount.length() < 4,
                ErrorCode.ACCOUNT_FORMAT_ERROR, "账号不能少于4位");
        ThrowUtils.throwIf(userPassword == null || userPassword.length() < 6,
                ErrorCode.PASSWORD_FORMAT_ERROR, "密码不能少于6位");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword),
                ErrorCode.PASSWORD_NOT_EQUAL);

        // 检查账号是否已存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.count(queryWrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.USER_EXIST);

        // 密码加密
        String encryptPassword = DigestUtil.md5Hex(SALT + userPassword);

        // 保存用户
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName(userAccount);
        user.setUserRole("user");
        boolean saved = this.save(user);
        ThrowUtils.throwIf(!saved, ErrorCode.SYSTEM_ERROR, "注册失败");

        return user.getId();
    }
}
