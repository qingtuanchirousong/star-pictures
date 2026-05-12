package com.phy.starpicture.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.phy.starpicture.model.entity.User;

public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);
}
